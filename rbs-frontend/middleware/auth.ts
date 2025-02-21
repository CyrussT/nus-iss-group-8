export default defineNuxtRouteMiddleware(async (to) => {
    const authStore = useAuthStore()

    if (authStore.token.value && isJWTExpired(authStore.token.value)) {
      authStore.logout()
      return navigateTo('/login')
    }
    
    if (!authStore.isAuthenticated.value) {
      return navigateTo('/login')
    }
  })