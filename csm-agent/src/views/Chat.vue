<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import * as api from '@/api'
import { initRealtime, onWs, wsOpen, wsSend } from '@/utils/realtime'
import { unlockAudio } from '@/utils/notify'
import { TOKEN_KEY } from '@/api/request'
import type { MessageVO, TicketVO, WsInbound } from '@/types/api'

const route = useRoute()
const router = useRouter()
const ticketId = Number(route.params.id)

const ticket = ref<TicketVO>()
const messages = ref<MessageVO[]>([])
const input = ref('')
const sending = ref(false)
const peerTyping = ref(false)
const peerReadSeq = ref(0)
const closed = computed(() => ticket.value?.status === 4)
const listRef = ref<HTMLElement>()
let unsub: (() => void) | null = null
let tempSeq = -1
let typingTimer: number | undefined
let lastTypingSent = 0

const transfer = reactive({ show: false, toAgentId: '' as string | number, reason: '' })

onMounted(async () => {
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
    ticket.value = await api.ticketDetail(ticketId)
    messages.value = await api.ticketMessages(ticketId)
    scrollToBottom()
    reportRead()
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
    // 重连后按最后序号增量恢复
    api.ticketMessages(ticketId, maxRealSeq()).then((list) => {
      list.forEach(upsert)
      scrollToBottom()
    })
    return
  }
  const data = msg.data || {}
  if (msg.type === 'chat') {
    const m = data as MessageVO
    if (m.ticketId !== ticketId) return
    upsert(m)
    scrollToBottom()
    reportRead()
  } else if (msg.type === 'typing') {
    if (data.ticketId === ticketId && data.from === 'user') showPeerTyping()
  } else if (msg.type === 'read') {
    if (data.ticketId === ticketId && data.seq != null) peerReadSeq.value = Math.max(peerReadSeq.value, data.seq)
  } else if (msg.type === 'ticket_status') {
    if (data.ticketId === ticketId && ticket.value) {
      ticket.value.status = data.status
      ticket.value.closeType = data.closeType
      if (data.status === 4) showToast('工单已完结')
    }
  } else if (msg.type === 'ack') {
    const i = messages.value.findIndex((x) => x._clientMsgId && x._clientMsgId === msg.clientMsgId)
    if (i >= 0 && msg.seq != null) {
      messages.value[i].seq = msg.seq
      messages.value[i]._pending = false
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
  if (seq <= 0) return
  wsSend({ type: 'read', ticketId, seq })
  api.markRead(ticketId, seq).catch(() => undefined)
}

function onInput() {
  const now = Date.now()
  if (now - lastTypingSent > 1500) {
    lastTypingSent = now
    wsSend({ type: 'typing', ticketId })
  }
}

async function send() {
  const content = input.value.trim()
  if (!content || closed.value) return
  unlockAudio()
  const clientMsgId = 'a-' + Date.now() + '-' + Math.floor(performance.now())
  const optimistic: MessageVO = {
    id: 0, ticketId, seq: tempSeq--, senderType: 2, contentType: 1, content,
    createdAt: new Date().toLocaleString(), _pending: true, _clientMsgId: clientMsgId
  }
  messages.value.push(optimistic)
  input.value = ''
  scrollToBottom()

  // 优先经 WebSocket 发送（服务端落库后回 ack）；连接不可用则降级 REST
  if (wsOpen()) {
    wsSend({ type: 'chat', ticketId, content, contentType: 1, clientMsgId })
  } else {
    sending.value = true
    try {
      const saved = await api.reply(ticketId, { content, contentType: 1, clientMsgId })
      const i = messages.value.findIndex((x) => x._clientMsgId === clientMsgId)
      if (i >= 0) messages.value[i] = saved
    } catch {
      const i = messages.value.findIndex((x) => x._clientMsgId === clientMsgId)
      if (i >= 0) messages.value.splice(i, 1)
    } finally {
      sending.value = false
    }
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function readTag(m: MessageVO): boolean {
  return m.senderType === 2 && !m._pending && peerReadSeq.value >= m.seq
}

async function doTransfer() {
  const id = Number(transfer.toAgentId)
  if (!id) {
    showToast('请输入目标客服ID')
    return
  }
  try {
    await api.transfer(ticketId, { toAgentId: id, reason: transfer.reason })
    transfer.show = false
    showSuccessToast('已转接')
    router.replace('/tickets')
  } catch {
    /* ignore */
  }
}

async function doClose() {
  await showConfirmDialog({ title: '强制关闭', message: '确认强制关闭该工单？' })
  try {
    await api.closeTicket(ticketId)
    showSuccessToast('已关闭')
    router.replace('/tickets')
  } catch {
    /* ignore */
  }
}

function senderClass(m: MessageVO): string {
  return m.senderType === 2 ? 'mine' : m.senderType === 3 ? 'sys' : 'peer'
}
</script>

<template>
  <div class="chat">
    <van-nav-bar :title="ticket?.nickname || ticket?.userId || '会话'" left-arrow @click-left="router.back()">
      <template #right>
        <span class="act" @click="transfer.show = true">转接</span>
        <span class="act danger" @click="doClose">关闭</span>
      </template>
    </van-nav-bar>

    <div ref="listRef" class="messages">
      <div v-for="m in messages" :key="m._clientMsgId || m.seq" class="row" :class="senderClass(m)">
        <div class="bubble">
          <van-image v-if="m.contentType === 2" :src="m.content" width="160" />
          <span v-else>{{ m.content }}</span>
        </div>
        <div class="sub">
          <span v-if="m._pending">发送中…</span>
          <span v-else-if="readTag(m)">已读</span>
          <span v-else-if="m.senderType === 2">已送达</span>
        </div>
      </div>
      <div v-if="peerTyping" class="row peer"><div class="bubble typing">对方正在输入…</div></div>
    </div>

    <div class="input-bar">
      <van-field v-model="input" :disabled="closed" placeholder="输入回复…" @update:model-value="onInput"
        @keyup.enter="send" />
      <van-button type="primary" size="small" :loading="sending" :disabled="closed" @click="send">发送</van-button>
    </div>

    <van-dialog v-model:show="transfer.show" title="转接工单" show-cancel-button :before-close="undefined"
      @confirm="doTransfer">
      <div class="dlg">
        <van-field v-model="transfer.toAgentId" type="digit" label="目标客服ID" placeholder="输入客服账号ID" />
        <van-field v-model="transfer.reason" label="原因" placeholder="选填" />
      </div>
    </van-dialog>
  </div>
</template>

<style scoped>
.chat { display: flex; flex-direction: column; height: 100vh; }
.act { color: #fff; margin-left: 12px; }
.act.danger { color: #ffe1e1; }
.messages { flex: 1; overflow-y: auto; padding: 12px; background: #f7f8fa; }
.row { display: flex; flex-direction: column; margin-bottom: 12px; }
.row .bubble { max-width: 72%; padding: 8px 12px; border-radius: 10px; word-break: break-word; }
.row .sub { font-size: 11px; color: #969799; margin-top: 2px; }
.row.peer { align-items: flex-start; }
.row.peer .bubble { background: #fff; }
.row.mine { align-items: flex-end; }
.row.mine .bubble { background: #1989fa; color: #fff; }
.row.mine .sub { text-align: right; }
.row.sys { align-items: center; }
.row.sys .bubble { background: #ededed; color: #646566; font-size: 13px; }
.bubble.typing { background: #fff; color: #969799; font-size: 13px; }
.input-bar { display: flex; align-items: center; gap: 8px; padding: 8px; background: #fff; border-top: 1px solid #ebedf0; }
.input-bar .van-field { flex: 1; }
.input-bar .van-button { margin-right: 4px; }
.dlg { padding: 8px 0; }
</style>
