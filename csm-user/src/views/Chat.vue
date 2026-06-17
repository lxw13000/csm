<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import * as api from '@/api'
import { useSessionStore } from '@/stores/session'
import { initRealtime, onWs, wsSend } from '@/utils/realtime'
import { beep, unlockAudio } from '@/utils/notify'
import { TOKEN_KEY } from '@/api/request'
import type { MessageVO, TicketVO, WsInbound } from '@/types/api'

const router = useRouter()
const session = useSessionStore()

const ticket = ref<TicketVO>()
const messages = ref<MessageVO[]>([])
const input = ref('')
const sending = ref(false)
const peerTyping = ref(false)
const peerReadSeq = ref(0)
const listRef = ref<HTMLElement>()
let unsub: (() => void) | null = null
let tempSeq = -1
let typingTimer: number | undefined

const evalDialog = reactive({ show: false, rating: 5, remark: '' })

const status = computed(() => ticket.value?.status ?? 1)
const closed = computed(() => status.value === 4)
const statusText = computed(() => {
  switch (status.value) {
    case 1: return '智能助手为您服务中'
    case 2: return '正在为您转接人工客服…'
    case 3: return '人工客服已接入'
    case 4: return '本次会话已结束'
    default: return ''
  }
})

onMounted(async () => {
  if (!session.isReady) {
    router.replace('/entry')
    return
  }
  initRealtime(localStorage.getItem(TOKEN_KEY) || '')
  unsub = onWs(handleWs)
  await loadAll()
})

onUnmounted(() => {
  unsub?.()
  if (typingTimer) clearTimeout(typingTimer)
})

async function loadAll() {
  try {
    ticket.value = await api.currentTicket()
    if (ticket.value) {
      messages.value = await api.messages(ticket.value.id)
      scrollToBottom()
      reportRead()
    }
  } catch {
    /* ignore */
  }
}

function maxRealSeq(): number {
  return messages.value.reduce((m, x) => (!x._pending && x.seq > m ? x.seq : m), 0)
}

function upsert(m: MessageVO) {
  if (m.seq != null) {
    const i = messages.value.findIndex((x) => x.seq === m.seq)
    if (i >= 0) {
      messages.value[i] = m
      return
    }
  }
  messages.value.push(m)
}

function handleWs(msg: WsInbound) {
  if (msg.type === '__open') {
    if (ticket.value) {
      api.messages(ticket.value.id, maxRealSeq()).then((list) => {
        list.forEach(upsert)
        scrollToBottom()
      })
    }
    return
  }
  const data = msg.data || {}
  if (msg.type === 'chat') {
    const m = data as MessageVO
    if (ticket.value && m.ticketId !== ticket.value.id) return
    upsert(m)
    if (m.senderType !== 1) beep()
    scrollToBottom()
    reportRead()
  } else if (msg.type === 'typing') {
    if (data.from === 'agent') showPeerTyping()
  } else if (msg.type === 'read') {
    if (data.seq != null) peerReadSeq.value = Math.max(peerReadSeq.value, data.seq)
  } else if (msg.type === 'ticket_status') {
    if (ticket.value && data.ticketId === ticket.value.id) {
      ticket.value.status = data.status
      ticket.value.closeType = data.closeType
      if (data.status === 4 && !evalDialog.show) showToast('客服已结束本次会话')
    }
  }
}

function showPeerTyping() {
  peerTyping.value = true
  if (typingTimer) clearTimeout(typingTimer)
  typingTimer = window.setTimeout(() => (peerTyping.value = false), 3000)
}

function reportRead() {
  const seq = maxRealSeq()
  if (seq <= 0 || !ticket.value) return
  wsSend({ type: 'read', ticketId: ticket.value.id, seq })
  api.markRead(ticket.value.id, seq).catch(() => undefined)
}

function onInput() {
  if (ticket.value) wsSend({ type: 'typing', ticketId: ticket.value.id })
}

async function send() {
  const content = input.value.trim()
  if (!content) return
  unlockAudio()
  const clientMsgId = 'u-' + Date.now() + '-' + Math.floor(performance.now())
  const optimistic: MessageVO = {
    id: 0, ticketId: ticket.value?.id ?? 0, seq: tempSeq--, senderType: 1, contentType: 1, content,
    createdAt: new Date().toLocaleString(), _pending: true, _clientMsgId: clientMsgId
  }
  messages.value.push(optimistic)
  input.value = ''
  scrollToBottom()
  sending.value = true
  try {
    const result = await api.sendMessage({ content, contentType: 1, clientMsgId })
    ticket.value = result.ticket
    const i = messages.value.findIndex((x) => x._clientMsgId === clientMsgId)
    if (i >= 0) messages.value[i] = result.message
    if (result.botReply) {
      upsert(result.botReply)
      beep()
    }
    if (result.transferred) showToast('正在为您转接人工客服…')
    scrollToBottom()
  } catch {
    const i = messages.value.findIndex((x) => x._clientMsgId === clientMsgId)
    if (i >= 0) messages.value.splice(i, 1)
  } finally {
    sending.value = false
  }
}

async function onRequestHuman() {
  try {
    ticket.value = await api.requestHuman()
    showToast('正在为您转接人工客服…')
  } catch {
    /* ignore */
  }
}

async function onResolved() {
  try {
    ticket.value = await api.resolve()
    evalDialog.show = true
  } catch {
    /* ignore */
  }
}

async function onUnresolved() {
  try {
    ticket.value = await api.unresolved()
    showToast('已为您继续会话')
  } catch {
    /* ignore */
  }
}

async function submitEvaluate() {
  try {
    await api.evaluate({ resolved: 1, rating: evalDialog.rating, remark: evalDialog.remark })
    showSuccessToast('感谢您的评价')
  } catch {
    /* ignore */
  } finally {
    evalDialog.show = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function senderClass(m: MessageVO): string {
  return m.senderType === 1 ? 'mine' : m.senderType === 3 ? 'bot' : 'agent'
}

function readTag(m: MessageVO): boolean {
  return m.senderType === 1 && !m._pending && peerReadSeq.value >= m.seq
}
</script>

<template>
  <div class="chat">
    <van-nav-bar :title="statusText" />

    <div ref="listRef" class="messages">
      <div v-for="m in messages" :key="m._clientMsgId || m.seq" class="row" :class="senderClass(m)">
        <div class="bubble">
          <van-image v-if="m.contentType === 2" :src="m.content" width="160" />
          <span v-else>{{ m.content }}</span>
        </div>
        <div class="sub">
          <span v-if="m._pending">发送中…</span>
          <span v-else-if="readTag(m)">已读</span>
        </div>
      </div>
      <div v-if="peerTyping" class="row agent"><div class="bubble typing">客服正在输入…</div></div>
      <van-empty v-if="!messages.length" description="发条消息开始咨询吧" />
    </div>

    <div class="toolbar">
      <van-button v-if="status === 1" size="small" plain type="primary" @click="onRequestHuman">转人工</van-button>
      <template v-if="status === 2 || status === 3">
        <van-button size="small" plain type="success" @click="onResolved">已解决</van-button>
        <van-button size="small" plain @click="onUnresolved">未解决</van-button>
      </template>
      <van-button v-if="closed" size="small" plain type="primary" @click="evalDialog.show = true">评价服务</van-button>
    </div>

    <div class="input-bar">
      <van-field v-model="input" placeholder="输入消息…" @update:model-value="onInput" @keyup.enter="send" />
      <van-button type="primary" size="small" :loading="sending" @click="send">发送</van-button>
    </div>

    <van-dialog v-model:show="evalDialog.show" title="服务评价" show-cancel-button @confirm="submitEvaluate">
      <div class="eval">
        <van-rate v-model="evalDialog.rating" />
        <van-field v-model="evalDialog.remark" rows="2" type="textarea" placeholder="说点什么…（选填）" />
      </div>
    </van-dialog>
  </div>
</template>

<style scoped>
.chat { display: flex; flex-direction: column; height: 100vh; }
.messages { flex: 1; overflow-y: auto; padding: 12px; }
.row { display: flex; flex-direction: column; margin-bottom: 12px; }
.row .bubble { max-width: 74%; padding: 8px 12px; border-radius: 10px; word-break: break-word; }
.row .sub { font-size: 11px; color: #969799; margin-top: 2px; }
.row.agent, .row.bot { align-items: flex-start; }
.row.agent .bubble { background: #fff; }
.row.bot .bubble { background: #eef3ff; color: #2b5cad; }
.row.mine { align-items: flex-end; }
.row.mine .bubble { background: #1989fa; color: #fff; }
.row.mine .sub { text-align: right; }
.bubble.typing { background: #fff; color: #969799; font-size: 13px; }
.toolbar { display: flex; gap: 8px; padding: 6px 12px; background: #f7f8fa; }
.input-bar { display: flex; align-items: center; gap: 8px; padding: 8px; background: #fff; border-top: 1px solid #ebedf0; }
.input-bar .van-field { flex: 1; }
.input-bar .van-button { margin-right: 4px; }
.eval { padding: 16px; text-align: center; }
.eval .van-field { margin-top: 12px; }
</style>
