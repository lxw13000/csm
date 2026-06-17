import { defineStore } from 'pinia'
import * as api from '@/api'
import type { LoginDTO, LoginVO, MenuVO } from '@/types/api'
import { ACCT_TYPE_KEY, APPID_KEY, TOKEN_KEY, USER_KEY } from '@/api/request'

interface AuthState {
  token: string
  user: LoginVO | null
  currentAppId: string
}

function loadUser(): LoginVO | null {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: loadUser(),
    currentAppId: localStorage.getItem(APPID_KEY) || ''
  }),
  getters: {
    isLogin: (s): boolean => !!s.token,
    isPlatformSuper: (s): boolean => s.user?.accountType === 1,
    accountType: (s): number | undefined => s.user?.accountType,
    permCodes: (s): string[] => s.user?.permCodes || [],
    menus: (s): MenuVO[] => s.user?.menus || []
  },
  actions: {
    async login(dto: LoginDTO) {
      const vo = await api.login(dto)
      this.applySession(vo)
      return vo
    },
    applySession(vo: LoginVO) {
      if (vo.token) {
        this.token = vo.token
        localStorage.setItem(TOKEN_KEY, vo.token)
      }
      this.user = vo
      localStorage.setItem(USER_KEY, JSON.stringify(vo))
      localStorage.setItem(ACCT_TYPE_KEY, String(vo.accountType))
    },
    async refreshMe() {
      const vo = await api.me()
      // /me 不下发 token，沿用已有 token
      vo.token = this.token
      this.applySession(vo)
    },
    setCurrentAppId(appId: string) {
      this.currentAppId = appId
      localStorage.setItem(APPID_KEY, appId)
    },
    logout() {
      this.token = ''
      this.user = null
      this.currentAppId = ''
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem(APPID_KEY)
      localStorage.removeItem(ACCT_TYPE_KEY)
    }
  }
})
