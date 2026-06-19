<template>
  <div class="china-map-shell">
    <div class="map-toolbar">
      <div>
        <h2>中国目的地地图</h2>
        <p class="meta">点击省份查看推荐城市；地图不可用时会自动切换为省份卡片。</p>
      </div>
      <el-tag :type="mapReady ? 'success' : 'warning'">
        {{ mapReady ? '地图已加载' : '省份卡片模式' }}
      </el-tag>
    </div>

    <div v-show="mapReady" ref="chartEl" class="china-map"></div>
    <div v-if="!mapReady" class="province-fallback-grid">
      <button
        v-for="name in provinces"
        :key="name"
        class="province-chip"
        :class="{ active: name === selectedProvince }"
        type="button"
        @click="$emit('select-province', name)"
      >
        {{ name }}
      </button>
    </div>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  provinces: { type: Array, default: () => [] },
  selectedProvince: { type: String, default: '' }
})

const emit = defineEmits(['select-province'])

const chartEl = ref(null)
const mapReady = ref(false)
let chart = null

async function loadChinaGeoJson() {
  const sources = [
    'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json'
  ]

  for (const url of sources) {
    try {
      const response = await fetch(url)
      if (response.ok) return await response.json()
    } catch {
      // Try the next source.
    }
  }

  return null
}

function renderChart() {
  if (!chartEl.value) return
  if (!chart) {
    chart = echarts.init(chartEl.value)
    chart.on('click', params => {
      if (params?.name) emit('select-province', params.name)
    })
  }

  chart.setOption({
    tooltip: { trigger: 'item' },
    visualMap: {
      show: false,
      min: 0,
      max: 100,
      inRange: { color: ['#e8f7f6', '#9ddbd3', '#3da6b0'] }
    },
    series: [{
      name: '目的地',
      type: 'map',
      map: 'china-citygo',
      roam: true,
      zoom: 1.15,
      layoutCenter: ['50%', '52%'],
      layoutSize: '110%',
      selectedMode: 'single',
      label: { show: false, color: '#33515c', fontSize: 10 },
      itemStyle: {
        areaColor: '#dff3f1',
        borderColor: '#ffffff',
        borderWidth: 1
      },
      emphasis: {
        label: { show: true, color: '#0f3d4a', fontWeight: 700 },
        itemStyle: { areaColor: '#7dd3c7' }
      },
      select: {
        itemStyle: { areaColor: '#14b8a6' },
        label: { show: true, color: '#ffffff' }
      },
      data: props.provinces.map((name, index) => ({
        name,
        value: 30 + (index % 12) * 5,
        selected: name === props.selectedProvince
      }))
    }]
  })
}

function resizeChart() {
  chart?.resize()
}

onMounted(async () => {
  const geoJson = await loadChinaGeoJson()
  if (!geoJson) {
    mapReady.value = false
    return
  }

  echarts.registerMap('china-citygo', geoJson)
  mapReady.value = true
  await nextTick()
  renderChart()
  window.addEventListener('resize', resizeChart)
})

watch(() => props.selectedProvince, () => {
  if (chart) renderChart()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})
</script>
