<template>
  <div class="auth-page auth-page-modern">
    <section class="auth-hero">
      <span class="hero-kicker">USER 默认角色</span>
      <h1>创建 CityGo 账号</h1>
      <p>注册后可以访问智慧旅游服务、生成规划并把行程保存到自己的账号下。后台管理入口仅管理员可见。</p>
      <div class="auth-feature-list">
        <span>独立账号</span>
        <span>私有行程</span>
        <span>真实数据提示</span>
      </div>
    </section>

    <el-card class="auth-card auth-card-modern">
      <h2>注册</h2>
      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名"><el-input v-model="form.username" placeholder="至少 2 个字符" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="confirmPassword" type="password" show-password /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="name@example.com" /></el-form-item>
        <el-form-item label="手机号（可选）"><el-input v-model="form.phone" /></el-form-item>
        <el-alert v-if="error" class="form-alert" :title="error" type="warning" show-icon :closable="false" />
        <el-button class="full-button" type="primary" :loading="loading" @click="submit">注册</el-button>
      </el-form>
      <p class="meta">已有账号？<el-link type="primary" @click="$router.push('/login')">返回登录</el-link></p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { register } = useAuth()
const loading = ref(false)
const error = ref('')
const confirmPassword = ref('')
const form = reactive({ username: '', password: '', email: '', phone: '' })

function validate() {
  if (!form.username.trim()) return '用户名不能为空。'
  if (form.password.length < 6) return '密码至少 6 位。'
  if (form.password !== confirmPassword.value) return '两次输入的密码不一致。'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return '邮箱格式不正确。'
  return ''
}

async function submit() {
  const message = validate()
  if (message) {
    error.value = message
    return
  }
  loading.value = true
  error.value = ''
  try {
    await register({
      username: form.username.trim(),
      password: form.password,
      email: form.email.trim(),
      phone: form.phone.trim()
    })
    ElMessage.success('注册成功，请登录。')
    router.replace('/login')
  } catch (e) {
    error.value = e?.response?.data?.message || e?.response?.data?.error || '注册失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}
</script>
