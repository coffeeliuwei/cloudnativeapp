import Mock from 'mockjs'
import { login, logout, getUserInfo } from './login'
import { getTableData, getDragList, uploadImage, getOrgData, getTreeSelectData } from './data'
import { getMessageInit, getContentByMsgId, hasRead, removeReaded, restoreTrash, messageCount } from './user'
import { findOrderList } from './order'

// ===== mockjs + axios 1.x 兼容补丁 =====
// axios 1.x 用 addEventListener('load') 代替 onload 属性；
// mockjs 的 MockXMLHttpRequest 不实现 addEventListener，导致触发 onerror。
// 补丁：在 Mock 拦截前把 addEventListener 代理到对应的 on* 属性。
const _open = XMLHttpRequest.prototype.open
XMLHttpRequest.prototype.open = function (...args) {
  this.addEventListener = (event, handler) => {
    this['on' + event] = handler
  }
  return _open.apply(this, args)
}

Mock.setup({ timeout: '10-50' })

// 登录相关和获取用户信息
Mock.mock(/\/login/, login)
Mock.mock(/\/get_info/, getUserInfo)
Mock.mock(/\/logout/, logout)
Mock.mock(/\/get_table_data/, getTableData)
Mock.mock(/\/get_drag_list/, getDragList)
Mock.mock(/\/save_error_logger/, 'success')
Mock.mock(/\/image\/upload/, uploadImage)
Mock.mock(/\/message\/init/, getMessageInit)
Mock.mock(/\/message\/content/, getContentByMsgId)
Mock.mock(/\/message\/has_read/, hasRead)
Mock.mock(/\/message\/remove_readed/, removeReaded)
Mock.mock(/\/message\/restore/, restoreTrash)
Mock.mock(/\/message\/count/, messageCount)
Mock.mock(/\/get_org_data/, getOrgData)
Mock.mock(/\/get_tree_select_data/, getTreeSelectData)

// 订单查询接口（开发环境后端未启动时使用 mock 数据）
Mock.mock(/\/findOrderList/, findOrderList)

export default Mock
