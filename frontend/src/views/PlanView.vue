<template>
  <div class="page">
    <div class="page-heading">
      <h1>智能规划</h1>
      <p class="meta">目的地由你手动填写；已选择景点只是候选清单，可来自多个城市。系统不会编造机票、高铁或酒店价格。</p>
    </div>

    <el-card class="filter-card">
      <el-form :model="form" label-width="90px" class="plan-form">
        <div class="toolbar">
          <el-form-item label="出发地" :error="cityErrors.origin">
            <el-input v-model="form.fromCity" placeholder="例如：成都" @blur="validateCityField('origin')" @input="clearCityValidation('origin')" />
            <p v-if="cityValidation.origin?.valid" class="city-validation-ok">已识别：{{ cityValidation.origin.normalizedCityName }}</p>
          </el-form-item>
          <el-form-item label="目的地" :error="cityErrors.destination">
            <el-input v-model="form.toCity" placeholder="请输入完整主旅行城市，例如：武汉" @blur="validateCityField('destination')" @input="clearCityValidation('destination')" />
            <p v-if="cityValidation.destination?.valid" class="city-validation-ok">已识别：{{ cityValidation.destination.normalizedCityName }}</p>
          </el-form-item>
          <el-form-item label="出发日期"><el-date-picker v-model="form.departureDate" value-format="YYYY-MM-DD" /></el-form-item>
          <el-form-item label="天数"><el-input-number v-model="form.days" :min="1" /></el-form-item>
          <el-form-item label="人数"><el-input-number v-model="form.people" :min="1" /></el-form-item>
          <el-form-item label="预算"><el-input-number v-model="form.budget" :min="0" /></el-form-item>
        </div>
        <el-form-item label="兴趣偏好">
          <el-checkbox-group v-model="form.interests">
            <el-checkbox label="景点" />
            <el-checkbox label="博物馆" />
            <el-checkbox label="公园" />
            <el-checkbox label="美食" />
          </el-checkbox-group>
        </el-form-item>
        <div class="toolbar">
          <el-form-item label="交通偏好">
            <el-select v-model="form.trafficPreference">
              <el-option label="飞机" value="flight" />
              <el-option label="高铁" value="train" />
              <el-option label="普通火车" value="normal-train" />
              <el-option label="自驾" value="self-driving" />
              <el-option label="暂不确定" value="unknown" />
            </el-select>
          </el-form-item>
          <el-form-item label="火车席别">
            <el-select v-model="form.trainSeatType">
              <el-option label="高铁二等座" value="high-speed-second" />
              <el-option label="高铁一等座" value="high-speed-first" />
              <el-option label="普速硬座" value="normal-hard-seat" />
              <el-option label="普速硬卧" value="normal-hard-sleeper" />
            </el-select>
          </el-form-item>
          <el-form-item label="住宿偏好">
            <el-select v-model="form.hotelPreference">
              <el-option label="经济型" value="economy" />
              <el-option label="舒适型" value="comfort" />
              <el-option label="高档型" value="upscale" />
              <el-option label="豪华型" value="luxury" />
            </el-select>
          </el-form-item>
          <el-form-item label="餐饮档次">
            <el-select v-model="form.foodPreference">
              <el-option label="经济型" value="economy" />
              <el-option label="普通型" value="standard" />
              <el-option label="品质型" value="quality" />
            </el-select>
          </el-form-item>
          <el-form-item label="市内交通">
            <el-select v-model="form.localTransportPreference">
              <el-option label="公共交通为主" value="public" />
              <el-option label="混合出行" value="mixed" />
              <el-option label="出租车为主" value="taxi" />
            </el-select>
          </el-form-item>
          <el-form-item label="是否往返">
            <el-switch v-model="form.roundTrip" active-text="是" inactive-text="否" />
          </el-form-item>
          <el-form-item label="房间数">
            <el-input-number v-model="form.roomCount" :min="1" />
          </el-form-item>
        </div>
        <el-button type="primary" :loading="loading || validatingCities" :disabled="loading || validatingCities" @click="generate">生成规划</el-button>
      </el-form>
    </el-card>

    <el-card class="section selected-plan-card">
      <template #header>
        <div class="card-header-row">
          <span>已选择景点清单</span>
          <el-button v-if="selectedSpots.length" size="small" text type="danger" @click="clearSpots">清空</el-button>
        </div>
      </template>
      <div v-if="selectedSpots.length" class="selected-spot-list inline">
        <div v-for="spot in selectedSpots" :key="spot.id" class="selected-spot-item">
          <div>
            <strong>{{ spot.name }}</strong>
            <el-tag class="spot-scope-tag" size="small" :type="isCrossCitySpot(spot) ? 'warning' : 'success'">
              {{ isCrossCitySpot(spot) ? '跨城市扩展，可能增加额外交通时间' : '主目的地景点' }}
            </el-tag>
            <p class="meta">{{ spot.provinceName || '未知省份' }} · {{ spot.cityName || '未知城市' }} · {{ spot.type || '未标注类型' }}</p>
            <p class="meta">{{ spot.address || '暂无地址' }}</p>
          </div>
          <el-button size="small" text type="danger" @click="removeSpot(spot.id)">删除</el-button>
        </div>
      </div>
      <el-empty v-else description="还没有从目的地探索页加入景点" />
    </el-card>

    <el-alert v-if="error" class="section" :title="error" type="warning" show-icon :closable="false" />

    <div v-if="result" class="plan-result section">
      <el-card class="section save-trip-card">
        <div class="save-trip-row">
          <div>
            <h3>保存这份智能规划</h3>
            <p class="meta">保存后可在“我的行程”中查看、改标题或删除。行程只绑定到当前登录账号。</p>
          </div>
          <div class="save-trip-actions">
            <el-tag v-if="savedTripId" type="success">已保存</el-tag>
            <el-button type="primary" :loading="savingTrip" :disabled="Boolean(savedTripId)" @click="saveTrip">
              {{ savedTripId ? '已保存' : '保存行程' }}
            </el-button>
            <el-button v-if="savedTripId" @click="router.push('/my-trips')">查看我的行程</el-button>
          </div>
        </div>
      </el-card>

      <el-card v-if="result.feasibility" class="section feasibility-card" :class="feasibilityClass">
        <template #header>行程合理性建议</template>
        <div class="feasibility-head">
          <el-tag :type="feasibilityTagType" size="large">{{ result.feasibility.level }}</el-tag>
          <strong>{{ result.feasibility.feasible ? '推荐执行' : '建议调整后再执行' }}</strong>
          <span>推荐天数：{{ result.feasibility.recommendedDays }} 天</span>
        </div>
        <el-alert
          v-if="!result.feasibility.feasible"
          class="table-note"
          title="以下计划仅供参考，建议根据提示调整天数或景点。"
          type="warning"
          show-icon
          :closable="false"
        />
        <div class="feasibility-grid">
          <div>
            <h3>问题提示</h3>
            <ul>
              <li v-for="warning in result.feasibility.warnings" :key="warning">{{ warning }}</li>
            </ul>
          </div>
          <div>
            <h3>优化建议</h3>
            <ul>
              <li v-for="suggestion in result.feasibility.suggestions" :key="suggestion">{{ suggestion }}</li>
            </ul>
          </div>
        </div>
      </el-card>

      <el-card class="section route-report-card">
        <template #header>主路线与城市段</template>
        <div class="main-route-line">
          <span>{{ result.fromCity }}</span>
          <b>→</b>
          <span>{{ result.toCity }}</span>
          <b>→</b>
          <span>{{ result.fromCity }}</span>
        </div>
        <el-alert
          v-if="crossCitySegments.length"
          class="table-note"
          title="你选择了不在主目的地的景点。系统不会强行将其安排进当前城市行程，建议增加天数或作为单独旅行计划。"
          type="warning"
          show-icon
          :closable="false"
        />
        <div v-if="result.citySegments?.length" class="city-segment-grid">
          <div v-for="segment in result.citySegments" :key="segment.segmentType + segment.cityName" class="city-segment-card">
            <el-tag :type="segment.segmentType === '跨城市扩展' ? 'warning' : 'success'">{{ segment.segmentType }}</el-tag>
            <h3>{{ segment.cityName }}</h3>
            <p>{{ segment.travelNotice }}</p>
            <p class="meta">建议停留：{{ segment.recommendedStayDays }} 天</p>
            <div v-if="segment.spots?.length" class="segment-spots">
              <el-tag v-for="spot in segment.spots" :key="spot.id || spot.name" size="small">{{ spot.name }}</el-tag>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="summary-card">
        <div class="summary-layout">
          <div>
            <h2>{{ result.title }}</h2>
            <p>{{ result.summary }}</p>
            <p class="budget-conclusion">{{ result.budgetConclusion }}</p>
          </div>
          <div class="summary-stats">
            <div><span>预算</span><strong>{{ result.budget || 0 }} 元</strong></div>
            <div><span>人数</span><strong>{{ result.peopleCount }} 人</strong></div>
            <div><span>推荐天数</span><strong>{{ result.recommendedDays }} 天</strong></div>
          </div>
        </div>
      </el-card>

      <div class="result-grid section">
        <el-card>
          <template #header>往返交通</template>
          <div class="transport-block">
            <h3>去程：{{ result.outboundTransport?.routeName }}</h3>
            <p>{{ result.outboundTransport?.message }}</p>
            <el-link v-if="result.outboundTransport?.officialTrainQueryUrl" :href="result.outboundTransport.officialTrainQueryUrl" target="_blank">前往 12306 官方查询</el-link>
          </div>
          <div class="transport-block">
            <h3>返程：{{ result.returnTransport?.routeName }}</h3>
            <p>{{ result.returnTransport?.message }}</p>
          </div>
        </el-card>

        <el-card>
          <template #header>天气提醒</template>
          <template v-if="result.weatherSummary">
            <p><strong>{{ result.weatherSummary.city }}</strong> {{ result.weatherSummary.weather }}，{{ result.weatherSummary.temperature }}℃</p>
            <p class="meta">湿度 {{ result.weatherSummary.humidity }}%，{{ result.weatherSummary.windDirection }}风 {{ result.weatherSummary.windPower }} 级</p>
            <el-alert :title="result.weatherSummary.travelTip" type="success" show-icon :closable="false" />
          </template>
          <el-alert v-else title="未获取到真实天气数据，请在出行前再次核验。" type="warning" show-icon :closable="false" />
        </el-card>
      </div>

      <el-card v-if="budgetReference" class="section budget-reference-card">
        <template #header>预算参考</template>
        <div class="budget-reference-head">
          <div>
            <h3>{{ budgetReference.budgetLevel }}</h3>
            <p class="meta">总预算参考区间：约 CNY {{ budgetReference.estimatedMin }} - {{ budgetReference.estimatedMax }}，典型参考约 CNY {{ budgetReference.estimatedTypical }}</p>
          </div>
          <div class="source-tags">
            <el-tag type="warning">预算参考价</el-tag>
            <el-tag type="info">非实时</el-tag>
            <el-tag type="info">{{ budgetReference.priceMode }}</el-tag>
          </div>
        </div>
        <div class="price-estimate-grid">
          <div v-for="item in budgetReference.items" :key="item.category" class="price-estimate-card">
            <div class="trip-card-head">
              <strong>{{ priceCategoryName(item.category) }}</strong>
              <el-tag size="small" type="warning">{{ item.confidence }} 可信度</el-tag>
            </div>
            <p class="price-range">约 CNY {{ item.minPrice }} - {{ item.maxPrice }}</p>
            <p class="meta">典型参考：CNY {{ item.typicalPrice }} · {{ item.unit }}</p>
            <ul>
              <li v-for="basis in item.basis" :key="basis">{{ basis }}</li>
            </ul>
            <p class="meta">{{ item.notice }}</p>
          </div>
        </div>
        <el-alert class="table-note" :title="budgetReference.notice" type="warning" show-icon :closable="false" />
        <ul v-if="budgetReference.suggestions?.length" class="budget-suggestions">
          <li v-for="suggestion in budgetReference.suggestions" :key="suggestion">{{ suggestion }}</li>
        </ul>
      </el-card>

      <el-card class="section">
        <template #header>预算明细</template>
        <el-table :data="budgetRows" border>
          <el-table-column prop="item" label="项目" min-width="130" />
          <el-table-column prop="amount" label="金额 / 状态" min-width="160" />
          <el-table-column prop="source" label="来源说明" min-width="240" />
        </el-table>
        <el-alert class="table-note" :title="result.budgetConclusion" type="info" show-icon :closable="false" />
      </el-card>

      <div class="split-section section">
        <el-card>
          <template #header>每日行程</template>
          <el-timeline>
            <el-timeline-item v-for="day in result.dailyPlans" :key="day.day" :timestamp="`${day.title} · ${day.date}`">
              <div v-for="activity in day.activities" :key="activity.period + activity.title" class="activity-item">
                <el-tag size="small">{{ activity.period }}</el-tag>
                <strong>{{ activity.title }}</strong>
                <p>{{ activity.description }}</p>
                <p class="meta">{{ activity.reason }} · {{ activity.costNote }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card>
          <template #header>路线示意</template>
          <RouteMap
            :origin-city="result.fromCity"
            :destination-city="result.toCity"
            :spots="result.recommendedSpots"
            :route-polyline="result.routeOverview?.polylinePoints || []"
          />
          <p v-if="result.routeOverview" class="meta route-meta">
            城际路线：{{ result.routeOverview.originName }} → {{ result.routeOverview.destinationName }}，
            {{ result.routeOverview.distanceText }}，预计 {{ result.routeOverview.durationText }}
          </p>
        </el-card>
      </div>

      <el-card class="section">
        <template #header>推荐景点</template>
        <div v-if="result.recommendedSpots?.length" class="spot-grid compact">
          <div v-for="spot in result.recommendedSpots.slice(0, 8)" :key="spot.id || spot.name" class="mini-spot">
            <div v-if="spot.photoUrl" class="mini-photo" :style="{ backgroundImage: `url(${spot.photoUrl})` }"></div>
            <div v-else class="mini-photo placeholder">暂无官方图片</div>
            <h3>{{ spot.name }}</h3>
            <p class="meta">{{ spot.cityName || result.toCity }} · {{ spot.type }}</p>
            <p>{{ spot.recommendationReason }}</p>
          </div>
        </div>
        <el-empty v-else description="未获取到可展示的真实景点数据" />
      </el-card>

      <el-card v-if="result.dataWarnings?.length" class="section">
        <template #header>数据提示</template>
        <el-alert v-for="warning in result.dataWarnings" :key="warning" :title="warning" type="warning" show-icon :closable="false" class="warning-item" />
      </el-card>

      <el-card v-if="result.dataSources?.length" class="section">
        <template #header>数据来源</template>
        <div class="source-tags">
          <el-tag
            v-for="source in result.dataSources"
            :key="source.name + source.status"
            :type="source.status === '真实接口返回' ? 'success' : 'warning'"
          >
            {{ source.name }}：{{ source.status }}
          </el-tag>
        </div>
      </el-card>

      <el-collapse class="section">
        <el-collapse-item title="原始接口数据（调试用）" name="debug">
          <pre>{{ JSON.stringify(result.debugRawData || result.data, null, 2) }}</pre>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, apiErrorMessage } from '../api/client'
import RouteMap from '../components/RouteMap.vue'
import { useTripSelection } from '../composables/useTripSelection'
import {
  SAVE_PAYLOAD_MAX_BYTES,
  buildTripSavePayload,
  measurePayloadBytes,
  payloadSizeLabel
} from '../utils/tripSavePayload'

const route = useRoute()
const router = useRouter()
const { selectedSpots, removeSpot, clearSpots } = useTripSelection()
const form = ref({
  fromCity: '成都',
  toCity: '武汉',
  departureDate: '2026-07-10',
  days: 3,
  people: 1,
  budget: 3000,
  interests: ['景点', '博物馆', '美食'],
  trafficPreference: 'train',
  hotelPreference: 'comfort',
  foodPreference: 'standard',
  localTransportPreference: 'mixed',
  roundTrip: true,
  roomCount: 1,
  trainSeatType: 'high-speed-second'
})
const result = ref(null)
const loading = ref(false)
const savingTrip = ref(false)
const savedTripId = ref(null)
const error = ref('')
const validatingCities = ref(false)
const cityErrors = ref({ origin: '', destination: '' })
const cityValidation = ref({ origin: null, destination: null })

function applyQuery() {
  const toCity = route.query.toCity
  const preference = route.query.preference
  if (typeof toCity === 'string' && toCity.trim()) {
    form.value.toCity = toCity.trim()
  }
  if (typeof preference === 'string' && preference.trim() && !form.value.interests.includes(preference.trim())) {
    form.value.interests = [preference.trim(), ...form.value.interests]
  }
}

const feasibilityClass = computed(() => {
  const level = result.value?.feasibility?.level
  if (level === '不建议') return 'danger'
  if (level === '偏紧') return 'warning'
  return 'success'
})

const feasibilityTagType = computed(() => {
  if (feasibilityClass.value === 'danger') return 'danger'
  if (feasibilityClass.value === 'warning') return 'warning'
  return 'success'
})

const budgetRows = computed(() => {
  const budget = result.value?.budgetSummary
  if (!budget) return []
  return [
    { item: '往返交通', amount: budget.transportRealCost ? `CNY ${budget.transportRealCost}` : '未接入实时票价', source: budget.transportStatus || '请自行查询官方平台' },
    { item: '住宿', amount: budget.hotelEstimate ? `约 CNY ${budget.hotelEstimate}` : '未估算', source: '用户预算规则估算，不是实时房价' },
    { item: '餐饮', amount: `约 CNY ${budget.foodEstimate}`, source: '规则估算：每人每天 120 元' },
    { item: '市内交通', amount: `约 CNY ${budget.localTrafficEstimate}`, source: '规则估算：每人每天 30 元' },
    { item: '门票', amount: budget.ticketEstimate, source: '以景区官方公告为准' },
    { item: '预算余额', amount: `约 CNY ${budget.remainingBudget}`, source: '规则估算后的参考值' }
  ]
})

const budgetReference = computed(() => result.value?.budgetSummary?.budgetReference || null)

const crossCitySegments = computed(() => (result.value?.citySegments || []).filter(item => item.segmentType === '跨城市扩展'))

function priceCategoryName(category) {
  const names = {
    FLIGHT: '机票预算参考',
    TRAIN: '火车票预算参考',
    HOTEL: '住宿预算参考',
    FOOD: '餐饮预算参考',
    LOCAL_TRANSPORT: '市内交通预算参考',
    TICKET_RESERVE: '景点门票预留',
    EMERGENCY_RESERVE: '应急预留'
  }
  return names[category] || category
}

function normalizeCityName(value) {
  return String(value || '').trim().replace(/市|地区|自治州|特别行政区/g, '')
}

function isCrossCitySpot(spot) {
  const toCity = normalizeCityName(form.value.toCity)
  const spotCity = normalizeCityName(spot.cityName)
  return Boolean(toCity && spotCity && toCity !== spotCity)
}

function cityInput(field) {
  return field === 'origin' ? form.value.fromCity : form.value.toCity
}

function setCityInput(field, value) {
  if (field === 'origin') {
    form.value.fromCity = value
  } else {
    form.value.toCity = value
  }
}

function clearCityValidation(field) {
  cityErrors.value[field] = ''
  cityValidation.value[field] = null
}

function applyCityError(data, fallbackField = 'destination') {
  const field = data?.field === 'origin' ? 'origin' : data?.field === 'destination' ? 'destination' : fallbackField
  cityErrors.value[field] = data?.message || '无法识别该城市，请输入完整城市名称。'
  cityValidation.value[field] = null
  result.value = null
  savedTripId.value = null
  return true
}

async function validateCityField(field) {
  const input = cityInput(field)
  if (!String(input || '').trim()) {
    applyCityError({ field, message: field === 'origin' ? '请输入完整出发城市名称。' : '请输入完整目的城市名称。' }, field)
    return null
  }
  try {
    const { data } = await api.get('/plans/validate-city', { params: { input, field } })
    cityErrors.value[field] = ''
    cityValidation.value[field] = data
    return data
  } catch (e) {
    applyCityError(e?.response?.data, field)
    return null
  }
}

async function validatePlanningCities() {
  validatingCities.value = true
  error.value = ''
  try {
    const [origin, destination] = await Promise.all([
      validateCityField('origin'),
      validateCityField('destination')
    ])
    if (!origin?.valid || !destination?.valid) {
      result.value = null
      error.value = '请先修正出发地和目的地城市后再生成规划。'
      return null
    }
    return { origin, destination }
  } finally {
    validatingCities.value = false
  }
}

async function generate() {
  if (loading.value || validatingCities.value) return
  error.value = ''
  savedTripId.value = null
  const validatedCities = await validatePlanningCities()
  if (!validatedCities) return
  loading.value = true
  try {
    const payload = {
      ...form.value,
      fromCity: validatedCities.origin.normalizedCityName,
      toCity: validatedCities.destination.normalizedCityName,
      selectedSpots: selectedSpots.value.map(({ photoUrl, fetchedAt, sourceName, realData, id, ...spot }) => spot)
    }
    result.value = (await api.post('/plans/generate', payload)).data
    setCityInput('origin', validatedCities.origin.normalizedCityName)
    setCityInput('destination', validatedCities.destination.normalizedCityName)
  } catch (e) {
    const response = e?.response?.data
    if (response?.field === 'origin' || response?.field === 'destination') {
      applyCityError(response)
      error.value = response.message
      return
    }
    error.value = e?.response?.data?.message || e?.message || '生成规划失败，请检查后端服务是否已启动。'
  } finally {
    loading.value = false
  }
}

function defaultTripTitle() {
  const days = result.value?.days || form.value.days
  return `${form.value.fromCity}出发 · ${form.value.toCity} ${days} 日智慧旅游计划`
}

async function saveTrip() {
  if (!result.value || savingTrip.value || savedTripId.value) return
  try {
    const { value } = await ElMessageBox.prompt('请确认或修改行程标题', '保存行程', {
      confirmButtonText: '确认保存',
      cancelButtonText: '取消',
      inputValue: defaultTripTitle(),
      inputValidator: value => Boolean(value && value.trim()) || '标题不能为空'
    })
    savingTrip.value = true
    const payload = buildTripSavePayload(result.value, form.value, selectedSpots.value, value.trim())
    const payloadBytes = measurePayloadBytes(payload)
    if (import.meta.env.DEV) {
      console.info('[CityGo] trip save payload', {
        bytes: payloadBytes,
        size: payloadSizeLabel(payloadBytes),
        fields: Object.keys(payload)
      })
    }
    if (payloadBytes > SAVE_PAYLOAD_MAX_BYTES) {
      error.value = '行程数据过大，无法保存。请减少跨城市景点或重新生成精简行程。'
      return
    }
    const { data } = await api.post('/trips', payload)
    savedTripId.value = data.tripId
    ElMessage.success('行程保存成功，可在“我的行程”中查看。')
  } catch (e) {
    if (e === 'cancel') return
    error.value = apiErrorMessage(e, '行程保存失败，请稍后重试。')
  } finally {
    savingTrip.value = false
  }
}

onMounted(applyQuery)
watch(() => route.query, applyQuery)
</script>
