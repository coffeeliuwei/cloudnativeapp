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
          <p slot="title" class="card-title">咖啡品类销售占比</p>
          <chart-pie style="height: 300px;" :value="pieData" text="今日销售"></chart-pie>
        </Card>
      </i-col>
      <i-col :md="24" :lg="16" style="margin-bottom: 20px;">
        <Card shadow>
          <p slot="title" class="card-title">本周每日订单量</p>
          <chart-bar style="height: 300px;" :value="barData" text="订单数"/>
        </Card>
      </i-col>
    </Row>
    <Row>
      <Card shadow>
        <p slot="title" class="card-title">各时段订单趋势（本周）</p>
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
        { title: '今日订单', icon: 'md-cafe', count: 1284, color: '#c8825a' },
        { title: '待配送', icon: 'md-bicycle', count: 47, color: '#f59e0b' },
        { title: '今日完成', icon: 'md-checkmark-circle', count: 1237, color: '#6a9e72' },
        { title: '总用户数', icon: 'md-people', count: 8903, color: '#7c4a2d' },
        { title: '今日收入', icon: 'md-card', count: 9680, color: '#9d7a5e' },
        { title: '好评率%', icon: 'md-star', count: 98, color: '#bda998' }
      ],
      pieData: [
        { value: 512, name: '拿铁' },
        { value: 389, name: '美式' },
        { value: 201, name: '卡布奇诺' },
        { value: 134, name: '摩卡' },
        { value: 48, name: '其他' }
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
