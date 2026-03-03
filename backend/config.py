import os
from pathlib import Path

BASE_DIR = Path(__file__).parent.resolve()
PROJECT_DIR = BASE_DIR.parent

class Config:
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'dev-secret-key-change-in-production'

    # Database configuration - support Docker volume mounts
    DATABASE_PATH = os.environ.get('DATABASE_PATH', '/app/storage/database/funhub.db')
    SQLALCHEMY_DATABASE_URI = os.environ.get('DATABASE_URL') or f'sqlite:///{DATABASE_PATH}'
    SQLALCHEMY_TRACK_MODIFICATIONS = False

    # Thumbnail folder - support Docker volume mounts
    THUMBNAIL_FOLDER = Path(os.environ.get('THUMBNAIL_FOLDER', '/app/storage/thumbnails'))
    THUMBNAIL_FOLDER.mkdir(parents=True, exist_ok=True)

    VIDEO_EXTENSIONS = {'mp4', 'mkv', 'avi', 'mov', 'wmv', 'flv', 'webm', 'm4v'}
    IMAGE_EXTENSIONS = {'jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'tiff', 'svg'}

    MAX_CONTENT_LENGTH = 16 * 1024 * 1024

    # CORS configuration - support both development and production
    CORS_ORIGINS = os.environ.get('CORS_ORIGINS', 'http://localhost:5173,http://127.0.0.1:5173,http://localhost,http://127.0.0.1').split(',')

    SCAN_CHUNK_SIZE = 100
