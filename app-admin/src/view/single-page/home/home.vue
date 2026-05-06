<template>
  <div>
    <Row :gutter="20">
      <i-col :xs="12" :md="8" :lg="4" v-for="(infor, i) in inforCardData" :key="`infor-${i}`" style="height: 120px;padding-bottom: 10px;">
        <infor-card shadow :color="infor.color" :icon="infor.icon" :icon-size="36">
          <count-to :end="infor.count" count-class="count-style"/>
          <p>{{ infor.title }}</p>
        </infor-card>
      </i-col>
    </Row>
    <Row :gutter="20" style="margin-top: 10px;">
      <i-col :md="24" :lg="8" style="margin-bottom: 20px;">
        <Card shadow>
          <template #title><p class="card-title">订单状态分布</p></template>
          <chart-pie style="height: 300px;" :value="pieData" text="当前订单"></chart-pie>
        </Card>
      </i-col>
      <i-col :md="24" :lg="16" style="margin-bottom: 20px;">
        <Card shadow>
          <template #title><p class="card-title">本周每日订单量</p></template>
          <chart-bar style="height: 300px;" :value="barData" text="订单数"/>
        </Card>
      </i-col>
    </Row>
    <Row>
      <Card shadow>
        <template #title><p class="card-title">各时段订单量趋势（本周）</p></template>
        <example style="height: 310px;"/>
      </Card>
    </Row>
  </div>
</template>

<script>
import InforCard from '_c/info-card'
import CountTo from '_c/count-to'
import { ChartPie, ChartBar } from '_c/charts'
import Example from './example.vue'
export default {
  name: 'home',
  components: {
    InforCard,
    CountTo,
    ChartPie,
    ChartBar,
    Example
  },
  data () {
    return {
      inforCardData: [
        { title: '订单总数', icon: 'md-list-box', count: 8426, color: '#c8825a' },
        { title: '配送中', icon: 'md-bicycle', count: 312, color: '#f59e0b' },
        { title: '已完成', icon: 'md-checkmark-circle', count: 7958, color: '#6a9e72' },
        { title: '待处理', icon: 'md-time', count: 156, color: '#7c4a2d' },
        { title: '总金额(¥)', icon: 'md-card', count: 298640, color: '#9d7a5e' },
        { title: '会员总数', icon: 'md-people', count: 3201, color: '#bda998' }
      ],
      pieData: [
        { value: 156, name: '待处理' },
        { value: 312, name: '配送中' },
        { value: 7958, name: '已完成' },
        { value: 98, name: '已取消' },
        { value: 24, name: '异常' }
      ],
      barData: {
        Mon: 1021,
        Tue: 1354,
        Wed: 1189,
        Thu: 982,
        Fri: 1476,
        Sat: 1832,
        Sun: 1654
      }
    }
  },
  mounted () {
    //
  }
}
</script>

<style lang="less">
.count-style {
  font-size: 50px;
  color: #7c4a2d;
}

.ivu-card {
  border-color: #e8d8c4;
}

.card-title {
  color: #7c4a2d;
  font-weight: 700;
  font-size: 14px;
}
</style>
