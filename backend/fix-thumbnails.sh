#!/bin/bash
# Fix missing thumbnails script

echo "🔧 Fixing missing thumbnails..."

cd "$(dirname "$0")"

# Activate virtual environment if exists
if [ -d "venv" ]; then
    source venv/bin/activate
fi

# Run the fix endpoint
echo "📡 Calling fix endpoint..."
curl -X POST http://localhost:5000/api/videos/thumbnails/fix

echo ""
echo "✅ Done!"
