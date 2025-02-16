export const useAuthStore = () => {
  const token = useState<string | null>('auth_token', () => {
    if (import.meta.client) {
      return localStorage.getItem('auth_token')
    }
    return null
  })

  const setToken = (newToken: string | null) => {
    token.value = newToken
    if (import.meta.client) {
      if (newToken) {
        localStorage.setItem('auth_token', newToken)
      } else {
        localStorage.removeItem('auth_token')
      }
    }
  }

  const isAuthenticated = computed(() => {
    const storedToken = import.meta.client ? localStorage.getItem('auth_token') : null
    return !!token.value
  })

  return {
    token,
    setToken,
    isAuthenticated
  }
}