import os
from pathlib import Path

BASE_DIR = Path(__file__).parent.resolve()
PROJECT_DIR = BASE_DIR.parent

class Config:
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'dev-secret-key-change-in-production'

    SQLALCHEMY_DATABASE_URI = os.environ.get('DATABASE_URL') or \
        f'sqlite:///{PROJECT_DIR}/storage/database/funhub.db'
    SQLALCHEMY_TRACK_MODIFICATIONS = False

    THUMBNAIL_FOLDER = PROJECT_DIR / 'storage' / 'thumbnails'
    THUMBNAIL_FOLDER.mkdir(parents=True, exist_ok=True)

    VIDEO_EXTENSIONS = {'mp4', 'mkv', 'avi', 'mov', 'wmv', 'flv', 'webm', 'm4v'}
    IMAGE_EXTENSIONS = {'jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'tiff', 'svg'}

    MAX_CONTENT_LENGTH = 16 * 1024 * 1024

    CORS_ORIGINS = ['http://localhost:5173', 'http://127.0.0.1:5173']

    SCAN_CHUNK_SIZE = 100
