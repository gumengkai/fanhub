import os
from flask import Blueprint, request, jsonify
from datetime import datetime
from ..models import db, Source, Video, Image
from ..services.scanner import scan_source
from ..services.nas_client import test_nas_connection

sources_bp = Blueprint('sources', __name__, url_prefix='/api/sources')


@sources_bp.route('', methods=['GET'])
def get_sources():
    """Get all sources."""
    media_type = request.args.get('media_type', None)
    
    query = Source.query
    if media_type:
        query = query.filter(Source.media_type == media_type)
    
    sources = query.all()
    return jsonify([source.to_dict() for source in sources])


@sources_bp.route('', methods=['POST'])
def create_source():
    """Create a new source."""
    data = request.get_json()

    if not data.get('name') or not data.get('type') or not data.get('path'):
        return jsonify({'error': 'Missing required fields: name, type, path'}), 400

    # Validate source type
    if data['type'] not in ['local', 'nas']:
        return jsonify({'error': 'Invalid source type. Must be "local" or "nas"'}), 400

    # Validate media_type
    media_type = data.get('media_type', 'all')
    if media_type not in ['all', 'video', 'image']:
        return jsonify({'error': 'Invalid media_type. Must be "all", "video", or "image"'}), 400

    # Validate path for local sources
    if data['type'] == 'local' and not os.path.exists(data['path']):
        return jsonify({'error': 'Path does not exist'}), 400

    source = Source(
        name=data['name'],
        type=data['type'],
        media_type=media_type,
        path=data['path'],
        nas_config=data.get('nas_config'),
        scan_interval=data.get('scan_interval', 60),
        is_active=data.get('is_active', True)
    )

    db.session.add(source)
    db.session.commit()

    return jsonify(source.to_dict()), 201


@sources_bp.route('/<int:source_id>', methods=['PUT'])
def update_source(source_id):
    """Update a source."""
    source = Source.query.get_or_404(source_id)
    data = request.get_json()

    if 'name' in data:
        source.name = data['name']
    if 'media_type' in data:
        if data['media_type'] not in ['all', 'video', 'image']:
            return jsonify({'error': 'Invalid media_type'}), 400
        source.media_type = data['media_type']
    if 'path' in data:
        if source.type == 'local' and not os.path.exists(data['path']):
            return jsonify({'error': 'Path does not exist'}), 400
        source.path = data['path']
    if 'nas_config' in data:
        source.nas_config = data['nas_config']
    if 'scan_interval' in data:
        source.scan_interval = data['scan_interval']
    if 'is_active' in data:
        source.is_active = data['is_active']

    db.session.commit()

    return jsonify(source.to_dict())


@sources_bp.route('/<int:source_id>', methods=['DELETE'])
def delete_source(source_id):
    """Delete a source and all its media records."""
    source = Source.query.get_or_404(source_id)

    # Delete associated videos and images based on media_type
    if source.media_type != 'image':
        Video.query.filter_by(source_id=source_id).delete()
    if source.media_type != 'video':
        Image.query.filter_by(source_id=source_id).delete()

    db.session.delete(source)
    db.session.commit()

    return jsonify({'message': 'Source deleted successfully'})


@sources_bp.route('/<int:source_id>/scan', methods=['POST'])
def scan_source_endpoint(source_id):
    """Manually trigger a source scan."""
    source = Source.query.get_or_404(source_id)

    try:
        stats = scan_source(source)
        source.last_scan_at = datetime.utcnow()
        db.session.commit()

        return jsonify({
            'message': 'Scan completed successfully',
            'stats': stats
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@sources_bp.route('/<int:source_id>/status', methods=['GET'])
def check_source_status(source_id):
    """Check source connection status."""
    source = Source.query.get_or_404(source_id)

    if source.type == 'local':
        is_accessible = os.path.exists(source.path) and os.access(source.path, os.R_OK)
        return jsonify({
            'accessible': is_accessible,
            'type': 'local',
            'path': source.path,
            'media_type': source.media_type
        })
    elif source.type == 'nas':
        result = test_nas_connection(source.nas_config)
        return jsonify({
            'accessible': result['success'],
            'type': 'nas',
            'media_type': source.media_type,
            'details': result
        })

    return jsonify({'error': 'Unknown source type'}), 400


@sources_bp.route('/<int:source_id>/stats', methods=['GET'])
def get_source_stats(source_id):
    """Get statistics for a source."""
    source = Source.query.get_or_404(source_id)

    video_count = 0
    image_count = 0
    total_video_size = 0
    total_image_size = 0

    if source.media_type != 'image':
        video_count = Video.query.filter_by(source_id=source_id).count()
        total_video_size = db.session.query(db.func.sum(Video.file_size)).filter_by(source_id=source_id).scalar() or 0
    
    if source.media_type != 'video':
        image_count = Image.query.filter_by(source_id=source_id).count()
        total_image_size = db.session.query(db.func.sum(Image.file_size)).filter_by(source_id=source_id).scalar() or 0

    return jsonify({
        'source_id': source_id,
        'media_type': source.media_type,
        'video_count': video_count,
        'image_count': image_count,
        'total_video_size': total_video_size,
        'total_image_size': total_image_size,
        'total_size': total_video_size + total_image_size
    })
