import React, { useEffect, useState, useCallback } from 'react'
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
  Switch,
  Checkbox,
} from 'antd'
import {
  DeleteOutlined,
  EyeOutlined,
  PlayCircleOutlined,
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
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 24,
    total: 0,
  })
  const [sortBy, setSortBy] = useState('created_at')
  const [sortOrder, setSortOrder] = useState('desc')
  const [searchQuery, setSearchQuery] = useState('')
  const [previewImage, setPreviewImage] = useState(null)
  const [previewVisible, setPreviewVisible] = useState(false)
  const [slideshowVisible, setSlideshowVisible] = useState(false)
  const [slideshowImages, setSlideshowImages] = useState([])
  const [slideshowStartIndex, setSlideshowStartIndex] = useState(0)
  const [favoriteOnly, setFavoriteOnly] = useState(false)
  const [selectMode, setSelectMode] = useState(false)
  const [selectedIds, setSelectedIds] = useState([])

  const fetchImages = useCallback(async () => {
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

      const response = await imagesApi.getList(params)

      setImages(response.items || [])
      setPagination({
        ...pagination,
        total: response.total || 0,
      })
    } catch (error) {
      message.error('获取图片列表失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }, [pagination.current, pagination.pageSize, sortBy, sortOrder, searchQuery, searchParams, favoriteOnly])

  const fetchAllImages = useCallback(async () => {
    try {
      const params = {}
      if (favoriteOnly) {
        params.favorite = true
      }
      const response = await imagesApi.getAll(params)
      setAllImages(response || [])
    } catch (error) {
      console.error('Failed to fetch all images:', error)
    }
  }, [favoriteOnly])

  useEffect(() => {
    fetchImages()
  }, [fetchImages])

  useEffect(() => {
    if (slideshowVisible && allImages.length === 0) {
      fetchAllImages()
    }
  }, [slideshowVisible])

  // Clear selection when leaving select mode
  useEffect(() => {
    if (!selectMode) {
      setSelectedIds([])
    }
  }, [selectMode])

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

  const handleFavorite = async (image) => {
    try {
      await imagesApi.toggleFavorite(image.id)
      message.success(image.is_favorite ? '已取消收藏' : '已添加到收藏')
      // 只更新当前图片的收藏状态，不刷新整个列表
      const newImages = images.map(img =>
        img.id === image.id ? { ...img, is_favorite: !img.is_favorite } : img
      )
      setImages(newImages)
      // 同步更新 allImages
      setAllImages(prev => prev.length > 0
        ? prev.map(img => img.id === image.id ? { ...img, is_favorite: !img.is_favorite } : img)
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
            onClick={handleSlideshowAll}
            disabled={images.length === 0}
          >
            幻灯片播放 ({images.length > 0 ? images.length : '...'})
          </Button>

          {/* Batch Operations */}
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

        {/* Selection Status Bar */}
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
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          onChange: handlePageChange,
        }}
        onFavorite={handleFavorite}
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
