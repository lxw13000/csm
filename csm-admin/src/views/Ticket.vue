<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import * as api from '@/api'
import type { MessageVO, TicketVO } from '@/types/api'

const STATUS_MAP: Record<number, { label: string; type: string }> = {
  1: { label: '智能问答', type: 'info' },
  2: { label: '人工转接中', type: 'warning' },
  3: { label: '处理中', type: 'primary' },
  4: { label: '已完结', type: 'success' }
}
const CLOSE_MAP: Record<number, string> = { 1: '用户已解决', 2: '超时自动', 3: '客服强制' }

const loading = ref(false)
const rows = ref<TicketVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, userId: '', status: undefined as number | undefined, agentId: undefined as number | undefined })

const drawer = ref(false)
const msgLoading = ref(false)
const messages = ref<MessageVO[]>([])
const currentTicket = ref<TicketVO>()

onMounted(load)

async function load() {
  loading.value = true
  try {
    const page = await api.ticketPage({ ...query })
    rows.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

async function openMessages(row: TicketVO) {
  currentTicket.value = row
  drawer.value = true
  msgLoading.value = true
  try {
    messages.value = await api.ticketMessages(row.id)
  } finally {
    msgLoading.value = false
  }
}

function senderLabel(t: number): string {
  return t === 1 ? '用户' : t === 2 ? '客服' : '系统'
}

const VIDEO_EXT = ['.mp4', '.webm', '.ogg', '.mov', '.m4v', '.m3u8']
function isVideoUrl(url: string): boolean {
  const u = (url || '').toLowerCase().split('?')[0]
  return VIDEO_EXT.some((e) => u.endsWith(e))
}
function fileNameOf(url: string): string {
  return (url || '').split('?')[0].split('/').pop() || '文件'
}
</script>

<template>
  <el-card v-loading="loading">
    <el-form inline>
      <el-form-item label="用户ID"><el-input v-model="query.userId" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="(v, k) in STATUS_MAP" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
      </el-form-item>
      <el-form-item label="客服ID"><el-input v-model.number="query.agentId" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
    </el-form>

    <el-table :data="rows" border size="small">
      <el-table-column prop="id" label="工单号" width="90" />
      <el-table-column prop="userId" label="用户" width="140" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="(STATUS_MAP[row.status]?.type as any) || 'info'">{{ STATUS_MAP[row.status]?.label || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="完结方式" width="110">
        <template #default="{ row }">{{ row.closeType ? CLOSE_MAP[row.closeType] : '-' }}</template>
      </el-table-column>
      <el-table-column prop="agentId" label="客服ID" width="90" />
      <el-table-column prop="lastMsgAt" label="最后消息" width="170" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="openMessages(row)">聊天记录</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" layout="total, prev, pager, next" :total="total"
      v-model:current-page="query.current" :page-size="query.size" @current-change="load" />

    <el-drawer v-model="drawer" :title="`工单 #${currentTicket?.id} 聊天记录`" size="480px">
      <div v-loading="msgLoading" class="msg-list">
        <div v-for="m in messages" :key="m.id" class="msg" :class="'s' + m.senderType">
          <div class="meta">{{ senderLabel(m.senderType) }} · {{ m.createdAt }}</div>
          <div class="bubble">
            <el-image v-if="m.contentType === 2" :src="m.content" :preview-src-list="[m.content]"
              preview-teleported fit="cover" style="max-width: 200px" />
            <video v-else-if="m.contentType === 3 && isVideoUrl(m.content)" :src="m.content" controls
              style="max-width: 240px" />
            <el-link v-else-if="m.contentType === 3" type="primary" :href="m.content" target="_blank">
              {{ fileNameOf(m.content) }}
            </el-link>
            <span v-else>{{ m.content }}</span>
          </div>
        </div>
        <el-empty v-if="!msgLoading && !messages.length" description="暂无消息" />
      </div>
    </el-drawer>
  </el-card>
</template>

<style scoped>
.pager { margin-top: 12px; justify-content: flex-end; }
.msg-list { display: flex; flex-direction: column; gap: 12px; }
.msg .meta { font-size: 12px; color: #909399; margin-bottom: 2px; }
.msg .bubble { display: inline-block; padding: 8px 12px; border-radius: 8px; background: #f0f2f5; max-width: 80%; }
.msg.s2 { text-align: right; }
.msg.s2 .bubble { background: #ecf5ff; }
.msg.s3 .bubble { background: #fdf6ec; }
</style>
