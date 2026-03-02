import React, { useState, useEffect } from 'react'
import { Tag, Space, Button, Select, message, Popover } from 'antd'
import { PlusOutlined, TagsOutlined } from '@ant-design/icons'
import { videosApi, tagsApi } from '@services/api'

const { Option } = Select

function VideoTags({ videoId, tags = [], onUpdate }) {
  const [allTags, setAllTags] = useState([])
  const [selectedTagId, setSelectedTagId] = useState(null)
  const [loading, setLoading] = useState(false)
  const [popoverVisible, setPopoverVisible] = useState(false)

  useEffect(() => {
    fetchAllTags()
  }, [])

  const fetchAllTags = async () => {
    try {
      const response = await tagsApi.getList()
      setAllTags(response || [])
    } catch (error) {
      console.error('Failed to fetch tags:', error)
    }
  }

  const handleAddTag = async () => {
    if (!selectedTagId) return

    setLoading(true)
    try {
      await videosApi.addTag(videoId, selectedTagId)
      message.success('标签已添加')
      setSelectedTagId(null)
      setPopoverVisible(false)
      // 更新标签列表，不刷新整个页面
      const addedTag = allTags.find(t => t.id === selectedTagId)
      if (addedTag) {
        onUpdate([...tags, addedTag])
      }
    } catch (error) {
      message.error('添加标签失败')
    } finally {
      setLoading(false)
    }
  }

  const handleRemoveTag = async (tagId) => {
    try {
      await videosApi.removeTag(videoId, tagId)
      message.success('标签已移除')
      // 更新标签列表，不刷新整个页面
      onUpdate(tags.filter(t => t.id !== tagId))
    } catch (error) {
      message.error('移除标签失败')
    }
  }

  const handleCreateTag = async (name) => {
    try {
      const response = await tagsApi.create({ name })
      setAllTags([...allTags, response])
      setSelectedTagId(response.id)
      message.success('标签已创建')
    } catch (error) {
      message.error('创建标签失败')
    }
  }

  // Filter out tags already added to video
  const availableTags = allTags.filter(
    (tag) => !tags.some((t) => t.id === tag.id)
  )

  const popoverContent = (
    <div style={{ width: 200 }}>
      <Select
        style={{ width: '100%', marginBottom: 8 }}
        placeholder="选择标签"
        value={selectedTagId}
        onChange={setSelectedTagId}
        showSearch
        optionFilterProp="children"
        dropdownRender={(menu) => (
          <>
            {menu}
            <div style={{ padding: '8px', borderTop: '1px solid #e8e8e8' }}>
              <Button
                type="link"
                size="small"
                onClick={() => {
                  const name = prompt('输入新标签名称：')
                  if (name) handleCreateTag(name)
                }}
              >
                + 创建新标签
              </Button>
            </div>
          </>
        )}
      >
        {availableTags.map((tag) => (
          <Option key={tag.id} value={tag.id}>
            <Tag color={tag.color}>{tag.name}</Tag>
          </Option>
        ))}
      </Select>
      <Space>
        <Button
          type="primary"
          size="small"
          onClick={handleAddTag}
          loading={loading}
          disabled={!selectedTagId}
        >
          添加
        </Button>
        <Button size="small" onClick={() => setPopoverVisible(false)}>
          取消
        </Button>
      </Space>
    </div>
  )

  return (
    <div className="video-tags-section">
      <Space size={[8, 8]} wrap>
        <TagsOutlined />
        <span>标签：</span>
        {tags.map((tag) => (
          <Tag
            key={tag.id}
            color={tag.color}
            closable
            onClose={() => handleRemoveTag(tag.id)}
          >
            {tag.name}
          </Tag>
        ))}
        <Popover
          content={popoverContent}
          title="添加标签"
          trigger="click"
          open={popoverVisible}
          onOpenChange={setPopoverVisible}
        >
          <Button type="dashed" size="small" icon={<PlusOutlined />}>
            添加标签
          </Button>
        </Popover>
      </Space>
    </div>
  )
}

export default VideoTags
