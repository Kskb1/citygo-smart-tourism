<template>
  <div class="page">
    <div class="page-heading trip-detail-heading">
      <div>
        <h1>{{ trip?.title || '行程详情' }}</h1>
        <p class="meta">{{ trip?.fromCity || '-' }} → {{ trip?.toCity || '-' }} · {{ trip?.days || '-' }} 天 · 创建于 {{ formatTime(trip?.createdAt) }}</p>
      </div>
      <div class="trip-actions">
        <el-button @click="router.push('/my-trips')">返回我的行程</el-button>
        <el-button v-if="trip && !isAdminScope" @click="renameTrip">修改标题</el-button>
        <el-button v-if="trip && !isAdminScope" type="danger" plain @click="deleteTrip">删除行程</el-button>
      </div>
    </div>

    <el-alert v-if="error" class="section" :title="error" type="warning" show-icon :closable="false" />
    <TripPlanReport v-if="trip?.planData" :plan="trip.planData" />
    <el-empty v-else-if="!loading" class="section" description="未找到该行程或行程数据为空。" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api/client'
import TripPlanReport from '../components/TripPlanReport.vue'
import { useAuth } from '../composables/useAuth'

const route = useRoute()
const router = useRouter()
const { isAdmin } = useAuth()
const loading = ref(false)
const error = ref('')
const trip = ref(null)
const isAdminScope = computed(() => route.query.scope === 'admin' && isAdmin.value)

async function loadTrip() {
  loading.value = true
  error.value = ''
  try {
    const url = isAdminScope.value ? `/admin/trips/${route.params.id}` : `/trips/${route.params.id}`
    const { data } = await api.get(url)
    trip.value = data
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '行程详情加载失败。'
  } finally {
    loading.value = false
  }
}

async function renameTrip() {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的行程标题', '修改标题', {
      inputValue: trip.value.title,
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValidator: value => Boolean(value && value.trim()) || '标题不能为空'
    })
    const { data } = await api.put(`/trips/${trip.value.id}`, { title: value.trim() })
    trip.value = data
    ElMessage.success('标题已更新。')
  } catch (e) {
    if (e !== 'cancel') error.value = e?.response?.data?.message || e?.message || '标题修改失败。'
  }
}

async function deleteTrip() {
  try {
    await ElMessageBox.confirm(`确定删除“${trip.value.title}”吗？`, '删除行程', { type: 'warning' })
    await api.delete(`/trips/${trip.value.id}`)
    ElMessage.success('行程已删除。')
    router.replace('/my-trips')
  } catch (e) {
    if (e !== 'cancel') error.value = e?.response?.data?.message || e?.message || '删除失败。'
  }
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(loadTrip)
</script>
