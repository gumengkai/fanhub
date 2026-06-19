"""Peak Library API Routes"""

import os
import subprocess
import shutil
from flask import Blueprint, request, jsonify, send_file, Response, current_app
from sqlalchemy import desc, asc, func
from ..models import db, Video, Source
import io
from PIL import Image as PILImage, ImageDraw, ImageFont

peak_bp = Blueprint('peak', __name__, url_prefix='/api/peak')


def get_peak_source_ids():
    """Get all source IDs with media_type='peak'"""
    sources = Source.query.filter_by(media_type='peak', is_active=True).all()
    return [s.id for s in sources]


def generate_placeholder(text="No Video", color=(51, 51, 51), text_color=(255, 255, 255)):
    """Generate a placeholder image."""
    img = PILImage.new('RGB', (320, 180), color=color)
    draw = ImageDraw.Draw(img)
    try:
        font = PILImage.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 20)
    except:
        font = PILImage.load_default()

    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    x = (320 - text_width) // 2
    y = (180 - text_height) // 2
    draw.text((x, y), text, fill=text_color, font=font)

    img_io = io.BytesIO()
    img.save(img_io, 'JPEG', quality=85)
    img_io.seek(0)
    return img_io


@peak_bp.route('', methods=['GET'])
def get_peak_videos():
    """Get peak library video list with pagination and filters."""
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 50, type=int)
    search = request.args.get('search', '')
    sort_by = request.args.get('sort_by', 'random')
    order = request.args.get('order', 'desc')
    favorite = request.args.get('favorite')
    liked = request.args.get('liked')
    unwatched = request.args.get('unwatched')

    # Get peak source IDs
    source_ids = get_peak_source_ids()
    if not source_ids:
        return jsonify({
            'items': [],
            'total': 0,
            'pages': 0,
            'current_page': page,
            'per_page': per_page
        })

    query = Video.query.filter(Video.source_id.in_(source_ids))

    if search:
        query = query.filter(
            db.or_(
                Video.title.ilike(f'%{search}%'),
                Video.path.ilike(f'%{search}%')
            )
        )

    if favorite is not None:
        is_fav = str(favorite).lower() in ('true', '1', 'yes', 'on')
        query = query.filter(Video.is_favorite == is_fav)

    if liked is not None:
        is_liked_val = str(liked).lower() in ('true', '1', 'yes', 'on')
        query = query.filter(Video.is_liked == is_liked_val)

    if unwatched is not None:
        is_unwatched = str(unwatched).lower() in ('true', '1', 'yes', 'on')
        if is_unwatched:
            from ..models import WatchHistory
            query = query.outerjoin(Video.watch_history).filter(Video.watch_history.id == None)

    if sort_by == 'random':
        # Random ordering - use database random function
        query = query.order_by(func.random())
    else:
        if sort_by == 'view_count':
            sort_column = Video.view_count
        elif sort_by == 'duration':
            sort_column = Video.duration
        elif sort_by == 'file_size':
            sort_column = Video.file_size
        elif sort_by == 'title':
            sort_column = Video.title
        else:
            sort_column = Video.created_at

        if order == 'desc':
            query = query.order_by(desc(sort_column))
        else:
            query = query.order_by(asc(sort_column))

    pagination = query.paginate(page=page, per_page=per_page, error_out=False)

    return jsonify({
        'items': [video.to_dict() for video in pagination.items],
        'total': pagination.total,
        'pages': pagination.pages,
        'current_page': page,
        'per_page': per_page
    })


@peak_bp.route('/<int:video_id>', methods=['GET'])
def get_peak_video(video_id):
    """Get peak video details."""
    source_ids = get_peak_source_ids()
    video = Video.query.filter(Video.id == video_id, Video.source_id.in_(source_ids)).first_or_404()
    video.view_count += 1
    db.session.commit()
    return jsonify(video.to_dict(include_details=True))


@peak_bp.route('/<int:video_id>/stream', methods=['GET'])
def stream_peak_video(video_id):
    """Stream peak video file with real-time transcoding for non-native formats."""
    source_ids = get_peak_source_ids()
    video = Video.query.filter(Video.id == video_id, Video.source_id.in_(source_ids)).first_or_404()

    if not os.path.exists(video.path):
        return jsonify({'error': 'Video file not found'}), 404

    # 获取文件扩展名
    ext = os.path.splitext(video.path)[1].lower()
    
    # 浏览器原生支持的格式直接传输
    native_formats = {'.mp4', '.m4v', '.mov', '.webm', '.ogv', '.ogg'}
    
    if ext in native_formats:
        # 原生支持格式，直接传输
        range_header = request.headers.get('Range', None)
        file_size = os.path.getsize(video.path)

        if range_header:
            byte_start, byte_end = range_header.replace('bytes=', '').split('-')
            byte_start = int(byte_start) if byte_start else 0
            byte_end = int(byte_end) if byte_end else file_size - 1
            remaining = byte_end - byte_start + 1

            def generate(remaining_bytes):
                with open(video.path, 'rb') as f:
                    f.seek(byte_start)
                    while remaining_bytes > 0:
                        chunk_size = min(65536, remaining_bytes)
                        data = f.read(chunk_size)
                        if not data:
                            break
                        remaining_bytes -= len(data)
                        yield data

            headers = {
                'Content-Range': f'bytes {byte_start}-{byte_end}/{file_size}',
                'Accept-Ranges': 'bytes',
                'Content-Length': str(remaining),
                'Content-Type': 'video/mp4',
                'Cache-Control': 'public, max-age=3600'
            }
            return Response(generate(remaining), 206, headers)
        else:
            return send_file(
                video.path,
                mimetype='video/mp4',
                as_attachment=False
            )
    else:
        # 非原生格式，使用 FFmpeg 实时转码为 MP4
        return transcode_video_peak(video.path)


def get_transcoded_path(video_path):
    """获取转码后的视频路径。"""
    cache_dir = '/app/storage/transcoded'
    os.makedirs(cache_dir, exist_ok=True)
    
    import hashlib
    file_hash = hashlib.md5(video_path.encode()).hexdigest()
    cache_path = os.path.join(cache_dir, f'{file_hash}.mp4')
    
    if os.path.exists(cache_path):
        if os.path.getmtime(cache_path) >= os.path.getmtime(video_path):
            return cache_path
    
    return None


def transcode_video_peak(video_path):
    """使用 FFmpeg 转码视频为 MP4 格式（Peak库）- 快速模式。"""
    if not shutil.which('ffmpeg'):
        return jsonify({'error': 'FFmpeg not available for transcoding'}), 500
    
    try:
        cache_path = get_transcoded_path(video_path)
        if cache_path and os.path.exists(cache_path):
            return serve_with_range_peak(cache_path)
        
        cache_dir = '/app/storage/transcoded'
        os.makedirs(cache_dir, exist_ok=True)
        
        import hashlib
        file_hash = hashlib.md5(video_path.encode()).hexdigest()
        cache_path = os.path.join(cache_dir, f'{file_hash}.mp4')
        
        # 使用更快的预设来加速转码
        cmd = [
            'ffmpeg',
            '-i', video_path,
            '-c:v', 'libx264',
            '-preset', 'ultrafast',  # 最快预设
            '-tune', 'fastdecode',   # 优化解码速度
            '-crf', '28',            # 稍低质量但更快
            '-c:a', 'aac',
            '-b:a', '96k',           # 稍低音频质量
            '-movflags', '+faststart',
            '-y',
            cache_path
        ]
        
        subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        
        import time
        time.sleep(0.5)  # 减少等待时间
        
        if os.path.exists(cache_path) and os.path.getsize(cache_path) > 0:
            max_wait = 15  # 减少最大等待时间
            waited = 0
            while waited < max_wait:
                size = os.path.getsize(cache_path)
                result = subprocess.run(['pgrep', '-f', f'ffmpeg.*{file_hash}'], capture_output=True)
                if result.returncode != 0:
                    break
                if size > 512 * 1024:  # 降低阈值到 512KB
                    break
                time.sleep(0.3)
                waited += 0.3
            
            return serve_with_range_peak(cache_path)
        else:
            return transcode_streaming_peak(video_path)
        
    except Exception as e:
        current_app.logger.error(f"Transcoding error: {e}")
        return jsonify({'error': f'Transcoding failed: {str(e)}'}), 500


def transcode_streaming_peak(video_path):
    """实时流式转码（后备方案）。"""
    try:
        cmd = [
            'ffmpeg',
            '-i', video_path,
            '-c:v', 'libx264',
            '-preset', 'ultrafast',
            '-tune', 'zerolatency',
            '-c:a', 'aac',
            '-b:a', '128k',
            '-movflags', 'frag_keyframe+empty_moov+faststart',
            '-f', 'mp4',
            'pipe:1'
        ]
        
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, bufsize=8192)
        
        def generate():
            try:
                while True:
                    chunk = process.stdout.read(65536)
                    if not chunk:
                        break
                    yield chunk
            finally:
                process.terminate()
                process.wait()
        
        return Response(generate(), mimetype='video/mp4', headers={
            'Content-Type': 'video/mp4',
            'Cache-Control': 'no-cache',
            'X-Transcoded': 'streaming'
        })
    except Exception as e:
        return jsonify({'error': f'Streaming failed: {str(e)}'}), 500


def serve_with_range_peak(file_path):
    """支持 Range 请求。"""
    range_header = request.headers.get('Range', None)
    file_size = os.path.getsize(file_path)
    
    if range_header:
        byte_start, byte_end = range_header.replace('bytes=', '').split('-')
        byte_start = int(byte_start) if byte_start else 0
        byte_end = int(byte_end) if byte_end else file_size - 1
        remaining = byte_end - byte_start + 1
        
        def generate(remaining_bytes):
            with open(file_path, 'rb') as f:
                f.seek(byte_start)
                while remaining_bytes > 0:
                    chunk_size = min(65536, remaining_bytes)
                    data = f.read(chunk_size)
                    if not data:
                        break
                    remaining_bytes -= len(data)
                    yield data
        
        headers = {
            'Content-Range': f'bytes {byte_start}-{byte_end}/{file_size}',
            'Accept-Ranges': 'bytes',
            'Content-Length': str(remaining),
            'Content-Type': 'video/mp4',
            'Cache-Control': 'public, max-age=3600'
        }
        return Response(generate(remaining), 206, headers)
    else:
        return send_file(file_path, mimetype='video/mp4', as_attachment=False)


@peak_bp.route('/<int:video_id>/thumbnail', methods=['GET'])
def get_peak_thumbnail(video_id):
    """Get peak video thumbnail."""
    source_ids = get_peak_source_ids()
    video = Video.query.filter(Video.id == video_id, Video.source_id.in_(source_ids)).first_or_404()

    if video.thumbnail_path and os.path.exists(video.thumbnail_path):
        response = send_file(
            video.thumbnail_path,
            mimetype='image/jpeg',
            as_attachment=False
        )
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        return response

    if not os.path.exists(video.path):
        img_io = generate_placeholder("Video Not Found", (100, 100, 100), (255, 100, 100))
        response = send_file(img_io, mimetype='image/jpeg')
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        return response

    try:
        from ..services.thumbnail import generate_video_thumbnail
        thumbnail_path = generate_video_thumbnail(video.path, video.id)
        video.thumbnail_path = thumbnail_path
        db.session.commit()
        response = send_file(
            thumbnail_path,
            mimetype='image/jpeg',
            as_attachment=False
        )
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        return response
    except Exception as e:
        current_app.logger.error(f"Thumbnail error for peak video {video_id}: {e}")
        img_io = generate_placeholder("Click to Play", (30, 30, 50), (254, 44, 85))
        response = send_file(img_io, mimetype='image/jpeg')
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        return response


@peak_bp.route('/<int:video_id>', methods=['DELETE'])
def delete_peak_video(video_id):
    """Delete peak video record and file."""
    source_ids = get_peak_source_ids()
    video = Video.query.filter(Video.id == video_id, Video.source_id.in_(source_ids)).first_or_404()

    if video.thumbnail_path and os.path.exists(video.thumbnail_path):
        try:
            os.remove(video.thumbnail_path)
        except OSError:
            pass

    if video.path and os.path.exists(video.path):
        try:
            os.remove(video.path)
        except OSError:
            pass

    db.session.delete(video)
    db.session.commit()
    return jsonify({'message': 'Peak video deleted successfully'})


@peak_bp.route('/<int:video_id>/like', methods=['POST'])
def toggle_peak_like(video_id):
    """Toggle like status for peak video."""
    source_ids = get_peak_source_ids()
    video = Video.query.filter(Video.id == video_id, Video.source_id.in_(source_ids)).first_or_404()
    video.is_liked = not video.is_liked
    db.session.commit()
    return jsonify({
        'message': 'Like status updated',
        'is_liked': video.is_liked
    })


@peak_bp.route('/<int:video_id>/favorite', methods=['POST'])
def toggle_peak_favorite(video_id):
    """Toggle favorite status for peak video."""
    source_ids = get_peak_source_ids()
    video = Video.query.filter(Video.id == video_id, Video.source_id.in_(source_ids)).first_or_404()
    video.is_favorite = not video.is_favorite
    db.session.commit()
    return jsonify({
        'message': 'Favorite status updated',
        'is_favorite': video.is_favorite
    })


@peak_bp.route('/stats', methods=['GET'])
def get_peak_stats():
    """Get peak library statistics."""
    source_ids = get_peak_source_ids()
    if not source_ids:
        return jsonify({
            'total': 0,
            'liked': 0,
            'favorite': 0,
            'total_size': 0,
            'total_duration': 0
        })

    total = Video.query.filter(Video.source_id.in_(source_ids)).count()
    liked = Video.query.filter(Video.source_id.in_(source_ids), Video.is_liked == True).count()
    favorite = Video.query.filter(Video.source_id.in_(source_ids), Video.is_favorite == True).count()
    total_size = db.session.query(func.sum(Video.file_size)).filter(
        Video.source_id.in_(source_ids)
    ).scalar() or 0
    total_duration = db.session.query(func.sum(Video.duration)).filter(
        Video.source_id.in_(source_ids)
    ).scalar() or 0

    return jsonify({
        'total': total,
        'liked': liked,
        'favorite': favorite,
        'total_size': total_size,
        'total_duration': total_duration
    })


@peak_bp.route('/<int:video_id>/history', methods=['POST', 'PUT'])
def update_peak_history(video_id):
    """Update watch progress for peak video."""
    source_ids = get_peak_source_ids()
    video = Video.query.filter(Video.id == video_id, Video.source_id.in_(source_ids)).first_or_404()
    data = request.get_json()
    playback_position = data.get('playback_position', 0)
    duration = data.get('duration') or 0
    is_completed = data.get('is_completed', False)

    from ..models import WatchHistory
    existing = WatchHistory.query.filter_by(video_id=video_id).first()
    if not existing:
        video.view_count += 1

    history = WatchHistory.query.filter_by(video_id=video_id).first()
    if history:
        history.playback_position = playback_position
        history.is_completed = is_completed or (duration > 0 and playback_position >= duration * 0.9)
    else:
        history = WatchHistory(
            video_id=video_id,
            playback_position=playback_position,
            is_completed=is_completed or (duration > 0 and playback_position >= duration * 0.9)
        )
        db.session.add(history)

    db.session.commit()
    return jsonify(history.to_dict())