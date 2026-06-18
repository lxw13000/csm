<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import * as api from '@/api'
import { useSessionStore } from '@/stores/session'
import { unlockAudio } from '@/utils/notify'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const loading = ref(false)
const manual = ref(false)
// 本地联调用：直接用 app_id+app_secret+user_id 换取凭证（正式环境由业务系统后端完成）
const form = reactive({ appId: 'biz_demo', appSecret: 'demo_secret_please_change', userId: '', nickname: '' })

onMounted(async () => {
  // 业务系统后端换好凭证后，由 WebView URL 注入：?app_id=biz_demo&credential=xxx
  const appId = (route.query.app_id || route.query.appId) as string | undefined
  const credential = (route.query.credential || route.query.token) as string | undefined
  if (credential) {
    session.setCredential(credential, { appId })
    unlockAudio()
    router.replace('/chat')
    return
  }
  if (session.isReady) {
    router.replace('/chat')
    return
  }
  manual.value = true
})

async function onSubmit() {
  if (!form.appId || !form.appSecret || !form.userId) {
    showFailToast('请填写 app_id、app_secret 与 user_id')
    return
  }
  loading.value = true
  try {
    const vo = await api.issueCredential({ ...form })
    session.useCredential(vo)
    unlockAudio()
    router.replace('/chat')
  } catch {
    showFailToast('换取凭证失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="entry">
    <div v-if="!manual" class="loading">
      <van-loading size="28px">正在接入客服…</van-loading>
    </div>

    <template v-else>
      <div class="brand">在线客服接入</div>
      <van-form @submit="onSubmit">
        <van-cell-group inset>
          <van-field v-model="form.appId" label="业务系统" placeholder="app_id" required />
          <van-field v-model="form.appSecret" label="密钥" placeholder="app_secret" required />
          <van-field v-model="form.userId" label="用户ID" placeholder="业务系统 user_id" required />
          <van-field v-model="form.nickname" label="昵称" placeholder="选填" />
        </van-cell-group>
        <div class="submit">
          <van-button round block type="primary" native-type="submit" :loading="loading">进入客服</van-button>
        </div>
      </van-form>
      <div class="tips">
        正式环境由业务系统后端用 <code>app_id+app_secret</code> 换取凭证，<br />
        再经 <code>?app_id=&credential=</code> 注入 H5；此表单仅用于本地联调。
      </div>
    </template>
  </div>
</template>

<style scoped>
.entry { padding-top: 16vh; }
.loading { text-align: center; }
.brand { text-align: center; font-size: 20px; font-weight: 700; color: #323233; margin-bottom: 24px; }
.submit { padding: 16px; }
.tips { text-align: center; color: #969799; font-size: 12px; line-height: 1.7; }
.tips code { color: #1989fa; }
</style>
