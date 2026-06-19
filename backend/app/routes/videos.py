import os
import subprocess
import shutil
from flask import Blueprint, request, jsonify, send_file, Response, current_app
from sqlalchemy import desc, asc, func
from ..models import db, Video, WatchHistory, Source
import io
from PIL import Image as PILImage, ImageDraw, ImageFont

videos_bp = Blueprint('videos', __name__, url_prefix='/api/videos')


def generate_placeholder(text="No Video", color=(51, 51, 51), text_color=(255, 255, 255)):
    """Generate a placeholder image."""
    img = PILImage.new('RGB', (320, 180), color=color)
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 20)
    except:
        font = ImageFont.load_default()

    # Center text
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


@videos_bp.route('', methods=['GET'])
def get_videos():
    """Get video list with pagination, search, and sorting."""
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 20, type=int)
    search = request.args.get('search', '')
    sort_by = request.args.get('sort_by', 'created_at')
    order = request.args.get('order', 'desc')
    source_id = request.args.get('source_id', type=int)
    favorite = request.args.get('favorite')
    unwatched = request.args.get('unwatched')
    liked = request.args.get('liked')

    # 获取非douyin类型的来源ID列表
    non_douyin_sources = Source.query.filter(Source.media_type != 'douyin').all()
    non_douyin_source_ids = [s.id for s in non_douyin_sources]

    # 如果没有非douyin来源，返回空结果
    if not non_douyin_source_ids:
        return jsonify({
            'items': [],
            'total': 0,
            'pages': 0,
            'current_page': page,
            'per_page': per_page
        })

    query = Video.query.filter(Video.source_id.in_(non_douyin_source_ids))

    if search:
        query = query.filter(
            db.or_(
                Video.title.ilike(f'%{search}%'),
                Video.path.ilike(f'%{search}%')
            )
        )

    if source_id:
        query = query.filter(Video.source_id == source_id)

    if favorite is not None:
        # 处理 'true', '1', 1 等值为 True
        is_fav = str(favorite).lower() in ('true', '1', 'yes', 'on')
        query = query.filter(Video.is_favorite == is_fav)

    if liked is not None:
        is_liked_val = str(liked).lower() in ('true', '1', 'yes', 'on')
        query = query.filter(Video.is_liked == is_liked_val)

    if unwatched is not None:
        # 筛选从未观看的视频（没有观看历史记录）
        is_unwatched = str(unwatched).lower() in ('true', '1', 'yes', 'on')
        if is_unwatched:
            query = query.outerjoin(WatchHistory).filter(WatchHistory.id == None)

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


@videos_bp.route('/<int:video_id>', methods=['GET'])
def get_video(video_id):
    """Get video details."""
    video = Video.query.get_or_404(video_id)
    video.view_count += 1
    db.session.commit()
    return jsonify(video.to_dict(include_details=True))


@videos_bp.route('/<int:video_id>', methods=['PUT'])
def update_video(video_id):
    """Update video metadata."""
    video = Video.query.get_or_404(video_id)
    data = request.get_json()

    if 'title' in data:
        video.title = data['title']
    if 'description' in data:
        video.description = data['description']
    if 'is_favorite' in data:
        video.is_favorite = data['is_favorite']

    db.session.commit()
    return jsonify(video.to_dict(include_details=True))


@videos_bp.route('/<int:video_id>/stream', methods=['GET'])
def stream_video(video_id):
    """Stream video file. Only native formats are supported via streaming."""
    video = Video.query.get_or_404(video_id)

    if not os.path.exists(video.path):
        return jsonify({'error': 'Video file not found'}), 404

    # 获取文件扩展名
    ext = os.path.splitext(video.path)[1].lower()
    
    # 浏览器/ExoPlayer 原生支持的格式直接传输
    native_formats = {'.mp4', '.m4v', '.mov', '.webm', '.ogv', '.ogg', '.mkv', '.3gp'}
    
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
        # 非原生格式（如 AVI），使用 FFmpeg 实时流式转码
        return transcode_streaming(video.path)


def get_transcoded_path(video_path):
    """获取转码后的视频路径，如果不存在则进行转码。"""
    # 创建转码缓存目录
    cache_dir = '/app/storage/transcoded'
    os.makedirs(cache_dir, exist_ok=True)
    
    # 生成缓存文件名（基于原文件路径的 hash）
    import hashlib
    file_hash = hashlib.md5(video_path.encode()).hexdigest()
    cache_path = os.path.join(cache_dir, f'{file_hash}.mp4')
    
    # 如果缓存文件已存在且比原文件新，直接返回
    if os.path.exists(cache_path):
        if os.path.getmtime(cache_path) >= os.path.getmtime(video_path):
            return cache_path
    
    return None


def transcode_video(video_path):
    """使用 FFmpeg 转码视频为 MP4 格式（支持进度条）。"""
    # 检查 FFmpeg 是否可用
    if not shutil.which('ffmpeg'):
        return jsonify({'error': 'FFmpeg not available for transcoding'}), 500
    
    try:
        # 检查是否已有转码缓存
        cache_path = get_transcoded_path(video_path)
        if cache_path and os.path.exists(cache_path):
            # 使用缓存文件，支持 range 请求
            return serve_with_range(cache_path)
        
        # 创建转码缓存目录
        cache_dir = '/app/storage/transcoded'
        os.makedirs(cache_dir, exist_ok=True)
        
        # 生成缓存文件名
        import hashlib
        file_hash = hashlib.md5(video_path.encode()).hexdigest()
        cache_path = os.path.join(cache_dir, f'{file_hash}.mp4')
        
        # 启动后台转码（异步）
        cmd = [
            'ffmpeg',
            '-i', video_path,
            '-c:v', 'libx264',
            '-preset', 'fast',  # 平衡速度和质量
            '-crf', '23',       # 质量设置
            '-c:a', 'aac',
            '-b:a', '128k',
            '-movflags', '+faststart',  # 支持流式传输
            '-y',               # 覆盖输出文件
            cache_path
        ]
        
        # 启动转码进程（不等待完成）
        subprocess.Popen(
            cmd,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL
        )
        
        # 等待一小段时间让转码开始并生成文件头
        import time
        time.sleep(1)
        
        # 检查转码是否已经开始
        if os.path.exists(cache_path) and os.path.getsize(cache_path) > 0:
            # 等待文件足够大（至少 1MB 或转码完成）
            max_wait = 30  # 最多等待 30 秒
            waited = 0
            while waited < max_wait:
                size = os.path.getsize(cache_path)
                # 检查转码是否完成（通过检查进程）
                result = subprocess.run(['pgrep', '-f', f'ffmpeg.*{file_hash}'], capture_output=True)
                if result.returncode != 0:  # 进程已结束
                    break
                if size > 1024 * 1024:  # 至少 1MB
                    break
                time.sleep(0.5)
                waited += 0.5
            
            # 返回正在生成的文件（支持 range 请求）
            return serve_with_range(cache_path)
        else:
            # 转码启动失败，回退到实时流式转码
            return transcode_streaming(video_path)
        
    except Exception as e:
        current_app.logger.error(f"Transcoding error: {e}")
        return jsonify({'error': f'Transcoding failed: {str(e)}'}), 500


def transcode_video_fast(video_path):
    """使用 FFmpeg 快速转码视频为 MP4 格式（低质量但快速）。"""
    # 检查 FFmpeg 是否可用
    if not shutil.which('ffmpeg'):
        return jsonify({'error': 'FFmpeg not available for transcoding'}), 500
    
    try:
        # 检查是否已有转码缓存
        cache_path = get_transcoded_path(video_path)
        if cache_path and os.path.exists(cache_path):
            # 使用缓存文件，支持 range 请求
            return serve_with_range(cache_path)
        
        # 创建转码缓存目录
        cache_dir = '/app/storage/transcoded'
        os.makedirs(cache_dir, exist_ok=True)
        
        # 生成缓存文件名
        import hashlib
        file_hash = hashlib.md5(video_path.encode()).hexdigest()
        cache_path = os.path.join(cache_dir, f'{file_hash}.mp4')
        
        # 启动快速转码（异步，使用更快的预设）
        cmd = [
            'ffmpeg',
            '-i', video_path,
            '-c:v', 'libx264',
            '-preset', 'ultrafast',  # 最快预设
            '-tune', 'fastdecode',   # 优化解码速度
            '-crf', '28',            # 稍低质量但更快
            '-c:a', 'aac',
            '-b:a', '96k',           # 稍低音频质量
            '-movflags', '+faststart',  # 支持流式传输
            '-y',               # 覆盖输出文件
            cache_path
        ]
        
        # 启动转码进程（不等待完成）
        subprocess.Popen(
            cmd,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL
        )
        
        # 等待一小段时间让转码开始并生成文件头
        import time
        time.sleep(0.5)
        
        # 检查转码是否已经开始
        if os.path.exists(cache_path) and os.path.getsize(cache_path) > 0:
            # 等待文件足够大（至少 512KB 或转码完成）
            max_wait = 15  # 最多等待 15 秒
            waited = 0
            while waited < max_wait:
                size = os.path.getsize(cache_path)
                # 检查转码是否完成（通过检查进程）
                result = subprocess.run(['pgrep', '-f', f'ffmpeg.*{file_hash}'], capture_output=True)
                if result.returncode != 0:  # 进程已结束
                    break
                if size > 512 * 1024:  # 至少 512KB
                    break
                time.sleep(0.3)
                waited += 0.3
            
            # 返回正在生成的文件（支持 range 请求）
            return serve_with_range(cache_path)
        else:
            # 转码启动失败，回退到实时流式转码
            return transcode_streaming(video_path)
        
    except Exception as e:
        current_app.logger.error(f"Transcoding error: {e}")
        return jsonify({'error': f'Transcoding failed: {str(e)}'}), 500


def transcode_streaming(video_path):
    """实时流式转码（作为后备方案）。"""
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
        
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            bufsize=8192
        )
        
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
        
        return Response(
            generate(),
            mimetype='video/mp4',
            headers={
                'Content-Type': 'video/mp4',
                'Cache-Control': 'no-cache',
                'X-Transcoded': 'streaming'
            }
        )
    except Exception as e:
        return jsonify({'error': f'Streaming failed: {str(e)}'}), 500


def serve_with_range(file_path):
    """支持 HTTP Range 请求的文件传输。"""
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
        return send_file(
            file_path,
            mimetype='video/mp4',
            as_attachment=False
        )


@videos_bp.route('/<int:video_id>', methods=['DELETE'])
def delete_video(video_id):
    """Delete video record and file from filesystem."""
    video = Video.query.get_or_404(video_id)

    # Delete thumbnail if exists
    if video.thumbnail_path and os.path.exists(video.thumbnail_path):
        try:
            os.remove(video.thumbnail_path)
        except OSError:
            pass

    # Delete video file if exists
    if video.path and os.path.exists(video.path):
        try:
            os.remove(video.path)
        except OSError:
            pass

    db.session.delete(video)
    db.session.commit()
    return jsonify({'message': 'Video deleted successfully'})


@videos_bp.route('/<int:video_id>/favorite', methods=['POST'])
def toggle_favorite(video_id):
    """Toggle favorite status."""
    video = Video.query.get_or_404(video_id)
    video.is_favorite = not video.is_favorite
    db.session.commit()
    return jsonify({
        'message': 'Favorite status updated',
        'is_favorite': video.is_favorite
    })


@videos_bp.route('/<int:video_id>/like', methods=['POST'])
def toggle_like(video_id):
    """Toggle like status."""
    video = Video.query.get_or_404(video_id)
    video.is_liked = not video.is_liked
    db.session.commit()
    return jsonify({
        'message': 'Like status updated',
        'is_liked': video.is_liked
    })


@videos_bp.route('/<int:video_id>/thumbnail', methods=['GET'])
def get_video_thumbnail(video_id):
    """Get video thumbnail with improved error handling and placeholder."""
    video = Video.query.get_or_404(video_id)

    # Check if thumbnail exists in filesystem
    if video.thumbnail_path and os.path.exists(video.thumbnail_path):
        response = send_file(
            video.thumbnail_path,
            mimetype='image/jpeg',
            as_attachment=False
        )
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        response.headers['Pragma'] = 'no-cache'
        response.headers['Expires'] = '0'
        return response

    # If video file doesn't exist, return placeholder
    if not os.path.exists(video.path):
        img_io = generate_placeholder("Video Not Found", (100, 100, 100), (255, 100, 100))
        response = send_file(img_io, mimetype='image/jpeg')
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        return response

    # Try to generate thumbnail
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
        response.headers['Pragma'] = 'no-cache'
        response.headers['Expires'] = '0'
        return response
    except Exception as e:
        current_app.logger.error(f"Thumbnail error for video {video_id}: {e}")
        img_io = generate_placeholder("Click to Play", (30, 30, 50), (251, 114, 153))
        response = send_file(img_io, mimetype='image/jpeg')
        response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
        return response


@videos_bp.route('/<int:video_id>/thumbnail', methods=['POST'])
def regenerate_thumbnail(video_id):
    """Regenerate video thumbnail."""
    video = Video.query.get_or_404(video_id)

    try:
        from ..services.thumbnail import generate_video_thumbnail
        thumbnail_path = generate_video_thumbnail(video.path, video.id)
        video.thumbnail_path = thumbnail_path
        db.session.commit()
        return jsonify({
            'message': 'Thumbnail regenerated successfully',
            'thumbnail_path': thumbnail_path
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@videos_bp.route('/thumbnails/fix', methods=['POST'])
def fix_missing_thumbnails():
    """Fix thumbnails for videos that don't have one."""
    videos = Video.query.filter(
        (Video.thumbnail_path == None) | (Video.thumbnail_path == '')
    ).all()

    results = {'success': 0, 'failed': 0, 'errors': []}

    for video in videos:
        try:
            if not os.path.exists(video.path):
                results['failed'] += 1
                results['errors'].append({'id': video.id, 'error': 'File not found'})
                continue

            from ..services.thumbnail import generate_video_thumbnail
            thumbnail_path = generate_video_thumbnail(video.path, video.id)
            video.thumbnail_path = thumbnail_path
            db.session.commit()
            results['success'] += 1

        except Exception as e:
            results['failed'] += 1
            results['errors'].append({'id': video.id, 'error': str(e)[:200]})

    return jsonify({
        'message': f'Fixed {results["success"]} thumbnails, {results["failed"]} failed',
        'results': results
    })


@videos_bp.route('/thumbnails/batch', methods=['POST'])
def batch_generate_thumbnails():
    """Generate thumbnails for multiple videos."""
    data = request.get_json()
    video_ids = data.get('video_ids', [])
    
    if not video_ids:
        return jsonify({'error': 'video_ids is required'}), 400
    
    videos = Video.query.filter(Video.id.in_(video_ids)).all()
    items = [{'id': v.id, 'path': v.path} for v in videos]
    
    from ..services.thumbnail import generate_batch_thumbnails
    
    def progress_callback(current, total, success, failed):
        current_app.logger.info(f"Thumbnail progress: {current}/{total}, success: {success}, failed: {failed}")
    
    results = generate_batch_thumbnails(items, 'video', progress_callback)
    return jsonify(results)


@videos_bp.route('/<int:video_id>/history', methods=['GET'])
def get_video_history(video_id):
    """Get watch history for a video."""
    history = WatchHistory.query.filter_by(video_id=video_id).first()
    if history:
        return jsonify(history.to_dict())
    return jsonify({'message': 'No history found'}), 404


@videos_bp.route('/<int:video_id>/history', methods=['POST', 'PUT'])
def update_video_history(video_id):
    """Update watch progress for a video."""
    video = Video.query.get_or_404(video_id)
    data = request.get_json()
    playback_position = data.get('playback_position', 0)
    duration = data.get('duration') or 0
    is_completed = data.get('is_completed', False)
    
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
