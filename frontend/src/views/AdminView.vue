<template>
  <div class="page admin-page">
    <div class="page-heading">
      <h1>后台管理</h1>
      <p class="meta">仅 ADMIN 可访问。查看运行期用户、数据源状态、API 日志和已保存行程。</p>
    </div>
    <el-button @click="load">刷新</el-button>
    <el-alert v-if="error" class="section" :title="error" type="warning" show-icon :closable="false" />

    <div class="admin-stat-grid section">
      <el-card v-for="item in stats" :key="item.label" class="admin-stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </el-card>
    </div>

    <div class="result-grid section">
      <el-card>
        <template #header>API 配置状态</template>
        <div class="source-tags">
          <el-tag v-for="item in statusItems" :key="item.name" :type="item.ok ? 'success' : 'warning'">
            {{ item.name }}：{{ item.text }}
          </el-tag>
        </div>
      </el-card>
      <el-card>
        <template #header>最近注册用户</template>
        <el-table :data="dashboard.recentUsers || []" height="240">
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="role" label="角色" width="120" />
          <el-table-column prop="status" label="状态" width="120" />
        </el-table>
      </el-card>
    </div>

    <el-card class="section price-rule-card">
      <template #header>
        <div class="card-header-row">
          <span>预算预估规则</span>
          <div class="source-tags">
            <el-tag type="warning">{{ estimateRules.priceMode || 'RULE_ESTIMATED' }}</el-tag>
            <el-tag type="info">规则版本 {{ estimateRules.ruleVersion || '-' }}</el-tag>
          </div>
        </div>
      </template>
      <el-alert
        class="table-note"
        :title="estimateRules.notice || '预算参考价仅用于规划阶段粗略判断，不代表实时市场价格。'"
        type="warning"
        show-icon
        :closable="false"
      />
      <div class="price-rule-summary">
        <div v-for="item in priceRuleSummary" :key="item.title" class="price-rule-item">
          <strong>{{ item.title }}</strong>
          <span>{{ item.text }}</span>
        </div>
      </div>
      <el-collapse class="rule-json-collapse">
        <el-collapse-item title="查看完整规则配置" name="rules">
          <pre class="rule-json">{{ formatRules(estimateRules) }}</pre>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <el-card class="section">
      <template #header>最近保存行程</template>
      <el-table :data="dashboard.recentTrips || []" height="300">
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="title" label="行程标题" min-width="220" />
        <el-table-column prop="fromCity" label="出发地" width="120" />
        <el-table-column prop="toCity" label="目的地" width="120" />
        <el-table-column prop="days" label="天数" width="80" />
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/my-trips/${row.id}?scope=admin`)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="section">
      <template #header>API 调用日志</template>
      <el-table :data="logs" height="420">
        <el-table-column prop="createdAt" label="时间" min-width="180" />
        <el-table-column prop="apiName" label="接口" min-width="160" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="errorMessage" label="错误" min-width="260" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/client'

const router = useRouter()
const status = ref({})
const dashboard = ref({})
const logs = ref([])
const estimateRules = ref({})
const error = ref('')

const stats = computed(() => [
  { label: '用户数量', value: dashboard.value.userCount ?? '-' },
  { label: '普通用户', value: dashboard.value.normalUserCount ?? '-' },
  { label: '管理员', value: dashboard.value.adminCount ?? '-' },
  { label: '总行程数', value: dashboard.value.tripCount ?? '-' },
  { label: '今日新增行程', value: dashboard.value.todayTripCount ?? '-' },
  { label: '今日 API 调用', value: dashboard.value.todayApiCalls ?? '-' },
  { label: 'API 成功率', value: dashboard.value.apiSuccessRate == null ? '-' : `${Number(dashboard.value.apiSuccessRate).toFixed(1)}%` }
])

const statusItems = computed(() => [
  { name: '高德地图', ok: status.value.amapMapConfigured, text: status.value.amapMapConfigured ? '已配置' : '未配置' },
  { name: '高德天气', ok: status.value.amapWeatherConfigured, text: status.value.amapWeatherConfigured ? '已配置' : '未配置' },
  { name: '高德 POI', ok: status.value.amapPoiConfigured, text: status.value.amapPoiConfigured ? '已配置' : '未配置' },
  { name: '路线规划', ok: status.value.amapRouteConfigured, text: status.value.amapRouteConfigured ? '已配置' : '未配置' },
  { name: '酒店位置推荐', ok: status.value.hotelPoiConfigured, text: status.value.hotelPoiConfigured ? '真实 POI，无实时房价' : '未配置' },
  { name: '机场位置推荐', ok: status.value.airportPoiConfigured, text: status.value.airportPoiConfigured ? '真实 POI，无实时航班' : '未配置' },
  { name: '高铁接口', ok: status.value.trainThirdPartyConfigured, text: status.value.trainThirdPartyConfigured ? '第三方接口' : '官方跳转' }
])

const priceRuleSummary = computed(() => [
  { title: '价格模式', text: 'RULE_ESTIMATED：非实时预算参考价，不是实时票价或房价。' },
  { title: '交通预算', text: '机票按距离区间估算；火车按路线距离和席别系数估算。' },
  { title: '住宿预算', text: '按目的地城市等级、住宿档次、晚数和房间数生成区间。' },
  { title: '餐饮与市内交通', text: '按人数、天数和用户选择档次生成参考区间。' },
  { title: '风险预留', text: `应急预留比例：${estimateRules.value.factors?.emergencyReserveRate ?? '-'}。` }
])

async function load() {
  error.value = ''
  try {
    const [s, d, l, r] = await Promise.all([
      api.get('/admin/provider-status'),
      api.get('/admin/dashboard'),
      api.get('/admin/api-logs'),
      api.get('/admin/price-estimate-rules')
    ])
    status.value = s.data || {}
    dashboard.value = d.data || {}
    logs.value = Array.isArray(l.data) ? l.data : []
    estimateRules.value = r.data || {}
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '后台数据加载失败，请确认已使用管理员账号登录。'
  }
}

function formatRules(value) {
  return JSON.stringify(value || {}, null, 2)
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(load)
</script>
