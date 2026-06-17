import { defineStore } from 'pinia'
import * as api from '@/api'
import type { LoginDTO, LoginVO } from '@/types/api'
import { TOKEN_KEY, USER_KEY } from '@/api/request'

function loadUser(): LoginVO | null {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: loadUser() as LoginVO | null
  }),
  getters: {
    isLogin: (s): boolean => !!s.token
  },
  actions: {
    async login(dto: LoginDTO) {
      const vo = await api.login(dto)
      this.token = vo.token || ''
      this.user = vo
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(vo))
      return vo
    },
    async refreshMe() {
      const vo = await api.me()
      this.user = vo
      localStorage.setItem(USER_KEY, JSON.stringify(vo))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
