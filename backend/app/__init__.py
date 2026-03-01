"""Flask Application Factory"""

from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS

from config import Config

db = SQLAlchemy()


def create_app(config_class=Config):
    """Application factory pattern"""
    app = Flask(__name__)
    app.config.from_object(config_class)

    db.init_app(app)

    CORS(app, origins=config_class.CORS_ORIGINS, supports_credentials=True)

    from app.routes.videos import videos_bp
    from app.routes.images import images_bp
    from app.routes.sources import sources_bp
    from app.routes.favorites import favorites_bp
    from app.routes.tags import tags_bp
    from app.routes.history import history_bp

    app.register_blueprint(videos_bp)
    app.register_blueprint(images_bp)
    app.register_blueprint(sources_bp)
    app.register_blueprint(favorites_bp)
    app.register_blueprint(tags_bp)
    app.register_blueprint(history_bp)

    @app.route('/api/health')
    def health_check():
        return {'status': 'ok', 'service': 'funhub-backend'}

    return app
