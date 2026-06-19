<template>
  <div class="page home-page">
    <section class="home-hero hero-section">
      <div class="hero-content">
        <span class="hero-kicker">真实数据优先 · 不伪造价格</span>
        <h1>智游 CityGo</h1>
        <p class="hero-lead">基于真实地图、天气与路线数据的智慧旅游规划平台。</p>
        <p class="hero-copy">帮助你判断行程是否玩得动、预算是否偏紧、跨城市景点是否应该拆成单独旅行。</p>
        <div class="hero-actions">
          <el-button class="hero-primary" size="large" @click="$router.push('/planner')">开始智能规划</el-button>
          <el-button class="hero-secondary" size="large" @click="$router.push('/spots')">探索目的地</el-button>
        </div>
      </div>

      <div class="hero-report-card">
        <div class="report-title">
          <span class="report-mark">AI</span>
          <strong>AI 行程报告</strong>
        </div>
        <div class="report-items">
          <div v-for="item in reportItems" :key="item.title" class="report-item">
            <span>{{ item.index }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.text }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="section-title">
        <div>
          <h2>数据源状态</h2>
          <p class="meta">未配置接口时只显示明确提示，不使用编造内容替代。</p>
        </div>
      </div>
      <el-skeleton v-if="loading" :rows="4" animated />
      <div v-else class="status-grid">
        <el-card
          v-for="item in statusItems"
          :key="item.name"
          class="status-card"
          :class="{ clickable: item.route }"
          @click="goRoute(item.route)"
        >
          <div class="status-icon">{{ item.icon }}</div>
          <div class="status-main">
            <span>{{ item.name }}</span>
            <small>{{ item.description }}</small>
          </div>
          <el-tag :class="item.className" effect="plain">{{ item.text }}</el-tag>
        </el-card>
      </div>
    </section>

    <section class="section feature-grid">
      <el-card v-for="item in features" :key="item.title" class="feature-card">
        <div class="feature-icon">{{ item.index }}</div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.text }}</p>
      </el-card>
    </section>

    <section class="section flow-band">
      <div v-for="(step, index) in flow" :key="step" class="flow-step">
        <span>{{ index + 1 }}</span>
        <strong>{{ step }}</strong>
      </div>
    </section>

    <el-alert v-if="error" class="section" :title="error" type="warning" show-icon :closable="false" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { providerStatus } from '../api/client'

const status = ref({})
const loading = ref(true)
const error = ref('')

const reportItems = [
  { index: '01', title: '合理性判断', text: '根据天数、距离和景点数量判断行程是否可执行。' },
  { index: '02', title: '跨城市风险提示', text: '识别异地景点和额外交通时间。' },
  { index: '03', title: '真实数据支持', text: '接入高德 POI、天气和路线数据。' }
]

const features = [
  { index: 'P', title: '真实 POI 推荐', text: '优先调用高德真实 POI，空数据时给出提示，不造景点。' },
  { index: 'F', title: '行程合理性判断', text: '识别天数、预算、跨城景点和交通报价缺失带来的风险。' },
  { index: 'C', title: '跨城市风险提示', text: '外地景点不会被硬塞进主目的地当天行程。' },
  { index: 'B', title: '预算规则估算', text: '餐饮、市内交通、住宿建议明确标注为规则估算。' },
  { index: 'W', title: '城市天气与出行提醒', text: '查询真实城市天气，并根据温度、降雨和风力给出旅游建议。' }
]

const flow = ['选择目的地', '加入景点清单', '生成行程报告', '查看预算和建议']

const statusItems = computed(() => [
  statusItem('MAP', '高德地图', status.value.amapMapConfigured, '路线与地理编码'),
  statusItem('WX', '高德天气', status.value.amapWeatherConfigured, '目的地天气提醒', '/weather'),
  statusItem('POI', '高德 POI', status.value.amapPoiConfigured, '真实景点与地点'),
  statusItem('ROUTE', '高德路线', status.value.amapRouteConfigured, '地面路线规划'),
  statusItem('HOT', '酒店信息', status.value.hotelPoiConfigured, '高德 POI，无实时房价', '/hotels'),
  statusItem('AIR', '机场信息', status.value.airportPoiConfigured, '高德 POI，无实时航班', '/transport'),
  {
    icon: 'RAIL',
    name: '高铁查询',
    description: '12306 官方跳转',
    ok: status.value.trainThirdPartyConfigured,
    text: status.value.trainThirdPartyConfigured ? '第三方接口' : '12306 官方跳转',
    className: status.value.trainThirdPartyConfigured ? 'tag-success' : 'tag-info'
  },
  {
    icon: 'PRICE',
    name: '实时房价',
    description: '当前采用位置推荐和官方查询模式',
    ok: false,
    text: '未接入',
    className: 'tag-info'
  },
  {
    icon: 'FLT',
    name: '实时航班',
    description: '以航空公司或正规平台为准',
    ok: false,
    text: '未接入',
    className: 'tag-info'
  }
])

function statusItem(icon, name, ok, description, route = '') {
  return {
    icon,
    name,
    description,
    route,
    ok,
    text: ok ? '已配置' : '未配置',
    className: ok ? 'tag-success' : 'tag-warning'
  }
}

function goRoute(route) {
  if (route) {
    window.location.href = route
  }
}

onMounted(async () => {
  try {
    status.value = await providerStatus()
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '数据源状态加载失败，请检查后端服务是否已启动。'
  } finally {
    loading.value = false
  }
})
</script>
