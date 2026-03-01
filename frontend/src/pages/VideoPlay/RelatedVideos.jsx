import React, { useState, useEffect } from 'react'
import { Card, List, Typography, Tag, Spin, Empty } from 'antd'
import { VideoCameraOutlined, TagsOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { videosApi } from '@services/api'

const { Title, Text } = Typography

function RelatedVideos({ videoId }) {
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
      title={
        <span>
          <TagsOutlined style={{ marginRight: 8 }} />
          相关推荐
        </span>
      }
      className="related-videos-card"
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: '20px' }}>
          <Spin size="small" />
        </div>
      ) : relatedVideos.length === 0 ? (
        <Empty description="暂无相关视频" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : (
        <List
          itemLayout="horizontal"
          dataSource={relatedVideos}
          renderItem={(video) => (
            <List.Item
              className="related-video-item"
              onClick={() => handleVideoClick(video.id)}
              style={{ cursor: 'pointer' }}
            >
              <List.Item.Meta
                avatar={
                  <div className="related-video-thumbnail">
                    {video.thumbnail_path ? (
                      <img
                        src={`/api/videos/${video.id}/thumbnail`}
                        alt={video.title}
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
                }
                title={
                  <Text ellipsis style={{ maxWidth: 200 }}>
                    {video.title}
                  </Text>
                }
                description={
                  <Space size="small" wrap>
                    {video.tags?.slice(0, 2).map((tag) => (
                      <Tag key={tag.id} color={tag.color} size="small">
                        {tag.name}
                      </Tag>
                    ))}
                    {video.tags?.length > 2 && (
                      <Tag size="small">+{video.tags.length - 2}</Tag>
                    )}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      )}
    </Card>
  )
}

export default RelatedVideos
