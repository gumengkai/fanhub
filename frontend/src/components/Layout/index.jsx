import React, { useState } from 'react'
import { Layout, Menu, Input, Space, theme, Button, Drawer, Tabs, Dropdown } from 'antd'
import {
  HomeOutlined,
  VideoCameraOutlined,
  PictureOutlined,
  SettingOutlined,
  HeartOutlined,
  SearchOutlined,
  PlayCircleOutlined,
  MenuOutlined,
  CloseOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import './index.css'

const { Header, Content } = Layout
const { Search } = Input

const navItems = [
  { key: '/', icon: <HomeOutlined />, label: '首页', path: '/' },
  { key: '/videos', icon: <VideoCameraOutlined />, label: '视频库', path: '/videos' },
  { key: '/short-video', icon: <PlayCircleOutlined />, label: '短视频', path: '/short-video' },
  { key: '/images', icon: <PictureOutlined />, label: '图片库', path: '/images' },
  { key: '/favorites', icon: <HeartOutlined />, label: '收藏', path: '/favorites' },
]

const mobileNavItems = [
  { key: '/', icon: <HomeOutlined />, label: '首页', path: '/' },
  { key: '/videos', icon: <VideoCameraOutlined />, label: '视频', path: '/videos' },
  { key: '/short-video', icon: <PlayCircleOutlined />, label: '短视频', path: '/short-video' },
  { key: '/images', icon: <PictureOutlined />, label: '图片', path: '/images' },
  { key: '/favorites', icon: <HeartOutlined />, label: '收藏', path: '/favorites' },
]

function AppLayout({ children }) {
  const [mobileMenuVisible, setMobileMenuVisible] = useState(false)
  const [searchVisible, setSearchVisible] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken()

  const handleSearch = (value) => {
    if (value.trim()) {
      if (location.pathname === '/images') {
        navigate(`/images?search=${encodeURIComponent(value)}`)
      } else {
        navigate(`/videos?search=${encodeURIComponent(value)}`)
      }
    }
    setSearchVisible(false)
  }

  const handleMobileNavClick = (path) => {
    navigate(path)
  }

  const handleTabChange = (key) => {
    navigate(key)
  }

  const settingsMenuItems = [
    {
      key: 'sources',
      icon: <SettingOutlined />,
      label: '来源设置',
      onClick: () => navigate('/sources'),
    },
  ]

  return (
    <Layout className="app-layout">
      <Header className="app-header">
        {/* Logo */}
        <div className="header-logo">
          <VideoCameraOutlined className="logo-icon" />
          <span className="logo-text">FunHub</span>
        </div>

        {/* Top Navigation Tabs */}
        <div className="header-nav">
          <Tabs
            activeKey={location.pathname}
            items={navItems.map(item => ({
              key: item.path,
              label: item.label,
              icon: item.icon,
            }))}
            onChange={handleTabChange}
            className="top-tabs"
          />
        </div>

        {/* Right Actions */}
        <div className="header-actions">
          {/* Search Popup */}
          <div className={`search-popup ${searchVisible ? 'visible' : ''}`}>
            <Search
              placeholder="搜索视频或图片..."
              allowClear
              enterButton={<SearchOutlined />}
              size="middle"
              onSearch={handleSearch}
              autoFocus={searchVisible}
              onBlur={() => setSearchVisible(false)}
            />
          </div>
          <Button
            type="text"
            icon={<SearchOutlined />}
            className="search-trigger"
            onClick={() => setSearchVisible(true)}
          />

          {/* Settings Dropdown */}
          <Dropdown
            menu={{ items: settingsMenuItems }}
            placement="bottomRight"
            arrow
          >
            <Button type="text" icon={<UserOutlined />} className="settings-btn" />
          </Dropdown>

          {/* Mobile Menu Toggle */}
          <Button
            type="text"
            icon={<MenuOutlined />}
            className="mobile-menu-toggle"
            onClick={() => setMobileMenuVisible(true)}
          />
        </div>
      </Header>

      <Content
        className="app-content"
        style={{
          background: colorBgContainer,
          borderRadius: borderRadiusLG,
        }}
      >
        {children}
      </Content>

      {/* Mobile Navigation */}
      <nav className="mobile-nav">
        <ul className="mobile-nav-menu">
          {mobileNavItems.map((item) => (
            <li
              key={item.key}
              className={`mobile-nav-item ${location.pathname === item.path ? 'active' : ''}`}
              onClick={() => handleMobileNavClick(item.path)}
            >
              {item.icon}
              <span>{item.label}</span>
            </li>
          ))}
        </ul>
      </nav>

      {/* Mobile Menu Drawer */}
      <Drawer
        placement="right"
        onClose={() => setMobileMenuVisible(false)}
        open={mobileMenuVisible}
        width={280}
        styles={{ body: { padding: 0 } }}
      >
        <div className="mobile-drawer-header">
          <Space>
            <VideoCameraOutlined className="drawer-icon" />
            <span className="drawer-title">FunHub</span>
          </Space>
          <CloseOutlined
            className="drawer-close"
            onClick={() => setMobileMenuVisible(false)}
          />
        </div>
        <Menu
          mode="vertical"
          selectedKeys={[location.pathname]}
          items={[
            ...navItems.map(item => ({
              ...item,
              label: <Link to={item.path}>{item.label}</Link>,
            })),
            {
              key: '/sources',
              icon: <SettingOutlined />,
              label: <Link to="/sources">来源设置</Link>,
            },
          ]}
          onClick={() => setMobileMenuVisible(false)}
        />
      </Drawer>
    </Layout>
  )
}

export default AppLayout
