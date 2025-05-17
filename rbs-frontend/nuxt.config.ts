// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  devtools: { enabled: true },
  modules: ["@nuxt/ui", '@nuxt/test-utils/module'],
  compatibilityDate: "2025-02-16",
  colorMode: {
    preference: "system",
    fallback: "dark",
    hid: 'nuxt-color-mode-script',
    globalName: '__NUXT_COLOR_MODE__',
    componentName: 'ColorScheme',
    classPrefix: '',
    classSuffix: '',
    storageKey: 'nuxt-color-mode'
  },
  runtimeConfig: {
    public: {
      apiUrl: process.env.API_BASE_URL || 'http://localhost:8080'
    }
  },
  icon: {
    provider: 'iconify'
  }
})
