export const SAVE_PAYLOAD_MAX_BYTES = 5 * 1024 * 1024

const FORBIDDEN_KEYS = new Set([
  'rawData',
  'debugData',
  'apiResponse',
  'response',
  'request',
  'config',
  'headers',
  'token',
  'password',
  'apiKey',
  'AMAP_API_KEY',
  'JWT_SECRET',
  'MYSQL_PASSWORD',
  'polyline',
  'path',
  'points',
  'coordinates',
  'map',
  'chart',
  'echarts',
  'dom',
  'base64',
  'photos',
  'photoUrls'
])

export function buildTripSavePayload(planResult, formData, selectedSpots = [], title = '') {
  const budgetReference = planResult?.budgetSummary?.budgetReference || null
  const planData = compactObject({
    title: planResult?.title || title,
    summary: planResult?.summary || '',
    budgetConclusion: planResult?.budgetConclusion || '',
    fromCity: planResult?.fromCity || formData?.fromCity || '',
    toCity: planResult?.toCity || formData?.toCity || '',
    days: planResult?.days || formData?.days || null,
    peopleCount: planResult?.peopleCount || formData?.people || null,
    budget: planResult?.budget || formData?.budget || null,
    recommendedDays: planResult?.recommendedDays || planResult?.feasibility?.recommendedDays || null,
    feasibility: sanitizeFeasibility(planResult?.feasibility),
    dailyPlans: sanitizeDailyPlans(planResult?.dailyPlans),
    citySegments: sanitizeCitySegments(planResult?.citySegments),
    budgetSummary: sanitizeBudgetSummary(planResult?.budgetSummary),
    priceEstimateSnapshot: sanitizeBudgetReference(budgetReference),
    selectedSpots: sanitizeSpots(selectedSpots),
    recommendedSpots: sanitizeSpots(planResult?.recommendedSpots),
    weatherSummary: sanitizeWeather(planResult?.weatherSummary),
    routeOverview: sanitizeRoute(planResult?.routeOverview || planResult?.route),
    accommodationSuggestions: sanitizeSuggestions(planResult?.accommodationSuggestions || planResult?.hotels),
    transportSuggestions: sanitizeSuggestions(planResult?.transportSuggestions || planResult?.transportPlan),
    dataSourceSummary: sanitizeSources(planResult?.dataSourceSummary || planResult?.dataSources),
    dataSources: sanitizeSources(planResult?.dataSources)
  })

  return compactObject({
    title,
    fromCity: planData.fromCity,
    toCity: planData.toCity,
    startDate: formData?.departureDate || planResult?.startDate || null,
    days: planData.days,
    peopleCount: planData.peopleCount,
    roomCount: formData?.roomCount || null,
    budget: planData.budget,
    feasibilityLevel: planResult?.feasibility?.level || '',
    recommendedDays: planData.recommendedDays,
    summary: planData.summary,
    priceMode: budgetReference?.priceMode || '',
    priceRuleVersion: budgetReference?.ruleVersion || '',
    planData
  })
}

export function measurePayloadBytes(payload) {
  const json = JSON.stringify(payload)
  if (typeof Blob !== 'undefined') {
    return new Blob([json]).size
  }
  return new TextEncoder().encode(json).length
}

export function payloadSizeLabel(bytes) {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / 1024 / 1024).toFixed(2)} MB`
  }
  return `${(bytes / 1024).toFixed(1)} KB`
}

function sanitizeFeasibility(value) {
  if (!value) return null
  return compactObject({
    level: value.level,
    feasible: value.feasible,
    recommendedDays: value.recommendedDays,
    warnings: compactList(value.warnings),
    suggestions: compactList(value.suggestions)
  })
}

function sanitizeDailyPlans(list) {
  return asArray(list).map(day => compactObject({
    day: day.day,
    date: day.date,
    title: day.title,
    summary: day.summary,
    cityName: day.cityName,
    activities: asArray(day.activities).map(activity => compactObject({
      period: activity.period,
      title: activity.title,
      description: activity.description,
      reason: activity.reason,
      costNote: activity.costNote,
      sourceName: activity.sourceName
    }))
  }))
}

function sanitizeCitySegments(list) {
  return asArray(list).map(segment => compactObject({
    segmentType: segment.segmentType,
    cityName: segment.cityName,
    recommendedStayDays: segment.recommendedStayDays,
    travelNotice: segment.travelNotice,
    spots: sanitizeSpots(segment.spots)
  }))
}

function sanitizeBudgetSummary(value) {
  if (!value) return null
  return compactObject({
    transportRealCost: value.transportRealCost,
    transportStatus: value.transportStatus,
    hotelEstimate: value.hotelEstimate,
    foodEstimate: value.foodEstimate,
    localTrafficEstimate: value.localTrafficEstimate,
    ticketEstimate: value.ticketEstimate,
    remainingBudget: value.remainingBudget,
    budgetReference: sanitizeBudgetReference(value.budgetReference)
  })
}

function sanitizeBudgetReference(value) {
  if (!value) return null
  return compactObject({
    priceMode: value.priceMode,
    ruleVersion: value.ruleVersion,
    estimatedMin: value.estimatedMin,
    estimatedTypical: value.estimatedTypical,
    estimatedMax: value.estimatedMax,
    budgetLevel: value.budgetLevel,
    generatedAt: value.generatedAt,
    notice: value.notice,
    suggestions: compactList(value.suggestions),
    items: asArray(value.items).map(item => compactObject({
      category: item.category,
      priceMode: item.priceMode,
      currency: item.currency,
      minPrice: item.minPrice,
      typicalPrice: item.typicalPrice,
      maxPrice: item.maxPrice,
      unit: item.unit,
      confidence: item.confidence,
      sourceName: item.sourceName,
      notice: item.notice,
      ruleVersion: item.ruleVersion,
      basis: compactList(item.basis)
    }))
  })
}

function sanitizeSpots(list) {
  return asArray(list).map(spot => compactObject({
    id: spot.id,
    name: spot.name,
    cityName: spot.cityName || spot.city,
    district: spot.district,
    address: spot.address,
    longitude: toNumber(spot.longitude),
    latitude: toNumber(spot.latitude),
    type: spot.type,
    sourceName: spot.sourceName || '高德开放平台',
    recommendationReason: spot.recommendationReason
  }))
}

function sanitizeWeather(value) {
  if (!value) return null
  return compactObject({
    city: value.city,
    weather: value.weather,
    temperature: value.temperature,
    humidity: value.humidity,
    windDirection: value.windDirection,
    windPower: value.windPower,
    reportTime: value.reportTime,
    travelTip: value.travelTip,
    travelTips: compactList(value.travelTips),
    sourceName: value.sourceName || '高德开放平台'
  })
}

function sanitizeRoute(value) {
  if (!value) return null
  return compactObject({
    from: value.from || value.originName,
    to: value.to || value.destinationName,
    mode: value.mode,
    distanceMeters: value.distanceMeters,
    durationMinutes: value.durationMinutes,
    distanceText: value.distanceText || value.distance,
    durationText: value.durationText || value.duration,
    dataMode: value.dataMode,
    sourceName: value.sourceName,
    notice: value.notice
  })
}

function sanitizeSuggestions(value) {
  if (Array.isArray(value)) {
    return value.slice(0, 20).map(item => sanitizePlainObject(item))
  }
  if (value && typeof value === 'object') {
    return sanitizePlainObject(value)
  }
  return value || null
}

function sanitizeSources(value) {
  return asArray(value).map(source => compactObject({
    name: source.name,
    provider: source.provider,
    status: source.status,
    realData: source.realData,
    message: source.message
  }))
}

function sanitizePlainObject(value) {
  if (!value || typeof value !== 'object') return value
  const out = {}
  for (const [key, item] of Object.entries(value)) {
    if (FORBIDDEN_KEYS.has(key)) continue
    if (Array.isArray(item)) {
      out[key] = item.slice(0, 20).map(child => sanitizePlainObject(child))
    } else if (item && typeof item === 'object') {
      out[key] = sanitizePlainObject(item)
    } else {
      out[key] = item
    }
  }
  return compactObject(out)
}

function compactObject(value) {
  if (!value || typeof value !== 'object') return value
  return Object.fromEntries(Object.entries(value).filter(([, item]) => {
    if (item == null || item === '') return false
    if (Array.isArray(item) && item.length === 0) return false
    return !(typeof item === 'object' && !Array.isArray(item) && Object.keys(item).length === 0)
  }))
}

function compactList(value) {
  return asArray(value).filter(item => item != null && item !== '')
}

function asArray(value) {
  return Array.isArray(value) ? value : []
}

function toNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : undefined
}
