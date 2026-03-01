import React, { useState, useEffect, useCallback, useRef } from 'react'
import { Modal, Button, Space, Slider, Switch, Typography, theme } from 'antd'
import {
  PlayCircleOutlined,
  PauseCircleOutlined,
  StepBackwardOutlined,
  StepForwardOutlined,
  OrderedListOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  CloseOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
} from '@ant-design/icons'

// Shuffle icon (inline SVG)
const ShuffleIcon = () => (
  <svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor">
    <path d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/>
  </svg>
)
import { imagesApi } from '@services/api'
import './index.css'

const { Text } = Typography

function Slideshow({ images, visible, onClose, initialIndex = 0 }) {
  const [currentIndex, setCurrentIndex] = useState(initialIndex)
  const [isPlaying, setIsPlaying] = useState(false)
  const [isRandom, setIsRandom] = useState(false)
  const [interval, setInterval] = useState(5)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [zoom, setZoom] = useState(1)
  const [loadedImages, setLoadedImages] = useState({})
  const containerRef = useRef(null)
  const playIntervalRef = useRef(null)

  useEffect(() => {
    if (visible) {
      setCurrentIndex(initialIndex)
      setZoom(1)
      setIsPlaying(false)
    }
  }, [visible, initialIndex])

  useEffect(() => {
    const preload = async () => {
      const preloadPromises = images.slice(0, 50).map(async (img) => {
        if (!loadedImages[img.id]) {
          return new Promise((resolve) => {
            const imgEl = new Image()
            imgEl.src = imagesApi.getFileUrl(img.id)
            imgEl.onload = () => {
              setLoadedImages(prev => ({ ...prev, [img.id]: true }))
              resolve()
            }
            imgEl.onerror = resolve
          })
        }
        return null
      })
      await Promise.all(preloadPromises)
    }

    if (visible && images.length > 0) {
      preload()
    }
  }, [visible, images])

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (!visible) return
      switch (e.key) {
        case 'ArrowRight':
        case ' ':
          e.preventDefault()
          nextImage()
          break
        case 'ArrowLeft':
          e.preventDefault()
          prevImage()
          break
        case 'Escape':
          onClose()
          break
        case 'f':
          toggleFullscreen()
          break
        case 'p':
          togglePlay()
          break
        case 'r':
          setIsRandom(prev => !prev)
          break
        default:
          break
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [visible, currentIndex, isPlaying])

  useEffect(() => {
    if (isPlaying) {
      playIntervalRef.current = setInterval(() => nextImage(), interval * 1000)
    }
    return () => {
      if (playIntervalRef.current) clearInterval(playIntervalRef.current)
    }
  }, [isPlaying, interval])

  useEffect(() => {
    const handleFullscreenChange = () => setIsFullscreen(!!document.fullscreenElement)
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange)
  }, [])

  const currentImage = images[currentIndex]

  const nextImage = useCallback(() => {
    if (images.length === 0) return
    if (isRandom) {
      let nextIndex
      do {
        nextIndex = Math.floor(Math.random() * images.length)
      } while (nextIndex === currentIndex && images.length > 1)
      setCurrentIndex(nextIndex)
    } else {
      setCurrentIndex((prev) => (prev + 1) % images.length)
    }
    setZoom(1)
  }, [images.length, currentIndex, isRandom])

  const prevImage = useCallback(() => {
    if (images.length === 0) return
    setCurrentIndex((prev) => (prev - 1 + images.length) % images.length)
    setZoom(1)
  }, [images.length])

  const togglePlay = () => setIsPlaying(prev => !prev)

  const toggleFullscreen = async () => {
    if (!document.fullscreenElement && containerRef.current) {
      try {
        await containerRef.current.requestFullscreen()
        setIsFullscreen(true)
      } catch (err) {
        console.error('Fullscreen error:', err)
      }
    } else {
      try {
        await document.exitFullscreen()
        setIsFullscreen(false)
      } catch (err) {
        console.error('Exit fullscreen error:', err)
      }
    }
  }

  const handleZoomIn = () => setZoom(prev => Math.min(prev + 0.25, 3))
  const handleZoomOut = () => setZoom(prev => Math.max(prev - 0.25, 0.5))
  const handleIntervalChange = (value) => setInterval(value)
  const handleRandomChange = (checked) => setIsRandom(checked)

  if (!currentImage) return null

  return (
    <Modal
      open={visible}
      footer={null}
      onCancel={onClose}
      width="100%"
      height="100%"
      centered
      className="slideshow-modal"
      closeIcon={null}
      maskClosable={false}
    >
      <div ref={containerRef} className={`slideshow-container ${isFullscreen ? 'fullscreen' : ''}`}>
        <div className="slideshow-header">
          <div className="slideshow-title">
            <Text strong>{currentImage.title}</Text>
            <Text className="slideshow-counter">{currentIndex + 1} / {images.length}</Text>
          </div>
          <Button type="text" icon={<CloseOutlined />} onClick={onClose} className="slideshow-close-btn" />
        </div>

        <div className="slideshow-image-wrapper">
          <div className="slideshow-image-container" style={{ transform: `scale(${zoom})` }}>
            {loadedImages[currentImage.id] ? (
              <img
                key={currentImage.id}
                src={imagesApi.getFileUrl(currentImage.id)}
                alt={currentImage.title}
                className="slideshow-image"
              />
            ) : (
              <div className="slideshow-loading">
                <div className="loading-spinner" />
              </div>
            )}
          </div>
          <Button className="slideshow-nav-btn prev" icon={<StepBackwardOutlined />} onClick={prevImage} size="large" />
          <Button className="slideshow-nav-btn next" icon={<StepForwardOutlined />} onClick={nextImage} size="large" />
        </div>

        <div className="slideshow-controls">
          <div className="controls-left">
            <Button
              type={isPlaying ? 'primary' : 'default'}
              icon={isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
              onClick={togglePlay}
              size="large"
              className="control-btn"
            >
              {isPlaying ? '暂停' : '播放'}
            </Button>
            <Button icon={<StepBackwardOutlined />} onClick={prevImage} size="large" className="control-btn" />
            <Button icon={<StepForwardOutlined />} onClick={nextImage} size="large" className="control-btn" />
          </div>

          <div className="controls-center">
            <Space size="middle">
              <Space>
                <Text>间隔:</Text>
                <Slider
                  value={interval}
                  min={1}
                  max={30}
                  marks={{ 1: '1s', 5: '5s', 10: '10s', 30: '30s' }}
                  onChange={handleIntervalChange}
                  style={{ width: 200 }}
                  tooltip={{ formatter: (v) => `${v}秒` }}
                />
              </Space>
              <Space>
                <Text>模式:</Text>
                <Switch
                  checked={isRandom}
                  onChange={handleRandomChange}
                  checkedChildren={<ShuffleIcon />}
                  unCheckedChildren={<OrderedListOutlined />}
                />
                <Text>{isRandom ? '随机' : '顺序'}</Text>
              </Space>
            </Space>
          </div>

          <div className="controls-right">
            <Space>
              <Button icon={<ZoomOutOutlined />} onClick={handleZoomOut} disabled={zoom <= 0.5} size="large" />
              <Text style={{ minWidth: 50, textAlign: 'center' }}>{Math.round(zoom * 100)}%</Text>
              <Button icon={<ZoomInOutlined />} onClick={handleZoomIn} disabled={zoom >= 3} size="large" />
              <Button
                icon={isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
                onClick={toggleFullscreen}
                size="large"
              />
            </Space>
          </div>
        </div>

        <div className="slideshow-progress">
          <div className="progress-bar" style={{ width: `${((currentIndex + 1) / images.length) * 100}%` }} />
        </div>
      </div>
    </Modal>
  )
}

export default Slideshow
