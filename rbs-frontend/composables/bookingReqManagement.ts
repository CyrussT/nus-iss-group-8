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
  email: string;
}

export function bookingRequestManagement() {
  const pendingBookings = ref<Booking[]>([]); 
  const loading = ref(false); // Loading state
  const sortBy = ref<string>('bookedDatetime'); // Default sorting key
  const sortOrder = ref<string>('asc'); // Default sorting order

  const fetchPendingBookings = async() => {
    try {
      loading.value = true;
      const response = await axios.get("http://localhost:8080/api/bookings/pending-bookings", {
        params: {
          status: "PENDING", // Filter by status
          sort: `${sortBy.value},${sortOrder.value}`, // Sorting by selected field and order
        }
      });

      pendingBookings.value = response.data; 
    } catch (error) {
      console.error("Error fetching pending bookings:", error);
    } finally {
      loading.value = false;
    }
  };

  const changeSorting = (field: string) => {
    if (sortBy.value === field) {
      // Toggle sort order if already sorted by this field
      sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
    } else {
      // Otherwise, set the new sorting field and default to ascending order
      sortBy.value = field;
      sortOrder.value = 'asc';
    }
    fetchPendingBookings(); // Refetch bookings with the new sorting
  };

  return {
    pendingBookings,
    loading,
    fetchPendingBookings,
    changeSorting
  };
}
