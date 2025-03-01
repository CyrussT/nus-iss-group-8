export default defineNuxtRouteMiddleware(() => {
    const authStore = useAuthStore()

    if (authStore.token.value && isJWTExpired(authStore.token.value)) {
      authStore.logout()
      return navigateTo('/login')
    }
  })