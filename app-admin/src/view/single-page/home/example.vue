<template>
    <div ref="dom"></div>
</template>

<script>
import * as echarts from 'echarts'
import { on, off } from '@/libs/tools'
export default {
  name: 'serviceRequests',
  data () {
    return {
      dom: null
    }
  },
  methods: {
    resize () {
      this.dom.resize()
    }
  },
  mounted () {
    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          label: { backgroundColor: '#9d7a5e' }
        }
      },
      legend: {
        data: ['待处理', '配送中', '已完成', '已取消'],
        textStyle: { color: '#9d7a5e' }
      },
      grid: {
        top: '12%',
        left: '1.2%',
        right: '1%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          boundaryGap: false,
          data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
          axisLine: { lineStyle: { color: '#e8d8c4' } },
          axisLabel: { color: '#9d7a5e' }
        }
      ],
      yAxis: [
        {
          type: 'value',
          axisLine: { lineStyle: { color: '#e8d8c4' } },
          axisLabel: { color: '#9d7a5e' },
          splitLine: { lineStyle: { color: '#f5ede3' } }
        }
      ],
      series: [
        {
          name: '待处理',
          type: 'line',
          stack: '总量',
          areaStyle: { color: 'rgba(124, 74, 45, 0.5)' },
          lineStyle: { color: '#7c4a2d' },
          itemStyle: { color: '#7c4a2d' },
          data: [32, 28, 45, 21, 38, 19, 24]
        },
        {
          name: '配送中',
          type: 'line',
          stack: '总量',
          areaStyle: { color: 'rgba(245, 158, 11, 0.45)' },
          lineStyle: { color: '#f59e0b' },
          itemStyle: { color: '#f59e0b' },
          data: [187, 245, 198, 227, 312, 278, 256]
        },
        {
          name: '已完成',
          type: 'line',
          stack: '总量',
          areaStyle: { color: 'rgba(106, 158, 114, 0.5)' },
          lineStyle: { color: '#6a9e72' },
          itemStyle: { color: '#6a9e72' },
          data: [756, 1045, 912, 698, 1089, 1487, 1342]
        },
        {
          name: '已取消',
          type: 'line',
          stack: '总量',
          label: { show: true, position: 'top', color: '#9d7a5e' },
          areaStyle: { color: 'rgba(200, 130, 90, 0.35)' },
          lineStyle: { color: '#c8825a' },
          itemStyle: { color: '#c8825a' },
          data: [12, 18, 9, 15, 22, 11, 8]
        }
      ]
    }
    this.$nextTick(() => {
      this.dom = echarts.init(this.$refs.dom)
      this.dom.setOption(option)
      on(window, 'resize', this.resize)
    })
  },
  beforeUnmount () {
    off(window, 'resize', this.resize)
  }
}
</script>
