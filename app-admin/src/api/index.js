import axios from 'axios'
// 定义接口请求方法
const postComplexRequest = (url, params) => {
    alert(url)
  return axios({
    method: 'post',
    url: `${url}`,
    data: params,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}
// 统一请求路径前缀
var baseURL = 'http://139.224.195.42'
// 获取订单列表
export const findOrderList = (params) => {
    return postComplexRequest(`${baseURL}` + '/findOrderList', params)
}
