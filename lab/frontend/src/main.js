import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useSettingsStore } from './stores/settings'
import './assets/css/global.css'
import './assets/css/mobile.css'
import './assets/css/element-plus-programmatic.js'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// 主题模式、界面密度与主色在挂载前落到 DOM
useSettingsStore().applyToDocument()

app.mount('#app')