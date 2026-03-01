from flask import Blueprint, request, jsonify
from sqlalchemy import desc
from ..models import db, Video, Image

favorites_bp = Blueprint('favorites', __name__, url_prefix='/api/favorites')


@favorites_bp.route('', methods=['GET'])
def get_favorites():
    """Get all favorites with pagination."""
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 20, type=int)
    media_type = request.args.get('type')  # 'video', 'image', or None for both

    videos = []
    images = []

    if media_type is None or media_type == 'video':
        videos = Video.query.filter_by(is_favorite=True).order_by(desc(Video.updated_at)).all()

    if media_type is None or media_type == 'image':
        images = Image.query.filter_by(is_favorite=True).order_by(desc(Image.created_at)).all()

    # Combine and sort by most recent
    all_favorites = []

    for video in videos:
        data = video.to_dict()
        data['media_type'] = 'video'
        all_favorites.append(data)

    for image in images:
        data = image.to_dict()
        data['media_type'] = 'image'
        all_favorites.append(data)

    # Sort by updated_at or created_at (descending)
    all_favorites.sort(key=lambda x: x.get('updated_at') or x.get('created_at'), reverse=True)

    # Pagination
    total = len(all_favorites)
    start = (page - 1) * per_page
    end = start + per_page
    paginated = all_favorites[start:end]

    return jsonify({
        'items': paginated,
        'total': total,
        'pages': (total + per_page - 1) // per_page,
        'current_page': page,
        'per_page': per_page
    })


@favorites_bp.route('/stats', methods=['GET'])
def get_favorites_stats():
    """Get favorites statistics."""
    video_count = Video.query.filter_by(is_favorite=True).count()
    image_count = Image.query.filter_by(is_favorite=True).count()

    return jsonify({
        'video_count': video_count,
        'image_count': image_count,
        'total_count': video_count + image_count
    })
