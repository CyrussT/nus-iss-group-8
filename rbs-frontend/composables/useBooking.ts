import { ref } from "vue";
import axios from "axios";
import type { Booking, Facility } from "./useFacility"; // Import interfaces from useFacility.ts

const { apiUrl } = useApi();
export interface SearchCriteria {
  resourceTypeId?: string;
  resourceName?: string;
  location?: string;
  capacity?: string | number;
  [key: string]: any; // Allow any additional properties
}

// Request body interface for creating a booking
export interface BookingRequest {
  facilityId: number;
  accountEmail: string;
  bookedDateTime: string;
  timeSlot: string;
  title: string;
  description: string;
  creditsUsed: string | number;
}

export function useBooking() {
  const upcomingApprovedBookings = ref<Booking[]>([]);
  const pendingBookings = ref<Booking[]>([]); 
  const pastBookings = ref<Booking[]>([]); 
  const availableCredits = ref<number | null>(null);
  const facilities = ref<Facility[]>([]);
  const searchLoading = ref(false);

  const fetchAccountId = async (email: string) => {
    try {
      const response = await axios.get(`${apiUrl}/api/auth/account-id`, {
        params: { email }
      });
      return response.data; // This should be the account ID
    }
    catch (error) {
      console.error("Error fetching Account ID:", error);
      return null;
    }
  };

  const fetchUpcomingApprovedBookings = async (accountId: number | null) => { 
    if (!accountId) return; // Ensure accountId is available before fetching

    try {
      const response = await axios.get(`${apiUrl}/api/bookings/upcoming-approved`, {
        params: { accountId }  // ✅ Send accountId as query param
      });

      upcomingApprovedBookings.value = response.data;  // ✅ Store upcoming approved bookings
    } catch (error) {
      console.error("Error fetching upcoming approved bookings:", error);
    }
  };

  const fetchPendingBookings = async (accountId: number | null) => { 
    if (!accountId) return; // Ensure accountId is available before fetching

    try {
      const response = await axios.get(`${apiUrl}/api/bookings/pending-future`, {
        params: { accountId }  // Using accountId since bookings are linked to it
      });

      pendingBookings.value = response.data;
    } catch (error) {
      console.error("Error fetching upcoming bookings:", error);
    }
  };

  const fetchPastBookings = async (accountId: number | null) => {
    if (!accountId) return; // Ensure accountId is available before fetching

    try {
      const response = await axios.post(`${apiUrl}/api/bookings/history`, {
        studentId: accountId,  // Using accountId since bookings are linked to it
        status: ""  // Fetch all past bookings, modify if needed
      });

      pastBookings.value = response.data;
    } catch (error) {
      console.error("Error fetching past bookings:", error);
    }
  };

  // Fetch available credits
  const fetchAvailableCredits = async (email: string | null) => {
    if (!email) return; // Ensure email is available before fetching

    try {
      const response = await axios.get(`${apiUrl}/api/credit`, {
        params: { email }  // Send email as query param
      });

      availableCredits.value = response.data;  // Store available credits
    } catch (error) {
      console.error("Error fetching available credits:", error);
      return null;
    }
  };

  // Search facilities with criteria
  const searchFacilities = async (apiUrl: string, token: string, criteria: SearchCriteria = {}, currentDate?: string) => {
    try {
      searchLoading.value = true;
      
      // Build query parameters from search criteria
      const params = new URLSearchParams();
      
      // Explicitly check for each potential property
      if (criteria.resourceTypeId) {
        params.append('resourceTypeId', criteria.resourceTypeId.toString());
      }
      
      if (criteria.resourceName) {
        params.append('resourceName', criteria.resourceName.toString());
      }
      
      if (criteria.location) {
        params.append('location', criteria.location.toString());
      }
      
      if (criteria.capacity) {
        params.append('capacity', criteria.capacity.toString());
      }
      
      // Always include the current calendar date if provided
      if (currentDate) {
        params.append('date', currentDate);
      }
      
      const response = await fetch(`${apiUrl}/api/bookings/facilities/search?${params.toString()}`, {
        headers: {
          Authorization: "Bearer " + token
        }
      });
      
      if (!response.ok) {
        throw new Error(`Failed to fetch facilities: ${response.status}`);
      }
      
      const filteredFacilities = await response.json();
      facilities.value = filteredFacilities;
      
      return filteredFacilities;
    } catch (error) {
      console.error('Error in searchFacilities:', error);
      throw error;
    } finally {
      searchLoading.value = false;
    }
  };

  // Fetch dropdown options
  const fetchDropdownOptions = async (apiUrl: string, token: string) => {
    try {
      // Make a single API call to get all dropdown options
      const response = await fetch(`${apiUrl}/api/bookings/dropdown-options`, {
        headers: {
          Authorization: "Bearer " + token
        }
      });
      
      if (!response.ok) {
        throw new Error(`Failed to fetch dropdown options: ${response.status}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Error fetching dropdown options:', error);
      throw error;
    }
  };

  // Create a new booking
  const createBooking = async (apiUrl: string, token: string, booking: any) => {
    try {
      searchLoading.value = true;
      
      // Create a proper local date from the booking date/time information
      let bookedDateTime;
      let localTimeSlot;
      
      if (booking.localDate && booking.localTime) {
        // Use the local date/time if available
        const [year, month, day] = booking.localDate.split('-').map(Number);
        const [hours, minutes] = booking.localTime.split(':').map(Number);
        
        // Create a date object using local components
        const localDate = new Date(year, month - 1, day, hours, minutes, 0, 0);
        
        // Format the timeslot (local time)
        const startTime = localDate.toLocaleTimeString('en-US', { 
          hour: '2-digit', 
          minute: '2-digit',
          hour12: false 
        });
        
        // Calculate end time (add duration minutes to start time)
        const durationMinutes = parseInt(booking.creditsUsed, 10);
        const endDateTime = new Date(localDate.getTime() + durationMinutes * 60000);
        
        const endTime = endDateTime.toLocaleTimeString('en-US', {
          hour: '2-digit', 
          minute: '2-digit',
          hour12: false
        });
        
        localTimeSlot = `${startTime} - ${endTime}`;
        
        // Format as ISO string WITHOUT timezone for LocalDateTime compatibility
        // Format: YYYY-MM-DDTHH:MM:SS
        bookedDateTime = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}T${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:00`;
        
        console.log('Booking datetime info:', {
          localDate: booking.localDate,
          localTime: booking.localTime,
          localDateObject: localDate,
          bookedDateTimeSG: bookedDateTime,
          timeSlot: localTimeSlot
        });
      } else {
        // Fallback to using the ISO string but strip timezone part
        const startDate = new Date(booking.start);
        const year = startDate.getFullYear();
        const month = startDate.getMonth() + 1;
        const day = startDate.getDate();
        const hours = startDate.getHours();
        const minutes = startDate.getMinutes();
        
        bookedDateTime = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}T${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:00`;
        
        // Calculate timeslot
        const startTime = startDate.toLocaleTimeString('en-US', { 
          hour: '2-digit', 
          minute: '2-digit',
          hour12: false 
        });
        
        // Calculate end time based on duration
        const durationMinutes = parseInt(booking.creditsUsed, 10);
        const endDateTime = new Date(startDate.getTime() + durationMinutes * 60000);
        
        const endTime = endDateTime.toLocaleTimeString('en-US', {
          hour: '2-digit', 
          minute: '2-digit',
          hour12: false
        });
        
        localTimeSlot = `${startTime} - ${endTime}`;
      }
      
      // Create the request body
      const requestBody: BookingRequest = {
        facilityId: parseInt(booking.resourceId),
        accountEmail: booking.accountEmail,
        bookedDateTime: bookedDateTime,
        timeSlot: localTimeSlot,
        title: booking.title,
        description: booking.description,
        creditsUsed: booking.creditsUsed
      };
      
      console.log('Sending booking request:', requestBody);
      
      // Make the API call
      const response = await fetch(`${apiUrl}/api/bookings`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(requestBody)
      });
      
      if (!response.ok) {
        throw new Error(`Failed to create booking: ${response.status}`);
      }
      
      // Return the created booking data
      return await response.json();
    } catch (error) {
      console.error('Error creating booking:', error);
      throw error;
    } finally {
      searchLoading.value = false;
    }
  };

  // Convert a date to Singapore timezone (UTC+8)
  const convertToSGTime = (date: Date): string => {
    // Create a formatter with explicit Singapore timezone
    const formatter = new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
      timeZone: 'Asia/Singapore'
    });
    
    // Get parts
    const parts = formatter.formatToParts(date);
    const mappedParts: {[key: string]: string} = {};
    
    // Map the parts
    for (const part of parts) {
      mappedParts[part.type] = part.value;
    }
    
    // Format as ISO-like string with explicit +08:00 timezone
    const sgTimeString = `${mappedParts.year}-${mappedParts.month}-${mappedParts.day}T${mappedParts.hour}:${mappedParts.minute}:${mappedParts.second}+08:00`;
    
    return sgTimeString;
  };

  return {
    upcomingApprovedBookings,
    pendingBookings,
    pastBookings,
    availableCredits,
    facilities,
    searchLoading,
    fetchAccountId,
    fetchUpcomingApprovedBookings,
    fetchPendingBookings,
    fetchPastBookings,
    fetchAvailableCredits,
    searchFacilities,
    fetchDropdownOptions,
    createBooking
  };
}