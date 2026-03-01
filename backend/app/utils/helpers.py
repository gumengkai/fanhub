import os
import hashlib
from datetime import datetime


def format_file_size(size_bytes):
    """Format file size in human-readable format."""
    if size_bytes is None:
        return "Unknown"

    for unit in ['B', 'KB', 'MB', 'GB', 'TB']:
        if size_bytes < 1024.0:
            return f"{size_bytes:.1f} {unit}"
        size_bytes /= 1024.0

    return f"{size_bytes:.1f} PB"


def format_duration(seconds):
    """Format duration in seconds to human-readable format."""
    if seconds is None:
        return "Unknown"

    hours = seconds // 3600
    minutes = (seconds % 3600) // 60
    secs = seconds % 60

    if hours > 0:
        return f"{hours}:{minutes:02d}:{secs:02d}"
    else:
        return f"{minutes}:{secs:02d}"


def format_resolution(width, height):
    """Format resolution string."""
    if width and height:
        return f"{width}x{height}"
    return "Unknown"


def get_file_hash(file_path, algorithm='md5', block_size=65536):
    """Calculate hash of a file."""
    hasher = hashlib.new(algorithm)

    try:
        with open(file_path, 'rb') as f:
            while True:
                data = f.read(block_size)
                if not data:
                    break
                hasher.update(data)
        return hasher.hexdigest()
    except Exception as e:
        return None


def sanitize_filename(filename):
    """Sanitize a filename for safe storage."""
    # Remove or replace unsafe characters
    unsafe_chars = '<>:"/\\|?*'
    for char in unsafe_chars:
        filename = filename.replace(char, '_')
    return filename


def parse_sort_param(sort_string):
    """Parse sort parameter into column and direction."""
    if sort_string.startswith('-'):
        return sort_string[1:], 'desc'
    return sort_string, 'asc'


def is_video_file(filename):
    """Check if a file is a video based on extension."""
    video_extensions = {'.mp4', '.mkv', '.avi', '.mov', '.flv', '.wmv', '.m4v', '.webm'}
    ext = os.path.splitext(filename)[1].lower()
    return ext in video_extensions


def is_image_file(filename):
    """Check if a file is an image based on extension."""
    image_extensions = {'.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.tiff', '.tif'}
    ext = os.path.splitext(filename)[1].lower()
    return ext in image_extensions


def ensure_directory(path):
    """Ensure a directory exists, creating it if necessary."""
    os.makedirs(path, exist_ok=True)
    return path
