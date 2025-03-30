import { ref } from "vue";
import axios from "axios";

// Define the expected type for booking objects
interface Booking {
  bookingId: number;
  facilityName: string;
  location: string;
  bookedDatetime: string;
  timeslot: string;
  status: string;
}

export function useBooking() {
  const upcomingApprovedBookings = ref<Booking[]>([]);
  const pendingBookings = ref<Booking[]>([]); 
  const pastBookings = ref<Booking[]>([]); 
  const availableCredits = ref<number | null>(null);

  const fetchAccountId = async (email: string) => {
    try {
      const response = await axios.get(`http://localhost:8080/api/auth/account-id`, {
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
      const response = await axios.get("http://localhost:8080/api/bookings/upcoming-approved", {
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
      const response = await axios.get("http://localhost:8080/api/bookings/pending-future", {
        params: { accountId }  // Using accountId since bookings are linked to it
      });

      pendingBookings.value = response.data;
    } catch (error) {
      console.error("Error fetching upcoming bookings:", error);
    }
  }

  const fetchPastBookings = async (accountId: number | null) => {
    if (!accountId) return; // Ensure accountId is available before fetching

    try {
      const response = await axios.post("http://localhost:8080/api/bookings/history", {
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
      const response = await axios.get("http://localhost:8080/api/credit", {
        params: { email }  // Send email as query param
      });

      availableCredits.value = response.data;  // Store available credits
    } catch (error) {
      console.error("Error fetching available credits:", error);
      return null;
    }
  }

  

  return {
    upcomingApprovedBookings,
    pendingBookings,
    pastBookings,
    availableCredits,
    fetchAccountId,
    fetchUpcomingApprovedBookings,
    fetchPendingBookings,
    fetchPastBookings,
    fetchAvailableCredits
  };
}
