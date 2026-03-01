import React from 'react'
import { Card, Badge, Tooltip, Button, Space } from 'antd'
import {
  HeartOutlined,
  HeartFilled,
  PlayCircleOutlined,
  ClockCircleOutlined,
  VideoCameraOutlined,
  PictureOutlined,
} from '@ant-design/icons'
import './index.css'

function MediaCard({ item, type, onFavorite, onClick, onDelete }) {
  const isVideo = type === 'video'
  const Icon = isVideo ? VideoCameraOutlined : PictureOutlined

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
    ? item.thumbnail_path
      ? `/api/videos/${item.id}/thumbnail`
      : null
    : `/api/images/${item.id}/thumbnail`

  return (
    <Card
      hoverable
      className="media-card"
      cover={
        <div className="media-thumbnail" onClick={onClick}>
          {thumbnailUrl ? (
            <img
              alt={item.title}
              src={thumbnailUrl}
              className="thumbnail-image"
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
          {isVideo && (
            <div className="play-overlay">
              <PlayCircleOutlined style={{ fontSize: 48 }} />
            </div>
          )}
        </div>
      }
      actions={[
        <Tooltip title={item.is_favorite ? '取消收藏' : '收藏'}>
          <Button
            type="text"
            icon={item.is_favorite ? <HeartFilled style={{ color: '#ff4d4f' }} /> : <HeartOutlined />}
            onClick={(e) => {
              e.stopPropagation()
              onFavorite?.(item)
            }}
          />
        </Tooltip>,
        <span className="file-size">{formatFileSize(item.file_size)}</span>,
        item.width && item.height && (
          <span className="resolution">{item.width}x{item.height}</span>
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
