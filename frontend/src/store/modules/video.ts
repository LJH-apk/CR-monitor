import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Video } from '@/types/video'
import { videoApi } from '@/api/video'

export const useVideoStore = defineStore('video', () => {
  const videos = ref<Video[]>([])
  const currentVideo = ref<Video | null>(null)
  const loading = ref(false)

  // 获取视频列表
  async function fetchVideos() {
    loading.value = true
    try {
      const response = await videoApi.getList()
      videos.value = response.data
    } catch (error) {
      console.error('Failed to fetch videos:', error)
    } finally {
      loading.value = false
    }
  }

  // 获取单个视频
  async function fetchVideo(id: number) {
    loading.value = true
    try {
      const response = await videoApi.getById(id)
      currentVideo.value = response.data
      return response.data
    } catch (error) {
      console.error('Failed to fetch video:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  // 上传视频
  async function uploadVideo(file: File) {
    loading.value = true
    try {
      const response = await videoApi.upload(file)
      await fetchVideos() // 刷新列表
      return response.data
    } catch (error) {
      console.error('Failed to upload video:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 删除视频
  async function deleteVideo(id: number) {
    try {
      await videoApi.delete(id)
      videos.value = videos.value.filter(v => v.id !== id)
      if (currentVideo.value?.id === id) {
        currentVideo.value = null
      }
    } catch (error) {
      console.error('Failed to delete video:', error)
      throw error
    }
  }

  // 设置当前视频
  function setCurrentVideo(video: Video | null) {
    currentVideo.value = video
  }

  return {
    videos,
    currentVideo,
    loading,
    fetchVideos,
    fetchVideo,
    uploadVideo,
    deleteVideo,
    setCurrentVideo
  }
})
