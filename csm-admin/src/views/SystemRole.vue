<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElTree, type FormInstance, type FormRules } from 'element-plus'
import * as api from '@/api'
import type { MenuVO, Role, RoleSaveDTO } from '@/types/api'

const loading = ref(false)
const roles = ref<Role[]>([])

const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<RoleSaveDTO>({ name: '', code: '', remark: '' })
const rules: FormRules = {
  name: [{ required: true, message: '请输入角色名', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const menuDialog = ref(false)
const menuTree = ref<MenuVO[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()
const checkedKeys = ref<number[]>([])
const grantingRoleId = ref<number | null>(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    roles.value = await api.roleList()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editId.value = null
  Object.assign(form, { name: '', code: '', remark: '' })
  dialogVisible.value = true
}

function openEdit(row: Role) {
  editId.value = row.id
  Object.assign(form, { name: row.name, code: row.code, remark: row.remark })
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (editId.value) await api.roleUpdate(editId.value, { ...form })
    else await api.roleCreate({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  })
}

async function remove(row: Role) {
  await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '提示', { type: 'warning' })
  await api.roleDelete(row.id)
  ElMessage.success('已删除')
  load()
}

async function openGrant(row: Role) {
  grantingRoleId.value = row.id
  const [menus, savedIds] = await Promise.all([api.menuTree(), api.roleMenus(row.id)])
  menuTree.value = menus
  // 只回显叶子节点的勾选；父级（目录/含子项的菜单）由 el-tree 依据子节点自动推导半选/全选。
  // 否则把已保存的父级 id 直接放入勾选会级联勾上其全部子节点，导致刚取消的子项被重新勾上。
  const leafIds = new Set<number>()
  collectLeafIds(menus, leafIds)
  checkedKeys.value = savedIds.filter((id) => leafIds.has(id))
  menuDialog.value = true
}

/** 收集所有叶子节点（无子节点）的 id。 */
function collectLeafIds(nodes: MenuVO[], out: Set<number>) {
  for (const n of nodes) {
    if (n.children?.length) collectLeafIds(n.children, out)
    else out.add(n.id)
  }
}

async function saveGrant() {
  if (!grantingRoleId.value || !treeRef.value) return
  const checked = treeRef.value.getCheckedKeys(false) as number[]
  const half = treeRef.value.getHalfCheckedKeys() as number[]
  // 保存全选 + 半选（父级目录），后端据此渲染用户可见菜单树
  await api.roleSetMenus(grantingRoleId.value, [...checked, ...half])
  ElMessage.success('授权成功')
  menuDialog.value = false
}
</script>

<template>
  <el-card v-loading="loading">
    <div class="toolbar">
      <span>角色与权限</span>
      <el-button type="success" @click="openCreate">新增角色</el-button>
    </div>

    <el-table :data="roles" border size="small">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="角色名" />
      <el-table-column prop="code" label="编码" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openGrant(row)">分配权限</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑角色' : '新增角色'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码" prop="code"><el-input v-model="form.code" :disabled="!!editId" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialog" title="分配菜单权限" width="420px" destroy-on-close>
      <el-tree ref="treeRef" :data="menuTree" show-checkbox node-key="id"
        :default-checked-keys="checkedKeys" :props="{ label: 'name', children: 'children' }" default-expand-all />
      <template #footer>
        <el-button @click="menuDialog = false">取消</el-button>
        <el-button type="primary" @click="saveGrant">保存授权</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
</style>
