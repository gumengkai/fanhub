import React, { useEffect, useState, useCallback } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import {
  Typography,
  Space,
  Select,
  Input,
  Button,
  message,
  Empty,
  Switch,
  Tag,
} from 'antd'
import {
  DeleteOutlined,
  HeartOutlined,
  HeartFilled,
  PlayCircleOutlined,
  FilterOutlined,
  TagsOutlined,
} from '@ant-design/icons'
import { videosApi, tagsApi } from '@services/api'
import MediaGrid from '@components/MediaGrid'

const { Title } = Typography
const { Option } = Select
const { Search } = Input

function VideoLibrary() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const [videos, setVideos] = useState([])
  const [loading, setLoading] = useState(false)
  const [tags, setTags] = useState([])
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 24,
    total: 0,
  })
  const [sortBy, setSortBy] = useState('created_at')
  const [sortOrder, setSortOrder] = useState('desc')
  const [searchQuery, setSearchQuery] = useState('')
  const [favoriteOnly, setFavoriteOnly] = useState(false)
  const [selectedTag, setSelectedTag] = useState(null)

  // Fetch all tags for filter dropdown
  useEffect(() => {
    fetchTags()
  }, [])

  const fetchTags = async () => {
    try {
      const response = await tagsApi.getList()
      setTags(response || [])
    } catch (error) {
      console.error('Failed to fetch tags:', error)
    }
  }

  const fetchVideos = useCallback(async () => {
    setLoading(true)
    try {
      const params = {
        page: pagination.current,
        per_page: pagination.pageSize,
        sort_by: sortBy,
        order: sortOrder,
        search: searchQuery || searchParams.get('search') || '',
      }
      if (favoriteOnly) {
        params.favorite = true
      }
      if (selectedTag) {
        params.tag_id = selectedTag
      }

      const response = await videosApi.getList(params)

      setVideos(response.items || [])
      setPagination({
        ...pagination,
        total: response.total || 0,
      })
    } catch (error) {
      message.error('获取视频列表失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }, [pagination.current, pagination.pageSize, sortBy, sortOrder, searchQuery, searchParams, favoriteOnly, selectedTag])

  useEffect(() => {
    fetchVideos()
  }, [fetchVideos])

  const handlePageChange = (page, pageSize) => {
    setPagination({
      ...pagination,
      current: page,
      pageSize: pageSize,
    })
  }

  const handleSortChange = (value) => {
    const [field, order] = value.split('_')
    setSortBy(field)
    setSortOrder(order)
    setPagination({ ...pagination, current: 1 })
  }

  const handleSearch = (value) => {
    setSearchQuery(value)
    setPagination({ ...pagination, current: 1 })
    if (value) {
      setSearchParams({ search: value })
    } else {
      setSearchParams({})
    }
  }

  const handleFavorite = async (video) => {
    try {
      await videosApi.toggleFavorite(video.id)
      message.success(video.is_favorite ? '已取消收藏' : '已添加到收藏')
      // 只更新当前视频的收藏状态，不刷新整个列表
      setVideos(videos.map(v =>
        v.id === video.id ? { ...v, is_favorite: !v.is_favorite } : v
      ))
    } catch (error) {
      message.error('操作失败')
    }
  }

  const handleDelete = async (video) => {
    try {
      await videosApi.delete(video.id)
      message.success('视频已删除')
      fetchVideos()
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handlePlay = (video) => {
    navigate(`/videos/${video.id}`)
  }

  const handlePlayByTag = () => {
    if (selectedTag) {
      navigate(`/short-video?tag=${selectedTag}`)
    } else {
      navigate('/short-video')
    }
  }

  const handleTagChange = (value) => {
    setSelectedTag(value)
    setPagination({ ...pagination, current: 1 })
  }

  const sortOptions = [
    { value: 'created_at_desc', label: '最新添加' },
    { value: 'created_at_asc', label: '最早添加' },
    { value: 'title_asc', label: '名称 (A-Z)' },
    { value: 'title_desc', label: '名称 (Z-A)' },
    { value: 'file_size_desc', label: '文件大小 (大 - 小)' },
    { value: 'file_size_asc', label: '文件大小 (小 - 大)' },
  ]

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ marginBottom: 16 }}>视频库</Title>

        <Space wrap style={{ marginBottom: 16 }}>
          <Search
            placeholder="搜索视频..."
            allowClear
            defaultValue={searchParams.get('search') || ''}
            onSearch={handleSearch}
            style={{ width: 300 }}
          />

          <Select
            value={`${sortBy}_${sortOrder}`}
            onChange={handleSortChange}
            style={{ width: 180 }}
          >
            {sortOptions.map((opt) => (
              <Option key={opt.value} value={opt.value}>{opt.label}</Option>
            ))}
          </Select>

          <Select
            value={selectedTag}
            onChange={handleTagChange}
            style={{ width: 150 }}
            placeholder={<><TagsOutlined /> 按标签筛选</>}
            allowClear
          >
            {tags.map((tag) => (
              <Option key={tag.id} value={tag.id}>
                <Tag color={tag.color} style={{ margin: 0 }}>{tag.name}</Tag>
              </Option>
            ))}
          </Select>

          <Space>
            <FilterOutlined />
            <Switch
              checked={favoriteOnly}
              onChange={setFavoriteOnly}
              checkedChildren="收藏"
              unCheckedChildren="全部"
              size="small"
            />
          </Space>

          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={handlePlayByTag}
          >
            {selectedTag ? '播放标签视频' : '短视频模式'}
          </Button>
        </Space>
      </div>

      <MediaGrid
        items={videos}
        type="video"
        loading={loading}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          onChange: handlePageChange,
        }}
        onFavorite={handleFavorite}
        onItemClick={handlePlay}
        onDelete={handleDelete}
      />
    </div>
  )
}

export default VideoLibrary
