<template>
  <div class="page">
    <div class="page-heading">
      <h1>我的行程</h1>
      <p class="meta">查看和管理你保存的智慧旅游计划。</p>
    </div>

    <el-card class="filter-card">
      <div class="toolbar trip-filter-row">
        <el-input v-model="filters.keyword" placeholder="关键词" clearable />
        <el-input v-model="filters.fromCity" placeholder="出发城市" clearable />
        <el-input v-model="filters.toCity" placeholder="目的城市" clearable />
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" :loading="loading" @click="loadTrips">搜索</el-button>
      </div>
    </el-card>

    <el-alert v-if="error" class="section" :title="error" type="warning" show-icon :closable="false" />

    <div v-if="trips.length" class="trip-card-grid section">
      <el-card v-for="trip in trips" :key="trip.id" class="trip-card">
        <div class="trip-card-head">
          <div>
            <h3>{{ trip.title }}</h3>
            <p class="meta">{{ trip.fromCity || '-' }} → {{ trip.toCity || '-' }}</p>
          </div>
          <el-tag :type="feasibilityType(trip.feasibilityLevel)">{{ trip.feasibilityLevel || '未评估' }}</el-tag>
        </div>
        <p class="trip-summary">{{ trip.summary || '暂无摘要。' }}</p>
        <div class="trip-meta-grid">
          <div><span>出发日期</span><strong>{{ trip.startDate || '-' }}</strong></div>
          <div><span>天数</span><strong>{{ trip.days || '-' }}</strong></div>
          <div><span>人数</span><strong>{{ trip.peopleCount || '-' }}</strong></div>
          <div><span>预算</span><strong>{{ money(trip.budget) }}</strong></div>
          <div><span>推荐天数</span><strong>{{ trip.recommendedDays || '-' }}</strong></div>
          <div><span>保存时间</span><strong>{{ formatTime(trip.createdAt) }}</strong></div>
        </div>
        <div class="trip-actions">
          <el-button type="primary" @click="router.push(`/my-trips/${trip.id}`)">查看详情</el-button>
          <el-button @click="renameTrip(trip)">修改标题</el-button>
          <el-button type="danger" plain @click="deleteTrip(trip)">删除</el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-else-if="!loading" class="section" description="你还没有保存行程，先去智能规划生成一份旅行计划吧。">
      <el-button type="primary" @click="router.push('/planner')">开始智能规划</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api/client'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const trips = ref([])
const filters = reactive({ keyword: '', fromCity: '', toCity: '' })

async function loadTrips() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get('/trips', { params: { ...filters } })
    trips.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '行程列表加载失败。'
  } finally {
    loading.value = false
  }
}

function reset() {
  filters.keyword = ''
  filters.fromCity = ''
  filters.toCity = ''
  loadTrips()
}

async function renameTrip(trip) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的行程标题', '修改标题', {
      inputValue: trip.title,
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValidator: value => Boolean(value && value.trim()) || '标题不能为空'
    })
    await api.put(`/trips/${trip.id}`, { title: value.trim() })
    ElMessage.success('标题已更新。')
    loadTrips()
  } catch (e) {
    if (e !== 'cancel') error.value = e?.response?.data?.message || e?.message || '标题修改失败。'
  }
}

async function deleteTrip(trip) {
  try {
    await ElMessageBox.confirm(`确定删除“${trip.title}”吗？`, '删除行程', { type: 'warning' })
    await api.delete(`/trips/${trip.id}`)
    ElMessage.success('行程已删除。')
    loadTrips()
  } catch (e) {
    if (e !== 'cancel') error.value = e?.response?.data?.message || e?.message || '删除失败。'
  }
}

function feasibilityType(level) {
  if (level === '合理') return 'success'
  if (level === '偏紧') return 'warning'
  if (level === '不建议') return 'danger'
  return 'info'
}

function money(value) {
  return value == null ? '-' : `CNY ${Number(value).toFixed(0)}`
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(loadTrips)
</script>
