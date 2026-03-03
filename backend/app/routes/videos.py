import os
from flask import Blueprint, request, jsonify, send_file, Response, current_app
from sqlalchemy import desc, asc, func
from ..models import db, Video, Tag, WatchHistory
import io
from PIL import Image as PILImage, ImageDraw, ImageFont

videos_bp = Blueprint('videos', __name__, url_prefix='/api/videos')


def generate_placeholder(text="No Video", color=(51, 51, 51), text_color=(255, 255, 255)):
    """Generate a placeholder image."""
    img = PILImage.new('RGB', (320, 180), color=color)
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 20)
    except:
        font = ImageFont.load_default()
    
    # Center text
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    x = (320 - text_width) // 2
    y = (180 - text_height) // 2
    draw.text((x, y), text, fill=text_color, font=font)
    
    img_io = io.BytesIO()
    img.save(img_io, 'JPEG', quality=85)
    img_io.seek(0)
    return img_io


@videos_bp.route('', methods=['GET'])
def get_videos():
    """Get video list with pagination, search, and sorting."""
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 20, type=int)
    search = request.args.get('search', '')
    sort_by = request.args.get('sort_by', 'created_at')
    order = request.args.get('order', 'desc')
    source_id = request.args.get('source_id', type=int)
    tag_id = request.args.get('tag_id', type=int)
    favorite = request.args.get('favorite')

    query = Video.query

    if search:
        query = query.filter(Video.title.ilike(f'%{search}%'))

    if source_id:
        query = query.filter(Video.source_id == source_id)

    if tag_id:
        query = query.join(Video.tags).filter(Tag.id == tag_id)

    if favorite is not None:
        # 处理 'true', '1', 1 等值为 True
        is_fav = str(favorite).lower() in ('true', '1', 'yes', 'on')
        query = query.filter(Video.is_favorite == is_fav)

    if sort_by == 'view_count':
        sort_column = Video.view_count
    elif sort_by == 'duration':
        sort_column = Video.duration
    elif sort_by == 'file_size':
        sort_column = Video.file_size
    elif sort_by == 'title':
        sort_column = Video.title
    else:
        sort_column = Video.created_at
    
    if order == 'desc':
        query = query.order_by(desc(sort_column))
    else:
        query = query.order_by(asc(sort_column))

    pagination = query.paginate(page=page, per_page=per_page, error_out=False)

    return jsonify({
        'items': [video.to_dict() for video in pagination.items],
        'total': pagination.total,
        'pages': pagination.pages,
        'current_page': page,
        'per_page': per_page
    })


@videos_bp.route('/<int:video_id>', methods=['GET'])
def get_video(video_id):
    """Get video details."""
    video = Video.query.get_or_404(video_id)
    video.view_count += 1
    db.session.commit()
    return jsonify(video.to_dict(include_details=True))


@videos_bp.route('/<int:video_id>', methods=['PUT'])
def update_video(video_id):
    """Update video metadata."""
    video = Video.query.get_or_404(video_id)
    data = request.get_json()

    if 'title' in data:
        video.title = data['title']
    if 'description' in data:
        video.description = data['description']
    if 'is_favorite' in data:
        video.is_favorite = data['is_favorite']

    db.session.commit()
    return jsonify(video.to_dict(include_details=True))


@videos_bp.route('/<int:video_id>/stream', methods=['GET'])
def stream_video(video_id):
    """Stream video file with optimized range requests."""
    video = Video.query.get_or_404(video_id)

    if not os.path.exists(video.path):
        return jsonify({'error': 'Video file not found'}), 404

    range_header = request.headers.get('Range', None)
    file_size = os.path.getsize(video.path)

    if range_header:
        byte_start, byte_end = range_header.replace('bytes=', '').split('-')
        byte_start = int(byte_start) if byte_start else 0
        byte_end = int(byte_end) if byte_end else file_size - 1
        remaining = byte_end - byte_start + 1

        def generate(remaining_bytes):
            with open(video.path, 'rb') as f:
                f.seek(byte_start)
                while remaining_bytes > 0:
                    chunk_size = min(65536, remaining_bytes)
                    data = f.read(chunk_size)
                    if not data:
                        break
                    remaining_bytes -= len(data)
                    yield data

        headers = {
            'Content-Range': f'bytes {byte_start}-{byte_end}/{file_size}',
            'Accept-Ranges': 'bytes',
            'Content-Length': str(remaining),
            'Content-Type': 'video/mp4',
            'Cache-Control': 'public, max-age=3600'
        }
        return Response(generate(remaining), 206, headers)
    else:
        return send_file(
            video.path,
            mimetype='video/mp4',
            as_attachment=False
        )


@videos_bp.route('/<int:video_id>', methods=['DELETE'])
def delete_video(video_id):
    """Delete video record and file from filesystem."""
    video = Video.query.get_or_404(video_id)

    # Delete thumbnail if exists
    if video.thumbnail_path and os.path.exists(video.thumbnail_path):
        try:
            os.remove(video.thumbnail_path)
        except OSError:
            pass

    # Delete video file if exists
    if video.path and os.path.exists(video.path):
        try:
            os.remove(video.path)
        except OSError:
            pass

    db.session.delete(video)
    db.session.commit()
    return jsonify({'message': 'Video deleted successfully'})


@videos_bp.route('/<int:video_id>/favorite', methods=['POST'])
def toggle_favorite(video_id):
    """Toggle favorite status."""
    video = Video.query.get_or_404(video_id)
    video.is_favorite = not video.is_favorite
    db.session.commit()
    return jsonify({
        'message': 'Favorite status updated',
        'is_favorite': video.is_favorite
    })


@videos_bp.route('/<int:video_id>/thumbnail', methods=['GET'])
def get_video_thumbnail(video_id):
    """Get video thumbnail with improved error handling and placeholder."""
    video = Video.query.get_or_404(video_id)

    # Check if thumbnail exists in filesystem
    if video.thumbnail_path and os.path.exists(video.thumbnail_path):
        response = send_file(
            video.thumbnail_path,
            mimetype='image/jpeg',
            as_attachment=False
        )
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        response.headers['Pragma'] = 'no-cache'
        response.headers['Expires'] = '0'
        return response

    # If video file doesn't exist, return placeholder
    if not os.path.exists(video.path):
        img_io = generate_placeholder("Video Not Found", (100, 100, 100), (255, 100, 100))
        response = send_file(img_io, mimetype='image/jpeg')
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        return response

    # Try to generate thumbnail
    try:
        from ..services.thumbnail import generate_video_thumbnail
        thumbnail_path = generate_video_thumbnail(video.path, video.id)
        video.thumbnail_path = thumbnail_path
        db.session.commit()
        response = send_file(
            thumbnail_path,
            mimetype='image/jpeg',
            as_attachment=False
        )
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        response.headers['Pragma'] = 'no-cache'
        response.headers['Expires'] = '0'
        return response
    except Exception as e:
        current_app.logger.error(f"Thumbnail error for video {video_id}: {e}")
        img_io = generate_placeholder("Click to Play", (30, 30, 50), (251, 114, 153))
        response = send_file(img_io, mimetype='image/jpeg')
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        return response


@videos_bp.route('/<int:video_id>/thumbnail', methods=['POST'])
def regenerate_thumbnail(video_id):
    """Regenerate video thumbnail."""
    video = Video.query.get_or_404(video_id)

    try:
        from ..services.thumbnail import generate_video_thumbnail
        thumbnail_path = generate_video_thumbnail(video.path, video.id)
        video.thumbnail_path = thumbnail_path
        db.session.commit()
        return jsonify({
            'message': 'Thumbnail regenerated successfully',
            'thumbnail_path': thumbnail_path
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@videos_bp.route('/thumbnails/fix', methods=['POST'])
def fix_missing_thumbnails():
    """Fix thumbnails for videos that don't have one."""
    videos = Video.query.filter(
        (Video.thumbnail_path == None) | (Video.thumbnail_path == '')
    ).all()

    results = {'success': 0, 'failed': 0, 'errors': []}

    for video in videos:
        try:
            if not os.path.exists(video.path):
                results['failed'] += 1
                results['errors'].append({'id': video.id, 'error': 'File not found'})
                continue

            from ..services.thumbnail import generate_video_thumbnail
            thumbnail_path = generate_video_thumbnail(video.path, video.id)
            video.thumbnail_path = thumbnail_path
            db.session.commit()
            results['success'] += 1

        except Exception as e:
            results['failed'] += 1
            results['errors'].append({'id': video.id, 'error': str(e)[:200]})

    return jsonify({
        'message': f'Fixed {results["success"]} thumbnails, {results["failed"]} failed',
        'results': results
    })


@videos_bp.route('/thumbnails/batch', methods=['POST'])
def batch_generate_thumbnails():
    """Generate thumbnails for multiple videos."""
    data = request.get_json()
    video_ids = data.get('video_ids', [])
    
    if not video_ids:
        return jsonify({'error': 'video_ids is required'}), 400
    
    videos = Video.query.filter(Video.id.in_(video_ids)).all()
    items = [{'id': v.id, 'path': v.path} for v in videos]
    
    from ..services.thumbnail import generate_batch_thumbnails
    
    def progress_callback(current, total, success, failed):
        current_app.logger.info(f"Thumbnail progress: {current}/{total}, success: {success}, failed: {failed}")
    
    results = generate_batch_thumbnails(items, 'video', progress_callback)
    return jsonify(results)


@videos_bp.route('/<int:video_id>/tags', methods=['GET'])
def get_video_tags(video_id):
    """Get all tags for a video."""
    video = Video.query.get_or_404(video_id)
    return jsonify([tag.to_dict() for tag in video.tags])


@videos_bp.route('/<int:video_id>/tags', methods=['POST'])
def add_tag_to_video(video_id):
    """Add a tag to video."""
    video = Video.query.get_or_404(video_id)
    data = request.get_json()
    tag_id = data.get('tag_id')

    if not tag_id:
        return jsonify({'error': 'tag_id is required'}), 400

    tag = Tag.query.get_or_404(tag_id)

    if tag not in video.tags:
        video.tags.append(tag)
        db.session.commit()

    return jsonify([t.to_dict() for t in video.tags])


@videos_bp.route('/<int:video_id>/tags/<int:tag_id>', methods=['DELETE'])
def remove_tag_from_video(video_id, tag_id):
    """Remove a tag from video."""
    video = Video.query.get_or_404(video_id)
    tag = Tag.query.get_or_404(tag_id)

    if tag in video.tags:
        video.tags.remove(tag)
        db.session.commit()

    return jsonify([t.to_dict() for t in video.tags])


@videos_bp.route('/<int:video_id>/related', methods=['GET'])
def get_related_videos(video_id):
    """Get related videos based on tags."""
    video = Video.query.get_or_404(video_id)
    limit = request.args.get('limit', 6, type=int)

    if not video.tags:
        related = Video.query.filter(Video.id != video_id).order_by(desc(Video.created_at)).limit(limit).all()
        return jsonify([v.to_dict() for v in related])

    tag_ids = [tag.id for tag in video.tags]
    related = Video.query.join(Video.tags).filter(
        Tag.id.in_(tag_ids),
        Video.id != video_id
    ).group_by(Video.id).order_by(desc(func.count(Tag.id))).limit(limit).all()

    return jsonify([v.to_dict() for v in related])


@videos_bp.route('/<int:video_id>/history', methods=['GET'])
def get_video_history(video_id):
    """Get watch history for a video."""
    history = WatchHistory.query.filter_by(video_id=video_id).first()
    if history:
        return jsonify(history.to_dict())
    return jsonify({'message': 'No history found'}), 404


@videos_bp.route('/<int:video_id>/history', methods=['POST', 'PUT'])
def update_video_history(video_id):
    """Update watch progress for a video."""
    video = Video.query.get_or_404(video_id)
    data = request.get_json()
    playback_position = data.get('playback_position', 0)
    duration = data.get('duration') or 0
    is_completed = data.get('is_completed', False)
    
    existing = WatchHistory.query.filter_by(video_id=video_id).first()
    if not existing:
        video.view_count += 1
    
    history = WatchHistory.query.filter_by(video_id=video_id).first()
    if history:
        history.playback_position = playback_position
        history.is_completed = is_completed or (duration > 0 and playback_position >= duration * 0.9)
    else:
        history = WatchHistory(
            video_id=video_id,
            playback_position=playback_position,
            is_completed=is_completed or (duration > 0 and playback_position >= duration * 0.9)
        )
        db.session.add(history)
    
    db.session.commit()
    return jsonify(history.to_dict())
