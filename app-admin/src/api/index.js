import axios from 'axios'

// 统一请求路径前缀
// 生产环境通过 VUE_APP_BASE_URL 环境变量注入，开发默认指向本地网关
const baseURL = process.env.VUE_APP_BASE_URL || 'http://localhost:8005'

const postComplexRequest = (url, params) => {
  return axios({
    method: 'post',
    url: `${url}`,
    data: params,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

// 获取订单列表（含快递轨迹）
export const findOrderList = (params) => {
  return postComplexRequest(`${baseURL}/findOrderList`, params)
}
