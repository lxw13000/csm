<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import * as api from '@/api'
import type { CustomerVO } from '@/types/api'

const loading = ref(false)
const rows = ref<CustomerVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '' })

const drawer = ref(false)
const detail = ref<CustomerVO>()
const detailLoading = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const page = await api.customerPage({ ...query })
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

async function openDetail(row: CustomerVO) {
  drawer.value = true
  detailLoading.value = true
  detail.value = row
  try {
    // 实时拉取业务系统最新用户信息
    detail.value = await api.customerDetail(row.userId)
  } catch {
    /* 失败时展示缓存信息 */
  } finally {
    detailLoading.value = false
  }
}
</script>

<template>
  <el-card v-loading="loading">
    <el-form inline>
      <el-form-item label="关键词"><el-input v-model="query.keyword" clearable placeholder="user_id/昵称" /></el-form-item>
      <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
    </el-form>

    <el-table :data="rows" border size="small">
      <el-table-column prop="userId" label="user_id" width="160" />
      <el-table-column label="头像" width="80">
        <template #default="{ row }"><el-avatar :size="32" :src="row.avatar" /></template>
      </el-table-column>
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="userLevel" label="等级" width="90" />
      <el-table-column prop="maskedPhone" label="手机号" width="140" />
      <el-table-column prop="registerTime" label="注册时间" width="170" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" layout="total, prev, pager, next" :total="total"
      v-model:current-page="query.current" :page-size="query.size" @current-change="load" />

    <el-drawer v-model="drawer" title="用户详情（实时查询）" size="420px">
      <div v-loading="detailLoading">
        <div class="avatar-row"><el-avatar :size="56" :src="detail?.avatar" /></div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="user_id">{{ detail?.userId }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ detail?.nickname }}</el-descriptions-item>
          <el-descriptions-item label="等级">{{ detail?.userLevel }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail?.maskedPhone }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ detail?.registerTime }}</el-descriptions-item>
          <el-descriptions-item label="数据来源">
            <el-tag :type="detail?.latest ? 'success' : 'warning'">
              {{ detail?.latest ? '业务系统实时' : '缓存(可能非最新)' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </el-card>
</template>

<style scoped>
.pager { margin-top: 12px; justify-content: flex-end; }
.avatar-row { text-align: center; margin-bottom: 16px; }
</style>
