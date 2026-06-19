import React, { useEffect, useState, useCallback, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Typography,
  Space,
  Select,
  Input,
  Button,
  message,
  Modal,
  Image,
  Checkbox,
} from 'antd'
import {
  DeleteOutlined,
  PlayCircleOutlined,
  HeartFilled,
  StarFilled,
  FilterOutlined,
  CheckSquareOutlined,
  CloseOutlined,
} from '@ant-design/icons'
import { imagesApi } from '@services/api'
import MediaGrid from '@components/MediaGrid'
import Slideshow from '@components/Slideshow'

const { Title } = Typography
const { Option } = Select
const { Search } = Input
const { confirm } = Modal

function ImageLibrary() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [images, setImages] = useState([])
  const [allImages, setAllImages] = useState([])
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
  const [previewImage, setPreviewImage] = useState(null)
  const [previewVisible, setPreviewVisible] = useState(false)
  const [slideshowVisible, setSlideshowVisible] = useState(false)
  const [slideshowImages, setSlideshowImages] = useState([])
  const [slideshowStartIndex, setSlideshowStartIndex] = useState(0)
  const [filterType, setFilterType] = useState(searchParams.get('filter') || 'all')
  const [selectMode, setSelectMode] = useState(false)
  const [selectedIds, setSelectedIds] = useState([])

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

  // 获取图片列表 - 使用 ref 中的最新值
  const fetchImages = useCallback(async () => {
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
      }

      console.log('Fetching images with params:', apiParams)

      const response = await imagesApi.getList(apiParams)

      setImages(response.items || [])
      setTotal(response.total || 0)
    } catch (error) {
      message.error('获取图片列表失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }, []) // 空依赖数组，使用 ref 获取最新值

  const fetchAllImages = useCallback(async () => {
    try {
      const params = {}
      if (filterType === 'liked') {
        params.liked = true
      } else if (filterType === 'favorite') {
        params.favorite = true
      }
      const response = await imagesApi.getAll(params)
      setAllImages(response || [])
    } catch (error) {
      console.error('Failed to fetch all images:', error)
    }
  }, [filterType])

  // 当依赖项变化时重新获取数据
  useEffect(() => {
    fetchImages()
  }, [currentPage, pageSize, sortBy, sortOrder, searchQuery, filterType, fetchImages])

  useEffect(() => {
    if (slideshowVisible && allImages.length === 0) {
      fetchAllImages()
    }
  }, [slideshowVisible, allImages.length, fetchAllImages])

  useEffect(() => {
    if (!selectMode) {
      setSelectedIds([])
    }
  }, [selectMode])

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
    setTimeout(() => fetchImages(), 0)
  }

  const handleSearch = (value) => {
    setSearchQuery(value)
    setCurrentPage(1)
    updateUrlParams({ search: value, page: 1 })
  }

  const handleFavorite = async (image) => {
    try {
      await imagesApi.toggleFavorite(image.id)
      message.success(image.is_favorite ? '已取消收藏' : '已添加到收藏')
      const newImages = images.map(img =>
        img.id === image.id ? { ...img, is_favorite: !img.is_favorite } : img
      )
      setImages(newImages)
      setAllImages(prev => prev.length > 0
        ? prev.map(img => img.id === image.id ? { ...img, is_favorite: !img.is_favorite } : img)
        : []
      )
    } catch (error) {
      message.error('操作失败')
    }
  }

  const handleLike = async (image) => {
    try {
      await imagesApi.toggleLike(image.id)
      message.success(image.is_liked ? '已取消喜欢' : '已添加到喜欢')
      const newImages = images.map(img =>
        img.id === image.id ? { ...img, is_liked: !img.is_liked } : img
      )
      setImages(newImages)
      setAllImages(prev => prev.length > 0
        ? prev.map(img => img.id === image.id ? { ...img, is_liked: !img.is_liked } : img)
        : []
      )
    } catch (error) {
      message.error('操作失败')
    }
  }

  const handleDelete = async (image) => {
    confirm({
      title: '确认删除',
      content: `确定要删除图片 "${image.title}" 吗？`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await imagesApi.delete(image.id)
          message.success('图片已删除')
          fetchImages()
          setAllImages([])
        } catch (error) {
          message.error('删除失败')
        }
      },
    })
  }

  const handleSelect = (imageId, selected) => {
    if (selected) {
      setSelectedIds(prev => [...prev, imageId])
    } else {
      setSelectedIds(prev => prev.filter(id => id !== imageId))
    }
  }

  const handleSelectAll = () => {
    if (selectedIds.length === images.length) {
      setSelectedIds([])
    } else {
      setSelectedIds(images.map(img => img.id))
    }
  }

  const handleBatchDelete = async () => {
    if (selectedIds.length === 0) {
      message.warning('请先选择要删除的图片')
      return
    }

    confirm({
      title: '确认批量删除',
      content: `确定要删除选中的 ${selectedIds.length} 张图片吗？此操作不可恢复。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await imagesApi.batchDelete(selectedIds)
          message.success(`已删除 ${selectedIds.length} 张图片`)
          setSelectedIds([])
          setSelectMode(false)
          fetchImages()
          setAllImages([])
        } catch (error) {
          message.error('批量删除失败')
        }
      },
    })
  }

  const handlePreview = (image) => {
    setPreviewImage(image)
    setPreviewVisible(true)
  }

  const handleSlideshow = (image) => {
    setSlideshowStartIndex(images.findIndex(img => img.id === image.id))
    setSlideshowImages(images)
    setSlideshowVisible(true)
  }

  const handleSlideshowAll = async () => {
    if (allImages.length === 0) {
      await fetchAllImages()
    }
    setSlideshowStartIndex(0)
    setSlideshowImages(allImages.length > 0 ? allImages : images)
    setSlideshowVisible(true)
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
        <Title level={2} style={{ marginBottom: 16 }}>图片库</Title>

        <Space wrap style={{ marginBottom: 16 }}>
          <Search
            placeholder="搜索图片..."
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
            style={{ width: 120 }}
          >
            <Option value="all">全部图片</Option>
            <Option value="liked">
              <HeartFilled style={{ color: '#ff4d4f', marginRight: 4 }} />已喜欢
            </Option>
            <Option value="favorite">
              <StarFilled style={{ color: '#faad14', marginRight: 4 }} />已收藏
            </Option>
          </Select>

          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={handleSlideshowAll}
            disabled={images.length === 0}
          >
            幻灯片播放 ({images.length > 0 ? images.length : '...'})
          </Button>

          {!selectMode ? (
            <Button
              icon={<CheckSquareOutlined />}
              onClick={() => setSelectMode(true)}
            >
              批量选择
            </Button>
          ) : (
            <>
              <Button
                icon={<CloseOutlined />}
                onClick={() => setSelectMode(false)}
              >
                取消
              </Button>
              <Button
                icon={<CheckSquareOutlined />}
                onClick={handleSelectAll}
              >
                {selectedIds.length === images.length ? '取消全选' : '全选'}
              </Button>
              <Button
                danger
                icon={<DeleteOutlined />}
                onClick={handleBatchDelete}
                disabled={selectedIds.length === 0}
              >
                删除 ({selectedIds.length})
              </Button>
            </>
          )}
        </Space>

        {selectMode && (
          <div style={{
            padding: '12px 16px',
            background: '#f6ffed',
            border: '1px solid #b7eb8f',
            borderRadius: 8,
            marginBottom: 16,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}>
            <Space>
              <Checkbox
                checked={selectedIds.length === images.length && images.length > 0}
                indeterminate={selectedIds.length > 0 && selectedIds.length < images.length}
                onChange={handleSelectAll}
              />
              <span>已选择 {selectedIds.length} / {images.length} 张图片</span>
            </Space>
          </div>
        )}
      </div>

      <MediaGrid
        items={images}
        type="image"
        loading={loading}
        pagination={{
          current: currentPage,
          pageSize: pageSize,
          total: total,
          onChange: handlePageChange,
        }}
        onFavorite={handleFavorite}
        onLike={handleLike}
        onItemClick={selectMode ? null : handleSlideshow}
        onDelete={handleDelete}
        onPreview={handlePreview}
        selectMode={selectMode}
        selectedIds={selectedIds}
        onSelect={handleSelect}
      />

      <Modal
        open={previewVisible}
        footer={null}
        onCancel={() => {
          setPreviewVisible(false)
          setPreviewImage(null)
        }}
        width="auto"
        centered
        destroyOnClose
      >
        {previewImage && (
          <Image
            src={imagesApi.getFileUrl(previewImage.id)}
            alt={previewImage.title}
            style={{ maxHeight: '80vh', maxWidth: '100%' }}
          />
        )}
      </Modal>

      <Slideshow
        images={slideshowImages}
        visible={slideshowVisible}
        onClose={() => setSlideshowVisible(false)}
        initialIndex={slideshowStartIndex}
      />
    </div>
  )
}

export default ImageLibrary
