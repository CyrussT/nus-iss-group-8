// middleware/studentOrAdmin.ts
export default defineNuxtRouteMiddleware(() => {
    const authStore = useAuthStore()
  
    const role = authStore.user.value?.role
    if (role !== 'STUDENT' && role !== 'ADMINISTRATOR') {
      return navigateTo('/unauthorized')
    }
  })
  