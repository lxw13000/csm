<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as api from '@/api'
import type { TicketStatsVO } from '@/types/api'

const range = ref<[string, string] | null>(null)
const stats = ref<TicketStatsVO>()
const loading = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {}
    if (range.value) {
      params.startDate = range.value[0]
      params.endDate = range.value[1]
    }
    stats.value = await api.statsTicket(params)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div v-loading="loading">
    <el-card>
      <el-form inline>
        <el-form-item label="日期范围">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
            start-placeholder="开始" end-placeholder="结束" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-descriptions :column="3" border class="mt">
        <el-descriptions-item label="工单总量">{{ stats?.total ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="智能问答">{{ stats?.qa ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="人工转接中">{{ stats?.transferring ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="处理中">{{ stats?.processing ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已完结">{{ stats?.closed ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="完结率">{{ ((stats?.closeRate ?? 0) * 100).toFixed(1) }}%</el-descriptions-item>
        <el-descriptions-item label="平均处理时长">{{ ((stats?.avgHandleSeconds ?? 0) / 60).toFixed(1) }} 分钟</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<style scoped>
.mt { margin-top: 16px; }
</style>
