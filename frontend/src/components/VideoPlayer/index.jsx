import React, { useState, useRef, useEffect } from 'react'
import { Slider, Tooltip, Select } from 'antd'
import {
  PlayCircleOutlined,
  PauseCircleOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  SoundOutlined,
  MutedOutlined,
  BackwardOutlined,
  ForwardOutlined,
} from '@ant-design/icons'
import './index.css'

// Custom PiP icon since PictureInPictureOutlined is not available
const PictureInPictureIcon = () => (
  <svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor">
    <path d="M19 11h-8v6h8v-6zm4 8V4.98C23 3.88 22.1 3 21 3H3c-1.1 0-2 .88-2 1.98V19c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2zm-2 .02H3V4.97h18v14.05z"/>
  </svg>
)

const { Option } = Select

function VideoPlayer({ video, onProgressUpdate }) {
  const videoRef = useRef(null)
  const containerRef = useRef(null)
  const [loading, setLoading] = useState(true)
  const [isPlaying, setIsPlaying] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [volume, setVolume] = useState(1)
  const [isMuted, setIsMuted] = useState(false)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [showControls, setShowControls] = useState(true)
  const [playbackRate, setPlaybackRate] = useState(1)
  const [showPiP, setShowPiP] = useState(false)
  const controlsTimeoutRef = useRef(null)
  const saveProgressRef = useRef(null)
  const [isDragging, setIsDragging] = useState(false)
  const savedPositionRef = useRef(0)
  const [error, setError] = useState(null)

  // Check if video format is supported
  const isFormatSupported = (filename) => {
    if (!filename) return false
    const ext = filename.split('.').pop().toLowerCase()
    const supportedFormats = ['mp4', 'webm', 'ogv', 'ogg', 'mov', 'm4v']
    return supportedFormats.includes(ext)
  }

  // Load saved progress
  useEffect(() => {
    if (video?.id) {
      // Reset saved position and error when video changes
      savedPositionRef.current = 0
      setError(null)

      // Check format support
      if (!isFormatSupported(video.path)) {
        setError(`不支持的格式: ${video.path?.split('.').pop()?.toUpperCase() || '未知'}`)
        setLoading(false)
        return
      }

      fetch(`/api/videos/${video.id}/history`)
        .then(res => res.json())
        .then(data => {
          if (data.playback_position && data.playback_position > 0) {
            savedPositionRef.current = data.playback_position
          }
        })
        .catch(() => {})
    }
  }, [video?.id, video?.path])

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    const handleTimeUpdate = () => {
      if (!isDragging) {
        setCurrentTime(video.currentTime)
      }
      if (Math.floor(video.currentTime) % 10 === 0) {
        saveProgress(video.currentTime, video.duration)
      }
    }
    
    const handleLoadedMetadata = () => {
      setDuration(video.duration)
      setLoading(false)
      // Apply saved playback position after metadata is loaded
      if (savedPositionRef.current > 0 && savedPositionRef.current < video.duration * 0.95) {
        video.currentTime = savedPositionRef.current
        setCurrentTime(savedPositionRef.current)
      }
    }
    const handlePlay = () => setIsPlaying(true)
    const handlePause = () => setIsPlaying(false)
    const handleEnded = () => {
      setIsPlaying(false)
      saveProgress(video.duration, video.duration, true)
    }
    const handleWaiting = () => setLoading(true)
    const handleCanPlay = () => setLoading(false)
    const handleError = (e) => {
      setLoading(false)
      setError('视频加载失败，可能是不支持的格式')
      console.error('Video error:', e)
    }

    video.addEventListener('timeupdate', handleTimeUpdate)
    video.addEventListener('loadedmetadata', handleLoadedMetadata)
    video.addEventListener('play', handlePlay)
    video.addEventListener('pause', handlePause)
    video.addEventListener('ended', handleEnded)
    video.addEventListener('waiting', handleWaiting)
    video.addEventListener('canplay', handleCanPlay)

    return () => {
      video.removeEventListener('timeupdate', handleTimeUpdate)
      video.removeEventListener('loadedmetadata', handleLoadedMetadata)
      video.removeEventListener('play', handlePlay)
      video.removeEventListener('pause', handlePause)
      video.removeEventListener('ended', handleEnded)
      video.removeEventListener('waiting', handleWaiting)
      video.removeEventListener('canplay', handleCanPlay)
    }
  }, [isDragging])

  const saveProgress = (position, totalDuration, completed = false) => {
    if (saveProgressRef.current) clearTimeout(saveProgressRef.current)
    saveProgressRef.current = setTimeout(() => {
      if (video?.id && onProgressUpdate) {
        onProgressUpdate(video.id, position, totalDuration, completed)
      }
    }, 500)
  }

  useEffect(() => {
    const handleMouseMove = () => {
      setShowControls(true)
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current)
      if (isPlaying) {
        controlsTimeoutRef.current = setTimeout(() => setShowControls(false), 3000)
      }
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
    const handleFullscreenChange = () => setIsFullscreen(!!document.fullscreenElement)
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange)
  }, [])

  useEffect(() => {
    setShowPiP('pictureInPictureEnabled' in document)
  }, [])

  const togglePlay = () => {
    if (videoRef.current) {
      if (isPlaying) videoRef.current.pause()
      else videoRef.current.play()
    }
  }

  const handleSeekChange = (value) => {
    setCurrentTime(value)
  }

  const handleSeekAfterChange = (value) => {
    if (videoRef.current) {
      videoRef.current.currentTime = value
      saveProgress(value, duration)
    }
    setIsDragging(false)
  }

  const handleSeekBeforeChange = () => {
    setIsDragging(true)
  }

  const handleVolumeChange = (value) => {
    if (videoRef.current) {
      videoRef.current.volume = value
      setVolume(value)
      setIsMuted(value === 0)
    }
  }

  const toggleMute = () => {
    if (videoRef.current) {
      const newMuted = !isMuted
      videoRef.current.muted = newMuted
      setIsMuted(newMuted)
    }
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

  const togglePlaybackRate = () => {
    const rates = [0.5, 0.75, 1, 1.25, 1.5, 2]
    const currentIndex = rates.indexOf(playbackRate)
    const nextRate = rates[(currentIndex + 1) % rates.length]
    setPlaybackRate(nextRate)
    if (videoRef.current) videoRef.current.playbackRate = nextRate
  }

  const togglePiP = async () => {
    if (videoRef.current) {
      try {
        if (document.pictureInPictureElement) await document.exitPictureInPicture()
        else await videoRef.current.requestPictureInPicture()
      } catch (error) { console.error('PiP error:', error) }
    }
  }

  const formatTime = (seconds) => {
    if (!seconds || isNaN(seconds)) return '0:00'
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    const hours = Math.floor(mins / 60)
    if (hours > 0) return `${hours}:${(mins % 60).toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const skip = (seconds) => {
    if (videoRef.current) {
      const newTime = Math.max(0, Math.min(duration, currentTime + seconds))
      videoRef.current.currentTime = newTime
      setCurrentTime(newTime)
      saveProgress(newTime, duration)
    }
  }

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0

  if (error) {
    return (
      <div ref={containerRef} className="video-player-container" data-video-id={video?.id}>
        <div className="video-error-overlay">
          <div className="error-icon">⚠️</div>
          <div className="error-title">无法播放此视频</div>
          <div className="error-message">{error}</div>
          <div className="error-message" style={{fontSize: '12px', marginTop: '8px'}}>
            支持的格式: MP4, WebM, MOV, OGV, M4V
          </div>
          <a
            href={`/api/videos/${video?.id}/stream`}
            download={video?.title}
            className="download-btn"
          >
            下载视频
          </a>
        </div>
      </div>
    )
  }

  return (
    <div ref={containerRef} className={`video-player-container ${isFullscreen ? 'fullscreen' : ''}`} data-video-id={video?.id}>
      {loading && (
        <div className="video-loading-overlay">
          <div className="loading-spinner" />
        </div>
      )}

      <video
        ref={videoRef}
        className="video-element"
        src={video ? `/api/videos/${video.id}/stream` : ''}
        onClick={togglePlay}
        controls={false}
        preload="metadata"
      />

      {!isPlaying && !loading && (
        <div className="big-play-button" onClick={togglePlay}>
          <PlayCircleOutlined />
        </div>
      )}

      <div className={`video-controls ${showControls ? 'visible' : 'hidden'}`}>
        <div className="progress-bar-container">
          <div className="progress-background">
            <div className="progress-buffered" style={{ width: `${progressPercent}%` }} />
          </div>
          <Slider
            className="progress-slider"
            value={currentTime}
            max={duration || 100}
            onChange={handleSeekChange}
            onChangeComplete={handleSeekAfterChange}
            onBeforeChange={handleSeekBeforeChange}
            tooltip={{ formatter: formatTime }}
            step={1}
          />
        </div>

        <div className="controls-bar">
          <div className="controls-left">
            <button className="control-btn" onClick={togglePlay}>
              {isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
            </button>
            <button className="control-btn" onClick={() => skip(-10)}>
              <BackwardOutlined />
            </button>
            <button className="control-btn" onClick={() => skip(10)}>
              <ForwardOutlined />
            </button>
            <div className="volume-control">
              <button className="control-btn" onClick={toggleMute}>
                {isMuted ? <MutedOutlined /> : <SoundOutlined />}
              </button>
              <Slider className="volume-slider" value={isMuted ? 0 : volume} max={1} step={0.1} onChange={handleVolumeChange} />
            </div>
            <span className="time-display">{formatTime(currentTime)} / {formatTime(duration)}</span>
          </div>

          <div className="controls-right">
            <button className="control-btn playback-rate" onClick={togglePlaybackRate}>
              {playbackRate}x
            </button>
            {showPiP && (
              <button className="control-btn" onClick={togglePiP} title="画中画">
                <svg viewBox="0 0 1024 1024" width="1em" height="1em" fill="currentColor">
                  <path d="M912 100H112c-6.6 0-12 5.4-12 12v800c0 6.6 5.4 12 12 12h800c6.6 0 12-5.4 12-12V112c0-6.6-5.4-12-12-12z m-12 800H124V124h776v776z"/>
                  <path d="M560 432h288c6.6 0 12-5.4 12-12V224c0-6.6-5.4-12-12-12H560c-6.6 0-12 5.4-12 12v196c0 6.6 5.4 12 12 12z"/>
                </svg>
              </button>
            )}
            <button className="control-btn" onClick={toggleFullscreen}>
              {isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default VideoPlayer
