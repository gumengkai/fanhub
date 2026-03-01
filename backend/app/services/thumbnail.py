import os
import subprocess
from PIL import Image as PILImage
from io import BytesIO

# Thumbnail settings
THUMBNAIL_WIDTH = 320
THUMBNAIL_HEIGHT = 180
THUMBNAIL_QUALITY = 85


def get_thumbnail_path(media_id, media_type):
    """Get the path for a thumbnail file."""
    from flask import current_app

    thumbs_dir = current_app.config['THUMBNAIL_FOLDER']

    # Create subdirectory based on media ID to avoid too many files in one dir
    subdir = str((media_id // 1000) * 1000)
    thumb_dir = os.path.join(thumbs_dir, subdir)

    os.makedirs(thumb_dir, exist_ok=True)

    return os.path.join(thumb_dir, f"{media_type}_{media_id}.jpg")


def generate_video_thumbnail(video_path, video_id=None):
    """Generate a thumbnail for a video using ffmpeg."""
    if video_id is None:
        # Generate a temporary path if no ID provided
        import tempfile
        thumb_path = tempfile.mktemp(suffix='.jpg')
    else:
        thumb_path = get_thumbnail_path(video_id, 'video')

    # Skip if thumbnail already exists
    if os.path.exists(thumb_path):
        return thumb_path

    try:
        # Use ffmpeg to extract a frame at 10% into the video or 10 seconds
        cmd = [
            'ffmpeg',
            '-i', video_path,
            '-ss', '00:00:10',  # Seek to 10 seconds
            '-vframes', '1',     # Extract 1 frame
            '-q:v', '2',         # High quality
            '-vf', f'scale={THUMBNAIL_WIDTH}:{THUMBNAIL_HEIGHT}:force_original_aspect_ratio=decrease,pad={THUMBNAIL_WIDTH}:{THUMBNAIL_HEIGHT}:(ow-iw)/2:(oh-ih)/2:black',
            '-y',                # Overwrite output
            thumb_path
        ]

        result = subprocess.run(
            cmd,
            capture_output=True,
            timeout=60
        )

        if result.returncode == 0 and os.path.exists(thumb_path):
            return thumb_path
        else:
            raise Exception(f"ffmpeg failed: {result.stderr.decode()}")

    except subprocess.TimeoutExpired:
        raise Exception("Thumbnail generation timed out")
    except Exception as e:
        # Clean up partial file if exists
        if os.path.exists(thumb_path):
            os.remove(thumb_path)
        raise Exception(f"Failed to generate thumbnail: {str(e)}")


def generate_image_thumbnail(image_path, image_id=None):
    """Generate a thumbnail for an image using Pillow."""
    if image_id is None:
        # Generate a temporary path if no ID provided
        import tempfile
        thumb_path = tempfile.mktemp(suffix='.jpg')
    else:
        thumb_path = get_thumbnail_path(image_id, 'image')

    # Skip if thumbnail already exists
    if os.path.exists(thumb_path):
        return thumb_path

    try:
        with PILImage.open(image_path) as img:
            # Convert to RGB if necessary (for PNG with transparency)
            if img.mode in ('RGBA', 'LA', 'P'):
                background = PILImage.new('RGB', img.size, (0, 0, 0))
                if img.mode == 'P':
                    img = img.convert('RGBA')
                background.paste(img, mask=img.split()[-1] if img.mode in ('RGBA', 'LA') else None)
                img = background

            # Calculate resize dimensions
            aspect_ratio = img.width / img.height
            target_ratio = THUMBNAIL_WIDTH / THUMBNAIL_HEIGHT

            if aspect_ratio > target_ratio:
                # Image is wider
                new_width = THUMBNAIL_WIDTH
                new_height = int(THUMBNAIL_WIDTH / aspect_ratio)
            else:
                # Image is taller
                new_height = THUMBNAIL_HEIGHT
                new_width = int(THUMBNAIL_HEIGHT * aspect_ratio)

            # Resize image
            img.thumbnail((new_width, new_height), PILImage.Resampling.LANCZOS)

            # Create padded thumbnail
            thumbnail = PILImage.new('RGB', (THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT), (0, 0, 0))

            # Center the resized image
            x = (THUMBNAIL_WIDTH - new_width) // 2
            y = (THUMBNAIL_HEIGHT - new_height) // 2
            thumbnail.paste(img, (x, y))

            # Save with quality setting
            thumbnail.save(thumb_path, 'JPEG', quality=THUMBNAIL_QUALITY, optimize=True)

        return thumb_path

    except Exception as e:
        # Clean up partial file if exists
        if os.path.exists(thumb_path):
            os.remove(thumb_path)
        raise Exception(f"Failed to generate thumbnail: {str(e)}")


def delete_thumbnail(media_id, media_type):
    """Delete a thumbnail file."""
    thumb_path = get_thumbnail_path(media_id, media_type)

    if os.path.exists(thumb_path):
        try:
            os.remove(thumb_path)
            return True
        except OSError:
            pass

    return False
