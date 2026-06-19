"""Database Models for fanhub"""

from datetime import datetime
from app import db


class Source(db.Model):
    """Media source configuration model"""
    __tablename__ = 'sources'

    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(128), nullable=False)
    type = db.Column(db.String(16), nullable=False, default='local')  # 'local' or 'nas'
    media_type = db.Column(db.String(16), nullable=False, default='all')  # 'all', 'video', 'image', 'douyin'
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
            'media_type': self.media_type,
            'path': self.path,
            'nas_config': self.nas_config,
            'scan_interval': self.scan_interval,
            'is_active': self.is_active,
            'last_scan_at': self.last_scan_at.isoformat() if self.last_scan_at else None,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'video_count': self.videos.count() if self.media_type != 'image' else 0,
            'image_count': self.images.count() if self.media_type != 'video' else 0
        }

    def __repr__(self):
        return f'<Source {self.name} ({self.type}, {self.media_type})>'


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
    is_liked = db.Column(db.Boolean, default=False)
    description = db.Column(db.Text, nullable=True)
    view_count = db.Column(db.Integer, default=0)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    file_modified_at = db.Column(db.DateTime, nullable=True)  # 文件实际修改时间，用于增量扫描

    watch_history = db.relationship('WatchHistory', backref='video', lazy='dynamic', cascade='all, delete-orphan')

    def to_dict(self, include_details=False):
        import os
        ext = os.path.splitext(self.path)[1].lower()
        # ExoPlayer 原生支持的格式
        native_formats = {'.mp4', '.m4v', '.mov', '.webm', '.ogv', '.ogg', '.mkv', '.3gp'}
        
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
            'is_liked': self.is_liked,
            'description': self.description,
            'view_count': self.view_count,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None,
            'file_modified_at': self.file_modified_at.isoformat() if self.file_modified_at else None,
            'format': ext,
            'requires_external_player': ext not in native_formats
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
    is_liked = db.Column(db.Boolean, default=False)
    view_count = db.Column(db.Integer, default=0)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    file_modified_at = db.Column(db.DateTime, nullable=True)  # 文件实际修改时间，用于增量扫描

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
            'is_liked': self.is_liked,
            'view_count': self.view_count,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'file_modified_at': self.file_modified_at.isoformat() if self.file_modified_at else None
        }
        if include_details:
            data['source'] = self.source.to_dict() if self.source else None
        return data

    def __repr__(self):
        return f'<Image {self.title}>'


class WatchHistory(db.Model):
    """Watch history model for tracking user playback"""
    __tablename__ = 'watch_history'

    id = db.Column(db.Integer, primary_key=True)
    video_id = db.Column(db.Integer, db.ForeignKey('videos.id'), nullable=False)
    playback_position = db.Column(db.Integer, default=0)
    is_completed = db.Column(db.Boolean, default=False)
    watched_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    def to_dict(self):
        return {
            'id': self.id,
            'video_id': self.video_id,
            'playback_position': self.playback_position,
            'is_completed': self.is_completed,
            'watched_at': self.watched_at.isoformat() if self.watched_at else None,
            'video': self.video.to_dict() if self.video else None
        }

    def __repr__(self):
        return f'<WatchHistory video_id={self.video_id} position={self.playback_position}>'


class ThumbnailCache(db.Model):
    """Thumbnail cache metadata for optimization"""
    __tablename__ = 'thumbnail_cache'

    id = db.Column(db.Integer, primary_key=True)
    media_type = db.Column(db.String(16), nullable=False)
    media_id = db.Column(db.Integer, nullable=False)
    file_path = db.Column(db.String(512), nullable=False)
    file_hash = db.Column(db.String(64), nullable=True)
    file_size = db.Column(db.BigInteger, nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    expires_at = db.Column(db.DateTime, nullable=True)

    __table_args__ = (
        db.UniqueConstraint('media_type', 'media_id', name='uq_thumbnail_media'),
        db.Index('idx_thumbnail_expires', 'expires_at'),
    )

    def to_dict(self):
        return {
            'id': self.id,
            'media_type': self.media_type,
            'media_id': self.media_id,
            'file_path': self.file_path,
            'file_size': self.file_size,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'expires_at': self.expires_at.isoformat() if self.expires_at else None
        }

    def __repr__(self):
        return f'<ThumbnailCache {self.media_type}_{self.media_id}>'


class WordCloudCache(db.Model):
    """WordCloud cache for storing generated wordcloud data"""
    __tablename__ = 'wordcloud_cache'

    id = db.Column(db.Integer, primary_key=True)
    cache_key = db.Column(db.String(64), nullable=False, unique=True)
    data = db.Column(db.JSON, nullable=False, default=list)
    version = db.Column(db.Integer, default=1)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    def to_dict(self):
        return {
            'id': self.id,
            'cache_key': self.cache_key,
            'data': self.data,
            'version': self.version,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None
        }

    @staticmethod
    def get_global_version():
        """Get or create global cache version"""
        version_record = WordCloudCache.query.filter_by(cache_key='__global_version__').first()
        if not version_record:
            version_record = WordCloudCache(
                cache_key='__global_version__',
                data=[],
                version=1
            )
            db.session.add(version_record)
            db.session.commit()
        return version_record.version

    @staticmethod
    def increment_global_version():
        """Increment global cache version to invalidate all caches"""
        version_record = WordCloudCache.query.filter_by(cache_key='__global_version__').first()
        if version_record:
            version_record.version += 1
        else:
            version_record = WordCloudCache(
                cache_key='__global_version__',
                data=[],
                version=1
            )
            db.session.add(version_record)
        db.session.commit()
        return version_record.version

    def __repr__(self):
        return f'<WordCloudCache {self.cache_key} v{self.version}>'


class ScanLog(db.Model):
    """Scan log for tracking source scanning operations"""
    __tablename__ = 'scan_logs'

    id = db.Column(db.Integer, primary_key=True)
    source_id = db.Column(db.Integer, db.ForeignKey('sources.id'), nullable=False)
    source_name = db.Column(db.String(128), nullable=False)
    status = db.Column(db.String(16), nullable=False, default='running')  # 'running', 'completed', 'failed'
    started_at = db.Column(db.DateTime, default=datetime.utcnow)
    completed_at = db.Column(db.DateTime, nullable=True)
    videos_added = db.Column(db.Integer, default=0)
    videos_updated = db.Column(db.Integer, default=0)
    videos_removed = db.Column(db.Integer, default=0)
    images_added = db.Column(db.Integer, default=0)
    images_updated = db.Column(db.Integer, default=0)
    images_removed = db.Column(db.Integer, default=0)
    errors = db.Column(db.JSON, default=list)  # 错误信息列表
    details = db.Column(db.JSON, default=list)  # 详细操作记录 [{type: 'added|removed|updated', path: '...', media_type: 'video|image'}]

    def to_dict(self):
        return {
            'id': self.id,
            'source_id': self.source_id,
            'source_name': self.source_name,
            'status': self.status,
            'started_at': self.started_at.isoformat() if self.started_at else None,
            'completed_at': self.completed_at.isoformat() if self.completed_at else None,
            'videos_added': self.videos_added,
            'videos_updated': self.videos_updated,
            'videos_removed': self.videos_removed,
            'images_added': self.images_added,
            'images_updated': self.images_updated,
            'images_removed': self.images_removed,
            'errors': self.errors or [],
            'details': self.details or []
        }

    def __repr__(self):
        return f'<ScanLog {self.source_name} {self.status}>'
