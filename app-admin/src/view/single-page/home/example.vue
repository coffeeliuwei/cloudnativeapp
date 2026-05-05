<template>
    <div ref="dom"></div>
</template>

<script>
import echarts from 'echarts'
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
        data: ['早晨 7-9点', '上午 9-11点', '午间 11-13点', '下午 13-17点', '傍晚 17-20点'],
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
          name: '早晨 7-9点',
          type: 'line',
          stack: '总量',
          areaStyle: { normal: { color: 'rgba(200, 130, 90, 0.6)' } },
          lineStyle: { color: '#c8825a' },
          itemStyle: { color: '#c8825a' },
          data: [98, 112, 87, 105, 143, 89, 76]
        },
        {
          name: '上午 9-11点',
          type: 'line',
          stack: '总量',
          areaStyle: { normal: { color: 'rgba(124, 74, 45, 0.5)' } },
          lineStyle: { color: '#7c4a2d' },
          itemStyle: { color: '#7c4a2d' },
          data: [210, 245, 198, 227, 312, 178, 156]
        },
        {
          name: '午间 11-13点',
          type: 'line',
          stack: '总量',
          areaStyle: { normal: { color: 'rgba(106, 158, 114, 0.5)' } },
          lineStyle: { color: '#6a9e72' },
          itemStyle: { color: '#6a9e72' },
          data: [345, 398, 312, 367, 421, 289, 265]
        },
        {
          name: '下午 13-17点',
          type: 'line',
          stack: '总量',
          areaStyle: { normal: { color: 'rgba(245, 158, 11, 0.4)' } },
          lineStyle: { color: '#f59e0b' },
          itemStyle: { color: '#f59e0b' },
          data: [287, 315, 276, 298, 356, 412, 389]
        },
        {
          name: '傍晚 17-20点',
          type: 'line',
          stack: '总量',
          label: {
            normal: { show: true, position: 'top', color: '#9d7a5e' }
          },
          areaStyle: { normal: { color: 'rgba(157, 122, 94, 0.35)' } },
          lineStyle: { color: '#9d7a5e' },
          itemStyle: { color: '#9d7a5e' },
          data: [198, 267, 234, 189, 312, 445, 398]
        }
      ]
    }
    this.$nextTick(() => {
      this.dom = echarts.init(this.$refs.dom)
      this.dom.setOption(option)
      on(window, 'resize', this.resize)
    })
  },
  beforeDestroy () {
    off(window, 'resize', this.resize)
  }
}
</script>
