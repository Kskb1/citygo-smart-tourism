<template>
  <div class="page travel-page">
    <section class="travel-hero transport-hero">
      <div>
        <span class="travel-kicker">真实地点 · 高德路线 · 规则预估</span>
        <h1>跨城交通与机场出行</h1>
        <p>
          用高德 POI 识别出发地和目的地机场，用高德路线计算机场接驳；跨城机票、火车和自驾费用只展示非实时预算参考，不生成航班、车次或余票。
        </p>
      </div>
      <div class="travel-hero-panel">
        <strong>本页数据边界</strong>
        <span>机场位置、地面路线来自真实高德数据。</span>
        <span>高铁查询跳转 12306 官方入口。</span>
        <span>预算为 RULE_ESTIMATED，不代表实时价格。</span>
      </div>
    </section>

    <el-card class="travel-filter-card section">
      <template #header>
        <div class="card-header-row">
          <strong>查询条件</strong>
          <el-tag type="success">真实 API 优先</el-tag>
        </div>
      </template>

      <el-form class="travel-query-grid" label-position="top" @keyup.enter="search">
        <el-form-item label="出发城市">
          <el-input v-model="form.fromCity" placeholder="例如：成都" />
        </el-form-item>
        <el-form-item label="目的城市">
          <el-input v-model="form.toCity" placeholder="例如：武汉" />
        </el-form-item>
        <el-form-item label="出发地点">
          <el-input v-model="form.fromPlace" placeholder="例如：春熙路" />
        </el-form-item>
        <el-form-item label="目的地点">
          <el-input v-model="form.toPlace" placeholder="例如：黄鹤楼" />
        </el-form-item>
        <el-form-item label="出行日期">
          <el-date-picker v-model="form.date" value-format="YYYY-MM-DD" type="date" />
        </el-form-item>
        <el-form-item label="交通偏好">
          <el-select v-model="form.preference">
            <el-option label="综合对比" value="compare" />
            <el-option label="飞机" value="flight" />
            <el-option label="高铁" value="high-speed" />
            <el-option label="普通火车" value="normal-train" />
            <el-option label="自驾" value="self-driving" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="travel-actions">
        <el-button type="primary" :loading="loading" @click="search">查询交通方案</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <div class="quick-chip-row">
        <span>热门线路</span>
        <el-button v-for="route in hotRoutes" :key="route.label" size="small" @click="applyHotRoute(route)">
          {{ route.label }}
        </el-button>
      </div>
    </el-card>

    <el-alert
      class="section"
      title="CityGo 不伪造实时航班、车次、余票或市场价格。跨城预算仅用于行程成本粗估，正式预订请前往航空公司、12306 或正规平台。"
      type="info"
      show-icon
      :closable="false"
    />
    <el-alert v-if="error" class="section" :title="error" type="warning" show-icon :closable="false" />

    <div v-if="loading" class="travel-loading-grid section">
      <el-skeleton v-for="item in 4" :key="item" animated />
    </div>

    <template v-else-if="hasResult">
      <section class="route-overview section">
        <el-card class="overview-card">
          <span>出发</span>
          <strong>{{ form.fromCity }}</strong>
          <small>{{ form.fromPlace || '未填写具体地点' }}</small>
        </el-card>
        <el-card class="overview-card">
          <span>跨城交通</span>
          <strong>{{ preferenceLabel }}</strong>
          <small>{{ form.date || '未选择日期' }}</small>
        </el-card>
        <el-card class="overview-card">
          <span>到达</span>
          <strong>{{ form.toCity }}</strong>
          <small>{{ form.toPlace || '未填写具体地点' }}</small>
        </el-card>
        <el-card class="overview-card">
          <span>预算模式</span>
          <strong>RULE_ESTIMATED</strong>
          <small>非实时价格，仅供预算参考</small>
        </el-card>
      </section>

      <section class="airport-section section">
        <div class="section-title">
          <div>
            <h2>机场出行 POI</h2>
            <p class="meta">民航机场单独展示，航站楼、停车场、餐饮等配套设施收纳为相关地点，避免把 T2/T3 当作独立机场。</p>
          </div>
          <div class="source-tags">
            <el-tag type="success">高德 POI</el-tag>
            <el-tag type="info">真实地点</el-tag>
          </div>
        </div>

        <div class="airport-columns">
          <airport-column
            title="出发城市机场"
            :city="form.fromCity"
            :group="departAirportGroup"
            :place="form.fromPlace"
            :route="groundRoutes.depart"
            route-label="规划去机场路线"
            @route="routeToAirport('depart', $event)"
          />
          <airport-column
            title="目的城市机场"
            :city="form.toCity"
            :group="arriveAirportGroup"
            :place="form.toPlace"
            :route="groundRoutes.arrive"
            route-label="规划接驳路线"
            @route="routeToAirport('arrive', $event)"
          />
        </div>
      </section>

      <section class="section">
        <div class="section-title">
          <div>
            <h2>跨城交通预算参考</h2>
            <p class="meta">基于城市距离的规则预估，不是航空公司、12306 或平台实时价格。</p>
          </div>
          <el-tag type="warning">非实时价格</el-tag>
        </div>

        <div class="budget-grid">
          <el-card v-for="budget in transportBudgets" :key="budget.key" class="budget-card">
            <div class="budget-card-head">
              <span>{{ budget.icon }}</span>
              <div>
                <strong>{{ budget.name }}</strong>
                <small>{{ budget.priceMode }}</small>
              </div>
            </div>
            <p class="budget-price">{{ budget.rangeText }}</p>
            <p class="meta">{{ budget.totalText }} · 约 {{ budget.distance }} 公里</p>
            <p>{{ budget.duration }}</p>
            <p class="meta">{{ budget.note }}</p>
          </el-card>
        </div>
      </section>

      <section class="official-query-grid section">
        <el-card class="official-card">
          <template #header>航班官方查询说明</template>
          <p>CityGo 当前不生成航班号、起飞时间、到达时间、实时票价或余票。</p>
          <p class="meta">请前往航空公司官网、航旅纵横或正规预订平台查询 {{ form.fromCity }} 到 {{ form.toCity }} 的实际航班。</p>
          <div class="source-tags">
            <el-tag type="info">官方查询</el-tag>
            <el-tag type="warning">无实时航班</el-tag>
          </div>
        </el-card>

        <el-card class="official-card">
          <template #header>高铁合规查询</template>
          <p>{{ train?.message || '高铁查询默认使用 12306 官方跳转，不爬取、不伪造车次或票价。' }}</p>
          <div class="source-tags">
            <el-tag type="warning">{{ train?.provider || 'official_redirect' }}</el-tag>
            <el-tag type="info">官方查询</el-tag>
          </div>
          <el-link v-if="train?.officialUrl" :href="train.officialUrl" target="_blank">前往 12306 官方查询</el-link>
        </el-card>
      </section>
    </template>

    <el-empty v-else class="section" description="请输入城市后查询机场、路线和交通预算。" />
  </div>
</template>

<script setup>
import { computed, defineComponent, h, reactive, ref } from 'vue'
import { ElButton, ElCard, ElCollapse, ElCollapseItem, ElEmpty, ElMessage, ElTag } from 'element-plus'
import { api } from '../api/client'
import { buildTransportBudget, classifyAirports, getPoiList } from '../utils/travelDisplay'

const today = new Date()
today.setDate(today.getDate() + 14)

const form = reactive({
  fromCity: '成都',
  toCity: '武汉',
  fromPlace: '春熙路',
  toPlace: '黄鹤楼',
  date: today.toISOString().slice(0, 10),
  preference: 'compare'
})

const hotRoutes = [
  { label: '成都 → 武汉', fromCity: '成都', toCity: '武汉', fromPlace: '春熙路', toPlace: '黄鹤楼' },
  { label: '北京 → 上海', fromCity: '北京', toCity: '上海', fromPlace: '天安门', toPlace: '外滩' },
  { label: '广州 → 杭州', fromCity: '广州', toCity: '杭州', fromPlace: '珠江新城', toPlace: '西湖' },
  { label: '西安 → 南京', fromCity: '西安', toCity: '南京', fromPlace: '钟楼', toPlace: '夫子庙' }
]

const departAirports = ref([])
const arriveAirports = ref([])
const train = ref(null)
const groundRoutes = reactive({ depart: null, arrive: null })
const loading = ref(false)
const error = ref('')
const searched = ref(false)

const departAirportGroup = computed(() => classifyAirports(departAirports.value))
const arriveAirportGroup = computed(() => classifyAirports(arriveAirports.value))
const transportBudgets = computed(() => buildTransportBudget({
  fromCity: form.fromCity,
  toCity: form.toCity,
  preference: form.preference
}))
const hasResult = computed(() => searched.value && (departAirports.value.length || arriveAirports.value.length || transportBudgets.value.length || train.value))
const preferenceLabel = computed(() => ({
  compare: '综合对比',
  flight: '飞机',
  'high-speed': '高铁',
  'normal-train': '普通火车',
  'self-driving': '自驾'
})[form.preference] || '综合对比')

async function search() {
  if (!form.fromCity.trim() || !form.toCity.trim()) {
    error.value = '请输入出发城市和目的城市。'
    return
  }

  loading.value = true
  error.value = ''
  searched.value = true
  groundRoutes.depart = null
  groundRoutes.arrive = null

  try {
    const [fromAirports, toAirports, trainResult] = await Promise.all([
      api.get('/airports/search', { params: { city: form.fromCity.trim(), keyword: '机场' } }),
      api.get('/airports/search', { params: { city: form.toCity.trim(), keyword: '机场' } }),
      api.get('/trains/search', { params: { fromCity: form.fromCity.trim(), toCity: form.toCity.trim(), date: form.date } })
    ])

    departAirports.value = getPoiList(fromAirports.data)
    arriveAirports.value = getPoiList(toAirports.data)
    train.value = trainResult.data

    const providerMessage = fromAirports.data?.message || toAirports.data?.message
    if (!departAirports.value.length && !arriveAirports.value.length && providerMessage) {
      error.value = friendlyError(providerMessage)
    }

    await autoRouteMainAirports()
  } catch (e) {
    error.value = friendlyError(e?.response?.data?.message || e?.message)
  } finally {
    loading.value = false
  }
}

async function autoRouteMainAirports() {
  const departAirport = departAirportGroup.value.main[0]
  const arriveAirport = arriveAirportGroup.value.main[0]
  const tasks = []
  if (form.fromPlace.trim() && departAirport?.locationText) tasks.push(routeToAirport('depart', departAirport, true))
  if (form.toPlace.trim() && arriveAirport?.locationText) tasks.push(routeToAirport('arrive', arriveAirport, true))
  await Promise.allSettled(tasks)
}

async function routeToAirport(type, airport, silent = false) {
  const origin = type === 'depart' ? form.fromPlace.trim() : airport.locationText
  const destination = type === 'depart' ? airport.locationText : form.toPlace.trim()
  const city = type === 'depart' ? form.fromCity : form.toCity

  if (!origin || !destination) {
    if (!silent) ElMessage.info('请先填写具体地点，并确保机场 POI 含有经纬度。')
    return
  }

  try {
    const { data } = await api.get('/routes', { params: { origin, destination, mode: 'driving', city } })
    if (!data.realData) {
      if (!silent) ElMessage.warning(data.message || '路线数据暂时无法获取。')
      return
    }
    groundRoutes[type] = { ...data.data, airportName: airport.name }
  } catch (e) {
    if (!silent) ElMessage.warning(e?.response?.data?.message || '路线数据暂时无法获取，请稍后重试。')
  }
}

function applyHotRoute(route) {
  Object.assign(form, route)
  search()
}

function reset() {
  Object.assign(form, {
    fromCity: '成都',
    toCity: '武汉',
    fromPlace: '春熙路',
    toPlace: '黄鹤楼',
    preference: 'compare'
  })
  departAirports.value = []
  arriveAirports.value = []
  train.value = null
  groundRoutes.depart = null
  groundRoutes.arrive = null
  error.value = ''
  searched.value = false
}

function friendlyError(message = '') {
  if (message.includes('未配置') || message.includes('AMAP_API_KEY')) return '高德接口未配置，暂时无法获取真实机场和路线数据。'
  if (message.includes('401')) return '登录状态已失效，请重新登录后再查询。'
  return '交通数据暂时获取失败，请稍后重试。'
}

const AirportColumn = defineComponent({
  name: 'AirportColumn',
  props: {
    title: String,
    city: String,
    group: Object,
    place: String,
    route: Object,
    routeLabel: String
  },
  emits: ['route'],
  setup(props, { emit }) {
    const renderAirport = airport => h('div', { class: 'airport-card' }, [
      h('div', { class: 'airport-card-head' }, [
        h('div', [
          h('strong', airport.name),
          h('p', { class: 'meta' }, `${airport.district || props.city || '-'} · ${airport.address || '高德未返回详细地址'}`)
        ]),
        h(ElTag, { type: airport.airportKind === '民航机场' ? 'success' : 'info' }, () => airport.airportKind || '机场 POI')
      ]),
      h('div', { class: 'source-tags' }, [
        h(ElTag, { type: 'success' }, () => airport.displayMode || '真实地点'),
        h(ElTag, { type: 'info' }, () => airport.sourceName || '高德开放平台')
      ]),
      h('div', { class: 'airport-card-actions' }, [
        h(ElButton, {
          size: 'small',
          type: 'primary',
          disabled: !props.place || !airport.locationText,
          onClick: () => emit('route', airport)
        }, () => props.routeLabel)
      ])
    ])

    return () => h(ElCard, { class: 'airport-column-card' }, {
      header: () => h('div', { class: 'card-header-row' }, [
        h('strong', props.title),
        h(ElTag, { type: 'info' }, () => props.city)
      ]),
      default: () => [
        props.group?.main?.length
          ? h('div', { class: 'airport-list' }, props.group.main.map(renderAirport))
          : h(ElEmpty, { description: '未查询到可作为主机场展示的 POI。' }),
        props.group?.general?.length
          ? h('div', { class: 'airport-related-block' }, [
              h('h4', '通用机场/特殊机场'),
              ...props.group.general.map(renderAirport)
            ])
          : null,
        props.route
          ? h('div', { class: 'ground-route-result' }, [
              h('strong', `${props.route.airportName || '机场'} 接驳路线`),
              h('p', `距离 ${props.route.distanceText || '-'}，预计 ${props.route.durationText || '-'}`),
              h(ElTag, { type: 'success' }, () => '高德真实路线')
            ])
          : null,
        props.group?.terminal?.length || props.group?.related?.length
          ? h(ElCollapse, { class: 'airport-collapse' }, () => [
              h(ElCollapseItem, { title: `航站楼/配套设施 ${props.group.terminal.length + props.group.related.length} 个`, name: 'related' }, () =>
                [...props.group.terminal, ...props.group.related].slice(0, 8).map(item =>
                  h('p', { class: 'meta airport-related-line', key: item.id || item.name }, `${item.name} · ${item.address || item.typeName || '高德 POI'}`)
                )
              )
            ])
          : null
      ]
    })
  }
})
</script>
