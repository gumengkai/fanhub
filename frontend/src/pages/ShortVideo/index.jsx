import React, { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Space, Tag, Modal, Input, message, Select, Switch, Tooltip, Slider } from 'antd'
import {
  HeartOutlined, HeartFilled, DeleteOutlined, TagsOutlined,
  PlayCircleOutlined, PauseCircleOutlined, StepBackwardOutlined, StepForwardOutlined,
  OrderedListOutlined, FullscreenOutlined, FullscreenExitOutlined,
  LeftOutlined, SoundOutlined, MutedOutlined, HomeOutlined, FilterOutlined,
  ClockCircleOutlined,
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

function ShortVideo() {
  const navigate = useNavigate()
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
  const [filterType, setFilterType] = useState('all')
  const [selectedTag, setSelectedTag] = useState(null)
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
  
  const videoRef = useRef(null)
  const containerRef = useRef(null)
  const controlsTimeoutRef = useRef(null)
  const touchStartRef = useRef(null)
  const saveProgressRef = useRef(null)

  useEffect(() => {
    fetchAllVideos()
    fetchTags()
  }, [])

  const fetchAllVideos = async () => {
    try {
      const response = await fetch('/api/videos?per_page=500')
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
    setCurrentIndex(0)
  }, [filterType, selectedTag, allVideos])

  useEffect(() => {
    const handleMouseMove = () => {
      setShowControls(true)
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current)
      if (isPlaying) controlsTimeoutRef.current = setTimeout(() => setShowControls(false), 3000)
    }

    const container = containerRef.current
    if (container) {
      container.addEventListener('mousemove', handleMouseMove)
      container.addEventListener('mouseleave', () => isPlaying && setShowControls(false))
      container.addEventListener('mouseenter', () => setShowControls(true))
    }

    return () => {
      if (container) container.removeEventListener('mousemove', handleMouseMove)
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current)
    }
  }, [isPlaying])

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

  useEffect(() => {
    const handleKeyDown = (e) => {
      switch (e.key) {
        case 'ArrowUp': e.preventDefault(); prevVideo(); break
        case 'ArrowDown': e.preventDefault(); nextVideo(); break
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

  const togglePlay = () => {
    if (videoRef.current) {
      if (isPlaying) videoRef.current.pause()
      else videoRef.current.play()
      setIsPlaying(!isPlaying)
    }
  }

  const nextVideo = () => {
    if (playlist.length === 0) return
    if (isRandom) {
      let nextIndex
      do { nextIndex = Math.floor(Math.random() * playlist.length) }
      while (nextIndex === currentIndex && playlist.length > 1)
      setCurrentIndex(nextIndex)
    } else {
      setCurrentIndex((prev) => (prev + 1) % playlist.length)
    }
    setIsPlaying(true)
    setTimeout(() => videoRef.current?.play(), 100)
  }

  const prevVideo = () => {
    if (playlist.length === 0) return
    setCurrentIndex((prev) => (prev - 1 + playlist.length) % playlist.length)
    setIsPlaying(true)
    setTimeout(() => videoRef.current?.play(), 100)
  }

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
        await containerRef.current.requestFullscreen()
        setIsFullscreen(true)
      } catch (err) { console.error('Fullscreen error:', err) }
    } else {
      try {
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
      await fetchAllVideos()
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

  const handleTouchStart = (e) => {
    touchStartRef.current = { x: e.touches[0].clientX, y: e.touches[0].clientY, time: Date.now() }
  }

  const handleTouchEnd = (e) => {
    if (!touchStartRef.current) return
    const deltaX = e.changedTouches[0].clientX - touchStartRef.current.x
    const deltaY = e.changedTouches[0].clientY - touchStartRef.current.y
    const deltaTime = Date.now() - touchStartRef.current.time
    if (deltaTime < 300) {
      if (Math.abs(deltaY) > 50) { if (deltaY > 0) prevVideo(); else nextVideo() }
      if (Math.abs(deltaX) > 50) { if (deltaX > 0) skip(10); else skip(-10) }
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
    <div className="short-video-page" ref={containerRef} onTouchStart={handleTouchStart} onTouchEnd={handleTouchEnd}>
      {showControls && (
        <div className="top-bar">
          <Button type="text" icon={<LeftOutlined />} onClick={() => navigate('/videos')} className="back-btn">返回</Button>
          <div className="filter-controls">
            <Select value={filterType} onChange={setFilterType} style={{ width: 120 }}>
              <Option value="all">全部视频</Option>
              <Option value="favorite">收藏</Option>
              <Option value="tag">标签筛选</Option>
            </Select>
            {filterType === 'tag' && (
              <Select value={selectedTag} onChange={setSelectedTag} style={{ width: 150 }} placeholder="选择标签" allowClear>
                {tags.map(tag => <Option key={tag.id} value={tag.id}>{tag.name}</Option>)}
              </Select>
            )}
          </div>
          <Space>
            <Button type={isRandom ? 'primary' : 'default'} icon={<ShuffleIcon />} onClick={() => setIsRandom(!isRandom)} size="small">
              {isRandom ? '随机' : '顺序'}
            </Button>
            <span className="video-counter">{currentIndex + 1} / {playlist.length}</span>
          </Space>
        </div>
      )}

      <div className="video-container">
        <video
          ref={videoRef}
          src={currentVideo ? `/api/videos/${currentVideo.id}/stream` : ''}
          className="video-element"
          onClick={togglePlay}
          autoPlay={isPlaying}
          loop
        />

        <div className="video-overlay">
          <div className="video-info">
            <h3 className="video-title">{currentVideo?.title}</h3>
            {currentVideo?.description && <p className="video-description">{currentVideo.description}</p>}
            {currentVideo?.tags && currentVideo.tags.length > 0 && (
              <div className="video-tags">
                {currentVideo.tags.map(tag => <Tag key={tag.id} color={tag.color}>{tag.name}</Tag>)}
              </div>
            )}
          </div>
        </div>

        {showControls && (
          <div className="action-buttons">
            <Tooltip title="收藏 (C)">
              <Button type="text" icon={currentVideo?.is_favorite ? <HeartFilled /> : <HeartOutlined />} onClick={toggleFavorite} size="large" className={`action-btn ${currentVideo?.is_favorite ? 'favorited' : ''}`} />
            </Tooltip>
            <Tooltip title="标签 (L)">
              <Button type="text" icon={<TagsOutlined />} onClick={openTagModal} size="large" className="action-btn" />
            </Tooltip>
            <Tooltip title="删除">
              <Button type="text" icon={<DeleteOutlined />} onClick={handleDelete} size="large" className="action-btn danger" />
            </Tooltip>
            <div className="video-stats"><span>▶ {currentVideo?.view_count || 0} 播放</span></div>
          </div>
        )}

        {!isPlaying && showControls && (
          <div className="center-play-btn" onClick={togglePlay}><PlayCircleOutlined /></div>
        )}

        {showControls && (
          <div className="bottom-controls">
            <div className="progress-container">
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

            <div className="control-buttons">
              <Button type="text" icon={<StepBackwardOutlined />} onClick={prevVideo} size="large" />
              <Button type="text" icon={<ClockCircleOutlined />} onClick={() => skip(-10)} size="large" />
              <Button type="primary" icon={isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />} onClick={togglePlay} size="large" className="play-btn" />
              <Button type="text" icon={<ClockCircleOutlined />} onClick={() => skip(10)} size="large" />
              <Button type="text" icon={<StepForwardOutlined />} onClick={nextVideo} size="large" />
              <div className="volume-control">
                <Button type="text" icon={isMuted ? <MutedOutlined /> : <VolumeUpOutlined />} onClick={toggleMute} />
                <Slider className="volume-slider" value={isMuted ? 0 : volume} max={1} step={0.1} onChange={handleVolumeChange} />
              </div>
              <Button type="text" icon={<span style={{ fontSize: 14, fontWeight: 'bold' }}>{playbackRate}x</span>} onClick={handlePlaybackRateChange} size="large" />
              <Button type="text" icon={isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />} onClick={toggleFullscreen} size="large" />
            </div>

            <div className="time-display">{formatTime(currentTime)} / {formatTime(duration)}</div>
          </div>
        )}
      </div>

      <Modal open={showTags} title="编辑视频信息" onCancel={() => setShowTags(false)} onOk={handleSaveTags} width={600}>
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
