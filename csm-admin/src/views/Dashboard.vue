<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as api from '@/api'
import { todayRange } from '@/utils/date'
import type { AgentStatVO, TicketStatsVO } from '@/types/api'

// 默认统计当天
const range = ref<[string, string] | null>(todayRange())
const stats = ref<TicketStatsVO>()
const agents = ref<AgentStatVO[]>([])
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
    // 工单统计 + 客服工作情况统计（均为本租户，按所选日期范围过滤）
    stats.value = await api.statsTicket(params)
    agents.value = await api.statsAgent(params)
  } catch {
    /* 错误已统一提示 */
  } finally {
    loading.value = false
  }
}

function reset() {
  range.value = todayRange()
  load()
}

function fmtMinutes(seconds?: number): string {
  if (!seconds) return '0 分'
  return (seconds / 60).toFixed(1) + ' 分'
}
</script>

<template>
  <div v-loading="loading">
    <el-card class="filter">
      <el-form inline>
        <el-form-item label="日期范围">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
            start-placeholder="开始日期" end-placeholder="结束日期" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <span class="scope">统计范围：当前租户（默认当天，可调整日期范围；清空则为全部时间）</span>
    </el-card>

    <el-row :gutter="16" class="mt">
      <el-col :span="4"><el-card shadow="hover"><div class="stat"><div class="num">{{ stats?.total ?? 0 }}</div><div class="label">工单总量</div></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="stat"><div class="num">{{ stats?.processing ?? 0 }}</div><div class="label">处理中</div></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="stat"><div class="num">{{ stats?.transferring ?? 0 }}</div><div class="label">转接中</div></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="stat"><div class="num">{{ stats?.closed ?? 0 }}</div><div class="label">已完结</div></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="stat"><div class="num">{{ ((stats?.closeRate ?? 0) * 100).toFixed(1) }}%</div><div class="label">完结率</div></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="stat"><div class="num">{{ fmtMinutes(stats?.avgHandleSeconds) }}</div><div class="label">平均处理时长</div></div></el-card></el-col>
    </el-row>

    <el-card class="mt" header="客服工作情况统计">
      <el-table :data="agents" size="small" border>
        <el-table-column prop="realName" label="客服" />
        <el-table-column prop="ticketCount" label="接待工单" />
        <el-table-column prop="replyCount" label="回复数" />
        <el-table-column label="平均响应">
          <template #default="{ row }">{{ row.avgResponseCost }} 秒</template>
        </el-table-column>
        <el-table-column label="在线时长">
          <template #default="{ row }">{{ (row.onlineSeconds / 3600).toFixed(1) }} 小时</template>
        </el-table-column>
        <el-table-column prop="forceCloseCount" label="强制关闭" />
      </el-table>
      <el-empty v-if="!agents.length" description="暂无客服工作数据" />
    </el-card>
  </div>
</template>

<style scoped>
.filter { display: flex; flex-direction: column; }
.filter .scope { color: #909399; font-size: 12px; }
.stat { text-align: center; padding: 8px 0; }
.stat .num { font-size: 26px; font-weight: 700; color: #303133; }
.stat .label { color: #909399; margin-top: 6px; }
.mt { margin-top: 16px; }
</style>
