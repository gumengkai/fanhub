"""Database Models for FunHub"""

from datetime import datetime
from app import db

# Association table for video tags
video_tags = db.Table('video_tags',
    db.Column('video_id', db.Integer, db.ForeignKey('videos.id'), primary_key=True),
    db.Column('tag_id', db.Integer, db.ForeignKey('tags.id'), primary_key=True)
)


class Source(db.Model):
    """Media source configuration model"""
    __tablename__ = 'sources'

    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(128), nullable=False)
    type = db.Column(db.String(16), nullable=False, default='local')  # 'local' or 'nas'
    path = db.Column(db.String(512), nullable=False)
    nas_config = db.Column(db.JSON, nullable=True)
    scan_interval = db.Column(db.Integer, default=60)
    is_active = db.Column(db.Boolean, default=True)
    last_scan_at = db.Column(db.DateTime, nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    videos = db.relationship('Video', backref='source', lazy='dynamic', cascade='all, delete-orphan')
    images = db.relationship('Image', backref='source', lazy='dynamic', cascade='all, delete-orphan')

    def to_dict(self):
        return {
            'id': self.id,
            'name': self.name,
            'type': self.type,
            'path': self.path,
            'nas_config': self.nas_config,
            'scan_interval': self.scan_interval,
            'is_active': self.is_active,
            'last_scan_at': self.last_scan_at.isoformat() if self.last_scan_at else None,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'video_count': self.videos.count(),
            'image_count': self.images.count()
        }

    def __repr__(self):
        return f'<Source {self.name} ({self.type})>'


class Tag(db.Model):
    """Tag model for categorizing videos"""
    __tablename__ = 'tags'

    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(64), nullable=False, unique=True)
    color = db.Column(db.String(7), default='#1890ff')  # Hex color
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    videos = db.relationship('Video', secondary=video_tags, back_populates='tags')

    def to_dict(self):
        return {
            'id': self.id,
            'name': self.name,
            'color': self.color,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'video_count': len(self.videos)
        }

    def __repr__(self):
        return f'<Tag {self.name}>'


class Video(db.Model):
    """Video media model"""
    __tablename__ = 'videos'

    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(256), nullable=False)
    path = db.Column(db.String(512), nullable=False, unique=True)
    source_id = db.Column(db.Integer, db.ForeignKey('sources.id'), nullable=False)
    file_size = db.Column(db.BigInteger, nullable=True)
    duration = db.Column(db.Integer, nullable=True)
    width = db.Column(db.Integer, nullable=True)
    height = db.Column(db.Integer, nullable=True)
    thumbnail_path = db.Column(db.String(512), nullable=True)
    is_favorite = db.Column(db.Boolean, default=False)
    description = db.Column(db.Text, nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    tags = db.relationship('Tag', secondary=video_tags, back_populates='videos')

    def to_dict(self, include_details=False):
        data = {
            'id': self.id,
            'title': self.title,
            'path': self.path,
            'source_id': self.source_id,
            'file_size': self.file_size,
            'duration': self.duration,
            'width': self.width,
            'height': self.height,
            'thumbnail_path': self.thumbnail_path,
            'is_favorite': self.is_favorite,
            'description': self.description,
            'tags': [tag.to_dict() for tag in self.tags] if self.tags else [],
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None
        }
        if include_details:
            data['source'] = self.source.to_dict() if self.source else None
        return data

    def __repr__(self):
        return f'<Video {self.title}>'


class Image(db.Model):
    """Image media model"""
    __tablename__ = 'images'

    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(256), nullable=False)
    path = db.Column(db.String(512), nullable=False, unique=True)
    source_id = db.Column(db.Integer, db.ForeignKey('sources.id'), nullable=False)
    file_size = db.Column(db.BigInteger, nullable=True)
    width = db.Column(db.Integer, nullable=True)
    height = db.Column(db.Integer, nullable=True)
    thumbnail_path = db.Column(db.String(512), nullable=True)
    is_favorite = db.Column(db.Boolean, default=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self, include_details=False):
        data = {
            'id': self.id,
            'title': self.title,
            'path': self.path,
            'source_id': self.source_id,
            'file_size': self.file_size,
            'width': self.width,
            'height': self.height,
            'thumbnail_path': self.thumbnail_path,
            'is_favorite': self.is_favorite,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }
        if include_details:
            data['source'] = self.source.to_dict() if self.source else None
        return data

    def __repr__(self):
        return f'<Image {self.title}>'
