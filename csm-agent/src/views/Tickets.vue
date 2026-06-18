<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import * as api from '@/api'
import { useAuthStore } from '@/stores/auth'
import { initRealtime, onWs } from '@/utils/realtime'
import { notifyNew, requestNotifyPermission, stopTitleFlash } from '@/utils/notify'
import { TOKEN_KEY } from '@/api/request'
import type { AccountBrief, TicketVO, WsInbound } from '@/types/api'

const router = useRouter()
const auth = useAuthStore()
const tickets = ref<TicketVO[]>([])
const online = ref(false)
const loading = ref(false)
// 处理中/待接入单数直接由列表派生，随列表刷新而更新（无需额外请求）
const processingCount = computed(() => tickets.value.filter((t) => t.status === 3).length)
const pendingCount = computed(() => tickets.value.filter((t) => t.status === 2).length)
const sheet = reactive({ show: false, actions: [] as { name: string; agentId: number }[] })
let transferTicketId = 0
let unsub: (() => void) | null = null

onMounted(async () => {
  if (!auth.user) {
    try {
      await auth.refreshMe()
    } catch {
      router.replace('/login')
      return
    }
  }
  requestNotifyPermission()
  initRealtime(localStorage.getItem(TOKEN_KEY) || '')
  unsub = onWs(handleWs)
  await refreshStatus()
  // 进入时加载一次；之后由 WS 实时刷新（新工单/消息/状态变更，含断线重连的 __open），或下拉刷新
  await loadList()
})

onUnmounted(() => {
  unsub?.()
})

function handleWs(msg: WsInbound) {
  // 新工单分发 / 状态变更 / 新消息 → 即时刷新「待我处理」列表并提醒
  if (['notification', 'ticket_status', 'chat', '__open'].includes(msg.type)) {
    loadList()
    if (msg.type === 'notification') notifyNew('新工单', '有新的工单分配给你', true)
    else if (msg.type === 'chat') notifyNew('新消息', '收到一条新消息', true)
  }
}

async function refreshStatus() {
  try {
    const st = await api.getStatus()
    online.value = st.onlineStatus === 1
  } catch {
    /* ignore */
  }
}

async function loadList() {
  loading.value = true
  try {
    tickets.value = await api.ticketList()
  } finally {
    loading.value = false
  }
}

async function toggleOnline(val: boolean) {
  try {
    const st = val ? await api.goOnline() : await api.goOffline()
    online.value = st.onlineStatus === 1
    showSuccessToast(online.value ? '已上线接单' : '已下线')
    if (online.value) loadList()
  } catch {
    online.value = !val
    showFailToast('操作失败')
  }
}

function statusText(s: number): string {
  return s === 2 ? '待接入' : s === 3 ? '处理中' : s === 1 ? '智能问答' : '已完结'
}

function openChat(t: TicketVO) {
  stopTitleFlash()
  router.push(`/chat/${t.id}`)
}

/** 从列表直接转交工单给其他客服。 */
async function openTransfer(t: TicketVO) {
  transferTicketId = t.id
  try {
    const targets = await api.transferTargets()
    sheet.actions = targets
      .filter((a: AccountBrief) => a.id !== t.agentId)
      .map((a) => ({ name: (a.realName || a.username) + ' (#' + a.id + ')', agentId: a.id }))
    if (!sheet.actions.length) {
      showFailToast('暂无其他可转交客服')
      return
    }
    sheet.show = true
  } catch {
    /* ignore */
  }
}

async function onPickAgent(action: { name: string; agentId: number }) {
  sheet.show = false // 选定后先关闭转交列表，再弹确认框
  try {
    await showConfirmDialog({ title: '转交工单', message: `确认将工单 #${transferTicketId} 转交给 ${action.name}？` })
  } catch {
    return // 用户取消
  }
  try {
    await api.transfer(transferTicketId, { toAgentId: action.agentId })
    showSuccessToast('已转交')
    loadList()
  } catch {
    /* ignore */
  }
}

async function onLogout() {
  await showConfirmDialog({ title: '提示', message: '确认退出登录？' })
  try {
    if (online.value) await api.goOffline()
    await api.logout()
  } catch {
    /* ignore */
  }
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="待我处理的工单">
      <template #right>
        <span class="logout" @click="onLogout">退出登录</span>
      </template>
    </van-nav-bar>

    <van-cell-group inset class="status">
      <van-cell title="接单状态" :label="`处理中 ${processingCount} · 待接入 ${pendingCount}`">
        <template #value>
          <van-switch v-model="online" size="22px" @change="toggleOnline" />
          <span class="state">{{ online ? '在线' : '离线' }}</span>
        </template>
      </van-cell>
    </van-cell-group>

    <van-pull-refresh v-model="loading" @refresh="loadList" class="list-wrap">
      <div class="list">
        <van-empty v-if="!tickets.length" description="暂无待处理的工单" />
        <van-cell v-for="t in tickets" :key="t.id" center @click="openChat(t)">
          <template #icon>
            <van-image round width="42" height="42" :src="t.avatar" class="avatar">
              <template #error><div class="ph">{{ (t.nickname || t.userId || '?').charAt(0) }}</div></template>
            </van-image>
          </template>
          <template #title>
            <span class="name">{{ t.nickname || t.userId }}</span>
            <van-tag :type="t.status === 2 ? 'warning' : 'success'" class="st">{{ statusText(t.status) }}</van-tag>
            <van-badge v-if="t.unreadCount" :content="t.unreadCount" class="badge" />
          </template>
          <template #label>工单 #{{ t.id }} · {{ t.lastMsgAt || t.createdAt || '' }}</template>
          <template #value>
            <van-button size="mini" type="primary" plain @click.stop="openTransfer(t)">转交</van-button>
          </template>
        </van-cell>
      </div>
    </van-pull-refresh>

    <van-action-sheet v-model:show="sheet.show" :actions="sheet.actions" cancel-text="取消"
      title="转交给" @select="onPickAgent" />
  </div>
</template>

<style scoped>
.page { min-height: 100%; background: #f7f8fa; }
.logout { color: #6d6d6d; }
.status { margin-top: 10px; }
.state { margin-left: 8px; color: #646566; font-size: 13px; vertical-align: middle; }
/* 与上方「接单状态」卡片拉开间距 */
.list-wrap { margin-top: 12px; }
.list { padding-bottom: 20px; background: #fff; }
.avatar { margin-right: 12px; }
.ph { width: 42px; height: 42px; border-radius: 50%; background: #c8c9cc; color: #fff; display: flex; align-items: center; justify-content: center; }
.name { font-weight: 600; }
.st { margin-left: 8px; }
.badge { margin-left: 8px; }
</style>
