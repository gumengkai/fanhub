import React from 'react'
import ReactDOM from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { BrowserRouter } from 'react-router-dom'
import App from './App'

import 'antd/dist/reset.css'

// Bilibili-inspired theme
const bilibiliTheme = {
  token: {
    colorPrimary: '#fb7299',
    colorLink: '#fb7299',
    colorLinkHover: '#ff85a8',
    colorLinkActive: '#ff9a9e',
    borderRadius: 8,
    borderRadiusLG: 12,
    borderRadiusXL: 16,
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif',
    fontSize: 14,
    colorBgLayout: '#f6f7f8',
    colorBgContainer: '#ffffff',
    colorText: '#333333',
    colorTextSecondary: '#666666',
    colorTextTertiary: '#999999',
    controlHeight: 40,
    controlHeightLG: 44,
    controlHeightSM: 32,
  },
  components: {
    Button: {
      borderRadius: 8,
      algorithm: true,
    },
    Card: {
      borderRadius: 12,
      boxShadowTertiary: '0 4px 24px rgba(0, 0, 0, 0.08)',
    },
    Input: {
      borderRadius: 8,
      borderRadiusLG: 12,
    },
    Tag: {
      borderRadius: 6,
    },
    Menu: {
      borderRadius: 8,
    },
  },
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ConfigProvider 
      locale={zhCN}
      theme={bilibiliTheme}
    >
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ConfigProvider>
  </React.StrictMode>,
)
