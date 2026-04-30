import React, { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { message, Modal } from 'antd'
import {
  HeartOutlined, HeartFilled, StarOutlined, StarFilled, DeleteOutlined,
  PlayCircleOutlined, LeftOutlined, SoundOutlined, MutedOutlined,
} from '@ant-design/icons'
import { douyinApi, tagsApi } from '@services/api'
import './index.css'

// Custom Shuffle icon
const ShuffleIcon = ({ style }) => (
  <svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor" style={style}>
    <path d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/>
  </svg>
)

const isMobile = () => window.innerWidth <= 768 || 'ontouchstart' in window

const DouyinRed = '#FE2C55'
const DouyinGold = '#FFD700'

function DouyinLibrary() {
  const navigate = useNavigate()
  const [allVideos, setAllVideos] = useState([])
  const [playlist, setPlaylist] = useState([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [isRandom, setIsRandom] = useState(false)
  const [showControls, setShowControls] = useState(true)
  const [isMuted, setIsMuted] = useState(false)
  const [volume, setVolume] = useState(1)
  const [filterType, setFilterType] = useState('all')
  const [showLikeAnimation, setShowLikeAnimation] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [isDragging, setIsDragging] = useState(false)
  const [isMobileView, setIsMobileView] = useState(false)
  const [tags, setTags] = useState([])
  const [selectedTag, setSelectedTag] = useState(null)
  const [isProgressVisible, setIsProgressVisible] = useState(false)
  const [showFilter, setShowFilter] = useState(false)

  const videoRef = useRef(null)
  const containerRef = useRef(null)
  const controlsTimeoutRef = useRef(null)
  const touchStartRef = useRef(null)
  const lastTapRef = useRef(null)
  const lastClickRef = useRef(null)
  const progressRef = useRef(null)
  const wheelTimeoutRef = useRef(null)

  useEffect(() => {
    const checkMobile = () => setIsMobileView(isMobile())
    checkMobile()
    window.addEventListener('resize', checkMobile)
    return () => window.removeEventListener('resize', checkMobile)
  }, [])

  useEffect(() => {
    fetchAllVideos()
    fetchTags()
  }, [])

  const fetchAllVideos = async () => {
    try {
      const response = await douyinApi.getList({ per_page: 1000 })
      const videos = response.items || []
      setAllVideos(videos)
      setPlaylist(videos)
    } catch (error) {
      message.error('获取抖音库视频失败')
    }
  }

  const fetchTags = async () => {
    try {
      const response = await tagsApi.getList()
      setTags(response || [])
    } catch (error) {
      console.error('Failed to fetch tags:', error)
    }
  }

  const currentVideo = playlist[currentIndex]

  useEffect(() => {
    let filtered = allVideos
    if (filterType === 'liked') filtered = allVideos.filter(v => v.is_liked)
    else if (filterType === 'favorite') filtered = allVideos.filter(v => v.is_favorite)
    else if (filterType === 'tag' && selectedTag) {
      filtered = allVideos.filter(v => v.tags && v.tags.some(t => t.id === selectedTag))
    }
    setPlaylist(filtered)
    const currentVideoId = playlist[currentIndex]?.id
    const newIndex = filtered.findIndex(v => v.id === currentVideoId)
    setCurrentIndex(newIndex === -1 ? 0 : newIndex)
  }, [filterType, selectedTag, allVideos])

  // 自动隐藏控制栏（播放时3秒后隐藏）
  useEffect(() => {
    if (isPlaying) {
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current)
      controlsTimeoutRef.current = setTimeout(() => {
        setShowControls(false)
        setShowFilter(false)
        setIsProgressVisible(false)
      }, 3000)
    }
    return () => {
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current)
    }
  }, [isPlaying])

  // 视频事件监听
  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    const handleTimeUpdate = () => {
      if (!isDragging) setCurrentTime(video.currentTime)
    }
    const handleLoadedMetadata = () => setDuration(video.duration)
    const handlePlay = () => setIsPlaying(true)
    const handlePause = () => setIsPlaying(false)
    const handleEnded = () => nextVideo()

    video.addEventListener('timeupdate', handleTimeUpdate)
    video.addEventListener('loadedmetadata', handleLoadedMetadata)
    video.addEventListener('play', handlePlay)
    video.addEventListener('pause', handlePause)
    video.addEventListener('ended', handleEnded)

    return () => {
      video.removeEventListener('timeupdate', handleTimeUpdate)
      video.removeEventListener('loadedmetadata', handleLoadedMetadata)
      video.removeEventListener('play', handlePlay)
      video.removeEventListener('pause', handlePause)
      video.removeEventListener('ended', handleEnded)
    }
  }, [currentIndex, isDragging])

  const togglePlay = () => {
    if (videoRef.current) {
      if (isPlaying) {
        videoRef.current.pause()
        // 暂停时显示控制栏
        setShowControls(true)
        setShowFilter(true)
        setIsProgressVisible(true)
      } else {
        videoRef.current.play()
      }
    }
  }

  const nextVideo = useCallback(() => {
    if (playlist.length === 0) return
    let nextIndex
    if (isRandom) {
      do { nextIndex = Math.floor(Math.random() * playlist.length) }
      while (nextIndex === currentIndex && playlist.length > 1)
    } else {
      nextIndex = (currentIndex + 1) % playlist.length
    }
    setCurrentIndex(nextIndex)
    setIsPlaying(true)
  }, [playlist, currentIndex, isRandom])

  const prevVideo = useCallback(() => {
    if (playlist.length === 0) return
    let prevIndex
    if (isRandom) {
      do { prevIndex = Math.floor(Math.random() * playlist.length) }
      while (prevIndex === currentIndex && playlist.length > 1)
    } else {
      prevIndex = (currentIndex - 1 + playlist.length) % playlist.length
    }
    setCurrentIndex(prevIndex)
    setIsPlaying(true)
  }, [playlist, currentIndex, isRandom])

  useEffect(() => {
    if (isPlaying && videoRef.current) {
      videoRef.current.play().catch(() => {})
    }
  }, [currentIndex])

  const toggleMute = () => {
    if (videoRef.current) {
      videoRef.current.muted = !isMuted
      setIsMuted(!isMuted)
    }
  }

  const handleVolumeChange = (e) => {
    const value = parseFloat(e.target.value)
    setVolume(value)
    if (videoRef.current) {
      videoRef.current.volume = value
      setIsMuted(value === 0)
    }
  }

  const handleProgressClick = (e) => {
    if (!progressRef.current || !duration) return
    const rect = progressRef.current.getBoundingClientRect()
    const percent = (e.clientX - rect.left) / rect.width
    const newTime = percent * duration
    if (videoRef.current) {
      videoRef.current.currentTime = newTime
      setCurrentTime(newTime)
    }
  }

  const toggleLike = async () => {
    if (!currentVideo) return
    try {
      await douyinApi.toggleLike(currentVideo.id)
      currentVideo.is_liked = !currentVideo.is_liked
      setPlaylist([...playlist])
      message.success(currentVideo.is_liked ? '已喜欢' : '已取消喜欢', 0.5)
    } catch (error) { message.error('操作失败') }
  }

  const toggleFavorite = async () => {
    if (!currentVideo) return
    try {
      await douyinApi.toggleFavorite(currentVideo.id)
      currentVideo.is_favorite = !currentVideo.is_favorite
      setPlaylist([...playlist])
      message.success(currentVideo.is_favorite ? '已收藏' : '已取消收藏', 0.5)
    } catch (error) { message.error('操作失败') }
  }

  const handleDelete = async () => {
    if (!currentVideo) return
    Modal.confirm({
      title: '确认删除',
      content: '删除后将无法恢复，是否继续？',
      okText: '删除',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          await douyinApi.delete(currentVideo.id)
          await fetchAllVideos()
          message.success('视频已删除')
        } catch (error) { message.error('删除失败') }
      },
    })
  }

  // 双击喜欢处理
  const handleDoubleClick = () => {
    if (!currentVideo?.is_liked) {
      toggleLike()
    }
    setShowLikeAnimation(true)
    setTimeout(() => setShowLikeAnimation(false), 800)
  }

  // 鼠标滚轮切换视频（桌面端）
  const handleWheel = (e) => {
    if (isMobileView) return

    // 防止连续滚动触发多次
    if (wheelTimeoutRef.current) return

    wheelTimeoutRef.current = setTimeout(() => {
      wheelTimeoutRef.current = null
    }, 300)

    if (e.deltaY > 30) {
      // 向下滚动 - 下一个视频
      nextVideo()
    } else if (e.deltaY < -30) {
      // 向上滚动 - 上一个视频
      prevVideo()
    }
  }

  // 单击处理 - 暂停/播放 + 显示控制栏
  const handleClick = (e) => {
    const now = Date.now()

    // 检测双击（300ms内两次点击）
    if (now - (lastClickRef.current || 0) < 300) {
      handleDoubleClick()
      lastClickRef.current = null
      return
    }
    lastClickRef.current = now

    // 延迟执行单击，等待可能的第二次点击
    setTimeout(() => {
      if (lastClickRef.current === now) {
        // 确认是单击
        togglePlay()
        // 显示控制栏
        setShowControls(true)
        setShowFilter(true)
        setIsProgressVisible(true)
      }
    }, 300)
  }

  // 触摸手势处理（移动端）
  const handleTouchStart = (e) => {
    touchStartRef.current = {
      x: e.touches[0].clientX,
      y: e.touches[0].clientY,
      time: Date.now()
    }
  }

  const handleTouchMove = (e) => {
    if (!touchStartRef.current) return
    const deltaY = e.touches[0].clientY - touchStartRef.current.y
    if (Math.abs(deltaY) > 10) {
      e.preventDefault()
    }
  }

  const handleTouchEnd = (e) => {
    if (!touchStartRef.current) return
    const deltaX = e.changedTouches[0].clientX - touchStartRef.current.x
    const deltaY = e.changedTouches[0].clientY - touchStartRef.current.y
    const deltaTime = Date.now() - touchStartRef.current.time

    // 双击检测
    const now = Date.now()
    if (now - (lastTapRef.current || 0) < 300 && Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
      handleDoubleClick()
      lastTapRef.current = null
      touchStartRef.current = null
      return
    }
    lastTapRef.current = now

    if (deltaTime < 300) {
      if (Math.abs(deltaY) > 50 && Math.abs(deltaY) > Math.abs(deltaX)) {
        // 垂直滑动切换视频
        if (deltaY > 0) prevVideo()
        else nextVideo()
      } else if (Math.abs(deltaX) > 50 && Math.abs(deltaX) > Math.abs(deltaY)) {
        // 水平滑动快进/快退
        if (videoRef.current) {
          if (deltaX > 0) videoRef.current.currentTime += 10
          else videoRef.current.currentTime -= 10
        }
      } else if (Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
        // 单击暂停/播放
        togglePlay()
        setShowControls(true)
        setShowFilter(true)
        setIsProgressVisible(true)
      }
    }
    touchStartRef.current = null
  }

  const formatTime = (seconds) => {
    if (!seconds || isNaN(seconds)) return '0:00'
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0

  if (playlist.length === 0) {
    return (
      <div className="douyin-page">
        <div className="douyin-empty">
          <h2>暂无抖音视频</h2>
          <p>请先在来源配置中添加抖音库来源</p>
          <button className="douyin-btn" onClick={() => navigate('/sources')}>配置来源</button>
        </div>
      </div>
    )
  }

  return (
    <div
      className="douyin-page"
      ref={containerRef}
      onWheel={handleWheel}
      onClick={handleClick}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
    >
      {/* 视频播放 */}
      <video
        ref={videoRef}
        src={currentVideo ? douyinApi.getStreamUrl(currentVideo.id) : ''}
        className="douyin-video"
        autoPlay={isPlaying}
        loop={!isMobileView}
        playsInline
        muted={isMuted}
      />

      {/* 双击喜欢动画 */}
      {showLikeAnimation && (
        <div className="like-animation">
          <HeartFilled style={{ fontSize: 80, color: DouyinRed }} />
        </div>
      )}

      {/* 顶部控制栏 */}
      <div className={`douyin-top-bar ${showControls ? '' : 'hidden'}`}>
        <button className="douyin-back-btn" onClick={(e) => { e.stopPropagation(); navigate('/videos') }}>
          <LeftOutlined />
        </button>
        <div className={`douyin-filter-group ${showFilter ? '' : 'hidden'}`}>
          <select
            className="douyin-filter-select"
            value={filterType}
            onChange={(e) => { e.stopPropagation(); setFilterType(e.target.value) }}
            onClick={(e) => e.stopPropagation()}
          >
            <option value="all">全部</option>
            <option value="liked">喜欢</option>
            <option value="favorite">收藏</option>
            <option value="tag">标签</option>
          </select>
          {filterType === 'tag' && (
            <select
              className="douyin-filter-select"
              value={selectedTag || ''}
              onChange={(e) => { e.stopPropagation(); setSelectedTag(e.target.value ? parseInt(e.target.value) : null) }}
              onClick={(e) => e.stopPropagation()}
            >
              <option value="">选择标签</option>
              {tags.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          )}
        </div>
        <div className="douyin-counter">
          {currentIndex + 1} / {playlist.length}
        </div>
      </div>

      {/* 播放模式切换 */}
      <div
        className={`douyin-mode-btn ${showControls ? '' : 'hidden'}`}
        onClick={(e) => { e.stopPropagation(); setIsRandom(!isRandom) }}
      >
        <ShuffleIcon style={{ color: isRandom ? DouyinRed : '#fff' }} />
        <span style={{ color: isRandom ? DouyinRed : '#fff', marginLeft: 4 }}>{isRandom ? '随机' : '顺序'}</span>
      </div>

      {/* 右侧操作按钮 - 始终显示 */}
      <div className="douyin-right-btns">
        <div className="douyin-btn-group">
          <button className="douyin-action-btn" onClick={(e) => { e.stopPropagation(); toggleLike() }}>
            {currentVideo?.is_liked
              ? <HeartFilled style={{ color: DouyinRed }} />
              : <HeartOutlined />}
          </button>
          <span className="douyin-btn-text">喜欢</span>
        </div>
        <div className="douyin-btn-group">
          <button className="douyin-action-btn" onClick={(e) => { e.stopPropagation(); toggleFavorite() }}>
            {currentVideo?.is_favorite
              ? <StarFilled style={{ color: DouyinGold }} />
              : <StarOutlined />}
          </button>
          <span className="douyin-btn-text">收藏</span>
        </div>
        <div className="douyin-btn-group">
          <button className="douyin-action-btn delete" onClick={(e) => { e.stopPropagation(); handleDelete() }}>
            <DeleteOutlined />
          </button>
          <span className="douyin-btn-text">删除</span>
        </div>
      </div>

      {/* 底部进度条 */}
      <div
        className={`douyin-progress ${isProgressVisible ? '' : 'hidden'}`}
        ref={progressRef}
        onClick={(e) => { e.stopPropagation(); handleProgressClick(e) }}
      >
        <div className="douyin-progress-bar">
          <div className="douyin-progress-filled" style={{ width: `${progressPercent}%` }} />
        </div>
        <span className="douyin-time">
          {formatTime(currentTime)} / {formatTime(duration)}
        </span>
      </div>

      {/* 视频信息 */}
      <div className={`douyin-video-info ${showControls ? '' : 'hidden'}`}>
        <h3>{currentVideo?.title}</h3>
        {currentVideo?.tags?.length > 0 && (
          <div className="douyin-tags">
            {currentVideo.tags.map(t => (
              <span key={t.id} className="douyin-tag" style={{ borderColor: t.color || DouyinRed }}>
                {t.name}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* 中央播放按钮 - 仅暂停时显示 */}
      {!isPlaying && (
        <div className="douyin-center-play" onClick={(e) => { e.stopPropagation(); togglePlay() }}>
          <PlayCircleOutlined />
        </div>
      )}

      {/* 音量控制 */}
      <div className={`douyin-volume ${showControls ? '' : 'hidden'}`}>
        <button onClick={(e) => { e.stopPropagation(); toggleMute() }}>
          {isMuted ? <MutedOutlined /> : <SoundOutlined />}
        </button>
        <input
          type="range"
          min="0"
          max="1"
          step="0.1"
          value={isMuted ? 0 : volume}
          onChange={(e) => { e.stopPropagation(); handleVolumeChange(e) }}
          onClick={(e) => e.stopPropagation()}
        />
      </div>
    </div>
  )
}

export default DouyinLibrary