import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { showFailToast } from 'vant'

export const TOKEN_KEY = 'csm_user_token'
export const SESSION_KEY = 'csm_user_session'

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
        localStorage.removeItem(SESSION_KEY)
        if (location.pathname !== '/entry') location.assign('/entry')
        return Promise.reject(new Error(body.msg || '会话已过期'))
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
