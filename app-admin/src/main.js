import { createApp } from 'vue'
import App from './App'
import router from './router'
import store from './store'
import ViewUIPlus from 'view-ui-plus'
import i18n from '@/locale'
import config from '@/config'
import importDirective from '@/directive'
import installPlugin from '@/plugin'
import './index.less'
import '@/assets/icons/iconfont.css'
import 'view-ui-plus/dist/styles/viewuiplus.css'


const app = createApp(App)

// 注册 ViewUI Plus，并接入 vue-i18n
app.use(ViewUIPlus, {
  i18n: (key, value) => i18n.global.t(key, value)
})

app.use(router)
app.use(store)
app.use(i18n)

// 全局注册应用配置（Vue 3 用 globalProperties 替代 Vue.prototype）
app.config.globalProperties.$config = config

// 注册自定义指令
importDirective(app)

// 注册 admin 内置插件
installPlugin(app)

app.mount('#app')
