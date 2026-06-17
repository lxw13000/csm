<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import * as api from '@/api'
import type { QaSaveDTO, QaVO } from '@/types/api'

const loading = ref(false)
const rows = ref<QaVO[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '', status: undefined as number | undefined })

const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<QaSaveDTO>({ question: '', answer: '', status: 1, keywords: [] })

const rules: FormRules = {
  question: [{ required: true, message: '请输入问题', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const page = await api.qaPage({ ...query })
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
  Object.assign(form, { question: '', answer: '', status: 1, keywords: [] })
  dialogVisible.value = true
}

async function openEdit(row: QaVO) {
  editId.value = row.id
  const detail = await api.qaGet(row.id)
  Object.assign(form, { question: detail.question, answer: detail.answer, status: detail.status, keywords: detail.keywords || [] })
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (editId.value) await api.qaUpdate(editId.value, { ...form })
    else await api.qaCreate({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  })
}

async function toggle(row: QaVO) {
  await api.qaStatus(row.id, row.status === 1 ? 0 : 1)
  load()
}

async function remove(row: QaVO) {
  await ElMessageBox.confirm('确认删除该问答对？', '提示', { type: 'warning' })
  await api.qaDelete(row.id)
  ElMessage.success('已删除')
  load()
}
</script>

<template>
  <el-card v-loading="loading">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键词"><el-input v-model="query.keyword" clearable placeholder="问题关键字" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="启用" :value="1" /><el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
      </el-form>
      <el-button type="success" @click="openCreate">新增问答</el-button>
    </div>

    <el-table :data="rows" border size="small">
      <el-table-column prop="question" label="问题" show-overflow-tooltip />
      <el-table-column prop="answer" label="答案" show-overflow-tooltip />
      <el-table-column label="关键词" width="220">
        <template #default="{ row }">
          <el-tag v-for="k in row.keywords" :key="k" size="small" class="kw">{{ k }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" layout="total, prev, pager, next" :total="total"
      v-model:current-page="query.current" :page-size="query.size" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑问答' : '新增问答'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="问题" prop="question"><el-input v-model="form.question" /></el-form-item>
        <el-form-item label="答案" prop="answer"><el-input v-model="form.answer" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="关键词">
          <el-select v-model="form.keywords" multiple filterable allow-create default-first-option
            :reserve-keyword="false" style="width: 100%" placeholder="输入关键词后回车添加，可关联多个">
            <el-option v-for="k in form.keywords" :key="k" :label="k" :value="k" />
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
.kw { margin-right: 6px; }
</style>
