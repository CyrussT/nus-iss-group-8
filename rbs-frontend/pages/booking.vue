<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { useApi } from '~/composables/useApi';
import { useToast } from '#imports';
import BookingCalendar from '~/components/booking/BookingCalendar.vue';
import BookingSearchBar from '~/components/booking/BookingSearchBar.vue';
import BookingCreateModal from '~/components/booking/BookingCreateModal.vue';
import BookingDetailsModal from '~/components/booking/BookingDetailsModal.vue';
import { useBooking } from '#imports';

const auth = useAuthStore();
const { apiUrl } = useApi();
const toast = useToast();
// Get all the booking related functions from the useBooking composable
const bookingModule = useBooking();
const { 
  availableCredits, 
  fetchAvailableCredits, 
  searchFacilities, 
  fetchDropdownOptions, 
  createBooking, 
  facilities, 
  searchLoading 
} = bookingModule;

// State variables
const bookings = ref([]);
const loading = ref(true);
const optionsLoading = ref(false);
const hasSearched = ref(false); // Track if a search has been performed

// Dropdown options
const resourceTypeOptions = ref([]);
const resourceNameOptions = ref([]);
const locationOptions = ref([]);

// Current search criteria to track for "No Results" messaging
const currentSearchCriteria = ref({});

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
  isPast: false,
  studentId: '',
  studentName: ''
});

// Legend items for booking status colors with more accessible colors
const legendItems = [
  { status: 'OCCUPIED', color: '#ff9999', label: 'Occupied' },     
  { status: 'PENDING', color: '#b8b8b8', label: 'Pending' },       
  { status: 'MY_BOOKING', color: '#b088ff', label: 'My Booking (Approved)' }, 
  { status: 'MY_PENDING_BOOKING', color: '#7fa2e6', label: 'My Booking (Pending)' } 
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
// const currentUserId = computed(() => auth.user.value?.id || '');

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
        
        /* Hide event title text */
        .fc-event-title {
          display: none !important;
        }
        
        /* Make timegrid cell display pointer cursor to indicate it's clickable */
        .fc-timegrid-slot, .fc-timeline-slot-frame {
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
const handleSearch = async (searchCriteria) => {
  try {
    // Set the hasSearched flag to true since we're performing a search
    hasSearched.value = true;
    
    // Store the current search criteria for potential "No Results" messaging
    // but exclude internal parameters that start with underscore
    currentSearchCriteria.value = { ...searchCriteria };
    Object.keys(currentSearchCriteria.value).forEach(key => {
      if (key.startsWith('_')) {
        delete currentSearchCriteria.value[key];
      }
    });
    
    // Get the current date from the calendar if available or from the search criteria
    let currentDate = '';
    
    // First check if date was explicitly provided in search criteria
    if (searchCriteria._date) {
      currentDate = searchCriteria._date;
      console.log("Using provided date parameter:", currentDate);
    } 
    // Then check the calendar reference
    else if (calendarRef.value) {
      currentDate = calendarRef.value.getCurrentCalendarDate();
      console.log("Using calendar date:", currentDate);
    } 
    // Finally use today as a fallback
    else {
      // If calendar ref is not available, use today's date as fallback
      const today = new Date();
      currentDate = formatDateForApi(today);
      console.log("Using today's date as fallback:", currentDate);
    }
    
    // Ensure we always have a date to prevent the backend error
    if (!currentDate) {
      const today = new Date();
      currentDate = formatDateForApi(today);
      console.log("No date available, using today:", currentDate);
    }
    
    // Remove internal parameters from the search criteria before sending to API
    const apiSearchCriteria = { ...searchCriteria };
    Object.keys(apiSearchCriteria).forEach(key => {
      if (key.startsWith('_')) {
        delete apiSearchCriteria[key];
      }
    });
    
    await searchFacilities(apiUrl, auth.token.value, apiSearchCriteria, currentDate);
    processBookings();
  } catch (error) {
    console.error('Error handling search:', error);
    toast.add({
      title: 'Error',
      description: 'An error occurred while searching. Please try again.',
      color: 'red'
    });
  }
};

// Handle reset search
const handleReset = () => {
  // Reset hasSearched flag
  hasSearched.value = false;
  
  // Clear the current search criteria but preserve the date
  currentSearchCriteria.value = {};
  
  // Get the current date from the calendar if available
  let currentDate = '';
  if (calendarRef.value) {
    currentDate = calendarRef.value.getCurrentCalendarDate();
    console.log("Reset using calendar date:", currentDate);
  } else {
    // If calendar ref is not available, use today's date as fallback
    const today = new Date();
    currentDate = formatDateForApi(today);
    console.log("Using today's date as fallback:", currentDate);
  }
  
  // Call handleSearch with empty criteria but include the current calendar date
  // This preserves the calendar's date while clearing all search criteria
  handleSearch({ _date: currentDate });
  
  // Emit an event to tell the BookingSearchBar to reset its form fields
  // We'll implement this in the BookingSearchBar component
  if (document.querySelector('.booking-search-bar')) {
    document.querySelector('.booking-search-bar').dispatchEvent(
      new CustomEvent('reset-search-fields')
    );
  }
};

// Handle reset search from calendar component
const handleResetSearchFromCalendar = (data) => {
  // Include date information if provided from the calendar
  const resetOptions = data && data.date ? { _date: data.date } : {};
  
  // Reset hasSearched flag
  hasSearched.value = false;
  
  // Clear current search criteria
  currentSearchCriteria.value = {};
  
  // Perform search with reset options
  handleSearch(resetOptions);
  
  // Also reset the search form
  if (document.querySelector('.booking-search-bar')) {
    document.querySelector('.booking-search-bar').dispatchEvent(
      new CustomEvent('reset-search-fields')
    );
  }
};

// Handle calendar date change
const handleDateChange = () => {
  handleSearch(currentSearchCriteria.value);
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
    title: event.extendedProps?.title || event.title || 'Untitled Booking', // Look for title in extendedProps first
    start: event.start,
    end: event.end,
    resourceId: event.getResources()[0]?.id || '',
    resourceName: event.getResources()[0]?.title || 'Unknown Resource',
    location: event.extendedProps?.location || '',
    status: event.extendedProps?.status || 'Unknown',
    description: event.extendedProps?.originalDescription || event.extendedProps?.description || '', // Use original description
    isPast: event.extendedProps?.isPast || false,
    studentId: event.extendedProps?.studentId || '',
    studentName: event.extendedProps?.studentName || ''
  });
  
  isViewModalOpen.value = true;
};

// Process bookings from facilities
const processBookings = () => {
  const allBookings = [];
  const now = new Date();

  // Get current user email for comparison
  console.log("Current user email:", currentUserEmail.value);
  
  if (facilities.value && facilities.value.length > 0) {
    facilities.value.forEach(facility => {
      if (facility.bookings && facility.bookings.length > 0) {
        facility.bookings.forEach(booking => {
          try {
            // Get the date from bookedDatetime
            const bookedDate = new Date(booking.bookedDatetime);
            
            if (!booking.timeslot) {
              console.error('Booking has no timeslot:', booking);
              return; // Skip this booking
            }
            
            // Log booking details for debugging
            console.log(`Processing booking ${booking.bookingId} with email: ${booking.email}, status: ${booking.status}`);
            
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
              
              // Create local date using the timeslot information rather than ISO date
              // Extract the local date from the bookedDatetime
              const year = bookedDate.getFullYear();
              const month = bookedDate.getMonth();
              const day = bookedDate.getDate();
              
              // Create a new date object with the local date and time from timeslot
              const startDateTime = new Date(year, month, day, startHour || 0, startMinute || 0, 0, 0);
              
              // Handle end time if it exists
              let endDateTime;
              
              if (endTime) {
                const [endHour, endMinute] = endTime.trim().split(":").map(Number);
                
                endDateTime = new Date(year, month, day, endHour || 0, endMinute || 0, 0, 0);
              } else {
                // Default to 30 minutes duration if no end time
                endDateTime = new Date(startDateTime);
                endDateTime.setMinutes(endDateTime.getMinutes() + 30);
              }
              
              // Check if this booking is in the past
              const isPastBooking = endDateTime < now;
              
              // Simple direct string comparison with booking email
              const isMyBooking = booking.email === currentUserEmail.value;
              
              console.log(`Is booking ${booking.bookingId} mine? ${isMyBooking}`);
              
              // Select color based on a combination of:
              // 1. Whether it's your booking or not
              // 2. The status of the booking (PENDING, APPROVED, etc.)
              let backgroundColor;
              let borderColor;
              let legendStatus;
              
              if (isMyBooking) {
                if (booking.status === 'PENDING') {
                  // Your pending booking - darker blue
                  backgroundColor = '#7fa2e6'; // Updated color
                  borderColor = '#6c8fd3'; // Darker border
                  legendStatus = 'MY_PENDING_BOOKING';
                  console.log(`Setting booking ${booking.bookingId} as MY PENDING BOOKING with blue color`);
                } else {
                  // Your approved booking - darker purple
                  backgroundColor = '#b088ff'; // Updated color
                  borderColor = '#9e75ec'; // Darker border
                  legendStatus = 'MY_BOOKING';
                  console.log(`Setting booking ${booking.bookingId} as MY APPROVED BOOKING with purple color`);
                }
              } else {
                if (booking.status === 'PENDING') {
                  // Other's pending booking - darker grey
                  backgroundColor = '#b8b8b8'; // Updated color
                  borderColor = '#a7a7a7'; // Darker border
                  legendStatus = 'PENDING';
                  console.log(`Setting booking ${booking.bookingId} as OTHER'S PENDING BOOKING with grey color`);
                } else {
                  // Other's approved booking - darker red
                  backgroundColor = '#ff9999'; // Updated color
                  borderColor = '#ee8888'; // Darker border
                  legendStatus = 'OCCUPIED';
                  console.log(`Setting booking ${booking.bookingId} as OTHER'S APPROVED BOOKING with red color`);
                }
              }
              
              // Create the booking event object for FullCalendar
              allBookings.push({
                id: booking.bookingId.toString(),
                resourceId: facility.facilityId.toString(),
                title: '', // Keep empty for visual purposes
                start: startDateTime.toISOString(),
                end: endDateTime.toISOString(),
                backgroundColor: backgroundColor,
                borderColor: borderColor,
                textColor: 'transparent', // Make text invisible
                editable: false, // Disable all drag and resize for bookings
                durationEditable: false,
                startEditable: false,
                extendedProps: {
                  status: booking.status,
                  legendStatus: legendStatus,
                  location: booking.location || facility.location,
                  isPast: isPastBooking,
                  description: booking.description || '',
                  studentId: booking.studentId || '',
                  studentName: booking.studentName || '',
                  isMyBooking: isMyBooking,
                  // Store the original title and description
                  title: booking.title || '', // Add this line
                  originalDescription: booking.description || '' // Add this line
                }
              });
            }
          } catch (err) {
            console.error('Error processing booking:', err, booking);
          }
        });
      }
    });
  }
  
  // Set the bookings array
  bookings.value = [...allBookings];
};

// Update the handleCreateBooking function - now returns a Promise for modal handling
const handleCreateBooking = async (booking) => {
  try {
    // Add user email to the booking
    booking.accountEmail = auth.user.value.email;
    
    // Create the booking using the composable function
    await createBooking(apiUrl, auth.token.value, booking);
    
    // Show success message using toast
    toast.add({
      title: 'Success',
      description: 'Booking created successfully!',
      color: 'green'
    });
    
    // Refresh the calendar to show the new booking
    await handleSearch(currentSearchCriteria.value);
    
    // Refresh available credits since we just used some
    await fetchAvailableCredits(auth.user.value.email);
    
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
  }
};

// Load dropdown options
const loadDropdownOptions = async () => {
  try {
    optionsLoading.value = true;
    const options = await fetchDropdownOptions(apiUrl, auth.token.value);
    
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
  } catch (error) {
    console.error('Error loading dropdown options:', error);
    toast.add({
      title: 'Error',
      description: 'Failed to load dropdown options. Please try refreshing the page.',
      color: 'red'
    });
  } finally {
    optionsLoading.value = false;
  }
};

// Fixed onMounted hook to prevent duplicate API calls
onMounted(async () => {
  console.log('Component mounted, initializing');
  loading.value = true;

  try {
    // Fetch available credits first
    await fetchAvailableCredits(auth.user.value.email);
    
    // Then load dropdown options
    await loadDropdownOptions();
    
    // Get today's date
    const today = new Date();
    const formattedToday = formatDateForApi(today);
    
    // Only after options are loaded, do a single search with today's date
    await handleSearch({});
    
    // Apply cursor styles
    addPointerCursorToEvents();
  } catch (error) {
    console.error('Error during initialization:', error);
    toast.add({
      title: 'Error',
      description: 'Failed to initialize booking system. Please try refreshing the page.',
      color: 'red'
    });
  } finally {
    loading.value = false;
  }
});

// Watch for bookings changes if needed
watch(() => bookings.value, (newBookings) => {
  if (calendarRef.value) {
    // Apply pointer cursor after booking updates
    addPointerCursorToEvents();
  }
}, { deep: true });

// Watch for facilities changes to detect empty results
watch(() => facilities.value, (newFacilities) => {
  console.log("Facilities updated:", newFacilities ? newFacilities.length : 0, "facilities found");
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
    <div class="mt-4 p-4 bg-white rounded-md shadow">
      <div class="flex justify-between items-center mb-3">
        <h3 class="text-sm font-medium">Booking Status Legend:</h3>
        <div class="flex items-center">
          <span class="text-sm font-medium mr-2">Available Credits:</span>
          <span class="text-sm bg-blue-100 text-blue-800 px-3 py-1 rounded font-semibold">
            {{ formatCredits }}
          </span>
        </div>
      </div>
      
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 mb-3">
        <div v-for="item in legendItems" :key="item.status" 
            class="flex items-center p-2 rounded-md border border-gray-100 hover:bg-gray-50 transition-colors">
          <div 
            class="w-7 h-7 rounded-md mr-3 border border-gray-200" 
            :style="{ backgroundColor: item.color }"
          ></div>
          <span class="text-sm font-medium">{{ item.label }}</span>
        </div>
      </div>
      
      <!-- Hint about calendar clicking -->
      <div class="text-xs text-gray-600 mt-3 flex items-center p-2 bg-blue-50 rounded-md">
        <UIcon name="i-heroicons-information-circle" class="mr-2 text-blue-500" size="sm" />
        <span>Click on an empty timeslot in the calendar grid to create a new booking. Click on a colored slot to view its details.</span>
      </div>
    </div>
    
    <!-- Centralized No results message - only shown when search is performed but no facilities found -->
    <div v-if="hasSearched && (!facilities || facilities.length === 0) && !loading && !searchLoading" class="mt-4 p-6 bg-white rounded-md shadow text-center">
      <UIcon name="i-heroicons-magnifying-glass" class="mx-auto mb-3 text-gray-400" size="lg" />
      <h3 class="text-xl font-medium text-gray-700 mb-2">No Facilities Found</h3>
      <p class="text-gray-500 mb-4">No resources match your search criteria for the selected date.</p>
      <div class="flex justify-center space-x-3">
        <UButton color="gray" variant="soft" @click="handleReset">
          Reset Search
        </UButton>
        <UButton color="primary" variant="soft" icon="i-heroicons-arrow-path" @click="calendarRef?.forceRefresh()">
          Try Again
        </UButton>
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
    
    <!-- Calendar component - Only show if we have facilities or are still loading -->
    <BookingCalendar
      v-if="!hasSearched || loading || searchLoading || (facilities && facilities.length > 0)"
      ref="calendarRef"
      class="mt-4"
      :facilities="facilities"
      :bookings="bookings"
      :loading="loading"
      @select-timeslot="handleTimeslotSelect"
      @click-event="handleEventClick"
      @date-change="handleDateChange"
      @reset-search="handleResetSearchFromCalendar"
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