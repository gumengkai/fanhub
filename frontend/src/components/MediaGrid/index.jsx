import React from 'react'
import { Row, Col, Empty, Pagination, Skeleton } from 'antd'
import MediaCard from '@components/MediaCard'
import './index.css'

function MediaGrid({
  items = [],
  type = 'video',
  loading = false,
  pagination,
  onFavorite,
  onItemClick,
  onDelete,
}) {
  if (loading) {
    return (
      <Row gutter={[16, 16]}>
        {[...Array(8)].map((_, index) => (
          <Col xs={24} sm={12} md={8} lg={6} xl={4} key={index}>
            <Skeleton.Image active style={{ width: '100%', height: 180 }} />
            <Skeleton active paragraph={{ rows: 1 }} />
          </Col>
        ))}
      </Row>
    )
  }

  if (items.length === 0) {
    return (
      <Empty
        description="暂无内容"
        style={{ marginTop: 64 }}
      />
    )
  }

  return (
    <div className="media-grid">
      <Row gutter={[16, 16]}>
        {items.map((item) => (
          <Col xs={24} sm={12} md={8} lg={6} xl={4} key={item.id}>
            <MediaCard
              item={item}
              type={type}
              onFavorite={onFavorite}
              onClick={() => onItemClick?.(item)}
              onDelete={onDelete}
            />
          </Col>
        ))}
      </Row>

      {pagination && pagination.total > 0 && (
        <div className="pagination-container">
          <Pagination
            current={pagination.current}
            pageSize={pagination.pageSize}
            total={pagination.total}
            onChange={pagination.onChange}
            onShowSizeChange={pagination.onShowSizeChange}
            showSizeChanger
            showQuickJumper
            showTotal={(total) => `共 ${total} 条`}
            pageSizeOptions={['12', '24', '48', '96']}
          />
        </div>
      )}
    </div>
  )
}

export default MediaGrid
