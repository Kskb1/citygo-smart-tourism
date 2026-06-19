<template>
  <el-card v-if="city" class="city-panel">
    <div class="city-hero">
      <div>
        <p class="meta">{{ provinceName }}</p>
        <h2>{{ city.name }}</h2>
        <p>{{ city.intro }}</p>
        <div class="city-tags">
          <el-tag v-for="tag in city.tags" :key="tag" type="success" effect="light">{{ tag }}</el-tag>
        </div>
      </div>
      <div class="city-facts">
        <div><span>推荐天数</span><strong>{{ city.recommendedDays }}</strong></div>
        <div><span>推荐季节</span><strong>{{ city.season }}</strong></div>
        <div><span>适合人群</span><strong>{{ city.suitableFor }}</strong></div>
      </div>
    </div>

    <div class="panel-actions">
      <el-button type="primary" @click="setAsDestination">设为目的地并开始规划</el-button>
      <el-button @click="selectionDrawerVisible = true">已选择景点（{{ selectionCount }}）</el-button>
      <el-button @click="loadTab(activeTab, true)">刷新当前真实数据</el-button>
    </div>

    <el-tabs v-model="activeTab" class="city-tabs" @tab-change="loadTab">
      <el-tab-pane label="知名景点" name="spots" />
      <el-tab-pane label="美食推荐" name="foods" />
      <el-tab-pane label="打卡地" name="checkins" />
      <el-tab-pane label="博物馆 / 文化" name="museums" />
      <el-tab-pane label="公园 / 自然" name="parks" />
      <el-tab-pane label="路线建议" name="routes" />
    </el-tabs>

    <div v-if="activeTab === 'routes'" class="route-advice">
      <el-alert
        title="路线建议基于城市知识库和已加载真实 POI 点位生成，不包含任何票价或交通价格。"
        type="info"
        show-icon
        :closable="false"
      />
      <div v-if="allLoadedSpots.length" class="city-point-line">
        <div v-for="(spot, index) in allLoadedSpots.slice(0, 6)" :key="spot.id || spot.name" class="city-point">
          <span>{{ index + 1 }}</span>
          <strong>{{ spot.name }}</strong>
          <small>{{ spot.location || '暂无坐标' }}</small>
        </div>
      </div>
      <el-empty
        v-else
        description="先查看景点、美食或公园 Tab，即可根据真实点位生成城市内游览顺序示意"
      />
    </div>

    <template v-else>
      <el-alert v-if="currentError" :title="currentError" type="warning" show-icon :closable="false" />
      <el-skeleton v-if="currentLoading" :rows="5" animated />
      <div v-else-if="currentSpots.length" class="spot-grid section">
        <el-card v-for="spot in currentSpots" :key="spot.id || spot.name" class="spot-card">
          <div v-if="spot.photoUrl" class="spot-photo" :style="{ backgroundImage: `url(${spot.photoUrl})` }"></div>
          <div v-else class="spot-photo placeholder">暂无官方图片</div>
          <div class="spot-title">
            <h3>{{ spot.name }}</h3>
            <el-tag size="small" type="success">真实接口返回</el-tag>
          </div>
          <p class="meta">{{ spot.type || tabLabel(activeTab) }}</p>
          <p>{{ spot.recommendationReason || '来自真实 POI 数据，暂无补充推荐语。' }}</p>
          <p class="meta">地址：{{ spot.address || '暂无地址' }}</p>
          <p class="meta">经纬度：{{ spot.location || '暂无坐标' }}</p>
          <p class="meta">数据来源：{{ spot.sourceName || 'amap' }} · {{ formatTime(spot.fetchedAt) }}</p>
          <div class="card-actions">
            <el-button size="small" type="primary" @click="addToSelection(spot)">加入清单</el-button>
            <el-button size="small" @click="setAsDestination">设为目的地</el-button>
            <el-button size="small" @click="openDetail(spot)">查看详情</el-button>
          </div>
        </el-card>
      </div>
      <el-empty v-else description="当前分类暂未获取到真实地点数据" />
    </template>

    <el-dialog v-model="detailVisible" :title="selectedSpot?.name" width="680px">
      <div v-if="selectedSpot" class="spot-detail">
        <div v-if="selectedSpot.photoUrl" class="detail-photo" :style="{ backgroundImage: `url(${selectedSpot.photoUrl})` }"></div>
        <div v-else class="detail-photo placeholder">暂无官方图片</div>
        <p><strong>地址：</strong>{{ selectedSpot.address || '暂无地址' }}</p>
        <p><strong>类型：</strong>{{ selectedSpot.type || '未标注类型' }}</p>
        <p><strong>城市：</strong>{{ selectedSpot.cityName || city.name }}</p>
        <p><strong>经纬度：</strong>{{ selectedSpot.location || '暂无坐标' }}</p>
        <p><strong>推荐理由：</strong>{{ selectedSpot.recommendationReason || '来自真实 POI 数据。' }}</p>
        <p><strong>出行建议：</strong>{{ travelAdvice(selectedSpot.type) }}</p>
        <p><strong>附近推荐关键词：</strong>{{ nearbyKeywords(selectedSpot.type) }}</p>
        <p class="meta">数据来源：{{ selectedSpot.sourceName || 'amap' }} · {{ formatTime(selectedSpot.fetchedAt) }}</p>
        <el-collapse>
          <el-collapse-item title="原始接口数据（调试用）" name="raw">
            <pre>{{ JSON.stringify(rawResult?.rawJson || {}, null, 2) }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-dialog>

    <el-drawer v-model="selectionDrawerVisible" title="已选择景点" size="420px">
      <div v-if="selectedSpots.length" class="selected-spot-list">
        <div v-for="spot in selectedSpots" :key="spot.id" class="selected-spot-item">
          <div>
            <strong>{{ spot.name }}</strong>
            <p class="meta">{{ spot.cityName || '未知城市' }} · {{ spot.type || '未标注类型' }}</p>
            <p class="meta">{{ spot.address || '暂无地址' }}</p>
          </div>
          <el-button size="small" text type="danger" @click="removeSpot(spot.id)">删除</el-button>
        </div>
      </div>
      <el-empty v-else description="还没有加入待规划景点" />
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="selectionDrawerVisible = false">继续浏览</el-button>
          <el-button :disabled="!selectedSpots.length" @click="clearSpots">清空清单</el-button>
          <el-button type="primary" @click="goPlanner">去智能规划</el-button>
        </div>
      </template>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'
import { useTripSelection } from '../composables/useTripSelection'

const props = defineProps({
  provinceName: { type: String, default: '' },
  city: { type: Object, default: null }
})

const router = useRouter()
const activeTab = ref('spots')
const cache = reactive({})
const detailVisible = ref(false)
const selectedSpot = ref(null)
const selectionDrawerVisible = ref(false)
const { selectedSpots, count: selectionCount, addSpot, removeSpot, clearSpots } = useTripSelection()

const currentState = computed(() => cache[activeTab.value] || {})
const currentSpots = computed(() => currentState.value.spots || [])
const currentLoading = computed(() => currentState.value.loading)
const currentError = computed(() => currentState.value.error)
const rawResult = computed(() => currentState.value.result)
const allLoadedSpots = computed(() => Object.values(cache).flatMap(item => item.spots || []))

const tabConfig = {
  spots: { label: '知名景点', keyword: '景点', preference: '景点' },
  foods: { label: '美食推荐', keyword: '美食', preference: '美食' },
  checkins: { label: '打卡地', keyword: '打卡', preference: '景点' },
  museums: { label: '博物馆 / 文化', keyword: '博物馆', preference: '博物馆' },
  parks: { label: '公园 / 自然', keyword: '公园', preference: '公园' }
}

function tabLabel(tab) {
  return tabConfig[tab]?.label || '地点'
}

function tabPreference(tab) {
  return tabConfig[tab]?.preference || '景点'
}

async function loadTab(tab = activeTab.value, force = false) {
  if (tab === 'routes' || !props.city) return
  if (cache[tab]?.loaded && !force) return

  cache[tab] = {
    loading: true,
    error: '',
    spots: cache[tab]?.spots || [],
    loaded: false,
    result: cache[tab]?.result
  }

  const keyword = props.city.poiKeywords?.[tab] || tabConfig[tab]?.keyword || '景点'
  try {
    const response = await api.get('/spots/search', { params: { city: props.city.name, keyword } })
    const result = response.data
    cache[tab] = {
      loading: false,
      error: result.realData ? '' : (result.message || result.errorMessage || '未获取到真实地点数据'),
      spots: result.data?.spots || [],
      loaded: true,
      result
    }
  } catch (e) {
    cache[tab] = {
      loading: false,
      error: e?.response?.data?.message || e?.message || '地点数据加载失败，请稍后重试。',
      spots: [],
      loaded: true,
      result: null
    }
  }
}

function openDetail(spot) {
  selectedSpot.value = spot
  detailVisible.value = true
}

function addToSelection(spot) {
  const result = addSpot({ ...spot, cityName: spot.cityName || props.city.name }, props.provinceName)
  if (result.added) {
    ElMessage.success(result.message)
  } else {
    ElMessage.warning(result.message)
  }
  selectionDrawerVisible.value = true
}

function setAsDestination() {
  router.push({ path: '/planner', query: { toCity: props.city.name } })
}

function goPlanner() {
  router.push({ path: '/planner' })
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function travelAdvice(type = '') {
  if (type.includes('风景名胜')) return '建议预留 1-2 小时，适合拍照和观光。'
  if (type.includes('博物馆')) return '建议提前确认开放时间，适合文化体验。'
  if (type.includes('公园')) return '适合休闲散步，建议选择天气较好时前往。'
  if (type.includes('餐饮')) return '建议结合附近景点安排午餐或晚餐。'
  return '建议结合交通距离和当天体力安排游览顺序。'
}

function nearbyKeywords(type = '') {
  if (type.includes('餐饮')) return '景点、商圈、夜市'
  if (type.includes('博物馆')) return '文化街区、咖啡、书店'
  if (type.includes('公园')) return '湖泊、绿道、亲子'
  return '美食、打卡、公共交通'
}

watch(() => props.city?.name, () => {
  Object.keys(cache).forEach(key => delete cache[key])
  activeTab.value = 'spots'
  loadTab('spots')
})

onMounted(() => loadTab('spots'))
</script>
