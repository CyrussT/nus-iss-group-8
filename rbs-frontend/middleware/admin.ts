export default defineNuxtRouteMiddleware(() => {
    const authStore = useAuthStore()

    if (authStore.user.value?.role !== 'ADMINISTRATOR') {
      return navigateTo('/unauthorized')
    }
  })