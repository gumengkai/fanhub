import React, { useEffect, useState, useCallback } from 'react'
import {
  Typography,
  Table,
  Tag,
  Button,
  Space,
  message,
  Card,
  Spin,
  Timeline,
  Empty,
  Popconfirm,
  Badge,
  Tooltip,
  Drawer,
  List,
  Descriptions
} from 'antd'
import {
  ReloadOutlined,
  DeleteOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  ClockCircleOutlined,
  FileAddOutlined,
  DeleteFilled,
  FileSyncOutlined,
  WarningOutlined
} from '@ant-design/icons'
import { sourcesApi } from '@services/api'
import dayjs from 'dayjs'

const { Title } = Typography

function ScanLogs() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(false)
  const [selectedLog, setSelectedLog] = useState(null)
  const [drawerVisible, setDrawerVisible] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  })

  const fetchLogs = useCallback(async (page = 1, pageSize = 20) => {
    setLoading(true)
    try {
      const response = await sourcesApi.getScanLogs({ page, per_page: pageSize })
      setLogs(response.items || [])
      setPagination({
        current: response.current_page || 1,
        pageSize: response.per_page || 20,
        total: response.total || 0,
      })
    } catch (error) {
      message.error('获取扫描日志失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchLogs()
  }, [fetchLogs])

  const handleClearLogs = async () => {
    try {
      await sourcesApi.clearScanLogs()
      message.success('扫描日志已清空')
      fetchLogs()
    } catch (error) {
      message.error('清空日志失败')
      console.error(error)
    }
  }

  const handleViewDetails = (log) => {
    setSelectedLog(log)
    setDrawerVisible(true)
  }

  const getStatusTag = (status) => {
    switch (status) {
      case 'running':
        return <Tag icon={<SyncOutlined spin />} color="processing">进行中</Tag>
      case 'completed':
        return <Tag icon={<CheckCircleOutlined />} color="success">已完成</Tag>
      case 'failed':
        return <Tag icon={<CloseCircleOutlined />} color="error">失败</Tag>
      default:
        return <Tag>{status}</Tag>
    }
  }

  const getDetailIcon = (type) => {
    switch (type) {
      case 'added':
        return <FileAddOutlined style={{ color: '#52c41a' }} />
      case 'removed':
        return <DeleteFilled style={{ color: '#ff4d4f' }} />
      case 'updated':
        return <FileSyncOutlined style={{ color: '#1890ff' }} />
      case 'error':
        return <WarningOutlined style={{ color: '#faad14' }} />
      default:
        return <ClockCircleOutlined />
    }
  }

  const getDetailColor = (type) => {
    switch (type) {
      case 'added':
        return 'green'
      case 'removed':
        return 'red'
      case 'updated':
        return 'blue'
      case 'error':
        return 'orange'
      default:
        return 'gray'
    }
  }

  const getDetailText = (detail) => {
    switch (detail.type) {
      case 'added':
        return `新增${detail.media_type === 'video' ? '视频' : '图片'}: ${detail.title || detail.path}`
      case 'removed':
        return `删除${detail.media_type === 'video' ? '视频' : '图片'}: ${detail.title || detail.path}`
      case 'updated':
        return `更新${detail.media_type === 'video' ? '视频' : '图片'}: ${detail.title || detail.path}`
      case 'error':
        return `错误: ${detail.error}`
      default:
        return detail.path || '未知操作'
    }
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '数据源',
      dataIndex: 'source_name',
      key: 'source_name',
      render: (text) => <strong>{text}</strong>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => getStatusTag(status),
    },
    {
      title: '统计',
      key: 'stats',
      render: (_, record) => (
        <Space size="small">
          {record.videos_added > 0 && (
            <Tooltip title={`新增 ${record.videos_added} 个视频`}>
              <Badge count={`+${record.videos_added}`} style={{ backgroundColor: '#52c41a' }} />
            </Tooltip>
          )}
          {record.videos_removed > 0 && (
            <Tooltip title={`删除 ${record.videos_removed} 个视频`}>
              <Badge count={`-${record.videos_removed}`} style={{ backgroundColor: '#ff4d4f' }} />
            </Tooltip>
          )}
          {record.videos_updated > 0 && (
            <Tooltip title={`更新 ${record.videos_updated} 个视频`}>
              <Badge count={`~${record.videos_updated}`} style={{ backgroundColor: '#1890ff' }} />
            </Tooltip>
          )}
          {record.images_added > 0 && (
            <Tooltip title={`新增 ${record.images_added} 张图片`}>
              <Badge count={`+${record.images_added}图`} style={{ backgroundColor: '#52c41a' }} />
            </Tooltip>
          )}
          {record.images_removed > 0 && (
            <Tooltip title={`删除 ${record.images_removed} 张图片`}>
              <Badge count={`-${record.images_removed}图`} style={{ backgroundColor: '#ff4d4f' }} />
            </Tooltip>
          )}
          {record.images_updated > 0 && (
            <Tooltip title={`更新 ${record.images_updated} 张图片`}>
              <Badge count={`~${record.images_updated}图`} style={{ backgroundColor: '#1890ff' }} />
            </Tooltip>
          )}
          {(record.errors?.length || 0) > 0 && (
            <Tooltip title={`${record.errors.length} 个错误`}>
              <Badge count={`!${record.errors.length}`} style={{ backgroundColor: '#faad14' }} />
            </Tooltip>
          )}
        </Space>
      ),
    },
    {
      title: '开始时间',
      dataIndex: 'started_at',
      key: 'started_at',
      render: (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '耗时',
      key: 'duration',
      render: (_, record) => {
        if (!record.completed_at || !record.started_at) return '-'
        const start = dayjs(record.started_at)
        const end = dayjs(record.completed_at)
        const seconds = end.diff(start, 'seconds')
        if (seconds < 60) return `${seconds}秒`
        const minutes = Math.floor(seconds / 60)
        const remainingSeconds = seconds % 60
        return `${minutes}分${remainingSeconds}秒`
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetails(record)}
          >
            详情
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ marginBottom: 16 }}>扫描日志</Title>
        <Space>
          <Button
            type="primary"
            icon={<ReloadOutlined />}
            onClick={() => fetchLogs(pagination.current, pagination.pageSize)}
          >
            刷新
          </Button>
          <Popconfirm
            title="确定要清空所有扫描日志吗？"
            onConfirm={handleClearLogs}
            okText="确定"
            cancelText="取消"
          >
            <Button danger icon={<DeleteOutlined />}>
              清空日志
            </Button>
          </Popconfirm>
        </Space>
      </div>

      <Card>
        <Table
          columns={columns}
          dataSource={logs}
          rowKey="id"
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            onChange: (page, pageSize) => fetchLogs(page, pageSize),
          }}
          locale={{
            emptyText: <Empty description="暂无扫描日志" />,
          }}
        />
      </Card>

      <Drawer
        title="扫描详情"
        placement="right"
        width={600}
        onClose={() => setDrawerVisible(false)}
        open={drawerVisible}
      >
        {selectedLog && (
          <div>
            <Descriptions bordered column={1} size="small" style={{ marginBottom: 24 }}>
              <Descriptions.Item label="数据源">{selectedLog.source_name}</Descriptions.Item>
              <Descriptions.Item label="状态">{getStatusTag(selectedLog.status)}</Descriptions.Item>
              <Descriptions.Item label="开始时间">
                {selectedLog.started_at ? dayjs(selectedLog.started_at).format('YYYY-MM-DD HH:mm:ss') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="结束时间">
                {selectedLog.completed_at ? dayjs(selectedLog.completed_at).format('YYYY-MM-DD HH:mm:ss') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="视频统计">
                新增: {selectedLog.videos_added}, 更新: {selectedLog.videos_updated}, 删除: {selectedLog.videos_removed}
              </Descriptions.Item>
              <Descriptions.Item label="图片统计">
                新增: {selectedLog.images_added}, 更新: {selectedLog.images_updated}, 删除: {selectedLog.images_removed}
              </Descriptions.Item>
              <Descriptions.Item label="错误数">
                {(selectedLog.errors?.length || 0) > 0 ? (
                  <Tag color="error">{selectedLog.errors.length} 个错误</Tag>
                ) : (
                  <Tag color="success">无错误</Tag>
                )}
              </Descriptions.Item>
            </Descriptions>

            <Title level={5} style={{ marginBottom: 16 }}>操作记录</Title>
            {selectedLog.details && selectedLog.details.length > 0 ? (
              <Timeline
                items={selectedLog.details.slice(0, 100).map((detail, index) => ({
                  key: index,
                  dot: getDetailIcon(detail.type),
                  color: getDetailColor(detail.type),
                  children: (
                    <div style={{ wordBreak: 'break-all' }}>
                      <span style={{ fontWeight: 500 }}>
                        {detail.type === 'added' && '新增 '}
                        {detail.type === 'removed' && '删除 '}
                        {detail.type === 'updated' && '更新 '}
                        {detail.type === 'error' && '错误 '}
                      </span>
                      {detail.media_type && (
                        <Tag size="small">
                          {detail.media_type === 'video' ? '视频' : '图片'}
                        </Tag>
                      )}
                      <div style={{ marginTop: 4, color: '#666', fontSize: 12 }}>
                        {detail.title && <div>标题: {detail.title}</div>}
                        {detail.path && <div>路径: {detail.path}</div>}
                        {detail.error && <div style={{ color: '#ff4d4f' }}>错误: {detail.error}</div>}
                        {detail.was_favorite && <Tag size="small" color="gold">原已收藏</Tag>}
                        {detail.was_liked && <Tag size="small" color="red">原已喜欢</Tag>}
                      </div>
                    </div>
                  ),
                }))}
              />
            ) : (
              <Empty description="无详细操作记录" />
            )}

            {selectedLog.errors && selectedLog.errors.length > 0 && (
              <>
                <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>错误详情</Title>
                <List
                  size="small"
                  bordered
                  dataSource={selectedLog.errors}
                  renderItem={(error) => (
                    <List.Item>
                      <span style={{ color: '#ff4d4f' }}>{error}</span>
                    </List.Item>
                  )}
                />
              </>
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}

export default ScanLogs
