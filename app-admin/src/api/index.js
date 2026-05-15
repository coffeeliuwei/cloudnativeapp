import axios from 'axios'

// 通过 VUE_APP_BASE_URL 环境变量注入，默认指向本地网关
const baseURL = process.env.VUE_APP_BASE_URL || 'http://localhost:8005'

const postComplexRequest = (url, params) => {
  return axios({
    method: 'post',
    url: `${baseURL}${url}`,
    data: params,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

// 获取快递轨迹列表（按 order_id 过滤）
export const findOrderList = (params) => {
  return postComplexRequest('/findOrderList', params)
}

// 获取订单列表（分页，可按 order_id / member_name 过滤）
export const findOrders = (params) => {
  return postComplexRequest('/findOrders', params)
}

// 创建订单并同步建快递单
export const createOrder = (params) => {
  return postComplexRequest('/createOrder', params)
}
