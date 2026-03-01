"""Fix missing video thumbnails with detailed logging"""

import sys
import os
import subprocess

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app import create_app, db
from app.models import Video
from app.services.thumbnail import generate_video_thumbnail, get_thumbnail_path

def test_ffmpeg():
    """Test if ffmpeg is available."""
    try:
        result = subprocess.run(['ffmpeg', '-version'], capture_output=True, timeout=5)
        return result.returncode == 0
    except Exception as e:
        print(f"❌ ffmpeg test failed: {e}")
        return False

def fix_video_thumbnails():
    """Generate thumbnails for videos that don't have one."""
    app = create_app()

    with app.app_context():
        print("=" * 60)
        print("🔧 Video Thumbnail Fix Tool")
        print("=" * 60)

        # Test ffmpeg
        print("\n📋 Testing ffmpeg...")
        if not test_ffmpeg():
            print("❌ ffmpeg is not available. Please install ffmpeg.")
            return
        print("✅ ffmpeg is available")

        # Get all videos without thumbnails
        videos = Video.query.filter(
            (Video.thumbnail_path == None) | (Video.thumbnail_path == '')
        ).all()

        print(f"\n📊 Found {len(videos)} videos without thumbnails")

        if len(videos) == 0:
            print("✅ All videos have thumbnails!")
            return

        success = 0
        failed = 0
        errors = []

        for i, video in enumerate(videos, 1):
            print(f"\n[{i}/{len(videos)}] Processing: {video.title}")
            print(f"  Path: {video.path}")

            try:
                # Check if video file exists
                if not os.path.exists(video.path):
                    print(f"  ⚠️  Video file not found, skipping")
                    failed += 1
                    errors.append({'id': video.id, 'title': video.title, 'error': 'File not found'})
                    continue

                # Generate thumbnail
                print(f"  🎬 Generating thumbnail...")
                thumbnail_path = get_thumbnail_path(video.id, 'video')

                # Check if thumbnail already exists (maybe not recorded in DB)
                if os.path.exists(thumbnail_path):
                    print(f"  ✅ Thumbnail already exists at: {thumbnail_path}")
                    video.thumbnail_path = thumbnail_path
                    db.session.commit()
                    success += 1
                    continue

                # Generate new thumbnail
                thumbnail_path = generate_video_thumbnail(video.path, video.id)

                if thumbnail_path and os.path.exists(thumbnail_path):
                    video.thumbnail_path = thumbnail_path
                    db.session.commit()
                    print(f"  ✅ Success: {thumbnail_path}")
                    success += 1
                else:
                    print(f"  ❌ Failed: Thumbnail not created")
                    failed += 1
                    errors.append({'id': video.id, 'title': video.title, 'error': 'Thumbnail not created'})

            except Exception as e:
                print(f"  ❌ Failed: {str(e)[:200]}")
                failed += 1
                errors.append({'id': video.id, 'title': video.title, 'error': str(e)[:200]})

        print("\n" + "=" * 60)
        print("📊 Summary:")
        print(f"  ✅ Success: {success}")
        print(f"  ❌ Failed: {failed}")
        print(f"  📊 Total: {len(videos)}")

        if errors:
            print("\n📝 Errors:")
            for err in errors[:10]:  # Show first 10 errors
                print(f"  - {err['title']}: {err['error']}")
            if len(errors) > 10:
                print(f"  ... and {len(errors) - 10} more")

        print("=" * 60)

if __name__ == '__main__':
    fix_video_thumbnails()
