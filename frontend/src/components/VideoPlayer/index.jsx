import React, { useState, useRef, useEffect } from 'react'
import { Slider, Tooltip } from 'antd'
import {
  PlayCircleOutlined,
  PauseCircleOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  SoundOutlined,
  MutedOutlined,
  BackwardOutlined,
  ForwardOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import './index.css'

function VideoPlayer({ video }) {
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
  const controlsTimeoutRef = useRef(null)

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    const handleTimeUpdate = () => setCurrentTime(video.currentTime)
    const handleLoadedMetadata = () => {
      setDuration(video.duration)
      setLoading(false)
    }
    const handlePlay = () => setIsPlaying(true)
    const handlePause = () => setIsPlaying(false)
    const handleEnded = () => setIsPlaying(false)
    const handleWaiting = () => setLoading(true)
    const handleCanPlay = () => setLoading(false)

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
  }, [])

  // Auto-hide controls
  useEffect(() => {
    const handleMouseMove = () => {
      setShowControls(true)
      if (controlsTimeoutRef.current) {
        clearTimeout(controlsTimeoutRef.current)
      }
      controlsTimeoutRef.current = setTimeout(() => {
        if (isPlaying) {
          setShowControls(false)
        }
      }, 3000)
    }

    const container = containerRef.current
    if (container) {
      container.addEventListener('mousemove', handleMouseMove)
      container.addEventListener('mouseleave', () => {
        if (isPlaying) setShowControls(false)
      })
      container.addEventListener('mouseenter', () => setShowControls(true))
    }

    return () => {
      if (container) {
        container.removeEventListener('mousemove', handleMouseMove)
      }
      if (controlsTimeoutRef.current) {
        clearTimeout(controlsTimeoutRef.current)
      }
    }
  }, [isPlaying])

  const handlePlayPause = () => {
    if (videoRef.current) {
      if (isPlaying) {
        videoRef.current.pause()
      } else {
        videoRef.current.play()
      }
    }
  }

  const handleSeek = (value) => {
    if (videoRef.current) {
      videoRef.current.currentTime = value
      setCurrentTime(value)
    }
  }

  const handleSeekChange = (value) => {
    // While dragging, just update the display
    setCurrentTime(value)
  }

  const handleSeekAfterChange = (value) => {
    // After drag ends, actually seek
    if (videoRef.current) {
      videoRef.current.currentTime = value
    }
  }

  const handleVolumeChange = (value) => {
    if (videoRef.current) {
      videoRef.current.volume = value
      setVolume(value)
      setIsMuted(value === 0)
    }
  }

  const handleMuteToggle = () => {
    if (videoRef.current) {
      const newMuted = !isMuted
      videoRef.current.muted = newMuted
      setIsMuted(newMuted)
    }
  }

  const handleFullscreen = () => {
    if (!document.fullscreenElement) {
      containerRef.current?.requestFullscreen()
      setIsFullscreen(true)
    } else {
      document.exitFullscreen()
      setIsFullscreen(false)
    }
  }

  const handlePlaybackRateChange = () => {
    const rates = [0.5, 0.75, 1, 1.25, 1.5, 2]
    const currentIndex = rates.indexOf(playbackRate)
    const nextRate = rates[(currentIndex + 1) % rates.length]
    setPlaybackRate(nextRate)
    if (videoRef.current) {
      videoRef.current.playbackRate = nextRate
    }
  }

  const formatTime = (seconds) => {
    if (!seconds || isNaN(seconds)) return '0:00'
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    const hours = Math.floor(mins / 60)
    if (hours > 0) {
      return `${hours}:${(mins % 60).toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    }
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const skip = (seconds) => {
    if (videoRef.current) {
      const newTime = Math.max(0, Math.min(duration, currentTime + seconds))
      videoRef.current.currentTime = newTime
      setCurrentTime(newTime)
    }
  }

  const handleVideoClick = (e) => {
    // Don't pause if clicking on controls
    if (e.target === videoRef.current) {
      handlePlayPause()
    }
  }

  return (
    <div
      ref={containerRef}
      className={`video-player-container ${isFullscreen ? 'fullscreen' : ''}`}
    >
      {loading && (
        <div className="video-loading-overlay">
          <div className="loading-spinner" />
        </div>
      )}

      <video
        ref={videoRef}
        className="video-element"
        src={video ? `/api/videos/${video.id}/stream` : ''}
        onClick={handleVideoClick}
        controls={false}
        preload="metadata"
      />

      {/* Big play button when paused */}
      {!isPlaying && !loading && (
        <div className="big-play-button" onClick={handlePlayPause}>
          <PlayCircleOutlined />
        </div>
      )}

      {/* Controls */}
      <div className={`video-controls ${showControls ? 'visible' : 'hidden'}`}>
        {/* Progress bar */}
        <div className="progress-bar-container">
          <Slider
            className="progress-slider"
            value={currentTime}
            max={duration || 100}
            onChange={handleSeekChange}
            onChangeComplete={handleSeekAfterChange}
            tooltip={{ formatter: formatTime }}
            step={1}
          />
        </div>

        <div className="controls-bar">
          <div className="controls-left">
            <button className="control-btn" onClick={handlePlayPause}>
              {isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
            </button>

            <button className="control-btn" onClick={() => skip(-10)}>
              <BackwardOutlined />
            </button>

            <button className="control-btn" onClick={() => skip(10)}>
              <ForwardOutlined />
            </button>

            <div className="volume-control">
              <button className="control-btn" onClick={handleMuteToggle}>
                {isMuted ? <MutedOutlined /> : <SoundOutlined />}
              </button>
              <Slider
                className="volume-slider"
                value={isMuted ? 0 : volume}
                max={1}
                step={0.1}
                onChange={handleVolumeChange}
              />
            </div>

            <span className="time-display">
              {formatTime(currentTime)} / {formatTime(duration)}
            </span>
          </div>

          <div className="controls-right">
            <button className="control-btn playback-rate" onClick={handlePlaybackRateChange}>
              {playbackRate}x
            </button>

            <button className="control-btn" onClick={handleFullscreen}>
              {isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default VideoPlayer
