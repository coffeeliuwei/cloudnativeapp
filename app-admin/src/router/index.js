import { createRouter, createWebHistory } from 'vue-router'
import routes from './routers'
import store from '@/store'
import { LoadingBar } from 'view-ui-plus'
import { setToken, getToken, canTurnTo, setTitle } from '@/libs/util'
import i18n from '@/locale'
import config from '@/config'
const { homeName } = config

const router = createRouter({
  routes,
  history: createWebHistory()
})

const LOGIN_PAGE_NAME = 'login'

const turnTo = (to, access, next) => {
  if (canTurnTo(to.name, access, routes)) next() // 有权限，可访问
  else next({ replace: true, name: 'error_401' }) // 无权限，重定向到401页面
}

router.beforeEach((to, from, next) => {
  LoadingBar.start()
  const token = getToken()
  if (!token && to.name !== LOGIN_PAGE_NAME) {
    // 未登录且要跳转的页面不是登录页
    next({
      name: LOGIN_PAGE_NAME // 跳转到登录页
    })
  } else if (!token && to.name === LOGIN_PAGE_NAME) {
    // 未登陆且要跳转的页面是登录页
    next() // 跳转
  } else if (token && to.name === LOGIN_PAGE_NAME) {
    // 已登录且要跳转的页面是登录页
    next({
      name: homeName // 跳转到homeName页
    })
  } else {
    if (store.state.user.hasGetInfo) {
      turnTo(to, store.state.user.access, next)
    } else {
      store.dispatch('getUserInfo').then(user => {
        // 拉取用户信息，通过用户权限和跳转的页面的name来判断是否有权限访问
        turnTo(to, user.access, next)
      }).catch(() => {
        setToken('')
        next({
          name: 'login'
        })
      })
    }
  }
})

router.afterEach(to => {
  LoadingBar.finish()
  // Vue 3 中 router.app 不再是组件实例，用 i18n.global 代替
  setTitle(to, { $t: i18n.global.t.bind(i18n.global) })
  window.scrollTo(0, 0)
})

export default router
