export default defineNuxtRouteMiddleware(async (to) => {
  const authStore = useAuthStore()
  
  if (authStore.isAuthenticated.value) {
    return navigateTo('/dashboard')
  }
})