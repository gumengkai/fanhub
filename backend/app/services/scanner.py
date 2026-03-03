import os
from datetime import datetime
from ..models import db, Video, Image, Source, WatchHistory

# Supported file extensions
VIDEO_EXTENSIONS = {'.mp4', '.mkv', '.avi', '.mov', '.flv', '.wmv', '.m4v', '.webm'}
IMAGE_EXTENSIONS = {'.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.tiff', '.tif'}


def scan_source(source):
    """Scan a source directory for media files."""
    stats = {
        'videos_added': 0,
        'videos_updated': 0,
        'videos_removed': 0,
        'images_added': 0,
        'images_updated': 0,
        'images_removed': 0,
        'errors': []
    }

    if source.type == 'local':
        # Scan based on media_type
        if source.media_type in ('all', 'video'):
            scan_local_directory(source, stats, 'video')
        if source.media_type in ('all', 'image'):
            scan_local_directory(source, stats, 'image')
    elif source.type == 'nas':
        scan_nas_directory(source, stats)

    return stats


def scan_local_directory(source, stats, media_type='all'):
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
                process_video_file(file_path, source, existing_videos if media_type != 'video' else {}, stats)
            elif media_type in ('all', 'image') and ext in IMAGE_EXTENSIONS:
                process_image_file(file_path, source, existing_images if media_type != 'image' else {}, stats)

    # Remove database records for files that no longer exist on disk
    if media_type in ('all', 'video'):
        for path, video in existing_videos.items():
            if path not in scanned_paths:
                try:
                    # Delete associated watch history first
                    WatchHistory.query.filter_by(video_id=video.id).delete()
                    # Delete the video record
                    db.session.delete(video)
                    stats['videos_removed'] = stats.get('videos_removed', 0) + 1
                except Exception as e:
                    stats['errors'].append(f"Error removing video record for {path}: {str(e)}")

    if media_type in ('all', 'image'):
        for path, image in existing_images.items():
            if path not in scanned_paths:
                try:
                    db.session.delete(image)
                    stats['images_removed'] = stats.get('images_removed', 0) + 1
                except Exception as e:
                    stats['errors'].append(f"Error removing image record for {path}: {str(e)}")

    db.session.commit()


def scan_nas_directory(source, stats):
    """Scan a NAS directory for media files."""
    # TODO: Implement NAS scanning using SMB/CIFS
    # For now, we'll just log that this needs to be implemented
    stats['errors'].append("NAS scanning not yet implemented")


def process_video_file(file_path, source, existing_videos, stats):
    """Process a single video file."""
    try:
        file_stat = os.stat(file_path)
        file_size = file_stat.st_size
        modified_time = datetime.fromtimestamp(file_stat.st_mtime)

        # Check if video already exists
        if file_path in existing_videos:
            video = existing_videos[file_path]
            # Update if file has been modified
            if video.updated_at and modified_time > video.updated_at:
                video.file_size = file_size
                video.updated_at = datetime.utcnow()
                stats['videos_updated'] += 1
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
                is_favorite=False
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

        db.session.commit()

    except Exception as e:
        stats['errors'].append(f"Error processing video {file_path}: {str(e)}")


def process_image_file(file_path, source, existing_images, stats):
    """Process a single image file."""
    try:
        file_stat = os.stat(file_path)
        file_size = file_stat.st_size
        modified_time = datetime.fromtimestamp(file_stat.st_mtime)

        # Check if image already exists
        if file_path in existing_images:
            image = existing_images[file_path]
            # Update if file has been modified
            if image.created_at and modified_time > image.created_at:
                image.file_size = file_size
                stats['images_updated'] += 1
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
                is_favorite=False
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

        db.session.commit()

    except Exception as e:
        stats['errors'].append(f"Error processing image {file_path}: {str(e)}")


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
