"""Watch History API Routes"""

from flask import Blueprint, request, jsonify
from sqlalchemy import desc
from ..models import db, WatchHistory, Video

history_bp = Blueprint('history', __name__, url_prefix='/api/history')


@history_bp.route('', methods=['GET'])
def get_history():
    """Get watch history with pagination."""
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 20, type=int)
    
    pagination = WatchHistory.query.order_by(
        desc(WatchHistory.watched_at)
    ).paginate(page=page, per_page=per_page, error_out=False)
    
    return jsonify({
        'items': [h.to_dict() for h in pagination.items],
        'total': pagination.total,
        'pages': pagination.pages,
        'current_page': page
    })


@history_bp.route('/video/<int:video_id>', methods=['GET'])
def get_video_history(video_id):
    """Get watch history for a specific video."""
    history = WatchHistory.query.filter_by(video_id=video_id).first()
    if history:
        return jsonify(history.to_dict())
    return jsonify({'message': 'No history found'}), 404


@history_bp.route('/video/<int:video_id>', methods=['POST', 'PUT'])
def update_video_history(video_id):
    """Update or create watch history for a video."""
    data = request.get_json()
    playback_position = data.get('playback_position', 0)
    duration = data.get('duration', 0)
    is_completed = data.get('is_completed', False)
    
    # Check if video exists
    video = Video.query.get(video_id)
    if not video:
        return jsonify({'error': 'Video not found'}), 404
    
    # Increment view count on first watch
    existing = WatchHistory.query.filter_by(video_id=video_id).first()
    if not existing:
        video.view_count += 1
    
    # Update or create history
    history = WatchHistory.query.filter_by(video_id=video_id).first()
    if history:
        history.playback_position = playback_position
        history.is_completed = is_completed or (playback_position >= duration * 0.9)
    else:
        history = WatchHistory(
            video_id=video_id,
            playback_position=playback_position,
            is_completed=is_completed or (playback_position >= duration * 0.9)
        )
        db.session.add(history)
    
    db.session.commit()
    return jsonify(history.to_dict())


@history_bp.route('/video/<int:video_id>', methods=['DELETE'])
def delete_video_history(video_id):
    """Delete watch history for a specific video."""
    history = WatchHistory.query.filter_by(video_id=video_id).first()
    if history:
        db.session.delete(history)
        db.session.commit()
        return jsonify({'message': 'History deleted'})
    return jsonify({'message': 'No history found'}), 404


@history_bp.route('/clear', methods=['POST'])
def clear_history():
    """Clear all watch history."""
    WatchHistory.query.delete()
    db.session.commit()
    return jsonify({'message': 'History cleared'})


@history_bp.route('/stats', methods=['GET'])
def get_history_stats():
    """Get watch history statistics."""
    total_watched = WatchHistory.query.count()
    completed = WatchHistory.query.filter_by(is_completed=True).count()
    in_progress = total_watched - completed
    
    return jsonify({
        'total_watched': total_watched,
        'completed': completed,
        'in_progress': in_progress
    })
