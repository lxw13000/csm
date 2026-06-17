<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import * as api from '@/api'
import type { AccountSaveDTO, AccountVO, Role } from '@/types/api'

const ACCOUNT_TYPES: Record<number, string> = { 1: '平台超管', 2: '租户管理员', 3: '客服' }

const loading = ref(false)
const rows = ref<AccountVO[]>([])
const total = ref(0)
const roles = ref<Role[]>([])
const query = reactive({ current: 1, size: 10, keyword: '', accountType: undefined as number | undefined, status: undefined as number | undefined })

const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<AccountSaveDTO>(emptyForm())

function emptyForm(): AccountSaveDTO {
  return { username: '', password: '', realName: '', accountType: 2, status: 1, roleIds: [] }
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  accountType: [{ required: true, message: '请选择账号类型', trigger: 'change' }]
}

onMounted(async () => {
  try {
    roles.value = await api.roleList()
  } catch {
    /* ignore */
  }
  await load()
})

async function load() {
  loading.value = true
  try {
    const page = await api.accountPage({ ...query })
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

function openEdit(row: AccountVO) {
  editId.value = row.id
  Object.assign(form, {
    username: row.username, password: '', realName: row.realName, accountType: row.accountType,
    status: row.status, roleIds: row.roleIds || []
  })
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (editId.value) await api.accountUpdate(editId.value, { ...form })
    else await api.accountCreate({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  })
}

async function toggleStatus(row: AccountVO) {
  await api.accountStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  load()
}

async function resetPwd(row: AccountVO) {
  const { value } = await ElMessageBox.prompt('请输入新密码', '重置密码', { inputType: 'password' })
  if (!value) return
  await api.accountResetPwd(row.id, value)
  ElMessage.success('密码已重置')
}
</script>

<template>
  <el-card v-loading="loading">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键词"><el-input v-model="query.keyword" clearable placeholder="账号/姓名" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.accountType" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="(label, val) in ACCOUNT_TYPES" :key="val" :label="label" :value="Number(val)" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
      </el-form>
      <el-button type="success" @click="openCreate">新增账号</el-button>
    </div>

    <el-table :data="rows" border size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ ACCOUNT_TYPES[row.accountType] || row.accountType }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="resetPwd(row)">重置密码</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" layout="total, prev, pager, next" :total="total"
      v-model:current-page="query.current" :page-size="query.size" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑账号' : '新增账号'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="username"><el-input v-model="form.username" :disabled="!!editId" /></el-form-item>
        <el-form-item :label="editId ? '新密码' : '密码'" :prop="editId ? '' : 'password'">
          <el-input v-model="form.password" type="password" show-password :placeholder="editId ? '留空则不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="类型" prop="accountType">
          <el-select v-model="form.accountType" style="width: 100%">
            <el-option v-for="(label, val) in ACCOUNT_TYPES" :key="val" :label="label" :value="Number(val)" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%" placeholder="可多选">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
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
</style>
