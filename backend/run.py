#!/usr/bin/env python3
"""FunHub Backend Application Entry Point"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from app import create_app, db
from app.models import Video, Image, Source

app = create_app()

@app.shell_context_processor
def make_shell_context():
    return {
        'db': db,
        'Video': Video,
        'Image': Image,
        'Source': Source
    }

if __name__ == '__main__':
    with app.app_context():
        db.create_all()
        print("Database initialized successfully!")

    app.run(
        host='0.0.0.0',
        port=5000,
        debug=True,
        use_reloader=True
    )
