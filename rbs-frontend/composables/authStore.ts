import type { IUser } from '~/types/auth'

export const useAuthStore = () => {
  const tokenCookie = useCookie('auth_token', {
    maxAge: 7 * 24 * 60 * 60, // 7 days
    path: '/',
    sameSite: 'strict'
  })

<<<<<<< HEAD
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
=======
  const token = useState<string | null>('auth_token', () => tokenCookie.value || null)
  const user = useState<IUser | null>("auth_user", () => null)

  // Helper function to decode and create user object
  const createUserFromToken = (token: string) => {
    const decoded = decodeJWT(token)
    if (!decoded || isJWTExpired(token)) return null
    
    return {
      email: decoded.email,
      role: decoded.role,
      exp: decoded.exp,
    }
  }
>>>>>>> main

  const setToken = (newToken: string | null) => {
    token.value = newToken
    tokenCookie.value = newToken

    user.value = newToken ? createUserFromToken(newToken) : null
    
    if (import.meta.client) {
      if (newToken) {
        localStorage.setItem('auth_token', newToken)
      } else {
        localStorage.removeItem('auth_token')
      }
    }
  }

  // init state from existing token
  if (token.value && !user.value) {
    user.value = createUserFromToken(token.value)
    if (!user.value) {
      setToken(null)
    }
  }
  // init from localStorage if no token
  else if (!token.value && import.meta.client) {
    const storedToken = localStorage.getItem('auth_token')
    if (storedToken && !isJWTExpired(storedToken)) {
      setToken(storedToken)
    } else if (storedToken) {
      localStorage.removeItem('auth_token')
    }
  }

  const isAuthenticated = computed(() => {
    return !!token.value && !isJWTExpired(token.value)
  })

  const logout = () => {
    setToken(null)
    tokenCookie.value = null
  }

  return {
    token,
    setToken,
    isAuthenticated,
    logout,
    user
  }
}
