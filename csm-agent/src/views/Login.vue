<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { requestNotifyPermission, unlockAudio } from '@/utils/notify'
import type { LoginDTO } from '@/types/api'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const form = reactive<LoginDTO>({ appId: 'biz_demo', username: 'agent1', password: 'agent123' })

async function onSubmit() {
  loading.value = true
  try {
    await auth.login({ ...form })
    // 用户手势内解锁音频 + 申请通知权限（H5 提醒前置条件）
    unlockAudio()
    requestNotifyPermission()
    showSuccessToast('登录成功')
    router.replace('/tickets')
  } catch {
    /* 错误已统一提示 */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <div class="brand">CSM 客服端</div>
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field v-model="form.appId" label="租户" placeholder="app_id" required />
        <van-field v-model="form.username" label="账号" placeholder="客服账号" required />
        <van-field v-model="form.password" type="password" label="密码" placeholder="登录密码" required />
      </van-cell-group>
      <div class="submit">
        <van-button round block type="primary" native-type="submit" :loading="loading">登 录</van-button>
      </div>
    </van-form>
    <div class="tips">演示：biz_demo / agent1 / agent123</div>
  </div>
</template>

<style scoped>
.login { padding-top: 18vh; }
.brand { text-align: center; font-size: 22px; font-weight: 700; color: #323233; margin-bottom: 28px; }
.submit { padding: 16px; }
.tips { text-align: center; color: #969799; font-size: 12px; }
</style>
