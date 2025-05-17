import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { decodeJWT, isJWTExpired } from '../jwt'

describe('JWT Utilities', () => {
  // Helper to create a valid JWT token with custom payload
  const createMockToken = (payload = {}, expiration = Math.floor(Date.now() / 1000) + 3600) => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    const defaultPayload = {
      sub: 'test@example.com',
      role: 'STUDENT',
      exp: expiration,
      studentId: '12345'
    }
    
    const tokenPayload = btoa(JSON.stringify({
      ...defaultPayload,
      ...payload
    }))
    
    // Use a dummy signature for testing
    const signature = btoa('dummy-signature')
    
    return `${header}.${tokenPayload}.${signature}`
  }
  
  describe('decodeJWT', () => {
    it('should decode a valid JWT token correctly', () => {
      const token = createMockToken()
      const decoded = decodeJWT(token)
      
      expect(decoded).toEqual({
        email: 'test@example.com',
        role: 'STUDENT',
        exp: expect.any(Number),
        studentId: '12345'
      })
    })
    
    it('should handle different roles in the token', () => {
      const token = createMockToken({ role: 'ADMIN' })
      const decoded = decodeJWT(token)
      
      expect(decoded?.role).toBe('ADMIN')
    })
    
    it('should return null for an invalid token format', () => {
      const invalidToken = 'invalid.token'
      const decoded = decodeJWT(invalidToken)
      
      expect(decoded).toBeNull();
    })
    
    it('should return null when token payload cannot be parsed', () => {
      // Create an invalid token with malformed payload
      const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
      const invalidPayload = btoa('not-valid-json')
      const signature = btoa('dummy-signature')
      
      const invalidToken = `${header}.${invalidPayload}.${signature}`
      const decoded = decodeJWT(invalidToken)
      
      expect(decoded).toBeNull();
    })
    
    it('should log an error when decoding fails', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      const invalidToken = 'invalid-token'
      
      decodeJWT(invalidToken)
      
      expect(consoleSpy).toHaveBeenCalledWith('Error decoding JWT:', expect.any(Error))
      consoleSpy.mockRestore()
    })
  })
  
  describe('isJWTExpired', () => {
    beforeEach(() => {
      // Mock Date.now to return a fixed timestamp
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2025-01-01T00:00:00Z'))
    })
    
    afterEach(() => {
      vi.useRealTimers()
    })
    
    it('should return false for a valid non-expired token', () => {
      // Token expires in 1 hour from now
      const expirationTime = Math.floor(Date.now() / 1000) + 3600
      const token = createMockToken({ exp: expirationTime })
      
      expect(isJWTExpired(token)).toBe(false)
    })
    
    it('should return false when exp timestamp equals current time', () => {
      // Token expires exactly now
      const expirationTime = Math.floor(Date.now() / 1000)
      const token = createMockToken({ exp: expirationTime })
      
      expect(isJWTExpired(token)).toBe(false)
    })
    
    it('should return true for an invalid token', () => {
      const invalidToken = 'invalid.token'
      
      expect(isJWTExpired(invalidToken)).toBe(true)
    })
    
    it('should handle tokens that expire in the future', () => {
      // Token that expires far in the future
      const farFutureExpiration = Math.floor(Date.now() / 1000) + 86400 * 365 // 1 year
      const token = createMockToken({ exp: farFutureExpiration })
      
      expect(isJWTExpired(token)).toBe(false)
    })
  })
})