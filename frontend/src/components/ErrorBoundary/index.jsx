import React from 'react'
import { Result, Button } from 'antd'

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null, errorInfo: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true }
  }

  componentDidCatch(error, errorInfo) {
    console.error('Error caught by boundary:', error, errorInfo)
    this.setState({ error, errorInfo })
  }

  render() {
    if (this.state.hasError) {
      return (
        <Result
          status="error"
          title="页面渲染出错"
          subTitle={this.state.error?.toString()}
          extra={[
            <Button type="primary" key="reload" onClick={() => window.location.reload()}>
              刷新页面
            </Button>,
          ]}
        >
          <div style={{ textAlign: 'left', background: '#f5f5f5', padding: 16, borderRadius: 4 }}>
            <p>错误详情：</p>
            <pre style={{ overflow: 'auto' }}>
              {this.state.errorInfo?.componentStack}
            </pre>
          </div>
        </Result>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary
