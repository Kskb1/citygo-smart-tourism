<template>
  <div class="auth-page auth-page-modern">
    <section class="auth-hero">
      <span class="hero-kicker">真实数据优先 · 行程私有保存</span>
      <h1>智游 CityGo</h1>
      <p>基于真实地图、天气、路线和 POI 数据的智慧旅游规划平台。登录后可以生成规划、保存个人行程并继续管理。</p>
      <div class="auth-feature-list">
        <span>真实 POI</span>
        <span>智能行程规划</span>
        <span>跨城市合理性判断</span>
        <span>个人行程保存</span>
      </div>
    </section>

    <el-card class="auth-card auth-card-modern">
      <h2>账号登录</h2>
      <p class="meta">登录后进入完整 CityGo 工作台。</p>
      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" autocomplete="current-password" />
        </el-form-item>
        <el-checkbox v-model="remember">记住登录</el-checkbox>
        <el-alert v-if="error" class="form-alert" :title="error" type="warning" show-icon :closable="false" />
        <el-button class="full-button" type="primary" :loading="loading" @click="submit">登录</el-button>
      </el-form>
      <div class="demo-actions">
        <el-button @click="fillDemo('admin')">填入管理员演示账号</el-button>
        <el-button @click="fillDemo('user')">填入普通用户演示账号</el-button>
      </div>
      <p class="meta">还没有账号？<el-link type="primary" @click="$router.push('/register')">立即注册</el-link></p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const route = useRoute()
const { login } = useAuth()
const loading = ref(false)
const error = ref('')
const remember = ref(true)
const form = reactive({ username: '', password: '' })

function fillDemo(role) {
  form.username = role
  form.password = '123456'
}

async function submit() {
  if (!form.username.trim() || !form.password) {
    error.value = '请输入用户名和密码。'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const user = await login(form.username.trim(), form.password)
    ElMessage.success(`欢迎回来，${user.username}`)
    router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
  } catch (e) {
    error.value = e?.response?.data?.message || e?.response?.data?.error || '登录失败，请检查用户名和密码。'
  } finally {
    loading.value = false
  }
}
</script>
