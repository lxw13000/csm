import { defineStore } from 'pinia'
import type { CredentialVO, CustomerVO } from '@/types/api'
import { SESSION_KEY, TOKEN_KEY } from '@/api/request'

interface SessionData {
  appId?: string
  userId?: string
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
    /** 业务系统已换好凭证：H5 直接拿凭证（如 URL 传入）建立会话。 */
    setCredential(token: string, data?: SessionData) {
      this.token = token
      this.session = data ?? this.session ?? {}
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(SESSION_KEY, JSON.stringify(this.session))
    },
    /** 用凭证颁发返回体落地会话（本地联调换取凭证后使用）。 */
    useCredential(vo: CredentialVO) {
      this.setCredential(vo.credential, { appId: vo.appId, userId: vo.userId, customer: vo.customer })
    },
    reset() {
      this.token = ''
      this.session = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(SESSION_KEY)
    }
  }
})
