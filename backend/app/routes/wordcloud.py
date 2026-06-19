"""WordCloud API - 从文件路径提取高频词汇"""

import os
import re
from flask import Blueprint, request, jsonify
from collections import Counter
from ..models import db, Video, Image, Source, WordCloudCache

wordcloud_bp = Blueprint('wordcloud', __name__, url_prefix='/api/wordcloud')

# 中文常见停用词
STOP_WORDS = set([
    '的', '和', '与', '或', '在', '是', '有', '为', '等', '及', '到', '从',
    '被', '把', '让', '给', '向', '对', '这', '那', '之', '也', '都', '就',
    '很', '能', '会', '可', '要', '应', '该', '已', '不', '没', '无', '如',
    '但', '而', '又', '则', '因', '所以', '因为', '如果', '虽然', '但是',
    '然而', '否则', '比如', '例如', '其中', '其他', '一些', '所有', '每个',
    '各个', '某种', '这样', '那样', '怎样', '如何', '什么', '哪里', '何时',
    '多少', '谁', '哪', '为何', '第', 'a', 'an', 'the', 'is', 'are', 'was',
    'were', 'be', 'been', 'being', 'have', 'has', 'had', 'do', 'does', 'did',
    'will', 'would', 'could', 'should', 'may', 'might', 'must', 'can', 'to',
    'of', 'in', 'for', 'on', 'with', 'at', 'by', 'from', 'as', 'into', 'through',
    'during', 'before', 'after', 'above', 'below', 'between', 'under', 'again',
    'further', 'then', 'once', 'here', 'there', 'when', 'where', 'why', 'how',
    'all', 'each', 'few', 'more', 'most', 'other', 'some', 'such', 'no', 'nor',
    'not', 'only', 'own', 'same', 'so', 'than', 'too', 'very', 'just', 'and',
    'but', 'if', 'or', 'because', 'until', 'while', 'about', 'against', 'between',
])


def extract_keywords_from_path(path):
    """从文件路径提取关键词"""
    # 获取文件名（不含扩展名）
    filename = os.path.splitext(os.path.basename(path))[0]
    # 获取所有目录名
    dirnames = []
    parts = path.split('/')
    for part in parts[:-1]:  # 排除文件名本身
        if part and not part.startswith('.') and len(part) > 1:
            dirnames.append(part)

    # 合并所有文本
    all_text = ' '.join([filename] + dirnames)

    # 分词：中文按字符，英文按单词
    keywords = []

    # 英文单词（连续字母）
    english_words = re.findall(r'[a-zA-Z]+', all_text)
    for word in english_words:
        if len(word) >= 2 and word.lower() not in STOP_WORDS:
            keywords.append(word.lower())

    # 中文词汇（连续汉字，按2-4字组合）
    chinese_chars = re.findall(r'[一-鿿]+', all_text)
    for chars in chinese_chars:
        # 添加整个词
        if len(chars) >= 2 and chars not in STOP_WORDS:
            keywords.append(chars)
        # 同时拆分为2字组合（用于提取高频词）
        for i in range(len(chars) - 1):
            two_char = chars[i:i+2]
            if two_char not in STOP_WORDS:
                keywords.append(two_char)

    return keywords


def _generate_wordcloud(media_type='all', limit=50, min_count=3):
    """生成词云数据（内部函数）"""
    all_keywords = []

    if media_type in ('video', 'all'):
        # 获取所有视频路径（排除 douyin/peak）
        sources = Source.query.filter(Source.media_type.in_(['all', 'video'])).all()
        source_ids = [s.id for s in sources]
        videos = Video.query.filter(Video.source_id.in_(source_ids)).all()
        for video in videos:
            all_keywords.extend(extract_keywords_from_path(video.path))

    if media_type in ('image', 'all'):
        sources = Source.query.filter(Source.media_type.in_(['all', 'image'])).all()
        source_ids = [s.id for s in sources]
        images = Image.query.filter(Image.source_id.in_(source_ids)).all()
        for image in images:
            all_keywords.extend(extract_keywords_from_path(image.path))

    # 统计词频
    counter = Counter(all_keywords)

    # 过滤并排序
    result = [
        {'word': word, 'count': count}
        for word, count in counter.items()
        if count >= min_count
    ]
    result.sort(key=lambda x: -x['count'])

    return result[:limit]


@wordcloud_bp.route('', methods=['GET'])
def get_wordcloud():
    """获取高频词汇列表（带数据库缓存）"""
    media_type = request.args.get('type', 'video')  # video | image | all
    limit = request.args.get('limit', 50, type=int)
    min_count = request.args.get('min_count', 3, type=int)

    # 构建缓存键
    cache_key = f"{media_type}_{limit}_{min_count}"

    # 获取全局版本号
    global_version = WordCloudCache.get_global_version()

    # 检查数据库缓存是否有效
    cached = WordCloudCache.query.filter_by(cache_key=cache_key).first()
    if cached and cached.version == global_version:
        return jsonify(cached.data)

    # 生成新的词云数据
    result = _generate_wordcloud(media_type, limit, min_count)

    # 存入数据库缓存
    if cached:
        cached.data = result
        cached.version = global_version
        cached.updated_at = db.func.now()
    else:
        cached = WordCloudCache(
            cache_key=cache_key,
            data=result,
            version=global_version
        )
        db.session.add(cached)
    db.session.commit()

    return jsonify(result)


@wordcloud_bp.route('/refresh', methods=['POST'])
def refresh_wordcloud():
    """刷新词云缓存（在扫描视频库/图片库后调用）"""
    new_version = WordCloudCache.increment_global_version()

    return jsonify({
        'message': 'Wordcloud cache refreshed',
        'new_version': new_version
    })
