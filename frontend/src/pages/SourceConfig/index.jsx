import React, { useEffect, useState } from 'react'
import {
  Typography,
  Button,
  Table,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  Switch,
  InputNumber,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  Statistic,
  Tooltip,
  Badge,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ScanOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  FolderOutlined,
  CloudOutlined,
  ReloadOutlined,
  VideoCameraOutlined,
  PictureOutlined,
  AppstoreOutlined,
} from '@ant-design/icons'
import { sourcesApi } from '@services/api'

const { Title, Text } = Typography
const { Option } = Select

function SourceConfig() {
  const [sources, setSources] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingSource, setEditingSource] = useState(null)
  const [form] = Form.useForm()
  const [scanning, setScanning] = useState({})
  const [testingConnection, setTestingConnection] = useState({})

  const fetchSources = async () => {
    setLoading(true)
    try {
      const response = await sourcesApi.getList()
      setSources(response || [])
    } catch (error) {
      message.error('获取来源列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchSources()
  }, [])

  const handleAdd = () => {
    setEditingSource(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record) => {
    setEditingSource(record)
    form.setFieldsValue({
      ...record,
      nas_config: record.nas_config || {},
    })
    setModalVisible(true)
  }

  const handleDelete = async (id) => {
    try {
      await sourcesApi.delete(id)
      message.success('来源已删除')
      fetchSources()
    } catch (error) {
      message.error('删除失败')
    }
  }

  const handleSubmit = async (values) => {
    try {
      if (editingSource) {
        await sourcesApi.update(editingSource.id, values)
        message.success('来源已更新')
      } else {
        await sourcesApi.create(values)
        message.success('来源已创建')
      }
      setModalVisible(false)
      fetchSources()
    } catch (error) {
      message.error(error.response?.data?.error || '操作失败')
    }
  }

  const handleScan = async (record) => {
    setScanning({ ...scanning, [record.id]: true })
    try {
      const response = await sourcesApi.scan(record.id)
      const stats = response.stats
      const messages = []
      if (stats.videos_added > 0) messages.push(`新增 ${stats.videos_added} 个视频`)
      if (stats.videos_updated > 0) messages.push(`更新 ${stats.videos_updated} 个视频`)
      if (stats.images_added > 0) messages.push(`新增 ${stats.images_added} 个图片`)
      if (stats.images_updated > 0) messages.push(`更新 ${stats.images_updated} 个图片`)
      
      message.success(`扫描完成：${messages.join(', ') || '无变化'}`)
      fetchSources()
    } catch (error) {
      message.error('扫描失败')
    } finally {
      setScanning({ ...scanning, [record.id]: false })
    }
  }

  const handleTestConnection = async (record) => {
    setTestingConnection({ ...testingConnection, [record.id]: true })
    try {
      const response = await sourcesApi.checkStatus(record.id)
      if (response.accessible) {
        message.success('连接正常')
      } else {
        message.error('连接失败：' + (response.details?.error || '无法访问'))
      }
    } catch (error) {
      message.error('测试连接失败')
    } finally {
      setTestingConnection({ ...testingConnection, [record.id]: false })
    }
  }

  const getMediaTypeLabel = (mediaType) => {
    switch (mediaType) {
      case 'video':
        return { text: '视频', icon: <VideoCameraOutlined />, color: 'blue' }
      case 'image':
        return { text: '图片', icon: <PictureOutlined />, color: 'green' }
      default:
        return { text: '全部', icon: <AppstoreOutlined />, color: 'default' }
    }
  }

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <Space>
          {record.type === 'local' ? <FolderOutlined /> : <CloudOutlined />}
          <Text strong>{text}</Text>
        </Space>
      ),
    },
    {
      title: '媒体类型',
      dataIndex: 'media_type',
      key: 'media_type',
      render: (mediaType) => {
        const { text, icon, color } = getMediaTypeLabel(mediaType)
        return (
          <Tag color={color} icon={icon}>
            {text}
          </Tag>
        )
      },
    },
    {
      title: '来源类型',
      dataIndex: 'type',
      key: 'type',
      render: (type) => (
        <Tag color={type === 'local' ? 'blue' : 'green'}>
          {type === 'local' ? '本地' : 'NAS'}
        </Tag>
      ),
    },
    {
      title: '路径',
      dataIndex: 'path',
      key: 'path',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'is_active',
      key: 'is_active',
      render: (isActive) => (
        <Badge
          status={isActive ? 'success' : 'default'}
          text={isActive ? '启用' : '禁用'}
        />
      ),
    },
    {
      title: '扫描间隔',
      dataIndex: 'scan_interval',
      key: 'scan_interval',
      render: (interval) => `${interval} 分钟`,
    },
    {
      title: '最后扫描',
      dataIndex: 'last_scan_at',
      key: 'last_scan_at',
      render: (date) => date ? new Date(date).toLocaleString() : '从未',
    },
    {
      title: '统计',
      key: 'stats',
      width: 120,
      render: (_, record) => (
        <Space direction="vertical" size="small">
          {record.media_type !== 'image' && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              <VideoCameraOutlined /> {record.video_count || 0}
            </Text>
          )}
          {record.media_type !== 'video' && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              <PictureOutlined /> {record.image_count || 0}
            </Text>
          )}
        </Space>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="测试连接">
            <Button
              icon={<CheckCircleOutlined />}
              size="small"
              loading={testingConnection[record.id]}
              onClick={() => handleTestConnection(record)}
            />
          </Tooltip>
          <Tooltip title="扫描">
            <Button
              icon={<ScanOutlined />}
              size="small"
              loading={scanning[record.id]}
              onClick={() => handleScan(record)}
            />
          </Tooltip>
          <Tooltip title="编辑">
            <Button
              icon={<EditOutlined />}
              size="small"
              onClick={() => handleEdit(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确认删除"
            description="删除来源将同时删除其所有媒体记录，是否继续？"
            onConfirm={() => handleDelete(record.id)}
            okText="删除"
            cancelText="取消"
          >
            <Tooltip title="删除">
              <Button
                danger
                icon={<DeleteOutlined />}
                size="small"
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={2} style={{ margin: 0 }}>来源配置</Title>
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={fetchSources}
            loading={loading}
          >
            刷新
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleAdd}
          >
            添加来源
          </Button>
        </Space>
      </div>

      <Card style={{ marginBottom: 24 }}>
        <Row gutter={16}>
          <Col span={8}>
            <Statistic
              title="视频源"
              value={sources.filter(s => s.media_type === 'video').length}
              prefix={<VideoCameraOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Col>
          <Col span={8}>
            <Statistic
              title="图片源"
              value={sources.filter(s => s.media_type === 'image').length}
              prefix={<PictureOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Col>
          <Col span={8}>
            <Statistic
              title="混合源"
              value={sources.filter(s => s.media_type === 'all').length}
              prefix={<AppstoreOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Col>
        </Row>
      </Card>

      <Table
        columns={columns}
        dataSource={sources}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      <Modal
        title={editingSource ? '编辑来源' : '添加来源'}
        open={modalVisible}
        onOk={() => form.submit()}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input placeholder="例如：我的视频库" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="type"
                label="来源类型"
                rules={[{ required: true, message: '请选择类型' }]}
              >
                <Select placeholder="选择来源类型">
                  <Option value="local">本地文件夹</Option>
                  <Option value="nas">NAS 存储</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="media_type"
                label="媒体类型"
                rules={[{ required: true, message: '请选择媒体类型' }]}
                initialValue="all"
              >
                <Select placeholder="选择媒体类型">
                  <Option value="all"><AppstoreOutlined /> 全部 (视频 + 图片)</Option>
                  <Option value="video"><VideoCameraOutlined /> 仅视频</Option>
                  <Option value="image"><PictureOutlined /> 仅图片</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="path"
            label="路径"
            rules={[{ required: true, message: '请输入路径' }]}
            extra="根据媒体类型，此路径将只扫描对应的文件"
          >
            <Input placeholder={form.getFieldValue('type') === 'nas' ? '例如：/shared/videos' : '例如：/home/user/videos'} />
          </Form.Item>

          <Form.Item shouldUpdate={(prevValues, currentValues) => prevValues.type !== currentValues.type}>
            {({ getFieldValue }) => {
              return getFieldValue('type') === 'nas' ? (
                <>
                  <Form.Item
                    name={['nas_config', 'protocol']}
                    label="协议"
                    initialValue="smb"
                  >
                    <Select>
                      <Option value="smb">SMB/CIFS</Option>
                      <Option value="nfs">NFS</Option>
                    </Select>
                  </Form.Item>
                  <Form.Item
                    name={['nas_config', 'host']}
                    label="主机地址"
                    rules={[{ required: true, message: '请输入主机地址' }]}
                  >
                    <Input placeholder="例如：192.168.1.100 或 nas.local" />
                  </Form.Item>
                  <Form.Item
                    name={['nas_config', 'share']}
                    label="共享名称"
                    rules={[{ required: true, message: '请输入共享名称' }]}
                  >
                    <Input placeholder="例如：videos" />
                  </Form.Item>
                  <Form.Item
                    name={['nas_config', 'username']}
                    label="用户名"
                  >
                    <Input placeholder="访问共享的用户名（可选）" />
                  </Form.Item>
                  <Form.Item
                    name={['nas_config', 'password']}
                    label="密码"
                  >
                    <Input.Password placeholder="访问共享的密码（可选）" />
                  </Form.Item>
                </>
              ) : null
            }}
          </Form.Item>

          <Form.Item
            name="scan_interval"
            label="扫描间隔（分钟）"
            initialValue={60}
          >
            <InputNumber min={5} max={1440} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="is_active"
            label="状态"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default SourceConfig
