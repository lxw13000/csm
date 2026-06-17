import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { showFailToast } from 'vant'

export const TOKEN_KEY = 'csm_agent_token'
export const USER_KEY = 'csm_agent_user'

const request: AxiosInstance = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.set('Authorization', 'Bearer ' + token)
  return config
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      if (body.code === 401) {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_KEY)
        if (location.pathname !== '/login') location.assign('/login')
        return Promise.reject(new Error(body.msg || '未登录'))
      }
      showFailToast(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body
  },
  (err) => {
    showFailToast(err?.message || '网络错误')
    return Promise.reject(err)
  }
)

export default request
