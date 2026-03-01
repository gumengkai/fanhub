"""Database Migration Script for FunHub"""

import sys
import os

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app import create_app, db
from app.models import Video, Image, WatchHistory, ThumbnailCache, Source
from sqlalchemy import inspect

def migrate():
    """Run database migrations."""
    app = create_app()
    
    with app.app_context():
        inspector = inspect(db.engine)
        existing_tables = inspector.get_table_names()
        
        print("📊 Checking database tables...")
        
        # Create new tables if they don't exist
        if 'watch_history' not in existing_tables:
            print("✨ Creating watch_history table...")
            db.create_all()
            print("✅ watch_history table created")
        else:
            print("✅ watch_history table exists")
        
        if 'thumbnail_cache' not in existing_tables:
            print("✨ Creating thumbnail_cache table...")
            db.create_all()
            print("✅ thumbnail_cache table created")
        else:
            print("✅ thumbnail_cache table exists")
        
        # Check sources table for media_type column
        print("\n📝 Checking columns...")
        
        source_columns = [col['name'] for col in inspector.get_columns('sources')]
        
        if 'media_type' not in source_columns:
            print("✨ Adding media_type to sources table...")
            with db.engine.connect() as conn:
                conn.execute(db.text("""
                    ALTER TABLE sources ADD COLUMN media_type VARCHAR(16) DEFAULT 'all'
                """))
                conn.commit()
            print("✅ media_type column added (default: 'all')")
        else:
            print("✅ media_type column exists")
        
        # Check videos table
        video_columns = [col['name'] for col in inspector.get_columns('videos')]
        
        if 'view_count' not in video_columns:
            print("✨ Adding view_count to videos table...")
            with db.engine.connect() as conn:
                conn.execute(db.text("ALTER TABLE videos ADD COLUMN view_count INTEGER DEFAULT 0"))
                conn.commit()
            print("✅ view_count column added")
        else:
            print("✅ view_count column exists")
        
        # Check images table
        image_columns = [col['name'] for col in inspector.get_columns('images')]
        
        if 'view_count' not in image_columns:
            print("✨ Adding view_count to images table...")
            with db.engine.connect() as conn:
                conn.execute(db.text("ALTER TABLE images ADD COLUMN view_count INTEGER DEFAULT 0"))
                conn.commit()
            print("✅ view_count column added")
        else:
            print("✅ view_count column exists")
        
        print("\n✅ Migration completed successfully!")
        print("\n📋 Summary:")
        print("  - Sources can now be configured for video-only, image-only, or all media")
        print("  - Video and image scanning is now separated by source media_type")
        print("  - Existing sources default to 'all' (backward compatible)")

if __name__ == '__main__':
    migrate()
