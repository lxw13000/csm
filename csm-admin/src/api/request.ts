import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// localStorage 键（与 stores/auth 保持一致）
export const TOKEN_KEY = 'csm_admin_token'
export const USER_KEY = 'csm_admin_user'
export const APPID_KEY = 'csm_admin_appid'
export const ACCT_TYPE_KEY = 'csm_admin_accttype'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.set('Authorization', 'Bearer ' + token)
  }
  // 平台超管：携带「当前选中租户」作为隔离键
  const acctType = localStorage.getItem(ACCT_TYPE_KEY)
  const appId = localStorage.getItem(APPID_KEY)
  if (acctType === '1' && appId) {
    config.headers.set('X-App-Id', appId)
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        return body.data
      }
      if (body.code === 401) {
        clearSession()
        if (location.pathname !== '/login') {
          location.assign('/login')
        }
        return Promise.reject(new Error(body.msg || '未登录或登录已过期'))
      }
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body
  },
  (err) => {
    ElMessage.error(err?.message || '网络错误')
    return Promise.reject(err)
  }
)

function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(APPID_KEY)
  localStorage.removeItem(ACCT_TYPE_KEY)
}

export default request
