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

// 获取订单列表（含快递轨迹）
export const findOrderList = (params) => {
  return postComplexRequest('/findOrderList', params)
}
