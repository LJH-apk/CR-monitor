import axios from '@/api/axios'

/**
 * 加载需要JWT认证的图片
 * @param url 图片URL
 * @returns Promise<string> 返回blob URL
 */
export async function loadAuthenticatedImage(url: string): Promise<string> {
  try {
    const response = await axios.get(url, {
      responseType: 'blob'
    })

    // 创建blob URL
    const blob = response.data
    const blobUrl = URL.createObjectURL(blob)

    return blobUrl
  } catch (error) {
    console.error('Failed to load authenticated image:', error)
    throw error
  }
}

/**
 * 释放blob URL
 * @param blobUrl blob URL
 */
export function revokeBlobUrl(blobUrl: string) {
  if (blobUrl && blobUrl.startsWith('blob:')) {
    URL.revokeObjectURL(blobUrl)
  }
}
