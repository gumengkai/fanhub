import React, { useEffect, useState, useCallback } from 'react'
import { useSearchParams, useNavigate, useLocation } from 'react-router-dom'
import {
  Typography,
  Space,
  Select,
  Input,
  Button,
  message,
  Empty,
  Tag,
} from 'antd'
import {
  DeleteOutlined,
  HeartOutlined,
  HeartFilled,
  StarOutlined,
  StarFilled,
  PlayCircleOutlined,
  FilterOutlined,
  TagsOutlined,
  EyeInvisibleOutlined,
} from '@ant-design/icons'
import { videosApi, tagsApi } from '@services/api'
import MediaGrid from '@components/MediaGrid'

const { Title } = Typography
const { Option } = Select
const { Search } = Input

function VideoLibrary() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const location = useLocation()
  const [videos, setVideos] = useState([])
  const [loading, setLoading] = useState(false)
  const [tags, setTags] = useState([])

  // 从 URL 参数恢复状态
  const [pagination, setPagination] = useState({
    current: parseInt(searchParams.get('page') || '1', 10),
    pageSize: parseInt(searchParams.get('pageSize') || '24', 10),
    total: 0,
  })
  const [sortBy, setSortBy] = useState(searchParams.get('sort')?.split('_')[0] || 'created_at')
  const [sortOrder, setSortOrder] = useState(searchParams.get('sort')?.split('_')[1] || 'desc')
  const [searchQuery, setSearchQuery] = useState(searchParams.get('search') || '')
  const [filterType, setFilterType] = useState(searchParams.get('filter') || 'all')
  const [selectedTag, setSelectedTag] = useState(searchParams.get('tag') ? parseInt(searchParams.get('tag'), 10) : null)

  // 更新 URL 参数的辅助函数
  const updateUrlParams = useCallback((updates = {}) => {
    const params = {}
    const page = updates.page ?? pagination.current
    const pageSize = updates.pageSize ?? pagination.pageSize
    const sort = updates.sort ?? `${sortBy}_${sortOrder}`
    const search = updates.search ?? searchQuery
    const filter = updates.filter ?? filterType
    const tag = updates.tag ?? selectedTag

    // 只添加非默认值的参数
    if (page > 1) params.page = page.toString()
    if (pageSize !== 24) params.pageSize = pageSize.toString()
    if (sort !== 'created_at_desc') params.sort = sort
    if (search) params.search = search
    if (filter !== 'all') params.filter = filter
    if (tag) params.tag = tag.toString()

    setSearchParams(params, { replace: true })
  }, [pagination.current, pagination.pageSize, sortBy, sortOrder, searchQuery, filterType, selectedTag, setSearchParams])

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
      if (filterType === 'liked') {
        params.liked = true
      } else if (filterType === 'favorite') {
        params.favorite = true
      } else if (filterType === 'unwatched') {
        params.unwatched = true
      }
      if (selectedTag) {
        params.tag_id = selectedTag
      }

      const response = await videosApi.getList(params)

      setVideos(response.items || [])
      setPagination(prev => ({
        ...prev,
        total: response.total || 0,
      }))
    } catch (error) {
      message.error('获取视频列表失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }, [pagination.current, pagination.pageSize, sortBy, sortOrder, searchQuery, searchParams, filterType, selectedTag])

  useEffect(() => {
    fetchVideos()
  }, [fetchVideos])

  const handlePageChange = (page, pageSize) => {
    setPagination(prev => ({
      ...prev,
      current: page,
      pageSize: pageSize,
    }))
    updateUrlParams({ page, pageSize })
  }

  const handleSortChange = (value) => {
    const [field, order] = value.split('_')
    setSortBy(field)
    setSortOrder(order)
    setPagination(prev => ({ ...prev, current: 1 }))
    updateUrlParams({ sort: value, page: 1 })
  }

  const handleSearch = (value) => {
    setSearchQuery(value)
    setPagination(prev => ({ ...prev, current: 1 }))
    updateUrlParams({ search: value, page: 1 })
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

  const handleLike = async (video) => {
    try {
      await videosApi.toggleLike(video.id)
      message.success(video.is_liked ? '已取消喜欢' : '已添加到喜欢')
      // 只更新当前视频的喜欢状态，不刷新整个列表
      setVideos(videos.map(v =>
        v.id === video.id ? { ...v, is_liked: !v.is_liked } : v
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
    // 保存当前路径（包含所有 URL 参数）用于返回
    const returnPath = location.pathname + location.search
    navigate(`/videos/${video.id}`, { state: { returnPath } })
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
    setPagination(prev => ({ ...prev, current: 1 }))
    updateUrlParams({ tag: value, page: 1 })
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

          <Select
            value={filterType}
            onChange={(value) => {
              setFilterType(value)
              setPagination(prev => ({ ...prev, current: 1 }))
              updateUrlParams({ filter: value, page: 1 })
            }}
            style={{ width: 130 }}
            placeholder={<><FilterOutlined /> 筛选</>}
          >
            <Option value="all">全部视频</Option>
            <Option value="liked">
              <HeartFilled style={{ color: '#ff4d4f', marginRight: 4 }} />已喜欢
            </Option>
            <Option value="favorite">
              <StarFilled style={{ color: '#faad14', marginRight: 4 }} />已收藏
            </Option>
            <Option value="unwatched">
              <EyeInvisibleOutlined style={{ marginRight: 4 }} />未观看
            </Option>
          </Select>

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
        onLike={handleLike}
        onItemClick={handlePlay}
        onDelete={handleDelete}
      />
    </div>
  )
}

export default VideoLibrary
