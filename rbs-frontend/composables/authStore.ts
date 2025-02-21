import type { IUser, IAuthTokenPayload } from '~/types/auth'

export const useAuthStore = () => {
  const token = useState<string | null>('auth_token', () => {
    if (import.meta.client) {
      const storedToken = localStorage.getItem('auth_token')
      if (storedToken && isJWTExpired(storedToken)) {
        localStorage.removeItem('auth_token')
        return null
      }
      return storedToken
    }
    return null
  })

  const user = useState<IUser | null>('auth_user', () => {
    if (import.meta.client && token.value) {
      const decoded = decodeJWT(token.value)
      return decoded ? {
        email: decoded.email,
        role: decoded.role,
        exp: decoded.exp
      } : null
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

  const logout = () => {
    token.value = null
    localStorage.removeItem('auth_token')
  }

  return {
    token,
    setToken,
    isAuthenticated,
    logout,
    user
  }
}