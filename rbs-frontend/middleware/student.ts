export default defineNuxtRouteMiddleware(() => {
    const authStore = useAuthStore()

    if (authStore.user.value?.role !== 'STUDENT') {
      return navigateTo('/unauthorized')
    }
  })