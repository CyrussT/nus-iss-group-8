export const useAuthStore = () => {
  const token = useState<string | null>('auth_token', () => null)

  const setToken = (newToken: string | null) => {
    token.value = newToken
  }

  return {
    token,
    setToken
  }
}
