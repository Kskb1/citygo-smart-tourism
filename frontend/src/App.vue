<template>
  <router-view v-if="isAuthPage" />

  <el-container v-else class="shell">
    <el-aside width="232px" class="sidebar">
      <div class="brand">
        <span class="brand-mark">C</span>
        <div>
          <strong>智游 CityGo</strong>
          <small>Smart Tourism</small>
        </div>
      </div>

      <el-menu router :default-active="activeMenu">
        <el-menu-item index="/"><span class="nav-icon">H</span>首页</el-menu-item>
        <el-menu-item index="/planner"><span class="nav-icon">P</span>智能规划</el-menu-item>
        <el-menu-item index="/my-trips"><span class="nav-icon">M</span>我的行程</el-menu-item>
        <el-menu-item index="/transport"><span class="nav-icon">T</span>交通查询</el-menu-item>
        <el-menu-item index="/weather"><span class="nav-icon">W</span>天气查询</el-menu-item>
        <el-menu-item index="/hotels"><span class="nav-icon">S</span>酒店查询</el-menu-item>
        <el-menu-item index="/spots"><span class="nav-icon">D</span>景点推荐</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/admin"><span class="nav-icon">A</span>后台管理</el-menu-item>
      </el-menu>

      <div class="auth-panel">
        <div class="user-card">
          <strong>{{ user?.username || '未登录' }}</strong>
          <el-tag size="small" :type="isAdmin ? 'danger' : 'success'">
            {{ isAdmin ? '管理员' : '普通用户' }}
          </el-tag>
        </div>
        <el-button class="full-button" @click="handleLogout">退出登录</el-button>
      </div>
    </el-aside>

    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from './composables/useAuth'

const route = useRoute()
const router = useRouter()
const { user, isAdmin, logout } = useAuth()

const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')
const activeMenu = computed(() => {
  if (route.path.startsWith('/my-trips')) return '/my-trips'
  if (route.path === '/plan') return '/planner'
  return route.path
})

function handleLogout() {
  logout()
  router.replace('/login')
}
</script>
