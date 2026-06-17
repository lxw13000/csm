import { defineStore } from 'pinia'
import * as api from '@/api'
import type { AccessDTO, CustomerVO } from '@/types/api'
import { SESSION_KEY, TOKEN_KEY } from '@/api/request'

interface SessionData {
  appId: string
  userId: string
  customer?: CustomerVO
}

function loadSession(): SessionData | null {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY) || 'null')
  } catch {
    return null
  }
}

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    session: loadSession() as SessionData | null
  }),
  getters: {
    isReady: (s): boolean => !!s.token,
    nickname: (s): string => s.session?.customer?.nickname || s.session?.userId || '我'
  },
  actions: {
    async access(dto: AccessDTO) {
      const vo = await api.access(dto)
      this.token = vo.sessionToken
      this.session = { appId: vo.appId, userId: vo.userId, customer: vo.customer }
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(SESSION_KEY, JSON.stringify(this.session))
      return vo
    },
    reset() {
      this.token = ''
      this.session = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(SESSION_KEY)
    }
  }
})
