<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as api from '@/api'
import type { AccountVO, AgentStatVO } from '@/types/api'

const range = ref<[string, string] | null>(null)
const agentId = ref<number>()
const agentOptions = ref<AccountVO[]>([])
const rows = ref<AgentStatVO[]>([])
const loading = ref(false)

onMounted(async () => {
  try {
    agentOptions.value = await api.agentsSimple()
  } catch {
    /* ignore */
  }
  await load()
})

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {}
    if (range.value) {
      params.startDate = range.value[0]
      params.endDate = range.value[1]
    }
    if (agentId.value) params.agentId = agentId.value
    rows.value = await api.statsAgent(params)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card v-loading="loading">
    <el-form inline>
      <el-form-item label="日期范围">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
          start-placeholder="开始" end-placeholder="结束" />
      </el-form-item>
      <el-form-item label="客服">
        <el-select v-model="agentId" clearable placeholder="全部" style="width: 160px">
          <el-option v-for="a in agentOptions" :key="a.id" :label="a.realName || a.username" :value="a.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" border size="small">
      <el-table-column prop="agentId" label="客服ID" width="90" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column label="在线时长">
        <template #default="{ row }">{{ (row.onlineSeconds / 3600).toFixed(1) }} 小时</template>
      </el-table-column>
      <el-table-column prop="ticketCount" label="接待工单" />
      <el-table-column prop="replyCount" label="回复数" />
      <el-table-column label="平均响应耗时">
        <template #default="{ row }">{{ row.avgResponseCost }} 秒</template>
      </el-table-column>
      <el-table-column prop="forceCloseCount" label="强制关闭" />
    </el-table>
  </el-card>
</template>
