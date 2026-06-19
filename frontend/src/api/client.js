import axios from 'axios'
import { ElMessage } from 'element-plus'

export const api = axios.create({
  baseURL: '/api',
  timeout: 20000
})

api.interceptors.request.use(config => {
  const token = window.localStorage.getItem('citygo:token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => response,
  error => {
    const status = error?.response?.status
    if (status === 401) {
      window.localStorage.removeItem('citygo:token')
      window.localStorage.removeItem('citygo:user')
      if (!window.location.pathname.startsWith('/login')) {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
      }
    } else if (status === 403) {
      ElMessage.warning(error?.response?.data?.message || '当前账号没有访问权限')
    }
    return Promise.reject(error)
  }
)

export function providerStatus() {
  return api.get('/system/provider-status').then(r => r.data)
}

export function apiErrorMessage(error, fallback = '操作失败，请稍后重试。') {
  const status = error?.response?.status
  const message = error?.response?.data?.message
  if (message) return message
  if (status === 401) return '登录已失效，请重新登录。'
  if (status === 403) return '没有权限执行此操作。'
  if (status === 404) return '保存接口不存在，请检查服务版本。'
  if (status === 409) return '该行程可能已经保存。'
  if (status === 413) return '行程数据过大，请精简后重新保存。'
  if (status >= 500) return '服务器保存失败，请稍后重试。'
  if (error?.code === 'ERR_NETWORK' || !error?.response) return '无法连接服务器，请检查服务状态。'
  return fallback
}
