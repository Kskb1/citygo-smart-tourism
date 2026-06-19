<template>
  <el-card>
    <template #header>
      <div>
        <strong>{{ title }}</strong>
        <el-tag size="small" :type="result?.realData ? 'success' : 'warning'" style="margin-left: 8px">
          {{ result?.realData ? '真实接口返回' : '未获取到真实数据' }}
        </el-tag>
      </div>
    </template>
    <p class="meta">数据来源：{{ result?.sourceName || '-' }}</p>
    <p class="meta">获取时间：{{ result?.fetchedAt || '-' }}</p>
    <el-alert v-if="result?.errorMessage" :title="result.errorMessage" type="warning" show-icon :closable="false" />
    <div v-if="result?.data && result?.data !== result?.rawJson" class="structured-preview">
      <pre>{{ JSON.stringify(result.data, null, 2) }}</pre>
    </div>
    <el-collapse v-if="result?.rawJson">
      <el-collapse-item title="原始接口数据（调试用）" name="raw">
        <pre>{{ JSON.stringify(result.rawJson, null, 2) }}</pre>
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<script setup>
defineProps({
  title: String,
  result: Object
})
</script>
