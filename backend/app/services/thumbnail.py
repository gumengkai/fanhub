import os
import subprocess
import hashlib
from datetime import datetime, timedelta
from PIL import Image as PILImage
from flask import current_app
from concurrent.futures import ThreadPoolExecutor, as_completed
from concurrent.futures import ThreadPoolExecutor, as_completed

# Thumbnail settings
THUMBNAIL_WIDTH = 320
THUMBNAIL_HEIGHT = 180
THUMBNAIL_QUALITY = 85
THUMBNAIL_CACHE_DAYS = 30


def get_file_hash(file_path):
    """Calculate hash from first 1MB + file size + modification time for fast cache invalidation."""
    try:
        hash_md5 = hashlib.md5()
        # Include file size and modification time for quick changes detection
        stat = os.stat(file_path)
        hash_md5.update(str(stat.st_size).encode())
        hash_md5.update(str(stat.st_mtime).encode())

        # Only read first 1MB for performance
        with open(file_path, "rb") as f:
            chunk = f.read(1024 * 1024)  # Read only first 1MB
            hash_md5.update(chunk)
        return hash_md5.hexdigest()
    except Exception:
        return None


def get_thumbnail_path(media_id, media_type):
    """Get the path for a thumbnail file."""
    thumbs_dir = current_app.config['THUMBNAIL_FOLDER']
    
    # Create subdirectory based on media ID to avoid too many files in one dir
    subdir = str((media_id // 1000) * 1000)
    thumb_dir = os.path.join(thumbs_dir, subdir)
    
    os.makedirs(thumb_dir, exist_ok=True)
    
    return os.path.join(thumb_dir, f"{media_type}_{media_id}.jpg")


def generate_video_thumbnail(video_path, video_id=None, position=10):
    """
    Generate a thumbnail for a video using ffmpeg.

    Args:
        video_path: Path to video file
        video_id: Optional video ID for caching
        position: Position in seconds to extract frame (default 10)

    Returns:
        Path to generated thumbnail
    """
    import tempfile
    import subprocess

    if video_id is None:
        thumb_path = tempfile.mktemp(suffix='.jpg')
    else:
        thumb_path = get_thumbnail_path(video_id, 'video')

    # Skip if thumbnail already exists and is recent
    if os.path.exists(thumb_path):
        stat = os.stat(thumb_path)
        # Check if thumbnail is less than 7 days old
        if datetime.fromtimestamp(stat.st_mtime) > datetime.now() - timedelta(days=7):
            return thumb_path

    try:
        # Verify video file exists
        if not os.path.exists(video_path):
            raise Exception(f"Video file not found: {video_path}")

        # First, get video duration to calculate safe position
        duration = get_video_duration(video_path)
        if duration:
            if position > duration * 0.9:
                position = int(duration * 0.3)  # Use 30% position if requested position is too late
            if position < 1:
                position = 1  # At least 1 second
        else:
            position = 1  # Default to 1 second if duration unknown

        # Use ffmpeg with optimized settings for faster generation
        # -ss after -i for better accuracy with keyframes
        cmd = [
            'ffmpeg',
            '-i', video_path,
            '-ss', str(position),
            '-vframes', '1',
            '-q:v', '3',
            '-vf', f'scale={THUMBNAIL_WIDTH}:{THUMBNAIL_HEIGHT}:force_original_aspect_ratio=decrease,pad={THUMBNAIL_WIDTH}:{THUMBNAIL_HEIGHT}:(ow-iw)/2:(oh-ih)/2:black',
            '-y',
            thumb_path
        ]

        result = subprocess.run(
            cmd,
            capture_output=True,
            timeout=60
        )

        if result.returncode == 0 and os.path.exists(thumb_path) and os.path.getsize(thumb_path) > 0:
            # Update cache metadata if video_id provided
            if video_id:
                update_thumbnail_cache('video', video_id, thumb_path, video_path)
            return thumb_path
        else:
            error_msg = result.stderr.decode()[:500] if result.stderr else "Unknown error"
            raise Exception(f"ffmpeg failed (code {result.returncode}): {error_msg}")

    except subprocess.TimeoutExpired:
        raise Exception("Thumbnail generation timed out after 60s")
    except Exception as e:
        # Clean up partial file
        if os.path.exists(thumb_path):
            try:
                os.remove(thumb_path)
            except:
                pass
        raise Exception(f"Failed to generate thumbnail: {str(e)[:200]}")


def get_video_duration(video_path):
    """Get video duration in seconds using ffprobe."""
    try:
        cmd = [
            'ffprobe',
            '-v', 'quiet',
            '-show_entries', 'format=duration',
            '-of', 'default=noprint_wrappers=1:nokey=1',
            video_path
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
        if result.stdout.strip():
            return float(result.stdout.strip())
    except Exception:
        pass
    return None


def update_thumbnail_cache(media_type, media_id, thumb_path, source_path=None):
    """Update thumbnail cache metadata."""
    from ..models import db, ThumbnailCache
    
    try:
        cache = ThumbnailCache.query.filter_by(
            media_type=media_type,
            media_id=media_id
        ).first()
        
        file_hash = get_file_hash(source_path) if source_path else None
        file_size = os.path.getsize(thumb_path) if os.path.exists(thumb_path) else None
        expires_at = datetime.now() + timedelta(days=THUMBNAIL_CACHE_DAYS)
        
        if cache:
            cache.file_path = thumb_path
            cache.file_hash = file_hash
            cache.file_size = file_size
            cache.expires_at = expires_at
        else:
            cache = ThumbnailCache(
                media_type=media_type,
                media_id=media_id,
                file_path=thumb_path,
                file_hash=file_hash,
                file_size=file_size,
                expires_at=expires_at
            )
            db.session.add(cache)
        
        db.session.commit()
    except Exception as e:
        print(f"Failed to update thumbnail cache: {e}")
        db.session.rollback()


def generate_image_thumbnail(image_path, image_id=None):
    """Generate a thumbnail for an image using Pillow."""
    if image_id is None:
        import tempfile
        thumb_path = tempfile.mktemp(suffix='.jpg')
    else:
        thumb_path = get_thumbnail_path(image_id, 'image')

    if os.path.exists(thumb_path):
        return thumb_path

    try:
        with PILImage.open(image_path) as img:
            if img.mode in ('RGBA', 'LA', 'P'):
                background = PILImage.new('RGB', img.size, (0, 0, 0))
                if img.mode == 'P':
                    img = img.convert('RGBA')
                background.paste(img, mask=img.split()[-1] if img.mode in ('RGBA', 'LA') else None)
                img = background

            aspect_ratio = img.width / img.height
            target_ratio = THUMBNAIL_WIDTH / THUMBNAIL_HEIGHT

            if aspect_ratio > target_ratio:
                new_width = THUMBNAIL_WIDTH
                new_height = int(THUMBNAIL_WIDTH / aspect_ratio)
            else:
                new_height = THUMBNAIL_HEIGHT
                new_width = int(THUMBNAIL_HEIGHT * aspect_ratio)

            img.thumbnail((new_width, new_height), PILImage.Resampling.LANCZOS)

            thumbnail = PILImage.new('RGB', (THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT), (0, 0, 0))
            x = (THUMBNAIL_WIDTH - new_width) // 2
            y = (THUMBNAIL_HEIGHT - new_height) // 2
            thumbnail.paste(img, (x, y))

            thumbnail.save(thumb_path, 'JPEG', quality=THUMBNAIL_QUALITY, optimize=True)

        if image_id:
            update_thumbnail_cache('image', image_id, thumb_path, image_path)
        
        return thumb_path

    except Exception as e:
        if os.path.exists(thumb_path):
            os.remove(thumb_path)
        raise Exception(f"Failed to generate thumbnail: {str(e)}")


def generate_batch_thumbnails(items, media_type, progress_callback=None):
    """
    Generate thumbnails for multiple items in parallel.

    Args:
        items: List of dicts with 'id' and 'path' keys
        media_type: 'video' or 'image'
        progress_callback: Optional callback(current, total, success_count, errors)

    Returns:
        dict with 'success', 'failed', 'errors'
    """
    from ..services.thumbnail import generate_video_thumbnail, generate_image_thumbnail
    from ..models import db, Video, Image

    results = {'success': 0, 'failed': 0, 'errors': []}
    total = len(items)

    generator = generate_video_thumbnail if media_type == 'video' else generate_image_thumbnail
    model = Video if media_type == 'video' else Image

    def process_item(item):
        """Process a single item and return result."""
        try:
            thumb_path = generator(item['path'], item['id'])
            return {'id': item['id'], 'success': True, 'thumb_path': thumb_path}
        except Exception as e:
            return {'id': item['id'], 'success': False, 'error': str(e)[:200], 'path': item['path']}

    # Use thread pool for parallel processing
    max_workers = min(4, max(1, len(items)))  # Max 4 parallel workers

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = {executor.submit(process_item, item): item for item in items}

        for i, future in enumerate(as_completed(futures)):
            result = future.result()

            if result['success']:
                # Update database record
                record = model.query.get(result['id'])
                if record:
                    record.thumbnail_path = result['thumb_path']
                    db.session.commit()
                results['success'] += 1
            else:
                results['failed'] += 1
                results['errors'].append({
                    'id': result['id'],
                    'path': result.get('path', ''),
                    'error': result['error']
                })

            if progress_callback:
                progress_callback(i + 1, total, results['success'], results['failed'])

    return results


def delete_thumbnail(media_id, media_type):
    """Delete a thumbnail file."""
    thumb_path = get_thumbnail_path(media_id, media_type)

    if os.path.exists(thumb_path):
        try:
            os.remove(thumb_path)
            # Also delete cache entry
            from ..models import db, ThumbnailCache
            cache = ThumbnailCache.query.filter_by(
                media_type=media_type,
                media_id=media_id
            ).first()
            if cache:
                db.session.delete(cache)
                db.session.commit()
            return True
        except OSError:
            pass

    return False


def cleanup_expired_thumbnails():
    """Clean up expired thumbnail cache entries."""
    from ..models import db, ThumbnailCache
    from datetime import datetime
    
    expired = ThumbnailCache.query.filter(
        ThumbnailCache.expires_at < datetime.now()
    ).all()
    
    deleted = 0
    for cache in expired:
        if os.path.exists(cache.file_path):
            try:
                os.remove(cache.file_path)
                deleted += 1
            except OSError:
                pass
        db.session.delete(cache)
    
    db.session.commit()
    return deleted
