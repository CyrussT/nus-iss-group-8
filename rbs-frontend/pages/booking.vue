<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { useApi } from '~/composables/useApi';
import { useToast } from '#imports'; // Updated import
import BookingCalendar from '~/components/booking/BookingCalendar.vue';
import BookingSearchBar from '~/components/booking/BookingSearchBar.vue';
import BookingCreateModal from '~/components/booking/BookingCreateModal.vue';
import BookingDetailsModal from '~/components/booking/BookingDetailsModal.vue';
import { useBooking } from '#imports';

const auth = useAuthStore();
const { apiUrl } = useApi();
const toast = useToast(); // Updated initialization
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
  duration: 30, // Default to 0.5 hours
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

// Format minutes to 0.5 hour increments
const formatCredits = computed(() => {
  if (!availableCredits.value && availableCredits.value !== 0) return '0 hrs';
  
  // Convert to 0.5 hour increments
  const halfHours = Math.floor(availableCredits.value / 30);
  
  if (halfHours === 0) {
    return '0 hrs';
  } else if (halfHours === 1) {
    return '0.5 hrs';
  } else if (halfHours === 2) {
    return '1 hr';
  } else if (halfHours % 2 === 0) {
    // Even number of half hours (whole hours)
    return `${halfHours / 2} hrs`;
  } else {
    // Odd number of half hours (X.5 hours)
    return `${Math.floor(halfHours / 2)}.5 hrs`;
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

// Check if current time is end of day (after 5 PM)
const isEndOfDay = () => {
  const now = new Date();
  return now.getHours() >= 17; // 5 PM or later
};

// Get the next business day (skip to Monday if it's Friday)
const getNextBusinessDay = () => {
  const now = new Date();
  const nextDay = new Date(now);
  nextDay.setDate(now.getDate() + 1);
  
  // If it's Friday, skip to Monday
  if (now.getDay() === 5) { // Friday
    nextDay.setDate(now.getDate() + 3);
  }
  
  return nextDay;
};

// Get the next half hour
const getNextHalfHour = () => {
  const now = new Date();
  const minutes = now.getMinutes();
  const newMinutes = minutes < 30 ? 30 : 0;
  const hoursToAdd = minutes < 30 ? 0 : 1;
  
  now.setMinutes(newMinutes);
  now.setSeconds(0);
  now.setMilliseconds(0);
  now.setHours(now.getHours() + hoursToAdd);
  
  return now;
};

// Format date for API (YYYY-MM-DD)
const formatDateForApi = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

// Open create booking modal from button
const openCreateBookingModal = () => {
  // Reset selections except for the date
  selectedBookingInfo.resourceId = '';
  selectedBookingInfo.resourceName = '';
  
  // Get the calendar's current date instead of clearing it
  if (calendarRef.value) {
    const currentCalendarDate = calendarRef.value.getCurrentCalendarDate();
    if (currentCalendarDate) {
      // Get the current date from the calendar
      const date = new Date(currentCalendarDate);
      
      // Get the current time
      const now = new Date();
      const currentHour = now.getHours();
      const currentMinute = now.getMinutes();
      
      // Check if the selected date is today
      const isToday = date.getDate() === now.getDate() && 
                     date.getMonth() === now.getMonth() && 
                     date.getFullYear() === now.getFullYear();
      
      let startHour, startMinute;
      
      if (isToday) {
        // If it's today, use the next half-hour slot
        if (currentMinute < 30) {
          // Use current hour and 30 minutes
          startHour = currentHour;
          startMinute = 30;
        } else {
          // Use next hour and 0 minutes
          startHour = currentHour + 1;
          startMinute = 0;
        }
        
        // Check if we're past business hours
        if (startHour >= 19) {
          // Default to start of next day
          date.setDate(date.getDate() + 1);
          startHour = 7; // First slot of the day
          startMinute = 0;
        }
      } else {
        // If it's a future date, use first time slot (7:00 AM)
        startHour = 7;
        startMinute = 0;
      }
      
      // Set the time
      date.setHours(startHour, startMinute, 0, 0);
      
      // Update startTime ISO string
      selectedBookingInfo.startTime = date.toISOString();
    } else {
      selectedBookingInfo.startTime = ''; // Fallback to empty if no date found
    }
  } else {
    selectedBookingInfo.startTime = ''; // Fallback to empty if no calendar ref
  }
  
  selectedBookingInfo.duration = 30; // Ensure duration is set to 0.5 hours
  
  // Show the create modal
  isCreateModalOpen.value = true;
};

// Add CSS to override FullCalendar's default cursor behavior
const addPointerCursorToEvents = () => {
  if (!calendarRef.value) return;
  
  setTimeout(() => {
    try {
      // Check if the calendar element exists directly
      const calendarEl = calendarRef.value.$el || calendarRef.value.el;
      
      if (!calendarEl) {
        console.warn('Calendar element not found');
        return;
      }
      
      // Add a style element with our custom CSS
      const styleEl = document.createElement('style');
      styleEl.textContent = `
        .fc-event,
        .fc-event-main,
        .fc-event-draggable,
        .fc-event-resizable,
        .fc-timegrid-event {
          cursor: pointer !important;
        }
      `;
      
      calendarEl.appendChild(styleEl);
    } catch (error) {
      console.error('Error adding pointer cursor to events:', error);
    }
  }, 200);
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
  // Get the selected date and time
  const selectedDateTime = new Date(info.startStr);
  const now = new Date();
  
  // Improved past date check
  const isPastDate = 
    (selectedDateTime.getFullYear() < now.getFullYear()) ||
    (selectedDateTime.getFullYear() === now.getFullYear() && 
     selectedDateTime.getMonth() < now.getMonth()) ||
    (selectedDateTime.getFullYear() === now.getFullYear() && 
     selectedDateTime.getMonth() === now.getMonth() && 
     selectedDateTime.getDate() < now.getDate());
  
  // Improved same-day past time check
  const isSameDay = 
    selectedDateTime.getFullYear() === now.getFullYear() &&
    selectedDateTime.getMonth() === now.getMonth() &&
    selectedDateTime.getDate() === now.getDate();
  
  const isPastTime = isSameDay && 
    (selectedDateTime.getHours() < now.getHours() ||
    (selectedDateTime.getHours() === now.getHours() && 
     selectedDateTime.getMinutes() < now.getMinutes()));
  
  // Only check for past time if it's the same day
  if (isPastDate || isPastTime) {
    toast.add({
      title: 'Error',
      description: 'Cannot create bookings in the past',
      color: 'red'
    });
    return;
  }
  
  // Default booking duration to 30 minutes
  selectedBookingInfo.resourceId = info.resource.id;
  selectedBookingInfo.resourceName = info.resource.title;
  selectedBookingInfo.startTime = info.startStr;
  
  // Force modal to use 30 minutes as default duration
  selectedBookingInfo.duration = 30;
  
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

// Update the handleCreateBooking function - now returns a Promise for modal handling
const handleCreateBooking = async (booking) => {
  try {
    // Show loading state
    searchLoading.value = true;
    
    // Get the current user email from auth store
    const accountEmail = auth.user.value.email;
    
    // Create a proper local date from the booking date/time information
    let bookedDateTime;
    
    if (booking.localDate && booking.localTime) {
      // Use the local date/time if available
      const [year, month, day] = booking.localDate.split('-').map(Number);
      const [hours, minutes] = booking.localTime.split(':').map(Number);
      
      // Month is 0-indexed in JavaScript Date
      bookedDateTime = new Date(year, month - 1, day, hours, minutes, 0, 0);
    } else {
      // Fallback to using the ISO string (which might have timezone issues)
      bookedDateTime = new Date(booking.start);
    }
    
    // Format the timeslot
    const startTime = bookedDateTime.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      hour12: false 
    });
    
    // Calculate end time (add duration minutes to start time)
    const durationMinutes = parseInt(booking.creditsUsed, 10);
    const endDateTime = new Date(bookedDateTime.getTime() + durationMinutes * 60000);
    
    const endTime = endDateTime.toLocaleTimeString('en-US', {
      hour: '2-digit', 
      minute: '2-digit',
      hour12: false
    });
    
    const timeSlot = `${startTime} - ${endTime}`;
    
    console.log("Booking datetime:", {
      originalStart: booking.start,
      localDateString: booking.localDate,
      localTimeString: booking.localTime,
      bookedDateTime: bookedDateTime.toISOString(),
      timeSlot
    });
    
    // Create the request body
    const requestBody = {
      facilityId: parseInt(booking.resourceId),
      accountEmail: accountEmail,
      bookedDateTime: bookedDateTime.toISOString(),
      timeSlot: timeSlot,
      title: booking.title,
      description: booking.description,
      attendees: booking.attendees,
      creditsUsed: booking.creditsUsed
    };
    
    console.log("Full booking request body:", requestBody);
    
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
    
    // Show success message using toast
    toast.add({
      title: 'Success',
      description: 'Booking created successfully!',
      color: 'green'
    });
    
    // Refresh the calendar to show the new booking
    searchFacilities({});
    
    // Refresh available credits since we just used some
    fetchAvailableCredits(auth.user.value.email);
    
    // Close the modal only on success
    isCreateModalOpen.value = false;
    
    // Return success for the modal component
    return true;
    
  } catch (error) {
    console.error('Error creating booking:', error);
    toast.add({
      title: 'Error',
      description: 'Failed to create booking. Please try again.',
      color: 'red'
    });
    
    // Return failure to keep modal open
    return false;
  } finally {
    searchLoading.value = false;
  }
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
                // Default to 30 minutes duration if no end time
                endDateTime = new Date(startDateTime);
                endDateTime.setMinutes(endDateTime.getMinutes() + 30);
              }
              
              // Check if this booking is in the past
              const isPastBooking = endDateTime < now;
              
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
                editable: false, // Disable all drag and resize for bookings
                durationEditable: false,
                startEditable: false,
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
      toast.add({
        title: 'Error',
        description: 'An error occurred while searching. Please try again.',
        color: 'red'
      });
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
    .then(() => {
      // Apply cursor styles
      addPointerCursorToEvents();
    })
    .catch(error => {
      console.error('Error during initialization:', error);
      loading.value = false;
      toast.add({
        title: 'Error',
        description: 'Failed to initialize booking system. Please try refreshing the page.',
        color: 'red'
      });
    });
});

// Expose methods to parent components that might need them
const updateCalendarResources = (resources) => {
  if (calendarRef.value) {
    calendarRef.value.updateCalendarResources(resources);
  }
};

const updateCalendarEvents = (events) => {
  if (calendarRef.value) {
    calendarRef.value.updateCalendarEvents(events);
  }
};

// Watch for bookings changes if needed
watch(() => bookings.value, (newBookings) => {
  if (calendarRef.value) {
    // Apply pointer cursor after booking updates
    addPointerCursorToEvents();
  }
}, { deep: true });
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
    
    <!-- New: Create Booking Button Section -->
    <div class="mt-4 mb-4 flex justify-end">
      <UButton 
        color="primary" 
        icon="i-heroicons-plus-circle" 
        @click="openCreateBookingModal"
        size="lg"
      >
        Create New Booking
      </UButton>
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
    
    <!-- Create booking modal - reused for both calendar select and button click -->
    <BookingCreateModal
      v-model="isCreateModalOpen"
      :resource-id="selectedBookingInfo.resourceId"
      :resource-name="selectedBookingInfo.resourceName"
      :start-time="selectedBookingInfo.startTime"
      :facilities="facilities"
      :available-credits="availableCredits"
      :loading="searchLoading"
      :key="`booking-modal-${isCreateModalOpen}`"
      @save="handleCreateBooking"
    />
    
    <!-- View booking details modal -->
    <BookingDetailsModal
      v-model="isViewModalOpen"
      :booking="selectedBooking"
    />
  </div>
</template>