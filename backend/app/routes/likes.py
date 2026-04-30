"""Likes API Routes for fanhub"""

from flask import Blueprint, request, jsonify
from sqlalchemy import desc
from ..models import db, Video, Image, Source

likes_bp = Blueprint('likes', __name__, url_prefix='/api/likes')


@likes_bp.route('', methods=['GET'])
def get_likes():
    """Get all liked items with pagination (excluding douyin)."""
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 20, type=int)
    media_type = request.args.get('type')  # 'video', 'image', or None for both

    # 获取非douyin类型的来源ID列表
    non_douyin_sources = Source.query.filter(Source.media_type != 'douyin').all()
    non_douyin_source_ids = [s.id for s in non_douyin_sources]

    videos = []
    images = []

    if media_type is None or media_type == 'video':
        if non_douyin_source_ids:
            videos = Video.query.filter(
                Video.is_liked == True,
                Video.source_id.in_(non_douyin_source_ids)
            ).order_by(desc(Video.updated_at)).all()

    if media_type is None or media_type == 'image':
        images = Image.query.filter_by(is_liked=True).order_by(desc(Image.created_at)).all()

    # Combine and sort by most recent
    all_likes = []

    for video in videos:
        data = video.to_dict()
        data['media_type'] = 'video'
        all_likes.append(data)

    for image in images:
        data = image.to_dict()
        data['media_type'] = 'image'
        all_likes.append(data)

    # Sort by updated_at or created_at (descending)
    all_likes.sort(key=lambda x: x.get('updated_at') or x.get('created_at'), reverse=True)

    # Pagination
    total = len(all_likes)
    start = (page - 1) * per_page
    end = start + per_page
    paginated = all_likes[start:end]

    return jsonify({
        'items': paginated,
        'total': total,
        'pages': (total + per_page - 1) // per_page,
        'current_page': page,
        'per_page': per_page
    })


@likes_bp.route('/stats', methods=['GET'])
def get_likes_stats():
    """Get likes statistics (excluding douyin)."""
    # 获取非douyin类型的来源ID列表
    non_douyin_sources = Source.query.filter(Source.media_type != 'douyin').all()
    non_douyin_source_ids = [s.id for s in non_douyin_sources]

    video_count = 0
    if non_douyin_source_ids:
        video_count = Video.query.filter(
            Video.is_liked == True,
            Video.source_id.in_(non_douyin_source_ids)
        ).count()

    image_count = Image.query.filter_by(is_liked=True).count()

    return jsonify({
        'video_count': video_count,
        'image_count': image_count,
        'total_count': video_count + image_count
    })