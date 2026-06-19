<template>
  <div class="page destination-page">
    <div class="page-heading destination-heading">
      <div>
        <h1>目的地探索</h1>
        <p class="meta">点击中国地图或省份卡片，选择城市后查看真实 POI、天气与规划入口。</p>
      </div>
      <div class="destination-search">
        <el-input
          v-model="searchText"
          placeholder="搜索省份 / 城市"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <el-card class="quick-card">
      <div class="quick-row">
        <span class="quick-label">热门省份</span>
        <el-button v-for="name in hotProvinces" :key="name" text @click="selectProvince(name)">
          {{ name }}
        </el-button>
      </div>
      <div class="quick-row">
        <span class="quick-label">热门城市</span>
        <el-button
          v-for="item in hotCities"
          :key="item.cityInfo.name"
          text
          @click="selectCity(item.provinceName, item.cityInfo)"
        >
          {{ item.cityInfo.name }}
        </el-button>
      </div>
    </el-card>

    <ChinaDestinationMap
      class="section"
      :provinces="provinceNames"
      :selected-province="selectedProvinceName"
      @select-province="selectProvince"
    />

    <CityTravelPanel
      v-if="selectedCity"
      class="section"
      :province-name="selectedProvinceName"
      :city="selectedCity"
    />

    <el-empty v-else class="section" description="请选择省份和城市后查看详情" />

    <el-drawer v-model="drawerVisible" :title="selectedProvinceName" size="420px">
      <template v-if="selectedProvince">
        <p>{{ selectedProvince.intro }}</p>
        <h3>推荐城市</h3>
        <div class="drawer-city-list">
          <el-card v-for="city in selectedProvince.cities" :key="city.name" class="drawer-city-card">
            <h4>{{ city.name }}</h4>
            <p class="meta">{{ city.intro }}</p>
            <div class="city-tags">
              <el-tag v-for="tag in city.tags" :key="tag" size="small">{{ tag }}</el-tag>
            </div>
            <div class="drawer-actions">
              <el-button size="small" type="primary" @click="selectCity(selectedProvinceName, city)">
                查看城市详情
              </el-button>
              <el-button size="small" @click="startPlanning(city.name, '景点')">
                设为规划目的地
              </el-button>
            </div>
          </el-card>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ChinaDestinationMap from '../components/ChinaDestinationMap.vue'
import CityTravelPanel from '../components/CityTravelPanel.vue'
import { findCityProfile, provinceCityData, provinceNames } from '../data/provinceCityData'

const router = useRouter()
const searchText = ref('')
const selectedProvinceName = ref('湖北省')
const selectedCity = ref(provinceCityData['湖北省'].cities[0])
const drawerVisible = ref(false)

const selectedProvince = computed(() => provinceCityData[selectedProvinceName.value])
const hotProvinces = ['四川省', '湖北省', '北京市', '上海市', '广东省', '浙江省']
const hotCities = computed(() => ['成都', '武汉', '北京', '上海', '广州', '杭州']
  .map(name => findCityProfile(name))
  .filter(Boolean))

function selectProvince(name) {
  const normalized = normalizeProvinceName(name)
  if (!provinceCityData[normalized]) {
    ElMessage.warning('当前省份暂未配置城市知识库，但可以通过搜索框搜索城市。')
    return
  }
  selectedProvinceName.value = normalized
  selectedCity.value = provinceCityData[normalized].cities[0]
  drawerVisible.value = true
}

function selectCity(provinceName, city) {
  selectedProvinceName.value = provinceName
  selectedCity.value = city
  drawerVisible.value = false
  window.setTimeout(() => {
    document.querySelector('.city-panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, 260)
}

function handleSearch() {
  const keyword = searchText.value.trim()
  if (!keyword) return

  const provinceName = normalizeProvinceName(keyword)
  if (provinceName) {
    selectProvince(provinceName)
    return
  }

  const cityProfile = findCityProfile(keyword)
  if (cityProfile) {
    selectCity(cityProfile.provinceName, cityProfile.cityInfo)
  }
}

function startPlanning(toCity, preference) {
  router.push({ path: '/planner', query: { toCity, preference } })
}

function normalizeProvinceName(name) {
  const keyword = String(name || '').trim()
  if (!keyword) return ''
  if (provinceCityData[keyword]) return keyword

  const aliases = {
    湖北: '湖北省',
    四川: '四川省',
    广东: '广东省',
    浙江: '浙江省',
    江苏: '江苏省',
    陕西: '陕西省',
    云南: '云南省',
    福建: '福建省',
    湖南: '湖南省',
    山东: '山东省',
    河南: '河南省',
    河北: '河北省',
    山西: '山西省',
    贵州: '贵州省',
    甘肃: '甘肃省',
    青海: '青海省',
    黑龙江: '黑龙江省',
    吉林: '吉林省',
    辽宁: '辽宁省',
    安徽: '安徽省',
    江西: '江西省',
    海南: '海南省',
    北京: '北京市',
    上海: '上海市',
    天津: '天津市',
    重庆: '重庆市',
    广西: '广西壮族自治区',
    新疆: '新疆维吾尔自治区',
    西藏: '西藏自治区',
    宁夏: '宁夏回族自治区',
    内蒙古: '内蒙古自治区'
  }
  if (aliases[keyword]) return aliases[keyword]

  return provinceNames.find(item => {
    const shortName = item
      .replace('壮族自治区', '')
      .replace('回族自治区', '')
      .replace('维吾尔自治区', '')
      .replace('自治区', '')
      .replace('省', '')
      .replace('市', '')
    return item.includes(keyword) || keyword.includes(shortName)
  })
}
</script>
