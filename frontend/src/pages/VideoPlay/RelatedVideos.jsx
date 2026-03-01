import React, { useState, useEffect } from 'react'
import { Card, List, Typography, Tag, Spin, Empty, Space } from 'antd'
import { VideoCameraOutlined, EyeOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { videosApi } from '@services/api'

const { Text } = Typography

function RelatedVideos({ videoId, currentVideo }) {
  const navigate = useNavigate()
  const [relatedVideos, setRelatedVideos] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchRelatedVideos()
  }, [videoId])

  const fetchRelatedVideos = async () => {
    setLoading(true)
    try {
      const response = await videosApi.getRelated(videoId, { limit: 8 })
      setRelatedVideos(response || [])
    } catch (error) {
      console.error('Failed to fetch related videos:', error)
    } finally {
      setLoading(false)
    }
  }

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

  const handleVideoClick = (id) => {
    navigate(`/videos/${id}`)
  }

  return (
    <Card
      title="相关推荐"
      className="related-videos-card"
      size="small"
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px 20px' }}>
          <Spin size="small" />
        </div>
      ) : relatedVideos.length === 0 ? (
        <Empty description="暂无相关视频" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : (
        <div className="related-videos-list">
          {relatedVideos.map((video) => (
            <div
              key={video.id}
              className="related-video-item"
              onClick={() => handleVideoClick(video.id)}
            >
              <div className="related-video-thumb">
                {video.thumbnail_path ? (
                  <img
                    src={`/api/videos/${video.id}/thumbnail`}
                    alt={video.title}
                    loading="lazy"
                  />
                ) : (
                  <div className="thumbnail-placeholder">
                    <VideoCameraOutlined />
                  </div>
                )}
                <span className="duration-badge">
                  {formatDuration(video.duration)}
                </span>
              </div>
              <div className="related-video-info">
                <div className="related-video-title">{video.title}</div>
                <div className="related-video-meta">
                  <span><EyeOutlined /> {video.view_count || 0}</span>
                  {video.tags?.length > 0 && (
                    <Tag 
                      color={video.tags[0].color || '#fb7299'} 
                      size="small"
                      style={{ marginLeft: 'auto' }}
                    >
                      {video.tags[0].name}
                    </Tag>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </Card>
  )
}

export default RelatedVideos
