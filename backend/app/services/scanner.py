import os
from datetime import datetime
from ..models import db, Video, Image, Source, WatchHistory, ThumbnailCache, ScanLog

# Supported file extensions (browser-compatible formats only)
VIDEO_EXTENSIONS = {'.mp4', '.mov', '.m4v', '.webm', '.ogv', '.ogg', '.avi', '.mkv', '.flv', '.wmv', '.mpg', '.mpeg', '.3gp', '.ts', '.m2ts'}
IMAGE_EXTENSIONS = {'.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.tiff', '.tif', '.svg', '.ico', '.heic', '.heif', '.raw', '.cr2', '.nef', '.arw'}


def scan_source(source, scan_log_id=None):
    """Scan a source directory for media files.
    
    Args:
        source: Source object to scan
        scan_log_id: Optional scan log ID for tracking progress
    
    Returns:
        dict: Statistics about the scan operation
    """
    stats = {
        'videos_added': 0,
        'videos_updated': 0,
        'videos_removed': 0,
        'images_added': 0,
        'images_updated': 0,
        'images_removed': 0,
        'errors': [],
        'details': []  # 详细操作记录
    }

    # 创建扫描日志
    scan_log = None
    if scan_log_id is None:
        scan_log = ScanLog(
            source_id=source.id,
            source_name=source.name,
            status='running'
        )
        db.session.add(scan_log)
        db.session.commit()
        scan_log_id = scan_log.id
    else:
        scan_log = ScanLog.query.get(scan_log_id)

    def update_scan_log():
        """Update scan log with current stats"""
        if scan_log:
            scan_log.videos_added = stats['videos_added']
            scan_log.videos_updated = stats['videos_updated']
            scan_log.videos_removed = stats['videos_removed']
            scan_log.images_added = stats['images_added']
            scan_log.images_updated = stats['images_updated']
            scan_log.images_removed = stats['images_removed']
            scan_log.errors = stats['errors']
            scan_log.details = stats['details']
            db.session.commit()

    try:
        # 记录是否需要刷新词云（只有扫描视频库或图片库时才刷新）
        should_refresh_wordcloud = source.media_type in ('all', 'video', 'image')

        if source.type == 'local':
            # Scan based on media_type
            # 'douyin' and 'peak' are treated as video-only
            if source.media_type in ('all', 'video', 'douyin', 'peak'):
                scan_local_directory(source, stats, 'video', scan_log_id)
                update_scan_log()
            if source.media_type in ('all', 'image'):
                scan_local_directory(source, stats, 'image', scan_log_id)
                update_scan_log()
        elif source.type == 'nas':
            scan_nas_directory(source, stats)

        # 扫描完成后刷新词云缓存（只在扫描视频库/图片库时）
        if should_refresh_wordcloud:
            try:
                from ..models import WordCloudCache
                new_version = WordCloudCache.increment_global_version()
                print(f"[WordCloud] Cache refreshed after scanning source {source.name} (version: {new_version})")
            except Exception as e:
                print(f"[WordCloud] Failed to refresh cache: {e}")

        # 标记扫描完成
        if scan_log:
            scan_log.status = 'completed'
            scan_log.completed_at = datetime.utcnow()
            db.session.commit()

    except Exception as e:
        # 标记扫描失败
        if scan_log:
            scan_log.status = 'failed'
            scan_log.completed_at = datetime.utcnow()
            scan_log.errors.append(str(e))
            db.session.commit()
        stats['errors'].append(f"Scan failed: {str(e)}")

    return stats


def scan_local_directory(source, stats, media_type='all', scan_log_id=None):
    """Scan a local directory for media files."""
    if not os.path.exists(source.path):
        stats['errors'].append(f"Path does not exist: {source.path}")
        return

    # Get all existing files from database for this source
    if media_type in ('all', 'video'):
        existing_videos = {v.path: v for v in Video.query.filter_by(source_id=source.id).all()}
    if media_type in ('all', 'image'):
        existing_images = {i.path: i for i in Image.query.filter_by(source_id=source.id).all()}

    scanned_paths = set()

    for root, dirs, files in os.walk(source.path):
        # Skip hidden directories
        dirs[:] = [d for d in dirs if not d.startswith('.')]

        for filename in files:
            if filename.startswith('.'):
                continue

            file_path = os.path.join(root, filename)
            scanned_paths.add(file_path)

            ext = os.path.splitext(filename)[1].lower()

            if media_type in ('all', 'video') and ext in VIDEO_EXTENSIONS:
                video = process_video_file(file_path, source, existing_videos, stats, scan_log_id)
                if video:
                    existing_videos[file_path] = video
            elif media_type in ('all', 'image') and ext in IMAGE_EXTENSIONS:
                image = process_image_file(file_path, source, existing_images, stats, scan_log_id)
                if image:
                    existing_images[file_path] = image

    # Remove database records for files that no longer exist on disk
    if media_type in ('all', 'video'):
        removed_videos = []
        for path, video in existing_videos.items():
            if path not in scanned_paths:
                removed_videos.append(video)

        for video in removed_videos:
            try:
                # 记录删除操作
                stats['details'].append({
                    'type': 'removed',
                    'media_type': 'video',
                    'path': video.path,
                    'title': video.title,
                    'was_favorite': video.is_favorite,
                    'was_liked': video.is_liked
                })

                # Delete thumbnail file from disk
                if video.thumbnail_path and os.path.exists(video.thumbnail_path):
                    try:
                        os.remove(video.thumbnail_path)
                    except OSError as e:
                        stats['errors'].append(f"Error deleting thumbnail file {video.thumbnail_path}: {str(e)}")

                # Delete ThumbnailCache record
                ThumbnailCache.query.filter_by(media_type='video', media_id=video.id).delete()

                # Delete associated watch history (cascade should handle this, but be explicit)
                WatchHistory.query.filter_by(video_id=video.id).delete()

                # Delete the video record (tags association will be auto-cleaned)
                db.session.delete(video)
                stats['videos_removed'] = stats.get('videos_removed', 0) + 1
            except Exception as e:
                stats['errors'].append(f"Error removing video record for {video.path}: {str(e)}")

    if media_type in ('all', 'image'):
        removed_images = []
        for path, image in existing_images.items():
            if path not in scanned_paths:
                removed_images.append(image)

        for image in removed_images:
            try:
                # 记录删除操作
                stats['details'].append({
                    'type': 'removed',
                    'media_type': 'image',
                    'path': image.path,
                    'title': image.title,
                    'was_favorite': image.is_favorite,
                    'was_liked': image.is_liked
                })

                # Delete thumbnail file from disk
                if image.thumbnail_path and os.path.exists(image.thumbnail_path):
                    try:
                        os.remove(image.thumbnail_path)
                    except OSError as e:
                        stats['errors'].append(f"Error deleting thumbnail file {image.thumbnail_path}: {str(e)}")

                # Delete ThumbnailCache record
                ThumbnailCache.query.filter_by(media_type='image', media_id=image.id).delete()

                # Delete the image record
                db.session.delete(image)
                stats['images_removed'] = stats.get('images_removed', 0) + 1
            except Exception as e:
                stats['errors'].append(f"Error removing image record for {image.path}: {str(e)}")

    db.session.commit()


def scan_nas_directory(source, stats):
    """Scan a NAS directory for media files."""
    # TODO: Implement NAS scanning using SMB/CIFS
    # For now, we'll just log that this needs to be implemented
    stats['errors'].append("NAS scanning not yet implemented")


def process_video_file(file_path, source, existing_videos, stats, scan_log_id=None):
    """Process a single video file.
    
    Returns:
        Video: The processed video object, or None if error
    """
    video = None
    try:
        file_stat = os.stat(file_path)
        file_size = file_stat.st_size
        modified_time = datetime.fromtimestamp(file_stat.st_mtime)

        # Check if video already exists
        if file_path in existing_videos:
            video = existing_videos[file_path]
            # Update if file has been modified (使用 file_modified_at 而不是 updated_at)
            if video.file_modified_at is None or modified_time > video.file_modified_at:
                video.file_size = file_size
                video.file_modified_at = modified_time
                # 保留原有的收藏和喜欢状态
                stats['videos_updated'] += 1
                stats['details'].append({
                    'type': 'updated',
                    'media_type': 'video',
                    'path': file_path,
                    'title': video.title,
                    'kept_favorite': video.is_favorite,
                    'kept_liked': video.is_liked
                })
                db.session.commit()
        else:
            # Extract video info
            video_info = get_video_info(file_path)

            # Create new video record first (to get ID)
            video = Video(
                title=os.path.splitext(os.path.basename(file_path))[0],
                path=file_path,
                source_id=source.id,
                file_size=file_size,
                duration=video_info.get('duration'),
                width=video_info.get('width'),
                height=video_info.get('height'),
                thumbnail_path=None,
                is_favorite=False,
                is_liked=False,
                file_modified_at=modified_time  # 记录文件修改时间
            )
            db.session.add(video)
            db.session.commit()  # Commit to get the ID

            # Generate thumbnail with the video ID
            thumbnail_path = None
            try:
                from ..services.thumbnail import generate_video_thumbnail
                thumbnail_path = generate_video_thumbnail(file_path, video.id)
                video.thumbnail_path = thumbnail_path
                db.session.commit()
            except Exception as e:
                print(f"Failed to generate thumbnail for {file_path}: {e}")

            stats['videos_added'] += 1
            stats['details'].append({
                'type': 'added',
                'media_type': 'video',
                'path': file_path,
                'title': video.title,
                'id': video.id
            })

        db.session.commit()

    except Exception as e:
        error_msg = f"Error processing video {file_path}: {str(e)}"
        stats['errors'].append(error_msg)
        stats['details'].append({
            'type': 'error',
            'media_type': 'video',
            'path': file_path,
            'error': str(e)
        })
    
    return video


def process_image_file(file_path, source, existing_images, stats, scan_log_id=None):
    """Process a single image file.
    
    Returns:
        Image: The processed image object, or None if error
    """
    image = None
    try:
        file_stat = os.stat(file_path)
        file_size = file_stat.st_size
        modified_time = datetime.fromtimestamp(file_stat.st_mtime)

        # Check if image already exists
        if file_path in existing_images:
            image = existing_images[file_path]
            # Update if file has been modified (使用 file_modified_at 而不是 created_at)
            if image.file_modified_at is None or modified_time > image.file_modified_at:
                image.file_size = file_size
                image.file_modified_at = modified_time
                # 保留原有的收藏和喜欢状态
                stats['images_updated'] += 1
                stats['details'].append({
                    'type': 'updated',
                    'media_type': 'image',
                    'path': file_path,
                    'title': image.title,
                    'kept_favorite': image.is_favorite,
                    'kept_liked': image.is_liked
                })
                db.session.commit()
        else:
            # Get image dimensions
            image_info = get_image_info(file_path)

            # Create new image record first (to get ID)
            image = Image(
                title=os.path.splitext(os.path.basename(file_path))[0],
                path=file_path,
                source_id=source.id,
                file_size=file_size,
                width=image_info.get('width'),
                height=image_info.get('height'),
                thumbnail_path=None,
                is_favorite=False,
                is_liked=False,
                file_modified_at=modified_time  # 记录文件修改时间
            )
            db.session.add(image)
            db.session.commit()  # Commit to get the ID

            # Generate thumbnail with the image ID
            thumbnail_path = None
            try:
                from ..services.thumbnail import generate_image_thumbnail
                thumbnail_path = generate_image_thumbnail(file_path, image.id)
                image.thumbnail_path = thumbnail_path
                db.session.commit()
            except Exception as e:
                print(f"Failed to generate thumbnail for {file_path}: {e}")

            stats['images_added'] += 1
            stats['details'].append({
                'type': 'added',
                'media_type': 'image',
                'path': file_path,
                'title': image.title,
                'id': image.id
            })

        db.session.commit()

    except Exception as e:
        error_msg = f"Error processing image {file_path}: {str(e)}"
        stats['errors'].append(error_msg)
        stats['details'].append({
            'type': 'error',
            'media_type': 'image',
            'path': file_path,
            'error': str(e)
        })
    
    return image


def get_video_info(file_path):
    """Get video metadata using ffprobe."""
    info = {'duration': None, 'width': None, 'height': None}

    try:
        import subprocess
        import json
        import shutil

        # Check if ffprobe is available
        if not shutil.which('ffprobe'):
            print(f"⚠️  ffprobe not found. Please install ffmpeg to get video metadata.")
            return info

        cmd = [
            'ffprobe',
            '-v', 'quiet',
            '-print_format', 'json',
            '-show_format',
            '-show_streams',
            file_path
        ]

        result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
        data = json.loads(result.stdout)

        # Get duration from format
        if 'format' in data and 'duration' in data['format']:
            try:
                info['duration'] = int(float(data['format']['duration']))
            except (ValueError, TypeError):
                pass

        # Get dimensions from first video stream
        for stream in data.get('streams', []):
            if stream.get('codec_type') == 'video':
                info['width'] = stream.get('width')
                info['height'] = stream.get('height')
                break

    except Exception as e:
        print(f"Error getting video info for {file_path}: {e}")

    return info


def get_image_info(file_path):
    """Get image metadata using PIL."""
    info = {'width': None, 'height': None}

    try:
        from PIL import Image as PILImage

        with PILImage.open(file_path) as img:
            info['width'] = img.width
            info['height'] = img.height

    except Exception as e:
        print(f"Error getting image info for {file_path}: {e}")

    return info
