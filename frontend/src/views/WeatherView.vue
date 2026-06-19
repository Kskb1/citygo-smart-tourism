<template>
  <div class="page weather-page">
    <div class="page-heading weather-heading">
      <div>
        <h1>天气查询</h1>
        <p class="meta">查询城市真实天气、未来天气趋势和旅游出行建议。</p>
      </div>
    </div>

    <el-card class="weather-search-card">
      <div class="weather-search-main">
        <el-input
          v-model="city"
          size="large"
          placeholder="请输入城市名称，例如成都、武汉、南京"
          clearable
          @keyup.enter="queryWeather"
        />
        <el-button size="large" type="primary" :loading="loading" @click="queryWeather">查询天气</el-button>
      </div>
      <div class="quick-row weather-quick-row">
        <span class="quick-label">热门城市</span>
        <el-button v-for="name in hotCities" :key="name" text @click="quickQuery(name)">{{ name }}</el-button>
      </div>
    </el-card>

    <el-alert v-if="error" class="section" :title="error" type="warning" show-icon :closable="false" />

    <template v-if="weatherResult">
      <section class="weather-current-grid section">
        <el-card class="weather-current-card">
          <div class="weather-current-head">
            <div>
              <el-tag class="tag-success" effect="plain">真实接口返回</el-tag>
              <h2>{{ weatherData.city || queryCity }}</h2>
              <p class="meta">{{ weatherData.province || '未知省份' }} · {{ weatherData.reportTime || '暂无发布时间' }}</p>
            </div>
            <div class="weather-icon">{{ weatherIcon(weatherData.weather) }}</div>
          </div>
          <div class="temperature-line">
            <strong>{{ valueOrDash(weatherData.temperature) }}</strong>
            <span>℃</span>
          </div>
          <p class="weather-text">{{ weatherData.weather || '暂无天气描述' }}</p>
          <div class="weather-metrics">
            <div><span>湿度</span><strong>{{ valueOrDash(weatherData.humidity) }}%</strong></div>
            <div><span>风向</span><strong>{{ valueOrDash(weatherData.windDirection) }}</strong></div>
            <div><span>风力</span><strong>{{ valueOrDash(weatherData.windPower) }}</strong></div>
          </div>
        </el-card>

        <el-card class="travel-advice-card">
          <template #header>旅游适宜度</template>
          <el-tag :type="suitabilityTagType" size="large">{{ suitability.level }}</el-tag>
          <p>{{ suitability.reason }}</p>
          <p class="meta">系统规则判断，不属于官方气象预警。</p>
          <div class="weather-actions">
            <el-button type="primary" @click="goPlanner">按当前城市开始规划</el-button>
            <el-button @click="$router.push('/spots')">查看该城市景点</el-button>
          </div>
        </el-card>
      </section>

      <el-card class="section">
        <template #header>未来天气预报</template>
        <div v-if="forecastDays.length" class="forecast-grid">
          <div v-for="item in forecastDays" :key="item.date" class="forecast-card">
            <div class="forecast-top">
              <strong>{{ item.date }}</strong>
              <span>{{ weekText(item.week) }}</span>
            </div>
            <div class="forecast-icon">{{ weatherIcon(item.dayWeather || item.dayweather) }}</div>
            <p>{{ item.dayWeather || item.dayweather || '暂无白天天气' }} / {{ item.nightWeather || item.nightweather || '暂无夜间天气' }}</p>
            <div class="forecast-temp">
              <span>{{ item.dayTemp || item.daytemp || '-' }}℃</span>
              <small>{{ item.nightTemp || item.nighttemp || '-' }}℃</small>
            </div>
            <p class="meta">{{ item.dayWindDirection || item.daywind || '-' }}风 · {{ item.dayWindPower || item.daypower || '-' }}</p>
          </div>
        </div>
        <el-empty v-else description="当前接口未返回未来天气预报。" />
      </el-card>

      <el-card class="section">
        <template #header>系统规则出行建议</template>
        <div class="advice-grid">
          <div v-for="tip in travelTips" :key="tip" class="advice-item">
            <span>TIP</span>
            <p>{{ tip }}</p>
          </div>
        </div>
      </el-card>

      <el-card class="section">
        <template #header>数据来源说明</template>
        <div class="source-tags">
          <el-tag class="tag-success" effect="plain">{{ weatherResult.sourceName || 'Amap Open Platform' }}</el-tag>
          <el-tag effect="plain">获取时间：{{ formatTime(weatherResult.fetchedAt) }}</el-tag>
          <el-tag :class="weatherResult.realData ? 'tag-success' : 'tag-warning'" effect="plain">
            {{ weatherResult.realData ? '真实接口返回' : '未获取真实数据' }}
          </el-tag>
        </div>
      </el-card>

      <el-collapse class="section">
        <el-collapse-item title="原始接口数据（调试用）" name="raw">
          <pre>{{ JSON.stringify(weatherResult.rawJson || {}, null, 2) }}</pre>
        </el-collapse-item>
      </el-collapse>
    </template>

    <el-empty v-else-if="!loading" class="section" description="输入城市名称后查询真实天气。" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'

const router = useRouter()
const city = ref('成都')
const queryCity = ref('')
const loading = ref(false)
const error = ref('')
const weatherResult = ref(null)
const hotCities = ['成都', '武汉', '北京', '上海', '南京', '杭州', '广州', '西安']

const weatherData = computed(() => weatherResult.value?.data || {})
const forecastDays = computed(() => {
  const forecasts = weatherResult.value?.rawJson?.forecasts
  if (!Array.isArray(forecasts) || !forecasts.length) return []
  const casts = forecasts[0]?.casts
  return Array.isArray(casts) ? casts : []
})

const travelTips = computed(() => buildTravelTips(weatherData.value))
const suitability = computed(() => evaluateSuitability(weatherData.value))
const suitabilityTagType = computed(() => {
  if (suitability.value.level === '适宜') return 'success'
  if (suitability.value.level === '一般') return 'warning'
  return 'danger'
})

async function queryWeather() {
  const name = city.value.trim()
  if (!name) {
    ElMessage.warning('请输入城市名称')
    return
  }
  if (loading.value) return
  loading.value = true
  error.value = ''
  weatherResult.value = null
  queryCity.value = name
  try {
    const { data } = await api.get('/weather', { params: { city: name } })
    if (!data.realData) {
      error.value = friendlyMessage(data.message || data.errorMessage)
      return
    }
    if (!data.data) {
      error.value = '未查询到该城市的天气数据，请检查城市名称。'
      return
    }
    weatherResult.value = data
  } catch (e) {
    error.value = friendlyMessage(e?.response?.data?.message || e?.message)
  } finally {
    loading.value = false
  }
}

function quickQuery(name) {
  city.value = name
  queryWeather()
}

function friendlyMessage(message = '') {
  if (message.includes('AMAP_API_KEY') || message.includes('未配置')) {
    return '高德天气接口未配置，暂时无法查询真实天气。'
  }
  if (message.includes('INVALID') || message.includes('不存在') || message.includes('not')) {
    return '未查询到该城市的天气数据，请检查城市名称。'
  }
  return '天气数据暂时获取失败，请稍后重试。'
}

function weatherIcon(text = '') {
  if (text.includes('晴')) return 'SUN'
  if (text.includes('云')) return 'CLD'
  if (text.includes('阴')) return 'OVC'
  if (text.includes('雨')) return 'RAIN'
  if (text.includes('雪')) return 'SNOW'
  if (text.includes('雾')) return 'FOG'
  return 'WX'
}

function buildTravelTips(data) {
  const tips = []
  const weather = data.weather || ''
  const temperature = Number(data.temperature)
  const humidity = Number(data.humidity)
  const windPower = data.windPower || ''

  if (weather.includes('雨')) tips.push('建议携带雨具，优先安排博物馆、展览馆等室内景点，并注意道路湿滑。')
  if (weather.includes('雪')) tips.push('注意保暖和道路结冰，出行前关注交通运行情况。')
  if (Number.isFinite(temperature) && temperature >= 35) tips.push('高温天气，避免中午长时间户外活动，注意补水与防晒。')
  if (Number.isFinite(temperature) && temperature <= 5) tips.push('气温较低，注意保暖，建议减少长时间户外停留。')
  if (weather.includes('晴')) tips.push('适合公园、自然景区和城市步行游览，但仍需注意防晒。')
  if (Number.isFinite(humidity) && humidity >= 75) tips.push('湿度较高，体感可能较闷热，建议合理安排休息时间。')
  if (/[4-9]|≥|大/.test(windPower)) tips.push('风力较大时不建议高空、索道或水上活动，具体以景区官方公告为准。')
  if (!tips.length) tips.push(data.travelTip || '天气条件暂无明显风险，可结合个人体力和景区开放情况安排行程。')
  return tips
}

function evaluateSuitability(data) {
  const weather = data.weather || ''
  const temperature = Number(data.temperature)
  const windPower = data.windPower || ''
  if (weather.includes('暴') || weather.includes('雪') || (Number.isFinite(temperature) && (temperature >= 35 || temperature <= 5))) {
    return { level: '谨慎安排', reason: '天气存在较明显影响，建议减少户外停留并优先确认交通和景区开放情况。' }
  }
  if (weather.includes('雨') || /[4-9]|≥|大/.test(windPower)) {
    return { level: '一般', reason: '天气对户外游览有一定影响，建议预留弹性时间并准备替代室内行程。' }
  }
  return { level: '适宜', reason: '天气条件整体适合城市游览，可结合景点距离和体力安排路线。' }
}

function weekText(value) {
  const map = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }
  return map[value] || `周${value || '-'}`
}

function valueOrDash(value) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function goPlanner() {
  const toCity = weatherData.value.city || queryCity.value || city.value
  router.push({ path: '/planner', query: { toCity } })
}
</script>
