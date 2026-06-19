<template>
  <div class="trip-report" v-if="plan">
    <el-card v-if="plan.feasibility" class="section feasibility-card">
      <template #header>合理性判断</template>
      <div class="feasibility-head">
        <el-tag :type="feasibilityType(plan.feasibility.level)" size="large">{{ plan.feasibility.level }}</el-tag>
        <strong>{{ plan.feasibility.feasible ? '推荐执行' : '建议调整后再执行' }}</strong>
        <span>推荐天数：{{ plan.feasibility.recommendedDays || plan.recommendedDays || '-' }} 天</span>
      </div>
      <div class="feasibility-grid">
        <div>
          <h3>问题提示</h3>
          <ul><li v-for="warning in plan.feasibility.warnings || []" :key="warning">{{ warning }}</li></ul>
        </div>
        <div>
          <h3>优化建议</h3>
          <ul><li v-for="suggestion in plan.feasibility.suggestions || []" :key="suggestion">{{ suggestion }}</li></ul>
        </div>
      </div>
    </el-card>

    <el-card class="section">
      <template #header>主路线与城市段</template>
      <div class="main-route-line">
        <span>{{ plan.fromCity || '-' }}</span>
        <b>→</b>
        <span>{{ plan.toCity || '-' }}</span>
      </div>
      <div v-if="plan.citySegments?.length" class="city-segment-grid">
        <div v-for="segment in plan.citySegments" :key="segment.segmentType + segment.cityName" class="city-segment-card">
          <el-tag :type="segment.segmentType === '跨城市扩展' ? 'warning' : 'success'">{{ segment.segmentType }}</el-tag>
          <h3>{{ segment.cityName }}</h3>
          <p>{{ segment.travelNotice }}</p>
          <p class="meta">建议停留：{{ segment.recommendedStayDays || '-' }} 天</p>
        </div>
      </div>
    </el-card>

    <el-card class="summary-card">
      <div class="summary-layout">
        <div>
          <h2>{{ plan.title || '智慧旅游计划' }}</h2>
          <p>{{ plan.summary || '暂无摘要。' }}</p>
          <p class="budget-conclusion">{{ plan.budgetConclusion }}</p>
        </div>
        <div class="summary-stats">
          <div><span>预算</span><strong>{{ plan.budget || 0 }} 元</strong></div>
          <div><span>人数</span><strong>{{ plan.peopleCount || '-' }} 人</strong></div>
          <div><span>推荐天数</span><strong>{{ plan.recommendedDays || '-' }} 天</strong></div>
        </div>
      </div>
    </el-card>

    <el-card class="section">
      <template #header>每日行程</template>
      <el-timeline v-if="plan.dailyPlans?.length">
        <el-timeline-item v-for="day in plan.dailyPlans" :key="day.day" :timestamp="`${day.title || ''} ${day.date || ''}`">
          <div v-for="activity in day.activities || []" :key="activity.period + activity.title" class="activity-item">
            <el-tag size="small">{{ activity.period }}</el-tag>
            <strong>{{ activity.title }}</strong>
            <p>{{ activity.description }}</p>
            <p class="meta">{{ activity.reason }} · {{ activity.costNote }}</p>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="该行程没有保存每日安排。" />
    </el-card>

    <div class="result-grid section">
      <el-card>
        <template #header>预算明细</template>
        <el-table :data="budgetRows" border>
          <el-table-column prop="item" label="项目" min-width="120" />
          <el-table-column prop="amount" label="金额 / 状态" min-width="140" />
          <el-table-column prop="source" label="来源说明" min-width="220" />
        </el-table>
      </el-card>
      <el-card>
        <template #header>天气建议</template>
        <template v-if="plan.weatherSummary">
          <p><strong>{{ plan.weatherSummary.city }}</strong> {{ plan.weatherSummary.weather }}，{{ plan.weatherSummary.temperature }}℃</p>
          <p class="meta">湿度 {{ plan.weatherSummary.humidity }}%，{{ plan.weatherSummary.windDirection }}风 {{ plan.weatherSummary.windPower }}</p>
          <el-alert :title="plan.weatherSummary.travelTip" type="success" show-icon :closable="false" />
        </template>
        <el-empty v-else description="该行程未保存天气摘要。" />
      </el-card>
    </div>

    <el-card v-if="budgetReference" class="section budget-reference-card">
      <template #header>非实时预算参考</template>
      <div class="budget-reference-head">
        <div>
          <h3>{{ budgetReference.budgetLevel }}</h3>
          <p class="meta">保存时预算参考区间：约 CNY {{ budgetReference.estimatedMin }} - {{ budgetReference.estimatedMax }}，典型参考约 CNY {{ budgetReference.estimatedTypical }}</p>
        </div>
        <div class="source-tags">
          <el-tag type="warning">预算参考价</el-tag>
          <el-tag type="info">非实时</el-tag>
          <el-tag type="info">{{ budgetReference.priceMode }}</el-tag>
        </div>
      </div>
      <div class="price-estimate-grid">
        <div v-for="item in budgetReference.items" :key="item.category" class="price-estimate-card">
          <strong>{{ priceCategoryName(item.category) }}</strong>
          <p class="price-range">约 CNY {{ item.minPrice }} - {{ item.maxPrice }}</p>
          <p class="meta">典型参考：CNY {{ item.typicalPrice }} · {{ item.unit }}</p>
          <ul><li v-for="basis in item.basis" :key="basis">{{ basis }}</li></ul>
        </div>
      </div>
      <el-alert class="table-note" :title="budgetReference.notice" type="warning" show-icon :closable="false" />
    </el-card>

    <el-card class="section">
      <template #header>推荐景点</template>
      <div v-if="plan.recommendedSpots?.length" class="spot-grid compact">
        <div v-for="spot in plan.recommendedSpots.slice(0, 8)" :key="spot.id || spot.name" class="mini-spot">
          <div v-if="spot.photoUrl" class="mini-photo" :style="{ backgroundImage: `url(${spot.photoUrl})` }"></div>
          <div v-else class="mini-photo placeholder">暂无官方图片</div>
          <h3>{{ spot.name }}</h3>
          <p class="meta">{{ spot.cityName || plan.toCity }} · {{ spot.type }}</p>
          <p>{{ spot.recommendationReason }}</p>
        </div>
      </div>
      <el-empty v-else description="该行程没有保存推荐景点。" />
    </el-card>

    <el-card v-if="plan.dataSources?.length" class="section">
      <template #header>数据来源</template>
      <div class="source-tags">
        <el-tag v-for="source in plan.dataSources" :key="source.name + source.status" :type="source.status === '真实接口返回' ? 'success' : 'warning'">
          {{ source.name }}：{{ source.status }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  plan: {
    type: Object,
    required: true
  }
})

const budgetRows = computed(() => {
  const budget = props.plan?.budgetSummary
  if (!budget) return []
  return [
    { item: '往返交通', amount: budget.transportRealCost ? `CNY ${budget.transportRealCost}` : '未接入实时票价', source: budget.transportStatus || '请自行查询官方平台' },
    { item: '住宿', amount: budget.hotelEstimate ? `约 CNY ${budget.hotelEstimate}` : '未估算', source: '用户预算规则估算，不是实时房价' },
    { item: '餐饮', amount: `约 CNY ${budget.foodEstimate || 0}`, source: '规则估算' },
    { item: '市内交通', amount: `约 CNY ${budget.localTrafficEstimate || 0}`, source: '规则估算' },
    { item: '门票', amount: budget.ticketEstimate || '以景区公告为准', source: '以景区官方公告为准' },
    { item: '预算余额', amount: `约 CNY ${budget.remainingBudget || 0}`, source: '规则估算后的参考值' }
  ]
})

const budgetReference = computed(() => props.plan?.budgetSummary?.budgetReference || null)

function feasibilityType(level) {
  if (level === '合理') return 'success'
  if (level === '偏紧') return 'warning'
  if (level === '不建议') return 'danger'
  return 'info'
}

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
</script>
