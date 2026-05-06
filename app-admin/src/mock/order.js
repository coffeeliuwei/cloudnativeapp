import Mock from 'mockjs'

const statusMap = ['待支付', '已支付', '已发货', '已完成', '已取消']

export const findOrderList = () => {
  const list = Mock.mock({
    'list|10-20': [{
      'orderId|+1': 1000,
      orderNo: /ORD-20[2-9][0-9][0-1][0-9][0-3][0-9]-[0-9]{6}/,
      'userId|100-999': 1,
      userName: '@cname',
      'totalAmount|50-9999.2': 1,
      'status|0-4': 0,
      createTime: '@datetime("yyyy-MM-dd HH:mm:ss")',
      'itemCount|1-10': 1
    }]
  })

  return {
    code: 200,
    message: 'success',
    data: list.list.map(item => ({
      ...item,
      statusText: statusMap[item.status]
    }))
  }
}
