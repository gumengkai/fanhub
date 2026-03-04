import React, { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Button, Space, Tag, Modal, Input, message, Select, Tooltip, Slider } from 'antd'
import {
  HeartOutlined, HeartFilled, DeleteOutlined, TagsOutlined,
  PlayCircleOutlined, PauseCircleOutlined, StepBackwardOutlined, StepForwardOutlined,
  OrderedListOutlined, FullscreenOutlined, FullscreenExitOutlined,
  LeftOutlined, SoundOutlined, MutedOutlined, HomeOutlined,
  ClockCircleOutlined, EditOutlined,
} from '@ant-design/icons'
import { videosApi, tagsApi } from '@services/api'
import './index.css'

// Custom Shuffle icon since ShuffleOutlined is not available
const ShuffleIcon = () => (
  <svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor">
    <path d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/>
  </svg>
)

const { Option } = Select
const { TextArea } = Input

// 检测是否为移动设备
const isMobile = () => window.innerWidth <= 768 || 'ontouchstart' in window

function ShortVideo() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const tagParam = searchParams.get('tag')
  const [allVideos, setAllVideos] = useState([])
  const [playlist, setPlaylist] = useState([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [isRandom, setIsRandom] = useState(false)
  const [showControls, setShowControls] = useState(true)
  const [isMuted, setIsMuted] = useState(false)
  const [volume, setVolume] = useState(1)
  const [showTags, setShowTags] = useState(false)
  const [showFilter, setShowFilter] = useState(false)
  const [filterType, setFilterType] = useState(tagParam ? 'tag' : 'all')
  const [selectedTag, setSelectedTag] = useState(tagParam ? parseInt(tagParam) : null)
  const [tagDropdownOpen, setTagDropdownOpen] = useState(false)
  const [tags, setTags] = useState([])
  const [editingVideo, setEditingVideo] = useState(null)
  const [editedTitle, setEditedTitle] = useState('')
  const [editedDescription, setEditedDescription] = useState('')
  const [selectedTags, setSelectedTags] = useState([])
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [playbackRate, setPlaybackRate] = useState(1)
  const [isDragging, setIsDragging] = useState(false)
  const [isMobileView, setIsMobileView] = useState(false)
  const [showVolumeSlider, setShowVolumeSlider] = useState(false)

  const videoRef = useRef(null)
  const containerRef = useRef(null)
  const controlsTimeoutRef = useRef(null)
  const touchStartRef = useRef(null)
  const saveProgressRef = useRef(null)
  const progressRef = useRef(null)
  const nextVideoRef = useRef(null)
  const prevVideoRef = useRef(null)

  // 检测移动设备
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
      // 先获取第一页以知道总数
      const firstResponse = await fetch('/api/videos?per_page=1')
      const firstData = await firstResponse.json()
      const total = firstData.total || 0

      if (total === 0) {
        setAllVideos([])
        setPlaylist([])
        return
      }

      // 获取全部视频
      const response = await fetch(`/api/videos?per_page=${total}`)
      const data = await response.json()
      const videos = data.items || []
      setAllVideos(videos)
      setPlaylist(videos)
      if (videos.length > 0) preloadVideo(videos[0])
    } catch (error) {
      message.error('获取视频列表失败')
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

  const preloadVideo = (video) => {
    const videoEl = document.createElement('video')
    videoEl.src = `/api/videos/${video.id}/stream`
    videoEl.load()
  }

  const currentVideo = playlist[currentIndex]

  useEffect(() => {
    let filtered = allVideos
    if (filterType === 'favorite') filtered = allVideos.filter(v => v.is_favorite)
    else if (filterType === 'tag' && selectedTag) {
      filtered = allVideos.filter(v => v.tags && v.tags.some(t => t.id === selectedTag))
    }
    setPlaylist(filtered)
    // 如果当前视频在新筛选列表中，保持当前索引，否则重置为0
    const currentVideoId = playlist[currentIndex]?.id
    const newIndex = filtered.findIndex(v => v.id === currentVideoId)
    if (newIndex === -1) {
      setCurrentIndex(0)
    } else if (newIndex !== currentIndex) {
      setCurrentIndex(newIndex)
    }
  }, [filterType, selectedTag, allVideos])

  useEffect(() => {
    const handleMouseMove = () => {
      setShowControls(true)
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current)
      // 如果下拉框打开或视频暂停，不自动隐藏控制栏
      if (isPlaying && !tagDropdownOpen) {
        controlsTimeoutRef.current = setTimeout(() => setShowControls(false), 3000)
      }
    }

    const container = containerRef.current
    if (container) {
      container.addEventListener('mousemove', handleMouseMove)
      container.addEventListener('mouseleave', () => {
        if (isPlaying && !tagDropdownOpen) setShowControls(false)
      })
      container.addEventListener('mouseenter', () => setShowControls(true))
    }

    return () => {
      if (container) container.removeEventListener('mousemove', handleMouseMove)
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current)
    }
  }, [isPlaying, tagDropdownOpen])

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    const handleTimeUpdate = () => {
      if (!isDragging) setCurrentTime(video.currentTime)
      if (Math.floor(video.currentTime) % 10 === 0) saveProgress(video.currentTime, video.duration)
    }
    const handleLoadedMetadata = () => setDuration(video.duration)
    const handlePlay = () => setIsPlaying(true)
    const handlePause = () => setIsPlaying(false)
    const handleEnded = () => nextVideoRef.current?.()

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

  useEffect(() => {
    const handleKeyDown = (e) => {
      switch (e.key) {
        case 'ArrowUp': e.preventDefault(); prevVideoRef.current?.(); break
        case 'ArrowDown': e.preventDefault(); nextVideoRef.current?.(); break
        case ' ': e.preventDefault(); togglePlay(); break
        case 'ArrowLeft': skip(-10); break
        case 'ArrowRight': skip(10); break
        case 'f': toggleFullscreen(); break
        case 'm': toggleMute(); break
        case 'r': setIsRandom(prev => !prev); break
        case 'l': if (currentVideo) openTagModal(); break
        case 'c': if (currentVideo) toggleFavorite(); break
        case 'Escape': if (isFullscreen) toggleFullscreen(); else navigate('/videos'); break
        default: break
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [currentIndex, isPlaying, currentVideo, isFullscreen])

  const saveProgress = (position, totalDuration, completed = false) => {
    if (saveProgressRef.current) clearTimeout(saveProgressRef.current)
    saveProgressRef.current = setTimeout(() => {
      if (currentVideo?.id) {
        fetch(`/api/videos/${currentVideo.id}/history`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ playback_position: position, duration: totalDuration, is_completed: completed })
        }).catch(console.error)
      }
    }, 500)
  }

  // 处理标签选择 - 只在确认后应用筛选
  const handleTagSelect = (value) => {
    setSelectedTag(value)
  }

  const togglePlay = () => {
    if (videoRef.current) {
      if (isPlaying) videoRef.current.pause()
      else videoRef.current.play()
      setIsPlaying(!isPlaying)
    }
  }

  const nextVideo = useCallback(() => {
    if (playlist.length === 0) return
    let nextIndex
    if (isRandom) {
      // 在全部视频中随机选择（不重复当前）
      do { nextIndex = Math.floor(Math.random() * playlist.length) }
      while (nextIndex === currentIndex && playlist.length > 1)
    } else {
      // 顺序播放，循环到开头
      nextIndex = (currentIndex + 1) % playlist.length
    }
    setCurrentIndex(nextIndex)
    setIsPlaying(true)
    // 预加载下一个视频
    const nextNextIndex = isRandom
      ? Math.floor(Math.random() * playlist.length)
      : (nextIndex + 1) % playlist.length
    if (playlist[nextNextIndex]) {
      preloadVideo(playlist[nextNextIndex])
    }
  }, [playlist, currentIndex, isRandom])

  const prevVideo = useCallback(() => {
    if (playlist.length === 0) return
    let prevIndex
    if (isRandom) {
      // 随机模式下也随机选择上一个
      do { prevIndex = Math.floor(Math.random() * playlist.length) }
      while (prevIndex === currentIndex && playlist.length > 1)
    } else {
      // 顺序播放
      prevIndex = (currentIndex - 1 + playlist.length) % playlist.length
    }
    setCurrentIndex(prevIndex)
    setIsPlaying(true)
  }, [playlist, currentIndex, isRandom])

  // 更新 ref
  useEffect(() => {
    nextVideoRef.current = nextVideo
    prevVideoRef.current = prevVideo
  }, [nextVideo, prevVideo])

  // 切换视频后自动播放
  useEffect(() => {
    if (isPlaying && videoRef.current) {
      videoRef.current.play().catch(() => {})
    }
  }, [currentIndex])

  const skip = (seconds) => {
    if (videoRef.current) {
      videoRef.current.currentTime += seconds
    }
  }

  const toggleMute = () => {
    if (videoRef.current) {
      videoRef.current.muted = !isMuted
      setIsMuted(!isMuted)
    }
  }

  const handleVolumeChange = (value) => {
    setVolume(value)
    if (videoRef.current) {
      videoRef.current.volume = value
      setIsMuted(value === 0)
    }
  }

  const handleSeekChange = (value) => setCurrentTime(value)
  const handleSeekAfterChange = (value) => {
    if (videoRef.current) {
      videoRef.current.currentTime = value
      saveProgress(value, duration)
    }
    setIsDragging(false)
  }
  const handleSeekBeforeChange = () => setIsDragging(true)

  // 移动端进度条点击处理
  const handleProgressClick = (e) => {
    if (!isMobileView || !progressRef.current) return
    const rect = progressRef.current.getBoundingClientRect()
    const percent = (e.clientX - rect.left) / rect.width
    const newTime = percent * duration
    if (videoRef.current) {
      videoRef.current.currentTime = newTime
      setCurrentTime(newTime)
      saveProgress(newTime, duration)
    }
  }

  const handlePlaybackRateChange = () => {
    const rates = [0.5, 0.75, 1, 1.25, 1.5, 2]
    const currentIndex = rates.indexOf(playbackRate)
    const nextRate = rates[(currentIndex + 1) % rates.length]
    setPlaybackRate(nextRate)
    if (videoRef.current) videoRef.current.playbackRate = nextRate
  }

  const toggleFullscreen = async () => {
    if (!document.fullscreenElement && containerRef.current) {
      try {
        // 移动端使用屏幕方向锁定
        if (isMobileView && screen.orientation) {
          await screen.orientation.lock('landscape').catch(() => {})
        }
        await containerRef.current.requestFullscreen()
        setIsFullscreen(true)
      } catch (err) { console.error('Fullscreen error:', err) }
    } else {
      try {
        if (screen.orientation) {
          await screen.orientation.unlock().catch(() => {})
        }
        await document.exitFullscreen()
        setIsFullscreen(false)
      } catch (err) { console.error('Exit fullscreen error:', err) }
    }
  }

  const toggleFavorite = async () => {
    if (!currentVideo) return
    try {
      await videosApi.toggleFavorite(currentVideo.id)
      currentVideo.is_favorite = !currentVideo.is_favorite
      setPlaylist([...playlist])
      message.success(currentVideo.is_favorite ? '已添加到收藏' : '已取消收藏')
    } catch (error) { message.error('操作失败') }
  }

  const openTagModal = () => {
    setEditingVideo(currentVideo)
    setEditedTitle(currentVideo.title)
    setEditedDescription(currentVideo.description || '')
    setSelectedTags(currentVideo.tags ? currentVideo.tags.map(t => t.id) : [])
    setShowTags(true)
  }

  const handleSaveTags = async () => {
    if (!editingVideo) return
    try {
      await videosApi.update(editingVideo.id, { title: editedTitle, description: editedDescription })
      const currentTagIds = editingVideo.tags ? editingVideo.tags.map(t => t.id) : []
      for (const tagId of currentTagIds) {
        if (!selectedTags.includes(tagId)) await videosApi.removeTag(editingVideo.id, tagId)
      }
      for (const tagId of selectedTags) {
        if (!currentTagIds.includes(tagId)) await videosApi.addTag(editingVideo.id, tagId)
      }
      // 只更新当前视频的信息，不刷新整个列表
      const updatedTags = tags.filter(t => selectedTags.includes(t.id))
      const updatedVideo = { ...editingVideo, title: editedTitle, description: editedDescription, tags: updatedTags }
      const newPlaylist = playlist.map((v, i) =>
        i === currentIndex ? updatedVideo : v
      )
      setPlaylist(newPlaylist)
      setAllVideos(prev => prev.map(v => v.id === editingVideo.id ? updatedVideo : v))
      setShowTags(false)
      message.success('保存成功')
    } catch (error) { message.error('保存失败') }
  }

  const handleDelete = async () => {
    if (!currentVideo) return
    Modal.confirm({
      title: '确认删除',
      content: '删除后将无法恢复，是否继续？',
      okText: '删除', cancelText: '取消', okType: 'danger',
      onOk: async () => {
        try {
          await videosApi.delete(currentVideo.id)
          await fetchAllVideos()
          message.success('视频已删除')
        } catch (error) { message.error('删除失败') }
      },
    })
  }

  // 触摸手势处理 - 优化移动端滑动体验
  const handleTouchStart = (e) => {
    touchStartRef.current = { x: e.touches[0].clientX, y: e.touches[0].clientY, time: Date.now() }
  }

  const handleTouchMove = (e) => {
    if (!touchStartRef.current) return
    // 防止默认滚动行为
    if (isMobileView) {
      const deltaY = e.touches[0].clientY - touchStartRef.current.y
      if (Math.abs(deltaY) > 10) {
        e.preventDefault()
      }
    }
  }

  const handleTouchEnd = (e) => {
    if (!touchStartRef.current) return
    const deltaX = e.changedTouches[0].clientX - touchStartRef.current.x
    const deltaY = e.changedTouches[0].clientY - touchStartRef.current.y
    const deltaTime = Date.now() - touchStartRef.current.time

    if (deltaTime < 300) {
      // 垂直滑动切换视频
      if (Math.abs(deltaY) > 50 && Math.abs(deltaY) > Math.abs(deltaX)) {
        if (deltaY > 0) prevVideo()
        else nextVideo()
      }
      // 水平滑动快进/快退
      else if (Math.abs(deltaX) > 50 && Math.abs(deltaX) > Math.abs(deltaY)) {
        if (deltaX > 0) skip(10)
        else skip(-10)
      }
      // 点击显示/隐藏控制栏
      else if (Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
        setShowControls(prev => !prev)
      }
    }
    touchStartRef.current = null
  }

  // 双击处理
  const handleDoubleClick = () => {
    if (isMobileView) {
      toggleFavorite()
    }
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
      <div className="short-video-page">
        <div className="empty-state">
          <h2>暂无视频</h2>
          <p>请先添加视频到视频库</p>
          <Button type="primary" onClick={() => navigate('/videos')}>返回视频库</Button>
        </div>
      </div>
    )
  }

  return (
    <div
      className="short-video-page"
      ref={containerRef}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      onDoubleClick={handleDoubleClick}
    >
      {/* 顶部导航栏 */}
      <div className={`top-bar ${showControls ? '' : 'hidden'}`}>
        <Button type="text" icon={<LeftOutlined />} onClick={() => navigate('/videos')} className="back-btn">返回</Button>
        <div className="filter-controls">
          <Select value={filterType} onChange={setFilterType} style={{ width: 120 }} size={isMobileView ? 'small' : 'middle'}>
            <Option value="all">全部视频</Option>
            <Option value="favorite">收藏</Option>
            <Option value="tag">标签筛选</Option>
          </Select>
          {filterType === 'tag' && (
            <Select
              value={selectedTag}
              onChange={handleTagSelect}
              style={{ width: isMobileView ? 120 : 150 }}
              placeholder="选择标签"
              allowClear
              onClear={() => handleTagSelect(null)}
              dropdownMatchSelectWidth={false}
              size={isMobileView ? 'small' : 'middle'}
              onDropdownVisibleChange={(open) => {
                setTagDropdownOpen(open)
                if (open) setShowControls(true)
              }}
            >
              {tags.map(tag => <Option key={tag.id} value={tag.id}>{tag.name}</Option>)}
            </Select>
          )}
        </div>
        <Space>
          <Button type={isRandom ? 'primary' : 'default'} icon={<ShuffleIcon />} onClick={() => setIsRandom(!isRandom)} size={isMobileView ? 'small' : 'middle'}>
            {isMobileView ? '' : (isRandom ? '随机' : '顺序')}
          </Button>
          <span className="video-counter">{currentIndex + 1} / {playlist.length}</span>
        </Space>
      </div>

      {/* 视频容器 */}
      <div className="video-container">
        <video
          ref={videoRef}
          src={currentVideo ? `/api/videos/${currentVideo.id}/stream` : ''}
          className="video-element"
          onClick={togglePlay}
          autoPlay={isPlaying}
          loop
          playsInline
          webkit-playsinline="true"
        />

        {/* 视频信息覆盖层 */}
        <div className={`video-overlay ${showControls ? '' : 'hidden'}`}>
          <div className="video-info">
            <h3 className="video-title">{currentVideo?.title}</h3>
            {currentVideo?.description && <p className="video-description">{currentVideo.description}</p>}
            {currentVideo?.tags && currentVideo.tags.length > 0 && (
              <div className="video-tags">
                {currentVideo.tags.map(tag => <Tag key={tag.id} color={tag.color || '#fb7299'}>{tag.name}</Tag>)}
              </div>
            )}
          </div>
        </div>

        {/* 右侧操作按钮 - 抖音风格 */}
        <div className={`action-buttons ${showControls ? '' : 'hidden'}`}>
          <div className="action-group">
            <Button
              type="text"
              icon={currentVideo?.is_favorite ? <HeartFilled /> : <HeartOutlined />}
              onClick={toggleFavorite}
              size="large"
              className={`action-btn ${currentVideo?.is_favorite ? 'favorited' : ''}`}
            />
            <span className="action-btn-text">喜欢</span>
          </div>

          <div className="action-group">
            <Button
              type="text"
              icon={<TagsOutlined />}
              onClick={openTagModal}
              size="large"
              className="action-btn"
            />
            <span className="action-btn-text">标签</span>
          </div>

          <div className="action-group delete-group">
            <Button
              type="text"
              icon={<DeleteOutlined />}
              onClick={handleDelete}
              size="large"
              className="action-btn danger"
            />
            <span className="action-btn-text">删除</span>
          </div>
        </div>

        {/* 中央播放按钮 */}
        {!isPlaying && (
          <div className="center-play-btn" onClick={togglePlay}>
            <PlayCircleOutlined />
          </div>
        )}

        {/* 底部控制栏 */}
        <div className={`bottom-controls ${showControls ? '' : 'hidden'}`}>
          {/* 进度条 */}
          <div className="progress-container" ref={progressRef} onClick={handleProgressClick}>
            <div className="progress-background">
              <div className="progress-buffered" style={{ width: `${progressPercent}%` }} />
            </div>
            <Slider
              className="progress-bar-slider"
              value={currentTime}
              max={duration || 100}
              onChange={handleSeekChange}
              onChangeComplete={handleSeekAfterChange}
              onBeforeChange={handleSeekBeforeChange}
              tooltip={{ formatter: formatTime }}
              step={1}
            />
          </div>

          {/* 控制按钮 */}
          <div className="control-buttons">
            <Button type="text" icon={<StepBackwardOutlined />} onClick={prevVideo} size="large" />

            {isMobileView ? (
              // 移动端简化控制
              <>
                <Button type="text" icon={<ClockCircleOutlined />} onClick={() => skip(-10)} size="large" />
                <Button
                  type="primary"
                  icon={isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                  onClick={togglePlay}
                  size="large"
                  className="play-btn"
                />
                <Button type="text" icon={<ClockCircleOutlined />} onClick={() => skip(10)} size="large" />
                <Button type="text" icon={<StepForwardOutlined />} onClick={nextVideo} size="large" />
                <Button
                  type="text"
                  icon={isMuted ? <MutedOutlined /> : <SoundOutlined />}
                  onClick={toggleMute}
                  size="large"
                />
              </>
            ) : (
              // 桌面端完整控制
              <>
                <Button type="text" icon={<ClockCircleOutlined />} onClick={() => skip(-10)} size="large" />
                <Button
                  type="primary"
                  icon={isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                  onClick={togglePlay}
                  size="large"
                  className="play-btn"
                />
                <Button type="text" icon={<ClockCircleOutlined />} onClick={() => skip(10)} size="large" />
                <Button type="text" icon={<StepForwardOutlined />} onClick={nextVideo} size="large" />
                <div className="volume-control">
                  <Button type="text" icon={isMuted ? <MutedOutlined /> : <SoundOutlined />} onClick={toggleMute} />
                  <Slider className="volume-slider" value={isMuted ? 0 : volume} max={1} step={0.1} onChange={handleVolumeChange} />
                </div>
                <Button
                  type="text"
                  icon={<span style={{ fontSize: 14, fontWeight: 'bold' }}>{playbackRate}x</span>}
                  onClick={handlePlaybackRateChange}
                  size="large"
                />
                <Button
                  type="text"
                  icon={isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
                  onClick={toggleFullscreen}
                  size="large"
                />
              </>
            )}
          </div>

          <div className="time-display">{formatTime(currentTime)} / {formatTime(duration)}</div>
        </div>
      </div>

      {/* 标签编辑弹窗 */}
      <Modal
        open={showTags}
        title="编辑视频信息"
        onCancel={() => setShowTags(false)}
        onOk={handleSaveTags}
        width={isMobileView ? '90%' : 600}
        centered
      >
        <div className="edit-modal">
          <div className="form-item">
            <label>标题</label>
            <Input value={editedTitle} onChange={(e) => setEditedTitle(e.target.value)} placeholder="视频标题" />
          </div>
          <div className="form-item">
            <label>描述</label>
            <TextArea value={editedDescription} onChange={(e) => setEditedDescription(e.target.value)} placeholder="视频描述（可选）" rows={3} />
          </div>
          <div className="form-item">
            <label>标签</label>
            <Select mode="multiple" value={selectedTags} onChange={setSelectedTags} style={{ width: '100%' }} placeholder="选择标签">
              {tags.map(tag => <Option key={tag.id} value={tag.id}>{tag.name}</Option>)}
            </Select>
          </div>
        </div>
      </Modal>
    </div>
  )
}

export default ShortVideo
