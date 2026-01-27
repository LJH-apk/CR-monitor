<template>
  <el-card class="alert-chart">
    <template #header>
      <span>预警统计</span>
    </template>

    <div class="charts-container">
      <!-- 按小时统计 -->
      <div class="chart-item">
        <h4>24小时预警趋势</h4>
        <v-chart :option="hourlyChartOption" style="height: 300px" />
      </div>

      <!-- 按行为类型统计 -->
      <div class="chart-item">
        <h4>预警类型分布</h4>
        <v-chart :option="behaviorChartOption" style="height: 300px" />
      </div>

      <!-- 按严重等级统计 -->
      <div class="chart-item">
        <h4>严重等级分布</h4>
        <v-chart :option="severityChartOption" style="height: 300px" />
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import type { DashboardStats } from '@/types/alert'

use([
  CanvasRenderer,
  BarChart,
  PieChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const props = defineProps<{
  stats: DashboardStats | null
}>()

const hourlyChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis'
  },
  xAxis: {
    type: 'category',
    data: props.stats?.alertsByHour.map(item => item.hour) || []
  },
  yAxis: {
    type: 'value'
  },
  series: [{
    data: props.stats?.alertsByHour.map(item => item.count) || [],
    type: 'line',
    smooth: true,
    areaStyle: {
      color: 'rgba(64, 158, 255, 0.2)'
    },
    itemStyle: {
      color: '#409EFF'
    }
  }]
}))

const behaviorChartOption = computed(() => ({
  tooltip: {
    trigger: 'item'
  },
  legend: {
    orient: 'vertical',
    left: 'left'
  },
  series: [{
    type: 'pie',
    radius: '50%',
    data: Object.entries(props.stats?.alertsByBehavior || {}).map(([name, value]) => ({
      name,
      value
    })),
    emphasis: {
      itemStyle: {
        shadowBlur: 10,
        shadowOffsetX: 0,
        shadowColor: 'rgba(0, 0, 0, 0.5)'
      }
    }
  }]
}))

const severityChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    }
  },
  xAxis: {
    type: 'category',
    data: Object.keys(props.stats?.alertsBySeverity || {})
  },
  yAxis: {
    type: 'value'
  },
  series: [{
    data: Object.values(props.stats?.alertsBySeverity || {}),
    type: 'bar',
    itemStyle: {
      color: '#67C23A'
    }
  }]
}))
</script>

<style scoped>
.alert-chart {
  margin-top: 20px;
}

.charts-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
}

.chart-item h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
}
</style>
