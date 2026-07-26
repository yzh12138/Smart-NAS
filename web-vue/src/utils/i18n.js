import { ref, computed } from 'vue'
import zh from '../assets/i18n/zh.json'
import en from '../assets/i18n/en.json'

const messages = { zh, en }
const currentLang = ref(localStorage.getItem('language') || 'zh')

function translate(key, params = {}) {
  const keys = key.split('.')
  let value = messages[currentLang.value]
  for (const k of keys) {
    if (value && typeof value === 'object') value = value[k]
    else return key
  }
  if (typeof value !== 'string') return key
  return value.replace(/\{(\w+)\}/g, (_, name) => params[name] ?? `{${name}}`)
}

export function useI18n() {
  // lang 必须在模板中被引用，这样 currentLang 变化时才会触发重渲染
  const lang = computed(() => currentLang.value)

  function t(key, params = {}) {
    // 读取 lang.value 让 Vue 收集依赖
    void lang.value
    return translate(key, params)
  }

  function setLang(l) {
    currentLang.value = l
    localStorage.setItem('language', l)
    document.documentElement.setAttribute('data-lang', l)
  }

  return { t, setLang, lang }
}
