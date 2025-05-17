import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { useBooking } from '../useBooking'
import axios from 'axios'
import { flushPromises } from '@vue/test-utils'
import { ref } from 'vue'

// Mock the useApi composable
vi.mock('#imports', () => ({
  useApi: () => ({
    apiUrl: 'http://localhost:8080'
  })
}))

// Mock axios
vi.mock('axios')

describe('useBooking', () => {
  // Common test data
  const mockEmail = 'test@example.com'
  const mockAccountId = 123
  const mockToken = 'mock-jwt-token'

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('fetchAccountId', () => {
    it('should fetch account ID successfully', async () => {
      // Arrange
      const { fetchAccountId } = useBooking()
      
      // Mock axios response
      vi.mocked(axios.get).mockResolvedValueOnce({ data: mockAccountId })
      
      // Act
      const result = await fetchAccountId(mockEmail)
      
      // Assert
      expect(axios.get).toHaveBeenCalledWith('http://localhost:8080/api/auth/account-id', {
        params: { email: mockEmail }
      })
      expect(result).toBe(mockAccountId)
    })
    
    it('should return null if an error occurs', async () => {
      // Arrange
      const { fetchAccountId } = useBooking()
      const mockError = new Error('API error')
      vi.mocked(axios.get).mockRejectedValueOnce(mockError)
      
      // Spy on console.error
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      
      // Act
      const result = await fetchAccountId(mockEmail)
      
      // Assert
      expect(result).toBeNull()
      expect(consoleSpy).toHaveBeenCalledWith('Error fetching Account ID:', mockError)
      
      // Cleanup
      consoleSpy.mockRestore()
    })
  })
  
  describe('fetchUpcomingApprovedBookings', () => {
    it('should fetch upcoming approved bookings successfully', async () => {
      // Arrange
      const mockBookings = [{ id: 1, status: 'CONFIRMED' }]
      const { fetchUpcomingApprovedBookings, upcomingApprovedBookings } = useBooking()
      
      vi.mocked(axios.get).mockResolvedValueOnce({ data: mockBookings })
      
      // Act
      await fetchUpcomingApprovedBookings(mockAccountId)
      
      // Assert
      expect(axios.get).toHaveBeenCalledWith('http://localhost:8080/api/bookings/upcoming-approved', {
        params: { accountId: mockAccountId }
      })
      expect(upcomingApprovedBookings.value).toEqual(mockBookings)
    })
    
    it('should not fetch if accountId is not provided', async () => {
      // Arrange
      const { fetchUpcomingApprovedBookings } = useBooking()
      
      // Act
      await fetchUpcomingApprovedBookings(null)
      
      // Assert
      expect(axios.get).not.toHaveBeenCalled()
    })
    
    it('should handle errors during fetch', async () => {
      // Arrange
      const { fetchUpcomingApprovedBookings, upcomingApprovedBookings } = useBooking()
      const mockError = new Error('API error')
      vi.mocked(axios.get).mockRejectedValueOnce(mockError)
      
      // Spy on console.error
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      
      // Act
      await fetchUpcomingApprovedBookings(mockAccountId)
      
      // Assert
      expect(consoleSpy).toHaveBeenCalledWith('Error fetching upcoming approved bookings:', mockError)
      expect(upcomingApprovedBookings.value).toEqual([]) // Value should not be changed
      
      // Cleanup
      consoleSpy.mockRestore()
    })
  })
  
  describe('fetchPendingBookings', () => {
    it('should fetch pending bookings successfully', async () => {
      // Arrange
      const mockBookings = [{ id: 2, status: 'PENDING' }]
      const { fetchPendingBookings, pendingBookings } = useBooking()
      
      vi.mocked(axios.get).mockResolvedValueOnce({ data: mockBookings })
      
      // Act
      await fetchPendingBookings(mockAccountId)
      
      // Assert
      expect(axios.get).toHaveBeenCalledWith('http://localhost:8080/api/bookings/pending-future', {
        params: { accountId: mockAccountId }
      })
      expect(pendingBookings.value).toEqual(mockBookings)
    })
    
    it('should handle errors during fetch', async () => {
      // Arrange
      const { fetchPendingBookings } = useBooking()
      const mockError = new Error('API error')
      vi.mocked(axios.get).mockRejectedValueOnce(mockError)
      
      // Spy on console.error
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      
      // Act
      await fetchPendingBookings(mockAccountId)
      
      // Assert
      expect(consoleSpy).toHaveBeenCalledWith('Error fetching upcoming bookings:', mockError)
      
      // Cleanup
      consoleSpy.mockRestore()
    })
  })
  
  describe('fetchPastBookings', () => {
    it('should fetch past bookings successfully', async () => {
      // Arrange
      const mockBookings = [{ id: 3, status: 'COMPLETED' }]
      const { fetchPastBookings, pastBookings } = useBooking()
      
      vi.mocked(axios.post).mockResolvedValueOnce({ data: mockBookings })
      
      // Act
      await fetchPastBookings(mockAccountId)
      
      // Assert
      expect(axios.post).toHaveBeenCalledWith('http://localhost:8080/api/bookings/history', {
        studentId: mockAccountId,
        status: ""
      })
      expect(pastBookings.value).toEqual(mockBookings)
    })
    
    it('should not fetch if accountId is not provided', async () => {
      // Arrange
      const { fetchPastBookings } = useBooking()
      
      // Act
      await fetchPastBookings(null)
      
      // Assert
      expect(axios.post).not.toHaveBeenCalled()
    })
  })
  
  describe('fetchAvailableCredits', () => {
    it('should fetch available credits successfully', async () => {
      // Arrange
      const mockCredits = 10
      const { fetchAvailableCredits, availableCredits } = useBooking()
      
      vi.mocked(axios.get).mockResolvedValueOnce({ data: mockCredits })
      
      // Act
      await fetchAvailableCredits(mockEmail)
      
      // Assert
      expect(axios.get).toHaveBeenCalledWith('http://localhost:8080/api/credit', {
        params: { email: mockEmail }
      })
      expect(availableCredits.value).toBe(mockCredits)
    })
    
    it('should not fetch if email is not provided', async () => {
      // Arrange
      const { fetchAvailableCredits } = useBooking()
      
      // Act
      await fetchAvailableCredits(null)
      
      // Assert
      expect(axios.get).not.toHaveBeenCalled()
    })
    
    it('should handle errors during fetch', async () => {
      // Arrange
      const { fetchAvailableCredits } = useBooking()
      const mockError = new Error('API error')
      vi.mocked(axios.get).mockRejectedValueOnce(mockError)
      
      // Spy on console.error
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      
      // Act
      const result = await fetchAvailableCredits(mockEmail)
      
      // Assert
      expect(consoleSpy).toHaveBeenCalledWith('Error fetching available credits:', mockError)
      expect(result).toBeNull()
      
      // Cleanup
      consoleSpy.mockRestore()
    })
  })
  
  describe('searchFacilities', () => {
    // Set up mock for fetch
    let fetchMock: any
    
    beforeEach(() => {
      // Create a mock fetch function
      fetchMock = vi.fn()
      global.fetch = fetchMock
    })
    
    afterEach(() => {
      // Clean up
      vi.restoreAllMocks()
    })
    
    it('should search facilities with no search criteria', async () => {
      // Arrange
      const mockFacilities = [{ id: 1, name: 'Room 1' }]
      const apiUrl = 'http://test-api'
      const { searchFacilities, facilities, searchLoading } = useBooking()
      
      fetchMock.mockResolvedValueOnce({
        ok: true,
        json: async () => mockFacilities
      })
      
      // Act
      const result = await searchFacilities(apiUrl, mockToken)
      await flushPromises()
      
      // Assert
      expect(fetchMock).toHaveBeenCalledWith(
        'http://test-api/api/bookings/facilities/search?', 
        expect.objectContaining({
          headers: {
            Authorization: `Bearer ${mockToken}`
          }
        })
      )
      expect(result).toEqual(mockFacilities)
      expect(facilities.value).toEqual(mockFacilities)
      expect(searchLoading.value).toBe(false)
    })
    
    it('should search facilities with complete search criteria', async () => {
      // Arrange
      const mockFacilities = [{ id: 1, name: 'Room 1' }]
      const apiUrl = 'http://test-api'
      const criteria = {
        resourceTypeId: '1',
        resourceName: 'Room',
        location: 'Building A',
        capacity: '10'
      }
      const currentDate = '2025-05-17'
      const { searchFacilities } = useBooking()
      
      fetchMock.mockResolvedValueOnce({
        ok: true,
        json: async () => mockFacilities
      })
      
      // Act
      const result = await searchFacilities(apiUrl, mockToken, criteria, currentDate)
      
      // Assert
      expect(fetchMock).toHaveBeenCalledWith(
        'http://test-api/api/bookings/facilities/search?resourceTypeId=1&resourceName=Room&location=Building+A&capacity=10&date=2025-05-17', 
        expect.anything()
      )
      expect(result).toEqual(mockFacilities)
    })
    
    it('should handle API errors', async () => {
      // Arrange
      const apiUrl = 'http://test-api'
      const { searchFacilities } = useBooking()
      
      fetchMock.mockResolvedValueOnce({
        ok: false,
        status: 500
      })
      
      // Act & Assert
      await expect(searchFacilities(apiUrl, mockToken)).rejects.toThrow('Failed to fetch facilities: 500')
    })
    
    it('should handle network errors', async () => {
      // Arrange
      const apiUrl = 'http://test-api'
      const { searchFacilities } = useBooking()
      const networkError = new Error('Network error')
      
      fetchMock.mockRejectedValueOnce(networkError)
      
      // Spy on console.error
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      
      // Act & Assert
      await expect(searchFacilities(apiUrl, mockToken)).rejects.toThrow('Network error')
      expect(consoleSpy).toHaveBeenCalledWith('Error in searchFacilities:', networkError)
      
      // Cleanup
      consoleSpy.mockRestore()
    })
  })
  
  describe('fetchDropdownOptions', () => {
    // Set up mock for fetch
    let fetchMock: any
    
    beforeEach(() => {
      // Create a mock fetch function
      fetchMock = vi.fn()
      global.fetch = fetchMock
    })
    
    afterEach(() => {
      // Clean up
      vi.restoreAllMocks()
    })
    
    it('should fetch dropdown options successfully', async () => {
      // Arrange
      const mockOptions = { 
        resourceTypes: [{ id: 1, name: 'Room' }], 
        locations: ['Building A', 'Building B']
      }
      const apiUrl = 'http://test-api'
      const { fetchDropdownOptions } = useBooking()
      
      fetchMock.mockResolvedValueOnce({
        ok: true,
        json: async () => mockOptions
      })
      
      // Act
      const result = await fetchDropdownOptions(apiUrl, mockToken)
      
      // Assert
      expect(fetchMock).toHaveBeenCalledWith(
        'http://test-api/api/bookings/dropdown-options', 
        expect.objectContaining({
          headers: {
            Authorization: `Bearer ${mockToken}`
          }
        })
      )
      expect(result).toEqual(mockOptions)
    })
    
    it('should handle API errors', async () => {
      // Arrange
      const apiUrl = 'http://test-api'
      const { fetchDropdownOptions } = useBooking()
      
      fetchMock.mockResolvedValueOnce({
        ok: false,
        status: 500
      })
      
      // Act & Assert
      await expect(fetchDropdownOptions(apiUrl, mockToken)).rejects.toThrow('Failed to fetch dropdown options: 500')
    })
  })
  
  describe('createBooking', () => {
    // Set up mock for fetch
    let fetchMock: any
    
    beforeEach(() => {
      // Create a mock fetch function
      fetchMock = vi.fn()
      global.fetch = fetchMock
      
      // Mock date methods to ensure consistent testing
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2025-05-17T12:00:00Z'))
    })
    
    afterEach(() => {
      // Clean up
      vi.restoreAllMocks()
      vi.useRealTimers()
    })
    
    it('should create a booking using localDate and localTime', async () => {
      // Arrange
      const mockResponse = { id: 1, status: 'PENDING' }
      const apiUrl = 'http://test-api'
      const { createBooking } = useBooking()
      
      const mockBookingData = {
        resourceId: '1',
        accountEmail: 'test@example.com',
        localDate: '2025-05-20',
        localTime: '14:30',
        title: 'Project Meeting',
        description: 'Team discussion',
        creditsUsed: '60' // 60 minutes
      }
      
      fetchMock.mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      })
      
      // Act
      const result = await createBooking(apiUrl, mockToken, mockBookingData)
      
      // Assert
      expect(fetchMock).toHaveBeenCalledWith(
        'http://test-api/api/bookings',
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${mockToken}`
          },
          body: JSON.stringify({
            facilityId: 1,
            accountEmail: 'test@example.com',
            bookedDateTime: '2025-05-20T14:30:00',
            timeSlot: '14:30 - 15:30',
            title: 'Project Meeting',
            description: 'Team discussion',
            creditsUsed: '60'
          })
        })
      )
      expect(result).toEqual(mockResponse)
    })
    
    it('should create a booking using start date', async () => {
      // Arrange
      const mockResponse = { id: 2, status: 'PENDING' }
      const apiUrl = 'http://test-api'
      const { createBooking } = useBooking()
      
      const startDate = new Date('2025-05-20T14:30:00Z')
      const mockBookingData = {
        resourceId: '1',
        accountEmail: 'test@example.com',
        start: startDate.toISOString(),
        title: 'Project Meeting',
        description: 'Team discussion',
        creditsUsed: '60' // 60 minutes
      }
      
      fetchMock.mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      })
      
      // Act
      const result = await createBooking(apiUrl, mockToken, mockBookingData)
      
      // Assert
      expect(fetchMock).toHaveBeenCalled()
      expect(result).toEqual(mockResponse)
    })
    
    it('should handle API errors', async () => {
      // Arrange
      const apiUrl = 'http://test-api'
      const { createBooking } = useBooking()
      
      const mockBookingData = {
        resourceId: '1',
        accountEmail: 'test@example.com',
        localDate: '2025-05-20',
        localTime: '14:30',
        title: 'Project Meeting',
        description: 'Team discussion',
        creditsUsed: '60'
      }
      
      fetchMock.mockResolvedValueOnce({
        ok: false,
        status: 400
      })
      
      // Act & Assert
      await expect(createBooking(apiUrl, mockToken, mockBookingData))
        .rejects.toThrow('Failed to create booking: 400')
    })
  })
})