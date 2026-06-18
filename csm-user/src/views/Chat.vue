<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showImagePreview, showSuccessToast, showToast } from 'vant'
import * as api from '@/api'
import { useSessionStore } from '@/stores/session'
import { initRealtime, onWs, wsOpen, wsSend } from '@/utils/realtime'
import { beep, unlockAudio } from '@/utils/notify'
import { contentTypeOfFile, fileNameOf, isVideoUrl } from '@/utils/media'
import { TOKEN_KEY } from '@/api/request'
import type { MessageVO, TicketVO, WsInbound } from '@/types/api'

const router = useRouter()
const session = useSessionStore()

const ticket = ref<TicketVO>()
const messages = ref<MessageVO[]>([])
const input = ref('')
const sending = ref(false)
const uploading = ref(false)
const peerTyping = ref(false)
const peerReadSeq = ref(0)
const PAGE = 10
const earliestId = ref<number | null>(null)
const loadingMore = ref(false)
const noMore = ref(false)
const listRef = ref<HTMLElement>()
const imageInput = ref<HTMLInputElement>()
const fileInput = ref<HTMLInputElement>()
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
    // 历史按当前用户（app_id+user_id）跨工单加载最近 PAGE(10) 条
    const list = await api.messagesBefore(undefined, PAGE)
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
    const older = await api.messagesBefore(earliestId.value, PAGE)
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
      api.messages(after).then((list) => {
        list.forEach(upsert)
        scrollToBottom()
      })
    }
    return
  }
  const data = msg.data || {}
  if (msg.type === 'chat') {
    const m = data as MessageVO
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
  if (!ticket.value) return
  const tid = ticket.value.id
  // 已读水位按工单维护：仅上报当前工单内的最大 seq
  const seq = messages.value.reduce(
    (m, x) => (!x._pending && x.ticketId === tid && x.seq > m ? x.seq : m), 0)
  if (seq <= 0) return
  // 单通道上报，避免「WS read + REST markRead」并发首次插入造成唯一键冲突
  if (wsOpen()) {
    wsSend({ type: 'read', ticketId: tid, seq })
  } else {
    api.markRead(tid, seq).catch(() => undefined)
  }
}

function onInput() {
  if (ticket.value) wsSend({ type: 'typing', ticketId: ticket.value.id })
}

function sendText() {
  const content = input.value.trim()
  if (!content) return
  input.value = ''
  sendContent(content, 1)
}

async function sendContent(content: string, contentType: number) {
  unlockAudio()
  const clientMsgId = 'u-' + Date.now() + '-' + Math.floor(performance.now())
  const optimistic: MessageVO = {
    id: 0, ticketId: ticket.value?.id ?? 0, seq: tempSeq--, senderType: 1, contentType, content,
    createdAt: new Date().toLocaleString(), _pending: true, _clientMsgId: clientMsgId
  }
  messages.value.push(optimistic)
  scrollToBottom()
  sending.value = true
  try {
    const result = await api.sendMessage({ content, contentType, clientMsgId })
    // 历史按用户连续，完结后再发会另建工单但仍属同一用户，消息直接接续，无需重置
    ticket.value = result.ticket
    const i = messages.value.findIndex((x) => x._clientMsgId === clientMsgId)
    if (i >= 0) messages.value[i] = result.message
    if (earliestId.value == null && result.message) earliestId.value = result.message.id
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

async function onFileChosen(e: Event) {
  const el = e.target as HTMLInputElement
  const file = el.files?.[0]
  el.value = ''
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
  // 已读水位按工单维护，仅对当前工单内自己的消息显示「已读」
  return m.senderType === 1 && !m._pending && m.ticketId === ticket.value?.id && peerReadSeq.value >= m.seq
}
</script>

<template>
  <div class="chat">
    <van-nav-bar :title="statusText" />

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
      <van-icon name="photograph" size="24" class="attach" @click="imageInput?.click()" />
      <van-icon name="add-o" size="24" class="attach" @click="fileInput?.click()" />
      <van-field v-model="input" placeholder="输入消息…" @update:model-value="onInput" @keyup.enter="sendText" />
      <van-button type="primary" size="small" :loading="sending || uploading" @click="sendText">发送</van-button>
      <input ref="imageInput" type="file" accept="image/*" hidden @change="onFileChosen" />
      <input ref="fileInput" type="file" accept="video/*,*/*" hidden @change="onFileChosen" />
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
.more { text-align: center; color: #969799; font-size: 12px; padding: 6px 0; }
.row { display: flex; flex-direction: column; margin-bottom: 12px; }
.row .bubble { max-width: 74%; padding: 8px 12px; border-radius: 10px; word-break: break-word; }
.row .bubble.media { padding: 4px; background: transparent !important; }
.row .sub { font-size: 11px; color: #969799; margin-top: 2px; }
.row.agent, .row.bot { align-items: flex-start; }
.row.agent .bubble { background: #fff; }
.row.bot .bubble { background: #eef3ff; color: #2b5cad; }
.row.mine { align-items: flex-end; }
.row.mine .bubble { background: #1989fa; color: #fff; }
.row.mine .sub { text-align: right; }
.bubble.typing { background: #fff; color: #969799; font-size: 13px; }
.video { max-width: 220px; border-radius: 6px; }
.file { display: flex; align-items: center; gap: 6px; background: #fff; color: #323233; padding: 8px 12px; border-radius: 8px; }
.file .fname { max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.toolbar { display: flex; gap: 8px; padding: 6px 12px; background: #f7f8fa; }
.input-bar { display: flex; align-items: center; gap: 6px; padding: 8px; background: #fff; border-top: 1px solid #ebedf0; }
.input-bar .attach { color: #646566; }
.input-bar .van-field { flex: 1; }
.input-bar .van-button { margin-right: 4px; }
.eval { padding: 16px; text-align: center; }
.eval .van-field { margin-top: 12px; }
</style>
