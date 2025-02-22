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

  const user = useState<IUser | null>("auth_user", () => null);
  watch(
    token,
    (newToken) => {
      if (newToken) {
        const decoded = decodeJWT(newToken);
        user.value = decoded
          ? {
              email: decoded.email,
              role: decoded.role,
              exp: decoded.exp,
            }
          : null;
      } else {
        user.value = null;
      }
    },
    { immediate: true } 
  );

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