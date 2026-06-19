<template>
  <div class="route-map">
    <div v-if="mapReady" ref="mapEl" class="real-map"></div>
    <div v-else class="route-diagram">
      <div class="route-line">
        <div class="route-node origin">
          <span>出发</span>
          <strong>{{ originCity }}</strong>
        </div>
        <div
          v-for="(spot, index) in visibleSpots"
          :key="spot.id || spot.name || index"
          class="route-node spot"
        >
          <span>{{ index + 1 }}</span>
          <strong>{{ spot.name }}</strong>
          <small>{{ spot.location || locationText(spot) }}</small>
        </div>
        <div class="route-node destination">
          <span>返程</span>
          <strong>{{ destinationCity }} → {{ originCity }}</strong>
        </div>
      </div>
      <p class="meta">当前未配置前端高德 JS Key，已使用路线示意图展示城市与景点顺序。</p>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'

const props = defineProps({
  originCity: String,
  destinationCity: String,
  spots: { type: Array, default: () => [] },
  routePolyline: { type: Array, default: () => [] }
})

const mapEl = ref(null)
const mapReady = ref(false)

const visibleSpots = computed(() => (props.spots || []).filter(Boolean).slice(0, 8))

function locationText(spot) {
  if (spot?.longitude && spot?.latitude) {
    return `${spot.longitude},${spot.latitude}`
  }
  return '暂无坐标'
}

function loadAmapScript(key) {
  if (window.AMap) {
    return Promise.resolve()
  }
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}`
    script.async = true
    script.onload = resolve
    script.onerror = reject
    document.head.appendChild(script)
  })
}

onMounted(async () => {
  const key = import.meta.env.VITE_AMAP_JS_API_KEY
    if (!key) {
      return
    }
    try {
      await loadAmapScript(key)
      mapReady.value = true
      await nextTick()
      if (!mapEl.value || !window.AMap) {
        mapReady.value = false
        return
      }
      const centerSpot = visibleSpots.value.find(spot => spot.longitude && spot.latitude)
    const center = centerSpot ? [centerSpot.longitude, centerSpot.latitude] : undefined
    const map = new window.AMap.Map(mapEl.value, {
      zoom: 11,
      center
    })
    visibleSpots.value.forEach((spot, index) => {
      if (!spot.longitude || !spot.latitude) return
      new window.AMap.Marker({
        map,
        position: [spot.longitude, spot.latitude],
        title: `${index + 1}. ${spot.name}`
      })
    })
    const path = (props.routePolyline || [])
      .map(point => String(point).split(',').map(Number))
      .filter(point => point.length === 2 && point.every(Number.isFinite))
    if (path.length > 1) {
      new window.AMap.Polyline({
        map,
        path,
        strokeColor: '#2563eb',
        strokeWeight: 5,
        strokeOpacity: 0.85
      })
      map.setFitView()
    }
  } catch {
    mapReady.value = false
  }
})
</script>
