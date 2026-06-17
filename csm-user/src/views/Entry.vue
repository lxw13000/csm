<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { useSessionStore } from '@/stores/session'
import { unlockAudio } from '@/utils/notify'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const loading = ref(false)
const manual = ref(false)
const form = reactive({ appId: 'biz_demo', token: '' })

onMounted(async () => {
  // 业务 App 通过 WebView URL 注入 app_id + token：?app_id=biz_demo&token=xxx
  const appId = (route.query.app_id || route.query.appId) as string | undefined
  const token = route.query.token as string | undefined
  if (session.isReady && !token) {
    router.replace('/chat')
    return
  }
  if (appId && token) {
    await doAccess(appId, token)
  } else {
    manual.value = true
  }
})

async function doAccess(appId: string, token: string) {
  loading.value = true
  try {
    await session.access({ appId, token })
    unlockAudio()
    router.replace('/chat')
  } catch {
    manual.value = true
    showFailToast('接入失败，请重试')
  } finally {
    loading.value = false
  }
}

function onSubmit() {
  if (!form.appId || !form.token) {
    showFailToast('请填写 app_id 与 token')
    return
  }
  doAccess(form.appId, form.token)
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
          <van-field v-model="form.token" label="临时token" placeholder="业务 App 下发的一次性 token" required />
        </van-cell-group>
        <div class="submit">
          <van-button round block type="primary" native-type="submit" :loading="loading">进入客服</van-button>
        </div>
      </van-form>
      <div class="tips">
        正式环境由业务 App 通过 <code>?app_id=&token=</code> 自动注入；<br />
        演示租户 <code>biz_demo</code> 的 mock 接口会按 token 生成 user_id。
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
