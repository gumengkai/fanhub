import React, { useState } from 'react'
import { Layout, Menu, Input, Badge, Avatar, Space, Dropdown, theme } from 'antd'
import {
  HomeOutlined,
  VideoCameraOutlined,
  PictureOutlined,
  SettingOutlined,
  HeartOutlined,
  SearchOutlined,
  UserOutlined,
  BellOutlined,
  DownOutlined,
} from '@ant-design/icons'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import './index.css'

const { Header, Sider, Content } = Layout
const { Search } = Input

const menuItems = [
  {
    key: '/',
    icon: <HomeOutlined />,
    label: <Link to="/">首页</Link>,
  },
  {
    key: '/videos',
    icon: <VideoCameraOutlined />,
    label: <Link to="/videos">视频库</Link>,
  },
  {
    key: '/images',
    icon: <PictureOutlined />,
    label: <Link to="/images">图片库</Link>,
  },
  {
    key: '/favorites',
    icon: <HeartOutlined />,
    label: <Link to="/favorites">收藏</Link>,
  },
  {
    key: '/sources',
    icon: <SettingOutlined />,
    label: <Link to="/sources">来源配置</Link>,
  },
]

const userMenuItems = [
  {
    key: 'profile',
    label: '个人资料',
  },
  {
    key: 'settings',
    label: '设置',
  },
  {
    type: 'divider',
  },
  {
    key: 'logout',
    label: '退出登录',
  },
]

function AppLayout({ children }) {
  const [collapsed, setCollapsed] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken()

  const handleSearch = (value) => {
    if (value.trim()) {
      const currentPath = location.pathname
      if (currentPath === '/images') {
        navigate(`/images?search=${encodeURIComponent(value)}`)
      } else {
        navigate(`/videos?search=${encodeURIComponent(value)}`)
      }
    }
  }

  return (
    <Layout className="app-layout">
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        theme="dark"
        className="app-sider"
      >
        <div className="logo">
          <VideoCameraOutlined className="logo-icon" />
          {!collapsed && <span className="logo-text">FunHub</span>}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          className="app-menu"
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 24px',
            background: colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div className="header-search">
            <Search
              placeholder="搜索视频或图片..."
              allowClear
              enterButton={<SearchOutlined />}
              size="middle"
              onSearch={handleSearch}
              style={{ width: 400 }}
            />
          </div>
          <Space size={24}>
            <Badge count={5} size="small">
              <BellOutlined style={{ fontSize: 20, cursor: 'pointer' }} />
            </Badge>
            <Dropdown
              menu={{ items: userMenuItems }}
              placement="bottomRight"
            >
              <Space style={{ cursor: 'pointer' }}>
                <Avatar icon={<UserOutlined />} />
                <span>管理员</span>
                <DownOutlined />
              </Space>
            </Dropdown>
          </Space>
        </Header>
        <Content
          style={{
            margin: '24px 16px',
            padding: 24,
            minHeight: 280,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
            overflow: 'auto',
          }}
        >
          {children}
        </Content>
      </Layout>
    </Layout>
  )
}

export default AppLayout
