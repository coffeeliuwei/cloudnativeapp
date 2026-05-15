import { findOrders, createOrder } from '@/api/index'

export default {
  data () {
    return {
      searchForm: {
        pageNum: 1,
        pageSize: 10,
        order_id: '',
        member_name: ''
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
          title: '会员姓名',
          key: 'member_name'
        },
        {
          title: '手机号',
          key: 'member_phone'
        },
        {
          title: '订单状态',
          key: 'order_status'
        },
        {
          title: '金额（元）',
          key: 'order_amount'
        }
      ],
      data: [],
      total: 0,
      // 新建订单弹窗
      showCreate: false,
      creating: false,
      createForm: {
        order_id: '',
        OneID: '',
        order_amount: ''
      },
      createError: ''
    }
  },
  mounted () {
    this.getOrders()
  },
  methods: {
    handleSubmit () {
      this.searchForm.pageNum = 1
      this.getOrders()
    },
    getOrders () {
      findOrders(this.searchForm).then(res => {
        const body = res.data
        if (body && body.success === true) {
          this.data = body.result.list
          this.total = body.result.total
        }
      }).catch(() => {})
    },
    openCreate () {
      this.createForm = { order_id: '', OneID: '', order_amount: '' }
      this.createError = ''
      this.showCreate = true
    },
    handleCreate () {
      if (!this.createForm.order_id || !this.createForm.OneID || !this.createForm.order_amount) {
        this.createError = '请填写所有字段'
        return
      }
      this.creating = true
      this.createError = ''
      createOrder({
        order_id: this.createForm.order_id,
        OneID: this.createForm.OneID,
        order_amount: parseFloat(this.createForm.order_amount)
      }).then(res => {
        this.creating = false
        const body = res.data
        if (body && body.success === true) {
          this.showCreate = false
          this.getOrders()
        } else {
          this.createError = body.message || '创建失败'
        }
      }).catch(err => {
        this.creating = false
        this.createError = err.message || '请求失败'
      })
    }
  }
}
