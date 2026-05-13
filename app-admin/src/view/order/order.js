import {
  findOrderList
  } from '@/api/index'
  import Util from '@/libs/util'
  export default {
    data () {
      return {
        searchForm: {
          pageNum: 1,
          pageSize: 10,
          order_id: ''
        },
        columns: [
          {
            type: 'index',
            width: 60,
            align: 'center',
            fixed: 'left'
          },
          {
            title: '订单编号',
            key: 'order_id'
          },
          {
            title: '快递编号',
            key: 'express_id'
          },
          {
            title: '重量',
            key: 'express_weight'
          },
          {
            title: '轨迹',
            key: 'track_show'
          }
        ],
        data: [],
        total: 0,
        chooseItem: false,
        showLuggage: false,
        detailLoading: true
      }
    },
    mounted () {
      this.init()
    },
    methods: {
      init () {
        this.getOrderList()
      },
      // 点击搜索按钮
      handleSubmit () {

        this.searchForm.pageNum = 1
        this.searchForm.pageSize = 10
        this.getOrderList()
      },
      getOrderList () {
        findOrderList(this.searchForm).then(res => {
          this.loading = false
          const body = res.data
          if (body && body.success === true) {
            this.data = body.result.list
            this.total = body.result.total
          }
        }).catch(() => {
          this.loading = false
        })
      },
      getTrip (row) {
        this.chooseItem = row
      }
    }
  }
  