<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from '@/api'
import type { TenantConfig } from '@/types/api'

const loading = ref(false)
const saving = ref(false)
const form = reactive<TenantConfig>({
  maxConcurrent: 0,
  autoCloseMinutes: 15,
  notifySound: 1,
  ext: ''
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const cfg = await api.configGet()
    Object.assign(form, cfg)
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const cfg = await api.configUpdate({ ...form })
    Object.assign(form, cfg)
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-card v-loading="loading" header="客服接入配置（本租户）">
    <el-form :model="form" label-width="180px" style="max-width: 560px">
      <el-form-item label="单客服最大接入量">
        <el-input-number v-model="form.maxConcurrent" :min="0" />
        <span class="hint">0 表示不限制</span>
      </el-form-item>
      <el-form-item label="自动完结时长(分钟)">
        <el-input-number v-model="form.autoCloseMinutes" :min="1" />
      </el-form-item>
      <el-form-item label="新消息声音提醒">
        <el-switch v-model="form.notifySound" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.hint { margin-left: 10px; color: #909399; font-size: 12px; }
</style>
