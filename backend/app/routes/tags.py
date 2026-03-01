from flask import Blueprint, request, jsonify
from ..models import db, Tag

tags_bp = Blueprint('tags', __name__, url_prefix='/api/tags')


@tags_bp.route('', methods=['GET'])
def get_tags():
    """Get all tags."""
    tags = Tag.query.all()
    return jsonify([tag.to_dict() for tag in tags])


@tags_bp.route('', methods=['POST'])
def create_tag():
    """Create a new tag."""
    data = request.get_json()

    if not data.get('name'):
        return jsonify({'error': 'Tag name is required'}), 400

    # Check if tag already exists
    existing = Tag.query.filter_by(name=data['name']).first()
    if existing:
        return jsonify({'error': 'Tag already exists'}), 409

    tag = Tag(
        name=data['name'],
        color=data.get('color', '#1890ff')
    )

    db.session.add(tag)
    db.session.commit()

    return jsonify(tag.to_dict()), 201


@tags_bp.route('/<int:tag_id>', methods=['PUT'])
def update_tag(tag_id):
    """Update a tag."""
    tag = Tag.query.get_or_404(tag_id)
    data = request.get_json()

    if 'name' in data:
        # Check if name is already taken by another tag
        existing = Tag.query.filter_by(name=data['name']).first()
        if existing and existing.id != tag_id:
            return jsonify({'error': 'Tag name already exists'}), 409
        tag.name = data['name']

    if 'color' in data:
        tag.color = data['color']

    db.session.commit()
    return jsonify(tag.to_dict())


@tags_bp.route('/<int:tag_id>', methods=['DELETE'])
def delete_tag(tag_id):
    """Delete a tag."""
    tag = Tag.query.get_or_404(tag_id)

    db.session.delete(tag)
    db.session.commit()

    return jsonify({'message': 'Tag deleted successfully'})
