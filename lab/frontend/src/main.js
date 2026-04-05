import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/css/global.css'
import request from './utils/request'

const app = createApp(App)
const pinia = createPinia()

// 全局属性
app.config.globalProperties.$request = request

app.use(pinia)
app.use(router)
app.use(ElementPlus)

app.mount('#app')