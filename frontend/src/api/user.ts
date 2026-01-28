import apiClient from './axios'

export interface UserDTO {
  id: number
  username: string
  email?: string
  role: string
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  username: string
  password: string
  email?: string
  role: string
}

export interface UpdateUserRequest {
  username?: string
  password?: string
  email?: string
  role?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export const userApi = {
  // 获取用户列表（分页）
  getAll(page = 0, size = 10, sortBy = 'id', sortDir = 'DESC') {
    return apiClient.get<PageResponse<UserDTO>>('/admin/users', {
      params: { page, size, sortBy, sortDir }
    })
  },

  // 获取用户详情
  getById(id: number) {
    return apiClient.get<UserDTO>(`/admin/users/${id}`)
  },

  // 创建新用户
  create(data: CreateUserRequest) {
    return apiClient.post<UserDTO>('/admin/users', data)
  },

  // 更新用户信息
  update(id: number, data: UpdateUserRequest) {
    return apiClient.put<UserDTO>(`/admin/users/${id}`, data)
  },

  // 删除用户
  delete(id: number) {
    return apiClient.delete(`/admin/users/${id}`)
  },

  // 修改用户角色
  updateRole(id: number, role: string) {
    return apiClient.put<UserDTO>(`/admin/users/${id}/role`, { role })
  }
}
