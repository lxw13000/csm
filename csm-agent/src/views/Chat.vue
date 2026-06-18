<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showImagePreview, showSuccessToast, showToast } from 'vant'
import * as api from '@/api'
import { initRealtime, onWs, wsOpen, wsSend } from '@/utils/realtime'
import { unlockAudio } from '@/utils/notify'
import { contentTypeOfFile, fileNameOf, isVideoUrl } from '@/utils/media'
import { TOKEN_KEY } from '@/api/request'
import type { AccountBrief, MessageVO, TicketVO, WsInbound } from '@/types/api'

const route = useRoute()
const router = useRouter()
const ticketId = Number(route.params.id)

const ticket = ref<TicketVO>()
const messages = ref<MessageVO[]>([])
const input = ref('')
const sending = ref(false)
const uploading = ref(false)
const peerTyping = ref(false)
const peerReadSeq = ref(0)
const PAGE = 20
const earliestId = ref<number | null>(null)
const loadingMore = ref(false)
const noMore = ref(false)
const closed = computed(() => ticket.value?.status === 4)
const listRef = ref<HTMLElement>()
const imageInput = ref<HTMLInputElement>()
const fileInput = ref<HTMLInputElement>()
let unsub: (() => void) | null = null
let tempSeq = -1
let typingTimer: number | undefined
let lastTypingSent = 0

const transfer = reactive({ show: false, toAgentId: 0, toAgentName: '', reason: '' })
const sheet = reactive({ show: false, actions: [] as { name: string; agentId: number }[] })

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
    // 点开工单 = 接入人工（转接中 -> 处理中）
    ticket.value = await api.acceptTicket(ticketId)
    // 按工单所属用户的全量历史加载最近 PAGE 条，向上滚动加载更早历史
    const list = await api.ticketMessagesBefore(ticketId, undefined, PAGE)
    messages.value = list
    noMore.value = list.length < PAGE
    earliestId.value = list.length ? list[0].id : null
    scrollToBottom()
    reportRead()
  } catch {
    /* ignore */
  }
}

/** 向上滚动加载更早历史（保持滚动位置）。 */
async function loadMore() {
  if (loadingMore.value || noMore.value || earliestId.value == null) return
  loadingMore.value = true
  const el = listRef.value
  const prevH = el ? el.scrollHeight : 0
  try {
    const older = await api.ticketMessagesBefore(ticketId, earliestId.value, PAGE)
    if (older.length < PAGE) noMore.value = true
    const seen = new Set(messages.value.map((m) => m.id))
    const fresh = older.filter((m) => !seen.has(m.id))
    if (fresh.length) {
      messages.value = [...fresh, ...messages.value]
      earliestId.value = fresh[0].id
      nextTick(() => {
        if (el) el.scrollTop = el.scrollHeight - prevH
      })
    }
  } catch {
    /* ignore */
  } finally {
    loadingMore.value = false
  }
}

function onScroll() {
  const el = listRef.value
  if (el && el.scrollTop < 40) loadMore()
}

function maxRealId(): number {
  return messages.value.reduce((m, x) => (!x._pending && x.id > m ? x.id : m), 0)
}

function upsert(m: MessageVO) {
  if (m.id) {
    const i = messages.value.findIndex((x) => x.id === m.id)
    if (i >= 0) {
      messages.value[i] = m
      return
    }
  }
  messages.value.push(m)
}

function handleWs(msg: WsInbound) {
  if (msg.type === '__open') {
    const after = maxRealId()
    if (after > 0) {
      api.ticketMessages(ticketId, after).then((list) => {
        list.forEach(upsert)
        scrollToBottom()
      })
    }
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
    if (i >= 0) {
      if (msg.id != null) messages.value[i].id = msg.id
      if (msg.seq != null) messages.value[i].seq = msg.seq
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
  // 已读水位按工单维护：仅上报当前工单内的最大 seq
  const seq = messages.value.reduce(
    (m, x) => (!x._pending && x.ticketId === ticketId && x.seq > m ? x.seq : m), 0)
  if (seq <= 0) return
  // 单通道上报，避免「WS read + REST markRead」并发首次插入造成唯一键冲突
  if (wsOpen()) wsSend({ type: 'read', ticketId, seq })
  else api.markRead(ticketId, seq).catch(() => undefined)
}

function onInput() {
  const now = Date.now()
  if (now - lastTypingSent > 1500) {
    lastTypingSent = now
    wsSend({ type: 'typing', ticketId })
  }
}

function sendText() {
  const content = input.value.trim()
  if (!content) return
  input.value = ''
  sendContent(content, 1)
}

function sendContent(content: string, contentType: number) {
  if (closed.value) {
    showToast('工单已完结')
    return
  }
  unlockAudio()
  const clientMsgId = 'a-' + Date.now() + '-' + Math.floor(performance.now())
  const optimistic: MessageVO = {
    id: 0, ticketId, seq: tempSeq--, senderType: 2, contentType, content,
    createdAt: new Date().toLocaleString(), _pending: true, _clientMsgId: clientMsgId
  }
  messages.value.push(optimistic)
  scrollToBottom()

  if (wsOpen()) {
    wsSend({ type: 'chat', ticketId, content, contentType, clientMsgId })
  } else {
    sending.value = true
    api.reply(ticketId, { content, contentType, clientMsgId })
      .then((saved) => {
        const i = messages.value.findIndex((x) => x._clientMsgId === clientMsgId)
        if (i >= 0) messages.value[i] = saved
      })
      .catch(() => {
        const i = messages.value.findIndex((x) => x._clientMsgId === clientMsgId)
        if (i >= 0) messages.value.splice(i, 1)
      })
      .finally(() => (sending.value = false))
  }
}

async function onFileChosen(e: Event) {
  const el = e.target as HTMLInputElement
  const file = el.files?.[0]
  el.value = '' // 允许重复选择同一文件
  if (!file) return
  uploading.value = true
  try {
    const res = await api.upload(file)
    sendContent(res.url, contentTypeOfFile(file))
  } catch {
    showToast('上传失败')
  } finally {
    uploading.value = false
  }
}

function previewImage(url: string) {
  showImagePreview([url])
}

function openFile(url: string) {
  window.open(url, '_blank')
}

function scrollToBottom() {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function readTag(m: MessageVO): boolean {
  // 已读水位按工单维护，仅对当前工单内自己的消息显示「已读」
  return m.senderType === 2 && !m._pending && m.ticketId === ticketId && peerReadSeq.value >= m.seq
}

async function openTransfer() {
  try {
    const targets = await api.transferTargets()
    const me = ticket.value?.agentId
    sheet.actions = targets
      .filter((t: AccountBrief) => t.id !== me)
      .map((t) => ({ name: (t.realName || t.username) + ' (#' + t.id + ')', agentId: t.id }))
    transfer.toAgentId = 0
    transfer.toAgentName = ''
    transfer.reason = ''
    transfer.show = true
  } catch {
    /* ignore */
  }
}

function onPickAgent(action: { name: string; agentId: number }) {
  transfer.toAgentId = action.agentId
  transfer.toAgentName = action.name
  sheet.show = false // 选定后关闭客服列表，回到转接弹窗
}

async function doTransfer() {
  if (!transfer.toAgentId) {
    showToast('请选择目标客服')
    return
  }
  try {
    await api.transfer(ticketId, { toAgentId: transfer.toAgentId, reason: transfer.reason })
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
        <span class="act" @click="openTransfer">转接</span>
        <span class="act danger" @click="doClose">关闭</span>
      </template>
    </van-nav-bar>

    <div ref="listRef" class="messages" @scroll="onScroll">
      <div v-if="loadingMore" class="more">加载中…</div>
      <div v-for="m in messages" :key="m._clientMsgId || m.id" class="row" :class="senderClass(m)">
        <div class="bubble" :class="{ media: m.contentType !== 1 }">
          <template v-if="m.contentType === 2">
            <van-image :src="m.content" width="160" fit="cover" radius="6" @click="previewImage(m.content)" />
          </template>
          <template v-else-if="m.contentType === 3 && isVideoUrl(m.content)">
            <video :src="m.content" controls preload="metadata" class="video" />
          </template>
          <template v-else-if="m.contentType === 3">
            <div class="file" @click="openFile(m.content)">
              <van-icon name="description" size="20" />
              <span class="fname">{{ fileNameOf(m.content) }}</span>
            </div>
          </template>
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
      <van-icon name="photograph" size="24" class="attach" @click="imageInput?.click()" />
      <van-icon name="add-o" size="24" class="attach" @click="fileInput?.click()" />
      <van-field v-model="input" :disabled="closed" placeholder="输入回复…" @update:model-value="onInput"
        @keyup.enter="sendText" />
      <van-button type="primary" size="small" :loading="sending || uploading" :disabled="closed" @click="sendText">发送</van-button>
      <input ref="imageInput" type="file" accept="image/*" hidden @change="onFileChosen" />
      <input ref="fileInput" type="file" accept="video/*,*/*" hidden @change="onFileChosen" />
    </div>

    <van-dialog v-model:show="transfer.show" title="转接工单" show-cancel-button @confirm="doTransfer">
      <div class="dlg">
        <van-field :model-value="transfer.toAgentName" readonly is-link label="目标客服"
          placeholder="点击选择客服" @click="sheet.show = true" />
        <van-field v-model="transfer.reason" label="原因" placeholder="选填" />
      </div>
    </van-dialog>

    <van-action-sheet v-model:show="sheet.show" :actions="sheet.actions" cancel-text="取消"
      title="选择客服" @select="onPickAgent" />
  </div>
</template>

<style scoped>
.chat { display: flex; flex-direction: column; height: 100vh; }
.act { color: #fff; margin-left: 12px; }
.act.danger { color: #ffe1e1; }
.messages { flex: 1; overflow-y: auto; padding: 12px; background: #f7f8fa; }
.more { text-align: center; color: #969799; font-size: 12px; padding: 6px 0; }
.row { display: flex; flex-direction: column; margin-bottom: 12px; }
.row .bubble { max-width: 72%; padding: 8px 12px; border-radius: 10px; word-break: break-word; }
.row .bubble.media { padding: 4px; background: transparent !important; }
.row .sub { font-size: 11px; color: #969799; margin-top: 2px; }
.row.peer { align-items: flex-start; }
.row.peer .bubble { background: #fff; }
.row.mine { align-items: flex-end; }
.row.mine .bubble { background: #1989fa; color: #fff; }
.row.mine .sub { text-align: right; }
.row.sys { align-items: center; }
.row.sys .bubble { background: #ededed; color: #646566; font-size: 13px; }
.bubble.typing { background: #fff; color: #969799; font-size: 13px; }
.video { max-width: 220px; border-radius: 6px; }
.file { display: flex; align-items: center; gap: 6px; background: #fff; color: #323233; padding: 8px 12px; border-radius: 8px; }
.file .fname { max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.input-bar { display: flex; align-items: center; gap: 6px; padding: 8px; background: #fff; border-top: 1px solid #ebedf0; }
.input-bar .attach { color: #646566; }
.input-bar .van-field { flex: 1; }
.input-bar .van-button { margin-right: 4px; }
.dlg { padding: 8px 0; }
</style>
