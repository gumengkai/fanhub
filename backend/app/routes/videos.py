import os
from flask import Blueprint, request, jsonify, send_file
from sqlalchemy import desc, asc
from ..models import db, Video, Tag
from ..services.thumbnail import generate_video_thumbnail

videos_bp = Blueprint('videos', __name__, url_prefix='/api/videos')


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

    query = Video.query

    if search:
        query = query.filter(Video.title.ilike(f'%{search}%'))

    if source_id:
        query = query.filter(Video.source_id == source_id)

    if tag_id:
        query = query.join(Video.tags).filter(Tag.id == tag_id)

    # Sorting
    sort_column = getattr(Video, sort_by, Video.created_at)
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
    """Stream video file."""
    video = Video.query.get_or_404(video_id)

    if not os.path.exists(video.path):
        return jsonify({'error': 'Video file not found'}), 404

    # Support range requests for video streaming
    range_header = request.headers.get('Range', None)

    if range_header:
        # Handle partial content (HTTP 206)
        file_size = os.path.getsize(video.path)
        byte_start, byte_end = range_header.replace('bytes=', '').split('-')
        byte_start = int(byte_start) if byte_start else 0
        byte_end = int(byte_end) if byte_end else file_size - 1

        remaining = byte_end - byte_start + 1

        def generate(remaining_bytes):
            with open(video.path, 'rb') as f:
                f.seek(byte_start)
                while remaining_bytes > 0:
                    chunk_size = min(8192, remaining_bytes)
                    data = f.read(chunk_size)
                    if not data:
                        break
                    remaining_bytes -= len(data)
                    yield data

        response = generate(remaining)
        headers = {
            'Content-Range': f'bytes {byte_start}-{byte_end}/{file_size}',
            'Accept-Ranges': 'bytes',
            'Content-Length': str(remaining),
            'Content-Type': 'video/mp4'
        }
        return response, 206, headers
    else:
        # Full file
        return send_file(
            video.path,
            mimetype='video/mp4',
            as_attachment=False
        )


@videos_bp.route('/<int:video_id>', methods=['DELETE'])
def delete_video(video_id):
    """Delete video record (not the file)."""
    video = Video.query.get_or_404(video_id)

    # Delete thumbnail if exists
    if video.thumbnail_path and os.path.exists(video.thumbnail_path):
        try:
            os.remove(video.thumbnail_path)
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


@videos_bp.route('/<int:video_id>/thumbnail', methods=['POST'])
def regenerate_thumbnail(video_id):
    """Regenerate video thumbnail."""
    video = Video.query.get_or_404(video_id)

    try:
        thumbnail_path = generate_video_thumbnail(video.path, video.id)
        video.thumbnail_path = thumbnail_path
        db.session.commit()
        return jsonify({
            'message': 'Thumbnail regenerated successfully',
            'thumbnail_path': thumbnail_path
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500


# Tag management routes
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
        return jsonify([])

    tag_ids = [tag.id for tag in video.tags]

    # Find videos with similar tags, excluding current video
    related = Video.query.join(Video.tags).filter(
        Tag.id.in_(tag_ids),
        Video.id != video_id
    ).group_by(Video.id).order_by(
        desc(db.func.count(Tag.id))
    ).limit(limit).all()

    return jsonify([v.to_dict() for v in related])
