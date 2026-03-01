import React, { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Typography,
  Button,
  Space,
  Tag,
  Divider,
  Row,
  Col,
  Card,
  Descriptions,
  message,
  Popconfirm,
  Empty,
  Spin,
} from 'antd'
import {
  HeartOutlined,
  HeartFilled,
  LeftOutlined,
  DeleteOutlined,
  EditOutlined,
  TagsOutlined,
  ClockCircleOutlined,
  FileOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons'
import { videosApi } from '@services/api'
import VideoPlayer from '@components/VideoPlayer'
import VideoTags from './VideoTags'
import RelatedVideos from './RelatedVideos'
import './index.css'

const { Title, Text, Paragraph } = Typography

function VideoPlay() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [video, setVideo] = useState(null)
  const [loading, setLoading] = useState(true)
  const [isEditing, setIsEditing] = useState(false)
  const [editedTitle, setEditedTitle] = useState('')
  const [editedDescription, setEditedDescription] = useState('')

  const fetchVideo = useCallback(async () => {
    setLoading(true)
    try {
      const response = await videosApi.getById(id)
      setVideo(response)
      setEditedTitle(response.title)
      setEditedDescription(response.description || '')
    } catch (error) {
      message.error('获取视频信息失败')
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    fetchVideo()
  }, [fetchVideo])

  const handleFavorite = async () => {
    try {
      const response = await videosApi.toggleFavorite(video.id)
      setVideo({ ...video, is_favorite: response.is_favorite })
      message.success(response.is_favorite ? '已添加到收藏' : '已取消收藏')
    } catch (error) {
      message.error('操作失败')
    }
  }

  const handleDelete = async () => {
    try {
      await videosApi.delete(video.id)
      message.success('视频已删除')
      navigate('/videos')
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleSaveEdit = async () => {
    try {
      const response = await videosApi.update(video.id, {
        title: editedTitle,
        description: editedDescription,
      })
      setVideo(response)
      setIsEditing(false)
      message.success('保存成功')
    } catch (error) {
      message.error('保存失败')
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

  const formatFileSize = (bytes) => {
    if (!bytes) return 'Unknown'
    const units = ['B', 'KB', 'MB', 'GB', 'TB']
    let size = bytes
    let unitIndex = 0
    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024
      unitIndex++
    }
    return `${size.toFixed(2)} ${units[unitIndex]}`
  }

  if (loading) {
    return (
      <div className="video-play-loading">
        <Spin size="large" tip="加载中..." />
      </div>
    )
  }

  if (!video) {
    return (
      <Empty
        description="视频不存在"
        extra={<Button onClick={() => navigate('/videos')}>返回视频库</Button>}
      />
    )
  }

  return (
    <div className="video-play-page">
      {/* Back button */}
      <Button
        icon={<LeftOutlined />}
        onClick={() => navigate('/videos')}
        style={{ marginBottom: 16 }}
      >
        返回视频库
      </Button>

      <Row gutter={[24, 24]}>
        {/* Main content - Video Player */}
        <Col xs={24} lg={16}>
          <VideoPlayer video={video} />

          {/* Video Info */}
          <Card className="video-info-card">
            {isEditing ? (
              <div className="video-edit-form">
                <input
                  className="video-title-input"
                  value={editedTitle}
                  onChange={(e) => setEditedTitle(e.target.value)}
                  placeholder="视频标题"
                />
                <textarea
                  className="video-desc-input"
                  value={editedDescription}
                  onChange={(e) => setEditedDescription(e.target.value)}
                  placeholder="视频描述（可选）"
                  rows={3}
                />
                <Space>
                  <Button type="primary" onClick={handleSaveEdit}>
                    保存
                  </Button>
                  <Button onClick={() => setIsEditing(false)}>取消</Button>
                </Space>
              </div>
            ) : (
              <>
                <div className="video-header">
                  <Title level={3} className="video-title">
                    {video.title}
                  </Title>
                  <Space>
                    <Button
                      type={video.is_favorite ? 'primary' : 'default'}
                      icon={video.is_favorite ? <HeartFilled /> : <HeartOutlined />}
                      onClick={handleFavorite}
                    >
                      {video.is_favorite ? '已收藏' : '收藏'}
                    </Button>
                    <Button
                      icon={<EditOutlined />}
                      onClick={() => setIsEditing(true)}
                    >
                      编辑
                    </Button>
                    <Popconfirm
                      title="确认删除"
                      description="删除后将无法恢复，是否继续？"
                      onConfirm={handleDelete}
                      okText="删除"
                      cancelText="取消"
                    >
                      <Button danger icon={<DeleteOutlined />}>
                        删除
                      </Button>
                    </Popconfirm>
                  </Space>
                </div>

                {video.description && (
                  <Paragraph className="video-description">
                    {video.description}
                  </Paragraph>
                )}

                {/* Tags */}
                <VideoTags videoId={video.id} tags={video.tags} onUpdate={fetchVideo} />

                <Divider />

                {/* Video metadata */}
                <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }}>
                  <Descriptions.Item label={<><ClockCircleOutlined /> 时长</>}>
                    {formatDuration(video.duration)}
                  </Descriptions.Item>
                  <Descriptions.Item label={<><VideoCameraOutlined /> 分辨率</>}>
                    {video.width && video.height ? `${video.width}x${video.height}` : 'Unknown'}
                  </Descriptions.Item>
                  <Descriptions.Item label={<><FileOutlined /> 文件大小</>}>
                    {formatFileSize(video.file_size)}
                  </Descriptions.Item>
                  <Descriptions.Item label="来源">
                    {video.source?.name || 'Unknown'}
                  </Descriptions.Item>
                  <Descriptions.Item label="添加时间">
                    {new Date(video.created_at).toLocaleString()}
                  </Descriptions.Item>
                  <Descriptions.Item label="更新时间">
                    {new Date(video.updated_at).toLocaleString()}
                  </Descriptions.Item>
                </Descriptions>
              </>
            )}
          </Card>
        </Col>

        {/* Sidebar - Related Videos */}
        <Col xs={24} lg={8}>
          <RelatedVideos videoId={video.id} />
        </Col>
      </Row>
    </div>
  )
}

export default VideoPlay
