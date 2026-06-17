<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import * as api from '@/api'
import type { MenuSaveDTO, MenuVO } from '@/types/api'

const TYPE_MAP: Record<number, string> = { 1: '目录', 2: '菜单', 3: '按钮' }

const loading = ref(false)
const tree = ref<MenuVO[]>([])
const flat = ref<{ id: number; name: string }[]>([])

const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<MenuSaveDTO>(emptyForm())

function emptyForm(): MenuSaveDTO {
  return { parentId: 0, name: '', type: 2, permCode: '', path: '', sort: 1 }
}

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    tree.value = await api.menuTree()
    flat.value = []
    flatten(tree.value)
  } finally {
    loading.value = false
  }
}

function flatten(nodes: MenuVO[]) {
  for (const n of nodes) {
    flat.value.push({ id: n.id, name: n.name })
    if (n.children?.length) flatten(n.children)
  }
}

function openCreate() {
  editId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row: MenuVO) {
  editId.value = row.id
  Object.assign(form, { parentId: row.parentId, name: row.name, type: row.type, permCode: row.permCode, path: row.path, sort: row.sort })
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (editId.value) await api.menuUpdate(editId.value, { ...form })
    else await api.menuCreate({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  })
}

async function remove(row: MenuVO) {
  await ElMessageBox.confirm(`确认删除菜单「${row.name}」？`, '提示', { type: 'warning' })
  await api.menuDelete(row.id)
  ElMessage.success('已删除')
  load()
}
</script>

<template>
  <el-card v-loading="loading">
    <div class="toolbar">
      <span>全局菜单 / 权限点字典</span>
      <el-button type="success" @click="openCreate">新增菜单</el-button>
    </div>

    <el-table :data="tree" row-key="id" border size="small" default-expand-all
      :tree-props="{ children: 'children' }">
      <el-table-column prop="name" label="名称" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ TYPE_MAP[row.type] }}</template>
      </el-table-column>
      <el-table-column prop="permCode" label="权限点" width="180" />
      <el-table-column prop="path" label="路由" width="180" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑菜单' : '新增菜单'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option label="顶级" :value="0" />
            <el-option v-for="m in flat" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="(label, val) in TYPE_MAP" :key="val" :label="label" :value="Number(val)" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限点"><el-input v-model="form.permCode" placeholder="如 ticket:list" /></el-form-item>
        <el-form-item label="路由"><el-input v-model="form.path" placeholder="如 /ticket" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
</style>
