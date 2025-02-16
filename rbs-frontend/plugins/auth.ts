export default defineNuxtPlugin((nuxtApp) => {
    const authStore = useAuthStore()

    if (import.meta.client) {
      const token = localStorage.getItem('auth_token')
      if (token) {
        authStore.setToken(token)
      }
    }
})
