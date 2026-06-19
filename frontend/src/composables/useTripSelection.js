import { computed, ref } from 'vue'

const STORAGE_KEY = 'citygo:selectedSpots'
const selectedSpots = ref(readSelection())

function readSelection() {
  try {
    const value = window.localStorage.getItem(STORAGE_KEY)
    const parsed = value ? JSON.parse(value) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function persist() {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(selectedSpots.value))
}

function spotKey(spot) {
  return spot?.id || `${spot?.name || ''}|${spot?.cityName || ''}|${spot?.address || ''}`
}

function toSelectionSpot(spot, provinceName = '') {
  return {
    id: spot.id || spotKey(spot),
    name: spot.name,
    cityName: spot.cityName || '',
    provinceName,
    address: spot.address || '',
    type: spot.type || '',
    longitude: spot.longitude ?? null,
    latitude: spot.latitude ?? null,
    photoUrl: spot.photoUrl || '',
    recommendationReason: spot.recommendationReason || '',
    sourceName: spot.sourceName || '',
    fetchedAt: spot.fetchedAt || '',
    realData: spot.realData !== false
  }
}

export function useTripSelection() {
  const count = computed(() => selectedSpots.value.length)

  function hasSpot(spot) {
    return selectedSpots.value.some(existing => spotKey(existing) === spotKey(spot))
  }

  function addSpot(spot, provinceName = '') {
    const item = toSelectionSpot(spot, provinceName)
    if (hasSpot(item)) {
      return { added: false, message: '该景点已在清单中。' }
    }
    selectedSpots.value = [...selectedSpots.value, item]
    persist()
    return { added: true, message: '已加入待规划景点清单。' }
  }

  function removeSpot(id) {
    selectedSpots.value = selectedSpots.value.filter(spot => spot.id !== id)
    persist()
  }

  function clearSpots() {
    selectedSpots.value = []
    persist()
  }

  return {
    selectedSpots,
    count,
    hasSpot,
    addSpot,
    removeSpot,
    clearSpots
  }
}
