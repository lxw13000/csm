<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import * as api from '@/api'
import type { MenuVO, Tenant } from '@/types/api'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const activePath = computed(() => '/' + route.path.replace(/^\//, ''))
// 仅渲染目录(1)/菜单(2)，按钮(3)不进侧边栏
const menuTree = computed<MenuVO[]>(() => auth.menus.filter((m) => m.type !== 3))

const tenants = ref<Tenant[]>([])
const selectedAppId = ref(auth.currentAppId)

onMounted(async () => {
  if (!auth.user) {
    try {
      await auth.refreshMe()
    } catch {
      router.replace('/login')
      return
    }
  }
  if (auth.isPlatformSuper) {
    try {
      const page = await api.tenantPage({ current: 1, size: 200 })
      tenants.value = page.records
      if (!selectedAppId.value && tenants.value.length) {
        // 平台超管默认选中首个租户，便于直接查看租户级数据
        selectedAppId.value = tenants.value[0].appId
        auth.setCurrentAppId(selectedAppId.value)
      }
    } catch {
      /* 忽略：无租户时不影响平台级页面 */
    }
  }
})

function onTenantChange(appId: string) {
  auth.setCurrentAppId(appId)
  ElMessage.success('已切换租户，正在刷新数据…')
  setTimeout(() => location.reload(), 300)
}

function childItems(menu: MenuVO): MenuVO[] {
  return (menu.children || []).filter((m) => m.type !== 3)
}

async function onLogout() {
  try {
    await api.logout()
  } catch {
    /* 忽略登出接口错误 */
  }
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">CSM 管理后台</div>
      <el-menu :default-active="activePath" router unique-opened background-color="#1f2d3d"
        text-color="#c0ccda" active-text-color="#409eff">
        <template v-for="menu in menuTree" :key="menu.id">
          <el-sub-menu v-if="menu.type === 1" :index="'dir-' + menu.id">
            <template #title>{{ menu.name }}</template>
            <el-menu-item v-for="c in childItems(menu)" :key="c.id" :index="c.path">{{ c.name }}</el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">{{ menu.name }}</el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="title">{{ route.meta.title || '' }}</div>
        <div class="right">
          <el-select v-if="auth.isPlatformSuper" v-model="selectedAppId" placeholder="选择租户"
            size="small" style="width: 200px" @change="onTenantChange">
            <el-option v-for="t in tenants" :key="t.appId" :label="`${t.name} (${t.appId})`" :value="t.appId" />
          </el-select>
          <span class="user">{{ auth.user?.realName || auth.user?.username }}</span>
          <el-button link type="primary" @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background: #1f2d3d;
  overflow-y: auto;
}
.aside .el-menu {
  border-right: none;
}
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-weight: 600;
  background: #16222e;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  background: #fff;
}
.header .title {
  font-size: 16px;
  font-weight: 600;
}
.header .right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header .user {
  color: #606266;
}
.main {
  background: #f5f7fa;
}
</style>
