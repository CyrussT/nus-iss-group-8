export const useApi = () => {
  const config = useRuntimeConfig()
  return {
    apiUrl: config.public.apiUrl
  }
}
