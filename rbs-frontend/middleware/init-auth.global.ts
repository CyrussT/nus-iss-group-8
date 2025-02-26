export default defineNuxtRouteMiddleware(async (to) => {
    const authStore = useAuthStore()
    const publicRoutes = ['/login', '/unauthorized']
    
    try {
      // check if token exists and is valid
      if (authStore.token.value) {
        if (isJWTExpired(authStore.token.value)) {
          authStore.logout()
          if (!publicRoutes.includes(to.path)) {
            return navigateTo('/login')
          }
        }
      }

      // ensure user state is consistent
      if (authStore.token.value && !authStore.user.value) {
        const decoded = decodeJWT(authStore.token.value)
        if (!decoded) {
          authStore.logout()
          if (!publicRoutes.includes(to.path)) {
            return navigateTo('/login')
          }
        }
      }
    } catch (error) {
      console.error('Auth initialization error:', error)
      authStore.logout()
      if (!publicRoutes.includes(to.path)) {
        return navigateTo('/login')
      }
    }
  })
