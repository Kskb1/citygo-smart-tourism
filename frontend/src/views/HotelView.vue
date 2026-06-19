<template>
  <div class="page travel-page">
    <section class="travel-hero hotel-hero">
      <div>
        <span class="travel-kicker">高德 POI · 真实路线 · 非实时预算</span>
        <h1>酒店位置与路线推荐</h1>
        <p>
          查询真实酒店 POI、地址、电话和到景点的地面路线；房价只展示规则预算参考，不把预估值伪装成酒店实时价格。
        </p>
      </div>
      <div class="travel-hero-panel">
        <strong>酒店页展示原则</strong>
        <span>酒店卡片只展示高德返回的真实地点信息。</span>
        <span>停车场、餐厅、入口等配套地点会自动过滤。</span>
        <span>预算为 RULE_ESTIMATED，请以正规平台实时报价为准。</span>
      </div>
    </section>

    <el-card class="travel-filter-card section">
      <template #header>
        <div class="card-header-row">
          <strong>酒店查询</strong>
          <el-tag type="success">真实地点优先</el-tag>
        </div>
      </template>

      <el-form class="hotel-query-grid" label-position="top" @keyup.enter="search">
        <el-form-item label="城市">
          <el-input v-model="form.city" placeholder="例如：武汉" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keyword" placeholder="酒店 / 住宿服务 / 民宿" />
        </el-form-item>
        <el-form-item label="参考地点">
          <el-input v-model="form.reference" placeholder="例如：黄鹤楼" />
        </el-form-item>
        <el-form-item label="区县/商圈">
          <el-input v-model="form.district" placeholder="例如：武昌区" />
        </el-form-item>
        <el-form-item label="住宿档次">
          <el-select v-model="form.grade">
            <el-option label="经济型" value="economy" />
            <el-option label="舒适型" value="comfort" />
            <el-option label="高档型" value="upscale" />
            <el-option label="豪华型" value="luxury" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索半径">
          <el-select v-model="form.radius">
            <el-option label="1 公里" :value="1000" />
            <el-option label="3 公里" :value="3000" />
            <el-option label="5 公里" :value="5000" />
            <el-option label="全城" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="入住日期">
          <el-date-picker v-model="form.checkIn" value-format="YYYY-MM-DD" type="date" />
        </el-form-item>
        <el-form-item label="离店日期">
          <el-date-picker v-model="form.checkOut" value-format="YYYY-MM-DD" type="date" />
        </el-form-item>
        <el-form-item label="房间数">
          <el-input-number v-model="form.rooms" :min="1" :max="8" />
        </el-form-item>
        <el-form-item label="入住人数">
          <el-input-number v-model="form.people" :min="1" :max="16" />
        </el-form-item>
      </el-form>

      <div class="travel-actions">
        <el-button type="primary" :loading="loading" @click="search">搜索酒店位置</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <div class="quick-chip-row">
        <span>快捷城市</span>
        <el-button v-for="city in hotCities" :key="city" size="small" @click="quickCity(city)">{{ city }}</el-button>
      </div>
    </el-card>

    <el-alert
      class="section"
      title="本页不展示伪造房价、房态、评分或库存。预算卡为规则预估，酒店卡片信息来自高德 POI。"
      type="info"
      show-icon
      :closable="false"
    />
    <el-alert v-if="error" class="section" :title="error" type="warning" show-icon :closable="false" />

    <section class="hotel-layout section">
      <aside class="hotel-side-panel">
        <el-card class="budget-card hotel-budget-card">
          <template #header>住宿预算参考</template>
          <div class="budget-card-head">
            <span>住</span>
            <div>
              <strong>{{ gradeLabel }}</strong>
              <small>{{ hotelBudget.priceMode }}</small>
            </div>
          </div>
          <p class="budget-price">{{ hotelBudget.rangeText }}</p>
          <p class="meta">{{ nights }} 晚 · {{ form.rooms }} 间房 · {{ form.people }} 人</p>
          <p class="budget-total">{{ hotelBudget.totalText }}</p>
          <p class="meta">{{ hotelBudget.note }}</p>
        </el-card>

        <el-card class="hotel-stat-card">
          <template #header>当前结果</template>
          <div class="hotel-stat-row">
            <span>真实酒店 POI</span>
            <strong>{{ hotels.length }}</strong>
          </div>
          <div class="hotel-stat-row">
            <span>过滤配套地点</span>
            <strong>{{ Math.max(0, rawHotels.length - hotels.length) }}</strong>
          </div>
          <div class="hotel-stat-row">
            <span>参考地点</span>
            <strong>{{ form.reference || '未填写' }}</strong>
          </div>
        </el-card>
      </aside>

      <main>
        <div v-if="loading" class="travel-loading-grid">
          <el-skeleton v-for="item in 6" :key="item" animated />
        </div>

        <div v-else-if="hotels.length" class="hotel-result-grid">
          <el-card v-for="hotel in hotels" :key="hotel.id || hotel.name" class="hotel-poi-card">
            <div class="trip-card-head">
              <div>
                <h3>{{ hotel.name }}</h3>
                <p class="meta">{{ hotel.district || hotel.cityName || '-' }} · {{ hotel.typeName || '住宿服务' }}</p>
              </div>
              <el-tag type="success">{{ hotel.displayMode }}</el-tag>
            </div>

            <p class="hotel-address">{{ hotel.address || '高德未返回详细地址。' }}</p>
            <p v-if="hotel.telephone" class="meta">电话：{{ hotel.telephone }}</p>
            <p v-if="hotel.distanceText" class="meta">{{ hotel.distanceMode || '距离' }}：{{ hotel.distanceText }}</p>

            <div class="source-tags">
              <el-tag type="success">{{ hotel.sourceName || '高德开放平台' }}</el-tag>
              <el-tag type="info">高德 POI</el-tag>
              <el-tag type="warning">无实时房价</el-tag>
            </div>

            <p class="meta">{{ hotel.notice || '真实房价、房态和取消规则请以酒店或正规预订平台为准。' }}</p>

            <div class="trip-actions">
              <el-button @click="openMap(hotel)">查看地图</el-button>
              <el-button type="primary" :disabled="!canRoute(hotel)" @click="planRoute(hotel)">规划路线</el-button>
              <el-button plain @click="bookingNotice">查询实时房价</el-button>
            </div>

            <el-alert
              v-if="selectedRouteHotel === routeKey(hotel) && routeResult"
              class="table-note"
              type="success"
              show-icon
              :closable="false"
              :title="`路线距离 ${routeResult.distanceText || '-'}，预计 ${routeResult.durationText || '-'}`"
            />
          </el-card>
        </div>

        <el-empty
          v-else
          description="没有查询到符合条件的真实酒店 POI。可调整城市、关键词、区县或参考地点后重试。"
        />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'
import { buildHotelBudget, dedupeHotels, getPoiList, nightsBetween, normalizeText } from '../utils/travelDisplay'

const hotCities = ['武汉', '成都', '南京', '北京', '上海', '杭州', '广州', '西安']
const today = new Date()
const tomorrow = new Date()
tomorrow.setDate(today.getDate() + 1)

const form = reactive({
  city: '武汉',
  keyword: '酒店',
  reference: '黄鹤楼',
  district: '',
  grade: 'comfort',
  radius: 3000,
  checkIn: today.toISOString().slice(0, 10),
  checkOut: tomorrow.toISOString().slice(0, 10),
  rooms: 1,
  people: 2
})

const rawHotels = ref([])
const loading = ref(false)
const error = ref('')
const routeResult = ref(null)
const selectedRouteHotel = ref('')

const nights = computed(() => nightsBetween(form.checkIn, form.checkOut))
const hotels = computed(() => {
  const deduped = dedupeHotels(rawHotels.value)
  const district = normalizeText(form.district)
  if (!district) return deduped
  return deduped.filter(item => normalizeText(`${item.district || ''}${item.address || ''}`).includes(district))
})
const hotelBudget = computed(() => buildHotelBudget({
  city: form.city,
  grade: form.grade,
  nights: nights.value,
  rooms: form.rooms
}))
const gradeLabel = computed(() => ({
  economy: '经济型住宿',
  comfort: '舒适型住宿',
  upscale: '高档型住宿',
  luxury: '豪华型住宿'
})[form.grade] || '舒适型住宿')

onMounted(() => {
  search()
})

async function search() {
  if (!form.city.trim()) {
    error.value = '请输入城市名称。'
    return
  }

  loading.value = true
  error.value = ''
  routeResult.value = null
  selectedRouteHotel.value = ''

  try {
    const { data } = await api.get('/hotels/search', {
      params: {
        city: form.city.trim(),
        keyword: form.keyword.trim() || '酒店',
        reference: form.reference.trim() || undefined,
        radius: form.radius || undefined,
        page: 1,
        pageSize: 25
      }
    })

    rawHotels.value = getPoiList(data)
    if (!data.realData) {
      error.value = friendlyError(data.message)
      return
    }
    if (!hotels.value.length) error.value = '没有查询到符合条件的真实酒店 POI，请调整关键词、城市或参考地点。'
  } catch (e) {
    error.value = friendlyError(e?.response?.data?.message || e?.message)
    rawHotels.value = []
  } finally {
    loading.value = false
  }
}

function reset() {
  form.city = '武汉'
  form.keyword = '酒店'
  form.reference = '黄鹤楼'
  form.district = ''
  form.grade = 'comfort'
  form.radius = 3000
  form.rooms = 1
  form.people = 2
  rawHotels.value = []
  error.value = ''
  routeResult.value = null
  selectedRouteHotel.value = ''
  search()
}

function quickCity(city) {
  form.city = city
  form.reference = city === '武汉' ? '黄鹤楼' : ''
  search()
}

function routeKey(hotel) {
  return hotel.id || `${hotel.name}-${hotel.locationText || hotel.address || ''}`
}

function canRoute(hotel) {
  return Boolean(form.reference.trim() && hotel.locationText)
}

async function planRoute(hotel) {
  if (!canRoute(hotel)) {
    ElMessage.info('请先填写参考地点，并确保酒店 POI 含有经纬度。')
    return
  }

  try {
    const { data } = await api.get('/routes', {
      params: {
        origin: form.reference.trim(),
        destination: hotel.locationText,
        mode: 'driving',
        city: form.city
      }
    })
    if (!data.realData) {
      ElMessage.warning(data.message || '路线数据暂时无法获取。')
      return
    }
    routeResult.value = data.data
    selectedRouteHotel.value = routeKey(hotel)
  } catch (e) {
    ElMessage.warning(e?.response?.data?.message || '路线数据暂时无法获取，请稍后重试。')
  }
}

function openMap(hotel) {
  if (!hotel.locationText) {
    ElMessage.info('高德未返回该酒店经纬度。')
    return
  }
  window.open(`https://uri.amap.com/marker?position=${hotel.locationText}&name=${encodeURIComponent(hotel.name)}`, '_blank')
}

function bookingNotice() {
  ElMessage.info('请前往酒店官网或正规预订平台查询实时房价、房态和取消规则。')
}

function friendlyError(message = '') {
  if (message.includes('未配置') || message.includes('AMAP_API_KEY')) return '高德接口未配置，暂时无法获取真实酒店地点数据。'
  if (message.includes('401')) return '登录状态已失效，请重新登录后再查询。'
  return '酒店地点数据暂时获取失败，请稍后重试。'
}
</script>
