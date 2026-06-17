<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { LoginDTO } from '@/types/api'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive<LoginDTO>({
  appId: '_platform_',
  username: 'admin',
  password: 'admin123'
})

const rules: FormRules = {
  appId: [{ required: true, message: '请输入租户标识', trigger: 'blur' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await auth.login({ ...form })
      ElMessage.success('登录成功')
      const redirect = route.query.redirect as string | undefined
      router.replace(redirect || '/')
    } catch (e) {
      ElMessage.error((e as Error).message || '登录失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 class="login-title">CSM 客服工单系统 · 管理后台</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" @keyup.enter="onSubmit">
        <el-form-item label="租户" prop="appId">
          <el-input v-model="form.appId" placeholder="平台超管：_platform_；租户：app_id" />
        </el-form-item>
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="登录密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="tips">
        演示账号：平台超管 <code>_platform_ / admin / admin123</code>；
        租户管理员 <code>biz_demo / admin / admin123</code>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d, #409eff);
}
.login-card {
  width: 420px;
}
.login-title {
  text-align: center;
  margin: 0 0 24px;
  font-size: 18px;
  color: #303133;
}
.tips {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
.tips code {
  color: #409eff;
}
</style>
