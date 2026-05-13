import Mock from 'mockjs'

const trackSteps = ['已揽收', '运输中', '派送中', '已签收']

const makeOrder = (orderId) => ({
  order_id: orderId || Mock.Random.integer(100000, 999999),
  express_id: Mock.mock(/SF[0-9]{12}/),
  express_weight: parseFloat(Mock.Random.float(0.1, 30, 1, 1)),
  track_show: trackSteps[Mock.Random.integer(0, 3)]
})

export const findOrderList = (options) => {
  let body = {}
  try { body = JSON.parse(options.body) } catch (e) { /* ignore */ }

  const { order_id, pageSize = 10 } = body

  // 有订单号时只返回精确匹配的一条
  if (order_id) {
    const matched = makeOrder(parseInt(order_id))
    return {
      success: true,
      result: { list: [matched], total: 1 }
    }
  }

  const list = Array.from({ length: pageSize }, () => makeOrder())
  return {
    success: true,
    result: {
      list,
      total: Mock.Random.integer(50, 200)
    }
  }
}
