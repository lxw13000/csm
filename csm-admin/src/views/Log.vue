<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import * as api from '@/api'
import type { OperationLog } from '@/types/api'

const loading = ref(false)
const rows = ref<OperationLog[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, module: '', action: '', operatorId: undefined as number | undefined })

onMounted(load)

async function load() {
  loading.value = true
  try {
    const page = await api.logPage({ ...query })
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
</script>

<template>
  <el-card v-loading="loading">
    <el-form inline>
      <el-form-item label="模块"><el-input v-model="query.module" clearable placeholder="如 ticket/qa" /></el-form-item>
      <el-form-item label="动作"><el-input v-model="query.action" clearable placeholder="如 create/close" /></el-form-item>
      <el-form-item label="操作人ID"><el-input v-model.number="query.operatorId" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
    </el-form>

    <el-table :data="rows" border size="small">
      <el-table-column prop="createdAt" label="时间" width="170" />
      <el-table-column prop="operatorName" label="操作人" width="120" />
      <el-table-column prop="module" label="模块" width="110" />
      <el-table-column prop="action" label="动作" width="110" />
      <el-table-column prop="targetType" label="对象类型" width="120" />
      <el-table-column prop="targetId" label="对象ID" width="100" />
      <el-table-column prop="detail" label="详情" show-overflow-tooltip />
      <el-table-column prop="clientIp" label="IP" width="140" />
    </el-table>

    <el-pagination class="pager" layout="total, prev, pager, next" :total="total"
      v-model:current-page="query.current" :page-size="query.size" @current-change="load" />
  </el-card>
</template>

<style scoped>
.pager { margin-top: 12px; justify-content: flex-end; }
</style>
