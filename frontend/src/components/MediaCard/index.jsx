import React, { useState, useEffect } from 'react'
import { Card, Badge, Tooltip, Button, Space, Checkbox } from 'antd'
import {
  HeartOutlined,
  HeartFilled,
  PlayCircleOutlined,
  ClockCircleOutlined,
  VideoCameraOutlined,
  PictureOutlined,
  EyeOutlined,
} from '@ant-design/icons'
import './index.css'

function MediaCard({
  item,
  type,
  onFavorite,
  onClick,
  onPreview,
  onDelete,
  selectMode = false,
  selected = false,
  onSelect,
}) {
  const isVideo = type === 'video'
  const Icon = isVideo ? VideoCameraOutlined : PictureOutlined
  const [thumbnailError, setThumbnailError] = useState(false)

  // Reset thumbnail error when item changes
  useEffect(() => {
    setThumbnailError(false)
  }, [item.id])

  const formatDuration = (seconds) => {
    if (!seconds) return '--:--'
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const secs = seconds % 60

    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`
  }

  const formatFileSize = (bytes) => {
    if (!bytes) return 'Unknown'
    const units = ['B', 'KB', 'MB', 'GB', 'TB']
    let size = bytes
    let unitIndex = 0
    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024
      unitIndex++
    }
    return `${size.toFixed(1)} ${units[unitIndex]}`
  }

  const thumbnailUrl = isVideo
    ? `/api/videos/${item.id}/thumbnail?t=${item.updated_at || item.id}`
    : `/api/images/${item.id}/thumbnail?t=${item.updated_at || item.id}`

  const handleClick = (e) => {
    if (selectMode) {
      e.preventDefault()
      e.stopPropagation()
      onSelect?.(item.id, !selected)
    } else {
      onClick?.(e)
    }
  }

  const handleCheckboxChange = (e) => {
    e.stopPropagation()
    onSelect?.(item.id, e.target.checked)
  }

  return (
    <Card
      hoverable={!selectMode}
      className={`media-card ${selectMode ? 'select-mode' : ''} ${selected ? 'selected' : ''}`}
      onClick={handleClick}
      cover={
        <div className="media-thumbnail">
          {/* Selection Checkbox */}
          {selectMode && (
            <div className="select-checkbox" onClick={(e) => e.stopPropagation()}>
              <Checkbox
                checked={selected}
                onChange={handleCheckboxChange}
              />
            </div>
          )}

          {!thumbnailError ? (
            <img
              alt={item.title}
              src={thumbnailUrl}
              className="thumbnail-image"
              onError={() => setThumbnailError(true)}
            />
          ) : (
            <div className="thumbnail-placeholder">
              <Icon style={{ fontSize: 48 }} />
            </div>
          )}
          {isVideo && item.duration > 0 && (
            <div className="duration-badge">
              <ClockCircleOutlined /> {formatDuration(item.duration)}
            </div>
          )}
          {!selectMode && isVideo && (
            <div className="play-overlay">
              <PlayCircleOutlined style={{ fontSize: 48 }} />
            </div>
          )}
          {!selectMode && !isVideo && (
            <div className="preview-overlay">
              <EyeOutlined style={{ fontSize: 32 }} />
            </div>
          )}
        </div>
      }
      actions={selectMode ? [] : [
        <Tooltip title={item.is_favorite ? '取消收藏' : '收藏'} key="favorite">
          <Button
            type="text"
            icon={item.is_favorite ? <HeartFilled style={{ color: '#ff4d4f' }} /> : <HeartOutlined />}
            onClick={(e) => {
              e.stopPropagation()
              onFavorite?.(item)
            }}
          />
        </Tooltip>,
        <Tooltip title="预览" key="preview">
          <Button
            type="text"
            icon={<EyeOutlined />}
            onClick={(e) => {
              e.stopPropagation()
              onPreview?.(item)
            }}
          />
        </Tooltip>,
        <span className="file-size" key="size">{formatFileSize(item.file_size)}</span>,
        item.width && item.height && (
          <span className="resolution" key="resolution">{item.width}x{item.height}</span>
        ),
      ].filter(Boolean)}
    >
      <Card.Meta
        title={
          <Tooltip title={item.title}>
            <div className="media-title">{item.title}</div>
          </Tooltip>
        }
        description={
          <Space size="small" wrap>
            {item.source_name && (
              <Badge text={item.source_name} status="default" />
            )}
          </Space>
        }
      />
    </Card>
  )
}

export default MediaCard
