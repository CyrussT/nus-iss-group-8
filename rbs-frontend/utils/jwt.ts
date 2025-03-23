import type { IAuthTokenPayload } from '~/types/auth'

export const decodeJWT = (token: string) => {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) throw new Error('Invalid token format')

    const payload = JSON.parse(atob(parts[1])) as IAuthTokenPayload
    
    return {
      email: payload.sub,
      role: payload.role,
      exp: payload.exp,
      studentId: payload.studentId,
    }
  } catch (error) {
    console.error('Error decoding JWT:', error)
    return null
  }
}

export const isJWTExpired = (token: string): boolean => {
  const decoded = decodeJWT(token)
  if (!decoded) return true
  
  const currentTime = Math.floor(Date.now() / 1000)
  return decoded.exp < currentTime
}