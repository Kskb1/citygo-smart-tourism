const AIRPORT_TERMINAL_PATTERN = /(T\d|航站楼|候机楼|出发层|到达层|停车场|停车楼|停车区|地铁站|公交站|巴士|餐饮|服务|贵宾|值机|安检|货运|物流|宾馆|酒店|派出所|加油站|收费站|入口|出口)/i
const AIRPORT_MAIN_PATTERN = /(国际机场|机场)$/
const GENERAL_AIRPORT_PATTERN = /(通用机场|通航|直升机场|航空运动|汉南)/i
const HOTEL_EXCLUDE_PATTERN = /(停车场|停车楼|停车区|入口|出口|餐厅|美食|咖啡|便利店|超市|商店|洗手间|前台|大堂|会议室|宴会厅|公交站|地铁站|充电站|加油站)/i

const CITY_DISTANCE_KM = {
  '成都-武汉': 1100,
  '武汉-成都': 1100,
  '成都-上海': 1950,
  '上海-成都': 1950,
  '北京-上海': 1200,
  '上海-北京': 1200,
  '广州-杭州': 1250,
  '杭州-广州': 1250,
  '西安-南京': 1050,
  '南京-西安': 1050,
  '武汉-上海': 830,
  '上海-武汉': 830
}

const HOTEL_TIER_1 = ['北京', '上海', '广州', '深圳']
const HOTEL_TIER_2 = ['成都', '武汉', '南京', '杭州', '西安', '重庆', '天津', '苏州', '青岛', '厦门', '长沙']

const HOTEL_PRICE_RULES = {
  tier1: {
    economy: [260, 460],
    comfort: [420, 760],
    upscale: [680, 1280],
    luxury: [1100, 2600]
  },
  tier2: {
    economy: [180, 350],
    comfort: [300, 560],
    upscale: [500, 920],
    luxury: [820, 1800]
  },
  tier3: {
    economy: [140, 280],
    comfort: [240, 460],
    upscale: [420, 780],
    luxury: [680, 1400]
  }
}

export function getPoiList(payload) {
  if (!payload?.realData || !Array.isArray(payload.data)) return []
  return payload.data
}

export function normalizeText(value = '') {
  return String(value).replace(/\s+/g, '').trim()
}

function stableKey(item) {
  return item.id || `${normalizeText(item.name)}-${normalizeText(item.address)}-${normalizeText(item.locationText)}`
}

export function classifyAirports(airports = []) {
  const seen = new Map()
  airports.forEach(item => {
    if (!item?.name) return
    const key = stableKey(item)
    if (!seen.has(key)) seen.set(key, item)
  })

  const main = []
  const terminal = []
  const general = []
  const related = []

  for (const item of seen.values()) {
    const name = item.name || ''
    const text = `${name} ${item.typeName || ''} ${item.address || ''}`
    if (GENERAL_AIRPORT_PATTERN.test(text)) {
      general.push({ ...item, airportKind: '通用机场', displayMode: '真实地点' })
    } else if (AIRPORT_TERMINAL_PATTERN.test(text) && !AIRPORT_MAIN_PATTERN.test(name)) {
      terminal.push({ ...item, airportKind: '航站楼/配套设施', displayMode: '高德 POI' })
    } else if (AIRPORT_MAIN_PATTERN.test(name) || name.includes('机场')) {
      main.push({ ...item, airportKind: '民航机场', displayMode: '真实地点' })
    } else {
      related.push({ ...item, airportKind: '机场相关地点', displayMode: '高德 POI' })
    }
  }

  const sortPoi = (a, b) => Number(Boolean(b.locationText)) - Number(Boolean(a.locationText)) || a.name.localeCompare(b.name, 'zh-CN')
  return {
    main: main.sort(sortPoi),
    terminal: terminal.sort(sortPoi),
    general: general.sort(sortPoi),
    related: related.sort(sortPoi)
  }
}

export function dedupeHotels(hotels = []) {
  const seen = new Map()
  hotels.forEach(item => {
    if (!item?.name) return
    const text = `${item.name} ${item.typeName || ''} ${item.address || ''}`
    if (HOTEL_EXCLUDE_PATTERN.test(text)) return
    const key = stableKey(item)
    if (!seen.has(key)) seen.set(key, { ...item, displayMode: '真实地点', priceMode: '无实时房价' })
  })
  return [...seen.values()]
}

export function estimateDistance(fromCity, toCity) {
  const key = `${fromCity}-${toCity}`
  if (CITY_DISTANCE_KM[key]) return CITY_DISTANCE_KM[key]
  if (!fromCity || !toCity || fromCity === toCity) return 60
  return 900
}

function rangeByRate(distance, lowRate, highRate, minLow, minHigh) {
  const low = Math.max(minLow, Math.round((distance * lowRate) / 10) * 10)
  const high = Math.max(minHigh, Math.round((distance * highRate) / 10) * 10)
  return [low, Math.max(high, low + 80)]
}

export function buildTransportBudget({ fromCity, toCity, preference = 'compare', people = 1 }) {
  const distance = estimateDistance(fromCity, toCity)
  const candidates = [
    {
      key: 'flight',
      name: '飞机',
      icon: '航',
      range: rangeByRate(distance, 0.55, 1.15, 360, 680),
      duration: distance > 1200 ? '约 2.5-4 小时空中交通，另需机场接驳' : '约 1.5-3 小时空中交通，另需机场接驳',
      note: '非实时机票价格，仅按城市距离和常见预算区间估算。'
    },
    {
      key: 'high-speed',
      name: '高铁',
      icon: '铁',
      range: rangeByRate(distance, 0.36, 0.55, 180, 330),
      duration: distance > 1000 ? '约 5-8 小时，实际以 12306 为准' : '约 3-6 小时，实际以 12306 为准',
      note: '不生成车次和余票，点击官方入口查询。'
    },
    {
      key: 'normal-train',
      name: '普通火车',
      icon: '普',
      range: rangeByRate(distance, 0.16, 0.32, 90, 180),
      duration: distance > 1000 ? '约 10-18 小时，实际以 12306 为准' : '约 7-14 小时，实际以 12306 为准',
      note: '仅用于预算粗估，不代表实时票价。'
    },
    {
      key: 'self-driving',
      name: '自驾',
      icon: '车',
      range: rangeByRate(distance, 0.72, 1.05, 220, 420),
      duration: distance > 1000 ? '约 11-14 小时车程，需考虑休息' : '约 7-11 小时车程，需考虑休息',
      note: '包含油费/电费与通行费的经验区间，真实路线以高德为准。'
    }
  ]

  const filtered = preference === 'compare' ? candidates : candidates.filter(item => item.key === preference)
  return filtered.map(item => ({
    ...item,
    distance,
    people,
    priceMode: 'RULE_ESTIMATED',
    rangeText: `约 ${item.range[0]}-${item.range[1]} 元/人`,
    totalText: people > 1 ? `约 ${item.range[0] * people}-${item.range[1] * people} 元/${people}人` : `约 ${item.range[0]}-${item.range[1]} 元/人`
  }))
}

export function buildHotelBudget({ city, grade = 'comfort', nights = 1, rooms = 1 }) {
  const tier = HOTEL_TIER_1.includes(city) ? 'tier1' : HOTEL_TIER_2.includes(city) ? 'tier2' : 'tier3'
  const range = HOTEL_PRICE_RULES[tier][grade] || HOTEL_PRICE_RULES[tier].comfort
  const safeNights = Math.max(1, Number(nights) || 1)
  const safeRooms = Math.max(1, Number(rooms) || 1)
  return {
    cityTier: tier,
    priceMode: 'RULE_ESTIMATED',
    rangeText: `约 ${range[0]}-${range[1]} 元/间/晚`,
    totalText: `总预算约 ${range[0] * safeNights * safeRooms}-${range[1] * safeNights * safeRooms} 元`,
    note: `按${safeNights}晚、${safeRooms}间房估算。该预算不是酒店实时房价，真实价格请以酒店或正规预订平台为准。`
  }
}

export function nightsBetween(checkIn, checkOut) {
  if (!checkIn || !checkOut) return 1
  const start = new Date(checkIn)
  const end = new Date(checkOut)
  const diff = Math.round((end.getTime() - start.getTime()) / 86400000)
  return Math.max(1, Number.isFinite(diff) ? diff : 1)
}
