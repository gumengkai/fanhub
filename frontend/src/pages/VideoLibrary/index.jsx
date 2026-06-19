import React, { useEffect, useState, useCallback, useRef } from 'react'
import { useSearchParams, useNavigate, useLocation } from 'react-router-dom'
import {
  Typography,
  Space,
  Select,
  Input,
  Button,
  message,
} from 'antd'
import {
  DeleteOutlined,
  HeartOutlined,
  HeartFilled,
  StarOutlined,
  StarFilled,
  PlayCircleOutlined,
  FilterOutlined,
  EyeInvisibleOutlined,
} from '@ant-design/icons'
import { videosApi } from '@services/api'
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

  // 从 URL 参数恢复状态
  const urlSort = searchParams.get('sort') || 'random_desc'
  const urlSortParts = urlSort.split('_')
  
  const [currentPage, setCurrentPage] = useState(parseInt(searchParams.get('page') || '1', 10))
  const [pageSize, setPageSize] = useState(parseInt(searchParams.get('pageSize') || '24', 10))
  const [total, setTotal] = useState(0)
  const [sortBy, setSortBy] = useState(urlSortParts[0] || 'random')
  const [sortOrder, setSortOrder] = useState(urlSortParts[1] || 'desc')
  const [searchQuery, setSearchQuery] = useState(searchParams.get('search') || '')
  const [filterType, setFilterType] = useState(searchParams.get('filter') || 'all')

  // 使用 ref 存储最新的请求参数，避免闭包问题
  const latestParams = useRef({
    currentPage,
    pageSize,
    sortBy,
    sortOrder,
    searchQuery,
    filterType
  })

  // 同步 ref 与 state
  useEffect(() => {
    latestParams.current = {
      currentPage,
      pageSize,
      sortBy,
      sortOrder,
      searchQuery,
      filterType
    }
  }, [currentPage, pageSize, sortBy, sortOrder, searchQuery, filterType])

  // 获取视频列表 - 使用 ref 中的最新值
  const fetchVideos = useCallback(async () => {
    const params = latestParams.current
    setLoading(true)
    try {
      const apiParams = {
        page: params.currentPage,
        per_page: params.pageSize,
        sort_by: params.sortBy,
        order: params.sortOrder,
        search: params.searchQuery,
      }
      if (params.filterType === 'liked') {
        apiParams.liked = true
      } else if (params.filterType === 'favorite') {
        apiParams.favorite = true
      } else if (params.filterType === 'unwatched') {
        apiParams.unwatched = true
      }

      console.log('Fetching videos with params:', apiParams)

      const response = await videosApi.getList(apiParams)

      setVideos(response.items || [])
      setTotal(response.total || 0)
    } catch (error) {
      message.error('获取视频列表失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }, []) // 空依赖数组，使用 ref 获取最新值

  // 当依赖项变化时重新获取数据
  useEffect(() => {
    fetchVideos()
  }, [currentPage, pageSize, sortBy, sortOrder, searchQuery, filterType, fetchVideos])

  // 更新 URL 参数
  const updateUrlParams = useCallback((updates) => {
    const params = {}
    const currentParams = latestParams.current
    const page = updates.page ?? currentParams.currentPage
    const size = updates.pageSize ?? currentParams.pageSize
    const sort = updates.sort ?? `${currentParams.sortBy}_${currentParams.sortOrder}`
    const search = updates.search ?? currentParams.searchQuery
    const filter = updates.filter ?? currentParams.filterType

    if (page > 1) params.page = page.toString()
    if (size !== 24) params.pageSize = size.toString()
    if (sort !== 'random_desc') params.sort = sort
    if (search) params.search = search
    if (filter !== 'all') params.filter = filter

    setSearchParams(params, { replace: true })
  }, [setSearchParams])

  const handlePageChange = (page, newPageSize) => {
    setCurrentPage(page)
    setPageSize(newPageSize)
    updateUrlParams({ page, pageSize: newPageSize })
  }

  const handleSortChange = (value) => {
    console.log('Sort changed to:', value)
    // 正确拆分：最后一个 _ 后面是 order，前面的是 field
    const lastUnderscoreIndex = value.lastIndexOf('_')
    const field = value.slice(0, lastUnderscoreIndex)
    const order = value.slice(lastUnderscoreIndex + 1)
    console.log('Parsed field:', field, 'order:', order)
    setSortBy(field)
    setSortOrder(order)
    setCurrentPage(1)
    updateUrlParams({ sort: value, page: 1 })
    // 立即触发重新获取，不等待 useEffect
    setTimeout(() => fetchVideos(), 0)
  }

  const handleSearch = (value) => {
    setSearchQuery(value)
    setCurrentPage(1)
    updateUrlParams({ search: value, page: 1 })
  }

  const handleFavorite = async (video) => {
    try {
      await videosApi.toggleFavorite(video.id)
      message.success(video.is_favorite ? '已取消收藏' : '已添加到收藏')
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
    const returnPath = location.pathname + location.search
    navigate(`/videos/${video.id}`, { state: { returnPath } })
  }

  const handleShortVideoMode = () => {
    navigate('/short-video')
  }

  const sortOptions = [
    { value: 'random_desc', label: '随机排序' },
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
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
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
            value={filterType}
            onChange={(value) => {
              setFilterType(value)
              setCurrentPage(1)
              updateUrlParams({ filter: value, page: 1 })
            }}
            style={{ width: 130 }}
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
            onClick={handleShortVideoMode}
          >
            短视频模式
          </Button>
        </Space>
      </div>

      <MediaGrid
        items={videos}
        type="video"
        loading={loading}
        pagination={{
          current: currentPage,
          pageSize: pageSize,
          total: total,
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
