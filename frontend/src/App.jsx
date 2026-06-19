import React from 'react'
import { Routes, Route } from 'react-router-dom'
import Layout from '@components/Layout'
import ErrorBoundary from '@components/ErrorBoundary'
import Home from '@pages/Home'
import VideoLibrary from '@pages/VideoLibrary'
import VideoPlay from '@pages/VideoPlay'
import ImageLibrary from '@pages/ImageLibrary'
import SourceConfig from '@pages/SourceConfig'
import ScanLogs from '@pages/ScanLogs'
import Favorites from '@pages/Favorites'
import ShortVideo from '@pages/ShortVideo'
import DouyinLibrary from '@pages/DouyinLibrary'
import PeakLibrary from '@pages/PeakLibrary'

function App() {
  return (
    <ErrorBoundary>
      <Layout>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/videos" element={<VideoLibrary />} />
          <Route path="/videos/:id" element={<VideoPlay />} />
          <Route path="/short-video" element={<ShortVideo />} />
          <Route path="/douyin" element={<DouyinLibrary />} />
          <Route path="/peak" element={<PeakLibrary />} />
          <Route path="/images" element={<ImageLibrary />} />
          <Route path="/sources" element={<SourceConfig />} />
          <Route path="/scan-logs" element={<ScanLogs />} />
          <Route path="/favorites" element={<Favorites />} />
        </Routes>
      </Layout>
    </ErrorBoundary>
  )
}

export default App
