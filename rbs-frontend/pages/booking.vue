<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useApi } from '~/composables/useApi';
import BookingCalendar from '~/components/booking/BookingCalendar.vue';
import BookingSearchBar from '~/components/booking/BookingSearchBar.vue';
import BookingCreateModal from '~/components/booking/BookingCreateModal.vue';
import BookingDetailsModal from '~/components/booking/BookingDetailsModal.vue';
import { useBooking } from '#imports';

const auth = useAuthStore();
const { apiUrl } = useApi();
const {availableCredits, fetchAvailableCredits} = useBooking();

// State variables
const facilities = ref([]);
const bookings = ref([]);
const loading = ref(true);
const searchLoading = ref(false);
const optionsLoading = ref(false);

// Dropdown options
const resourceTypeOptions = ref([]);
const resourceNameOptions = ref([]);
const locationOptions = ref([]);

// Selected booking info for modals
const selectedBookingInfo = reactive({
  resourceId: '',
  resourceName: '',
  startTime: '',
});

const selectedBooking = reactive({
  id: '',
  title: '',
  start: '',
  end: '',
  resourceId: '',
  resourceName: '',
  location: '',
  status: '',
  description: '',
  attendees: '',
  isPast: false,
  studentId: '',
  studentName: ''
});

// Legend items for booking status colors
const legendItems = [
  { status: 'APPROVED', color: '#2e7d32', label: 'Approved' },
  { status: 'CONFIRMED', color: '#1976d2', label: 'Confirmed' },
  { status: 'PENDING', color: '#f57c00', label: 'Pending' },
  { status: 'MY_BOOKING', color: '#9c27b0', label: 'My Booking' }
];

// Format minutes to hours and minutes
const formatCredits = computed(() => {
  if (!availableCredits.value) return '0 mins';
  
  if (availableCredits.value < 60) {
    return `${availableCredits.value} mins`;
  } else {
    const hours = Math.floor(availableCredits.value / 60);
    const mins = availableCredits.value % 60;
    if (mins === 0) {
      return `${hours} hr${hours > 1 ? 's' : ''}`;
    } else {
      return `${hours} hr${hours > 1 ? 's' : ''} ${mins} min${mins > 1 ? 's' : ''}`;
    }
  }
});

// Get current user email for checking own bookings
const currentUserEmail = computed(() => auth.user.value?.email || '');
const currentUserId = computed(() => auth.user.value?.id || '');

// Modal visibility
const isCreateModalOpen = ref(false);
const isViewModalOpen = ref(false);

// Refs for components
const calendarRef = ref(null);

// Format date for API (YYYY-MM-DD)
const formatDateForApi = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

// Handle search with criteria
const handleSearch = (searchCriteria) => {
  searchLoading.value = true;
  searchFacilities(searchCriteria);
};

// Handle reset search
const handleReset = () => {
  searchFacilities({});
};

// Handle calendar date change
const handleDateChange = (dateInfo) => {
  searchFacilities({});
};

// Handle timeslot selection
const handleTimeslotSelect = (info) => {
  selectedBookingInfo.resourceId = info.resource.id;
  selectedBookingInfo.resourceName = info.resource.title;
  selectedBookingInfo.startTime = info.startStr;
  isCreateModalOpen.value = true;
};

// Handle event click
const handleEventClick = (event) => {
  Object.assign(selectedBooking, {
    id: event.id,
    title: event.title,
    start: event.start,
    end: event.end,
    resourceId: event.getResources()[0]?.id || '',
    resourceName: event.getResources()[0]?.title || 'Unknown Resource',
    location: event.extendedProps?.location || '',
    status: event.extendedProps?.status || 'Unknown',
    description: event.extendedProps?.description || '',
    attendees: event.extendedProps?.attendees || '',
    isPast: event.extendedProps?.isPast || false,
    studentId: event.extendedProps?.studentId || '',
    studentName: event.extendedProps?.studentName || ''
  });
  
  isViewModalOpen.value = true;
};

// Handle event drop
const handleEventDrop = ({ eventId, newStart, newEnd, newResourceId }) => {
  // Update the event in our reactive bookings array
  const bookingIndex = bookings.value.findIndex(b => b.id === eventId);
  if (bookingIndex !== -1) {
    bookings.value[bookingIndex] = {
      ...bookings.value[bookingIndex],
      start: newStart.toISOString(),
      end: newEnd.toISOString(),
      resourceId: newResourceId
    };
  }
  
  // TODO: API call to update booking on backend
  // updateBookingOnBackend(eventId, newStart, newEnd, newResourceId);
};

// Handle event resize
const handleEventResize = ({ eventId, newStart, newEnd }) => {
  // Update the event in our reactive bookings array
  const bookingIndex = bookings.value.findIndex(b => b.id === eventId);
  if (bookingIndex !== -1) {
    bookings.value[bookingIndex] = {
      ...bookings.value[bookingIndex],
      start: newStart.toISOString(),
      end: newEnd.toISOString()
    };
  }
  
  // TODO: API call to update booking on backend
  // updateBookingOnBackend(eventId, newStart, newEnd);
};

// Update the handleCreateBooking function
const handleCreateBooking = async (booking) => {
  try {
    // Show loading state
    searchLoading.value = true;
    
    // Get the current user email from auth store
    const accountEmail = auth.user.value.email;
    
    // Parse start time to get the date component
    const startDate = new Date(booking.start);
    
    // Format the timeslot
    const startTime = startDate.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      hour12: false 
    });
    
    const endDate = new Date(booking.end);
    const endTime = endDate.toLocaleTimeString('en-US', {
      hour: '2-digit', 
      minute: '2-digit',
      hour12: false
    });
    
    const timeSlot = `${startTime} - ${endTime}`;

   
    // Create the request body
    const requestBody = {
      facilityId: parseInt(booking.resourceId),
      accountEmail: accountEmail,
      bookedDateTime: startDate.toISOString(),
      timeSlot: timeSlot,
      title: booking.title,
      description: booking.description,
      attendees: booking.attendees,
      creditsUsed: booking.creditsUsed
    };
    
    // Make the API call
    const response = await fetch(`${apiUrl}/api/bookings`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${auth.token.value}`
      },
      body: JSON.stringify(requestBody)
    });
    
    if (!response.ok) {
      throw new Error(`Failed to create booking: ${response.status}`);
    }
    
    // Get the created booking data
    const createdBooking = await response.json();
    
    // Show success message
    alert('Booking created successfully!');
    
    // Refresh the calendar to show the new booking
    searchFacilities({});
    
    // Refresh available credits since we just used some
    fetchAvailableCredits(auth.user.value.email);
    
  } catch (error) {
    console.error('Error creating booking:', error);
    alert('Failed to create booking. Please try again.');
  } finally {
    searchLoading.value = false;
  }
};

// Handle edit booking
const handleEditBooking = (booking) => {
  // Placeholder for future implementation
  console.log('Edit booking:', booking);
  alert('Edit functionality will be implemented in a future update');
};

// Search facilities with date filtering
async function searchFacilities(criteria = {}) {
  try {
    // Determine which loading state to use
    const isInitialLoad = loading.value;
    
    // Ensure loading state is set appropriately
    if (isInitialLoad) {
      loading.value = true;
    } else {
      searchLoading.value = true;
    }
    
    // Get the current date from the calendar
    let currentDate;
    if (calendarRef.value) {
      currentDate = calendarRef.value.getCurrentCalendarDate();
    } else {
      // Fallback to today's date if calendar not initialized
      currentDate = formatDateForApi(new Date());
    }
    
    // Build query parameters from search criteria
    const params = new URLSearchParams();
    if (criteria.resourceType) {
      params.append('resourceType', criteria.resourceType);
    }
    if (criteria.resourceName) {
      params.append('resourceName', criteria.resourceName);
    }
    if (criteria.location) {
      params.append('location', criteria.location);
    }
    if (criteria.capacity) {
      params.append('capacity', criteria.capacity);
    }
    
    // Always include the current calendar date
    params.append('date', currentDate);
    console.log('Searching facilities for date:', currentDate);
    
    console.log('Searching facilities with params:', params.toString());
    
    const response = await fetch(`${apiUrl}/api/bookings/facilities/search?${params.toString()}`, {
      headers: {
        Authorization: "Bearer " + auth.token.value
      }
    });
    
    if (!response.ok) {
      throw new Error(`Failed to fetch facilities: ${response.status}`);
    }
    
    // Process the filtered facilities from backend
    const filteredFacilities = await response.json();
    console.log('Received facilities:', filteredFacilities);
    facilities.value = filteredFacilities;
    
    // Process all bookings
    const allBookings = [];
    const now = new Date();
    
    facilities.value.forEach(facility => {
      if (facility.bookings && facility.bookings.length > 0) {
        console.log(`Processing ${facility.bookings.length} bookings for facility ${facility.resourceName}`);
        
        facility.bookings.forEach(booking => {
          try {
            // Get the date from bookedDatetime
            const bookedDate = new Date(booking.bookedDatetime);
            
            if (!booking.timeslot) {
              console.error('Booking has no timeslot:', booking);
              return; // Skip this booking
            }
            
            // Handle different timeslot formats (with or without spaces)
            let startTime, endTime;
            
            if (booking.timeslot.includes(' - ')) {
              [startTime, endTime] = booking.timeslot.split(' - ');
            } else if (booking.timeslot.includes('-')) {
              [startTime, endTime] = booking.timeslot.split('-');
            } else {
              startTime = booking.timeslot;
              endTime = null;
            }
            
            if (startTime) {
              // Parse start time (HH:MM)
              const [startHour, startMinute] = startTime.trim().split(":").map(Number);
              
              // Create a new date object with the booked date and start time
              const startDateTime = new Date(bookedDate);
              startDateTime.setHours(startHour || 0, startMinute || 0, 0, 0);
              
              // Handle end time if it exists
              let endDateTime;
              
              if (endTime) {
                const [endHour, endMinute] = endTime.trim().split(":").map(Number);
                
                endDateTime = new Date(bookedDate);
                endDateTime.setHours(endHour || 0, endMinute || 0, 0, 0);
              } else {
                // Default to 1 hour duration if no end time
                endDateTime = new Date(startDateTime);
                endDateTime.setHours(endDateTime.getHours() + 1);
              }
              
              // Check if this booking is in the past
              const isPastBooking = endDateTime < now;
              console.log("Auth: ", auth.user.value);
              // Check if this booking belongs to the current user
              const isMyBooking = booking.studentId === currentUserId.value || 
                                 booking.studentId === auth.user.value?.studentId;
              
              // Select color based on status and whether it's user's own booking
              let backgroundColor;
              if (isMyBooking) {
                backgroundColor = '#9c27b0'; // Purple for own bookings
              } else {
                backgroundColor = booking.status === 'APPROVED' ? '#2e7d32' : 
                                 booking.status === 'CONFIRMED' ? '#1976d2' : '#f57c00';
              }
              
              // Create the booking event object for FullCalendar
              allBookings.push({
                id: booking.bookingId.toString(),
                resourceId: facility.facilityId.toString(),
                title: booking.facilityName || facility.resourceName,
                start: startDateTime.toISOString(),
                end: endDateTime.toISOString(),
                backgroundColor: backgroundColor,
                editable: !isPastBooking,
                durationEditable: !isPastBooking,
                startEditable: !isPastBooking,
                extendedProps: {
                  status: booking.status,
                  location: booking.location || facility.location,
                  isPast: isPastBooking,
                  description: '',
                  attendees: '',
                  studentId: booking.studentId || '',
                  studentName: booking.studentName || '',
                  isMyBooking: isMyBooking
                }
              });
            }
          } catch (err) {
            console.error('Error processing booking:', err, booking);
          }
        });
      }
    });
    
    console.log('Processed bookings:', allBookings.length);
    
    // Set the bookings array
    bookings.value = [...allBookings];
    
  } catch (error) {
    console.error('Error in searchFacilities:', error);
    if (!isInitialLoad) {
      alert('An error occurred while searching. Please try again.');
    }
  } finally {
    loading.value = false;
    searchLoading.value = false;
  }
}

// Function to fetch dropdown options from backend
async function fetchDropdownOptions() {
  try {
    console.log('Fetching dropdown options');
    optionsLoading.value = true;
    
    // Make a single API call to get all dropdown options
    const response = await fetch(`${apiUrl}/api/bookings/dropdown-options`, {
      headers: {
        Authorization: "Bearer " + auth.token.value
      }
    });
    
    if (response.ok) {
      const options = await response.json();
      console.log('Received dropdown options:', options);
      
      // Extract each type of option from the map
      if (options.resourceTypes) {
        resourceTypeOptions.value = options.resourceTypes;
      }
      
      if (options.locations) {
        locationOptions.value = options.locations;
      }
      
      if (options.resourceNames) {
        resourceNameOptions.value = options.resourceNames;
      }
    } else {
      console.error('Failed to fetch dropdown options:', response.status);
    }
    
  } catch (error) {
    console.error('Error fetching dropdown options:', error);
  } finally {
    optionsLoading.value = false;
  }
}

onMounted(() => {
  console.log('Component mounted, initializing');
  loading.value = true;

  fetchAvailableCredits(auth.user.value.email);
  
  // First fetch dropdown options
  fetchDropdownOptions()
    .then(() => {
      // Then search for facilities
      return searchFacilities({});
    })
    .catch(error => {
      console.error('Error during initialization:', error);
      loading.value = false;
    });

});
</script>

<template>
  <div class="mx-auto w-3/4 mt-8">
    <h1 class="text-2xl font-bold">Booking Management</h1>
    
    <!-- Search UI component -->
    <BookingSearchBar
      class="mt-4"
      :loading="searchLoading"
      :resource-type-options="resourceTypeOptions"
      :resource-name-options="resourceNameOptions"
      :location-options="locationOptions"
      :options-loading="optionsLoading"
      @search="handleSearch"
      @reset="handleReset"
    />
    
    <!-- Legend for booking status colors and available credits -->
    <div class="mt-4 p-3 bg-white rounded-md shadow">
      <div class="flex justify-between items-center mb-2">
        <h3 class="text-sm font-medium">Booking Status Legend:</h3>
        <div class="flex items-center">
          <span class="text-sm font-medium mr-2">Available Credits:</span>
          <span class="text-sm bg-blue-100 text-blue-800 px-2 py-1 rounded font-semibold">
            {{ formatCredits }}
          </span>
        </div>
      </div>
      <div class="flex flex-wrap gap-4">
        <div v-for="item in legendItems" :key="item.status" class="flex items-center">
          <div 
            class="w-4 h-4 rounded mr-1" 
            :style="{ backgroundColor: item.color }"
          ></div>
          <span class="text-sm">{{ item.label }}</span>
        </div>
      </div>
    </div>
    
    <!-- Calendar component -->
    <BookingCalendar
      ref="calendarRef"
      class="mt-4"
      :facilities="facilities"
      :bookings="bookings"
      :loading="loading"
      @select-timeslot="handleTimeslotSelect"
      @click-event="handleEventClick"
      @drop-event="handleEventDrop"
      @resize-event="handleEventResize"
      @date-change="handleDateChange"
    />
    
    <!-- Create booking modal -->
    <BookingCreateModal
      v-model="isCreateModalOpen"
      :resource-id="selectedBookingInfo.resourceId"
      :resource-name="selectedBookingInfo.resourceName"
      :start-time="selectedBookingInfo.startTime"
      @save="handleCreateBooking"
    />
    
    <!-- View booking details modal -->
    <BookingDetailsModal
      v-model="isViewModalOpen"
      :booking="selectedBooking"
      @edit="handleEditBooking"
    />
  </div>
</template>