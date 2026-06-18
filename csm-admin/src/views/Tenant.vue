<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import * as api from '@/api'
import type { Tenant, TenantSaveDTO } from '@/types/api'

const loading = ref(false)
const rows = ref<Tenant[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '', status: undefined as number | undefined })

const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<TenantSaveDTO>(emptyForm())

function emptyForm(): TenantSaveDTO {
  return { appId: '', appSecret: '', name: '', credentialExpireMinutes: 120, ipWhitelist: '', status: 1, remark: '' }
}

const rules: FormRules = {
  appId: [{ required: true, message: '请输入 app_id', trigger: 'blur' }],
  appSecret: [{ required: true, message: '请输入 app_secret', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const page = await api.tenantPage({ ...query })
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

function openCreate() {
  editId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row: Tenant) {
  editId.value = row.id
  Object.assign(form, {
    appId: row.appId, appSecret: row.appSecret, name: row.name,
    credentialExpireMinutes: row.credentialExpireMinutes, ipWhitelist: row.ipWhitelist,
    status: row.status, remark: row.remark
  })
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (editId.value) {
      await api.tenantUpdate(editId.value, { ...form })
    } else {
      await api.tenantCreate({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  })
}

async function toggleStatus(row: Tenant) {
  const next = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${next === 1 ? '启用' : '停用'}租户「${row.name}」？`, '提示')
  await api.tenantStatus(row.id, next)
  ElMessage.success('操作成功')
  load()
}
</script>

<template>
  <el-card v-loading="loading">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键词"><el-input v-model="query.keyword" clearable placeholder="名称/app_id" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="启用" :value="1" /><el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
      </el-form>
      <el-button type="success" @click="openCreate">新增租户</el-button>
    </div>

    <el-table :data="rows" border size="small">
      <el-table-column prop="appId" label="app_id" width="140" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="appSecret" label="app_secret" show-overflow-tooltip />
      <el-table-column prop="credentialExpireMinutes" label="凭证有效期(分钟)" width="130" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" layout="total, prev, pager, next" :total="total"
      v-model:current-page="query.current" :page-size="query.size" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑租户' : '新增租户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="app_id" prop="appId"><el-input v-model="form.appId" :disabled="!!editId" /></el-form-item>
        <el-form-item label="app_secret" prop="appSecret"><el-input v-model="form.appSecret" /></el-form-item>
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="凭证有效期">
          <el-input-number v-model="form.credentialExpireMinutes" :min="1" :max="525600" />
          <span class="unit">分钟</span>
        </el-form-item>
        <el-form-item label="IP 白名单"><el-input v-model="form.ipWhitelist" placeholder="逗号分隔，可空" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; }
.pager { margin-top: 12px; justify-content: flex-end; }
.unit { margin-left: 8px; color: #909399; }
</style>
