import os
from flask import Blueprint, request, jsonify, send_file
from sqlalchemy import desc, asc
from ..models import db, Image
from ..services.thumbnail import generate_image_thumbnail

images_bp = Blueprint('images', __name__, url_prefix='/api/images')


@images_bp.route('', methods=['GET'])
def get_images():
    """Get image list with pagination and search."""
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 20, type=int)
    search = request.args.get('search', '')
    sort_by = request.args.get('sort_by', 'created_at')
    order = request.args.get('order', 'desc')
    source_id = request.args.get('source_id', type=int)

    query = Image.query

    if search:
        query = query.filter(Image.title.ilike(f'%{search}%'))

    if source_id:
        query = query.filter(Image.source_id == source_id)

    # Sorting
    sort_column = getattr(Image, sort_by, Image.created_at)
    if order == 'desc':
        query = query.order_by(desc(sort_column))
    else:
        query = query.order_by(asc(sort_column))

    pagination = query.paginate(page=page, per_page=per_page, error_out=False)

    return jsonify({
        'items': [image.to_dict() for image in pagination.items],
        'total': pagination.total,
        'pages': pagination.pages,
        'current_page': page,
        'per_page': per_page
    })


@images_bp.route('/<int:image_id>', methods=['GET'])
def get_image(image_id):
    """Get image details."""
    image = Image.query.get_or_404(image_id)
    return jsonify(image.to_dict())


@images_bp.route('/<int:image_id>/file', methods=['GET'])
def get_image_file(image_id):
    """Get original image file."""
    image = Image.query.get_or_404(image_id)

    if not os.path.exists(image.path):
        return jsonify({'error': 'Image file not found'}), 404

    # Determine mimetype from extension
    ext = os.path.splitext(image.path)[1].lower()
    mimetype_map = {
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.png': 'image/png',
        '.gif': 'image/gif',
        '.webp': 'image/webp',
        '.bmp': 'image/bmp'
    }
    mimetype = mimetype_map.get(ext, 'application/octet-stream')

    return send_file(
        image.path,
        mimetype=mimetype,
        as_attachment=False
    )


@images_bp.route('/<int:image_id>/thumbnail', methods=['GET'])
def get_image_thumbnail(image_id):
    """Get image thumbnail."""
    image = Image.query.get_or_404(image_id)

    if image.thumbnail_path and os.path.exists(image.thumbnail_path):
        return send_file(
            image.thumbnail_path,
            mimetype='image/jpeg',
            as_attachment=False
        )

    # Generate thumbnail if not exists
    try:
        thumbnail_path = generate_image_thumbnail(image.path, image.id)
        image.thumbnail_path = thumbnail_path
        db.session.commit()
        return send_file(
            thumbnail_path,
            mimetype='image/jpeg',
            as_attachment=False
        )
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@images_bp.route('/<int:image_id>', methods=['DELETE'])
def delete_image(image_id):
    """Delete image record (not the file)."""
    image = Image.query.get_or_404(image_id)

    # Delete thumbnail if exists
    if image.thumbnail_path and os.path.exists(image.thumbnail_path):
        try:
            os.remove(image.thumbnail_path)
        except OSError:
            pass

    db.session.delete(image)
    db.session.commit()

    return jsonify({'message': 'Image deleted successfully'})


@images_bp.route('/<int:image_id>/favorite', methods=['POST'])
def toggle_favorite(image_id):
    """Toggle favorite status."""
    image = Image.query.get_or_404(image_id)
    image.is_favorite = not image.is_favorite
    db.session.commit()

    return jsonify({
        'message': 'Favorite status updated',
        'is_favorite': image.is_favorite
    })
