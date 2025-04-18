<script setup>
definePageMeta({
  middleware: ['auth', 'student']
});

import { ref, reactive, onMounted, computed, watch } from 'vue';
import { useApi } from '~/composables/useApi';
import { useToast } from '#imports';
import BookingCalendar from '~/components/booking/BookingCalendar.vue';
import BookingSearchBar from '~/components/booking/BookingSearchBar.vue';
import BookingCreateModal from '~/components/booking/BookingCreateModal.vue';
import BookingDetailsModal from '~/components/booking/BookingDetailsModal.vue';
import { useBooking } from '#imports';
import { useMaintenance } from '~/composables/useMaintenance';

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

// Get maintenance related functions
const maintenanceModule = useMaintenance();
const {
  checkMultipleFacilities,
  isUnderMaintenance,
  maintenanceLoading,
  facilitiesUnderMaintenance
} = maintenanceModule;

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
  { status: 'OCCUPIED', color: '#ff9999', darkColor: '#ff6666', label: 'Occupied' },     
  { status: 'PENDING', color: '#b8b8b8', darkColor: '#a0a0a0', label: 'Pending' },       
  { status: 'MY_BOOKING', color: '#b088ff', darkColor: '#c09fff', label: 'My Booking (Approved)' }, 
  { status: 'MY_PENDING_BOOKING', color: '#7fa2e6', darkColor: '#8fb8ff', label: 'My Booking (Pending)' },
  { status: 'MAINTENANCE', color: '#f6c46a', darkColor: '#ffcc66', label: 'Maintenance' }
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

// Modal visibility
const isCreateModalOpen = ref(false);
const isViewModalOpen = ref(false);

// Refs for components
const calendarRef = ref(null);

// Format date for API (YYYY-MM-DD)
const formatDateForApi = (date) => {
  if (!date) return '';
  const d = new Date(date);
  const formattedDate = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  return formattedDate;
};

// Helper function to check if a facility is under maintenance
const isFacilityUnderMaintenance = (facilityId) => {
  if (!facilityId) return false;
  
  // Get the calendar's current date
  let currentDate = '';
  if (calendarRef.value) {
    currentDate = calendarRef.value.getCurrentCalendarDate();
  } else {
    // If calendar ref is not available, use today's date as fallback
    currentDate = formatDateForApi(new Date());
  }
  
  return isUnderMaintenance(facilityId, currentDate);
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
        // If it's today, check if we're in business hours (7 AM - 7 PM)
        if (currentHour < 7) {
          // Before business hours - use 7 AM
          startHour = 7;
          startMinute = 0;
        } else if (currentHour >= 19) {
          // After business hours - use 7 AM next day
          date.setDate(date.getDate() + 1);
          startHour = 7;
          startMinute = 0;
        } else {
          // During business hours - use next half-hour slot
          if (currentMinute < 30) {
            // Use current hour and 30 minutes
            startHour = currentHour;
            startMinute = 30;
          } else {
            // Use next hour and 0 minutes
            startHour = currentHour + 1;
            startMinute = 0;
            
            // Check if next hour would be after business hours
            if (startHour >= 19) {
              // If so, use next day at 7 AM
              date.setDate(date.getDate() + 1);
              startHour = 7;
              startMinute = 0;
            }
          }
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
    } 
    // Then check the calendar reference
    else if (calendarRef.value) {
      currentDate = calendarRef.value.getCurrentCalendarDate();
    } 
    // Finally use today as a fallback
    else {
      // If calendar ref is not available, use today's date as fallback
      const today = new Date();
      currentDate = formatDateForApi(today);
    }
    
    // Ensure we always have a date to prevent the backend error
    if (!currentDate) {
      const today = new Date();
      currentDate = formatDateForApi(today);
    }
    
    // Remove internal parameters from the search criteria before sending to API
    const apiSearchCriteria = { ...searchCriteria };
    Object.keys(apiSearchCriteria).forEach(key => {
      if (key.startsWith('_')) {
        delete apiSearchCriteria[key];
      }
    });
    
    // First search for facilities
    await searchFacilities(apiUrl, auth.token.value, apiSearchCriteria, currentDate);
    
    // Check maintenance status for all facilities in a single API call
    if (facilities.value && facilities.value.length > 0) {
      const facilityIds = facilities.value.map(facility => facility.facilityId);
      
      // Ensure we pass the same date used for facility search to the maintenance check
      await checkMultipleFacilities(facilityIds, currentDate);
    }
    
    // Process bookings after all data is fetched
    processBookings();
    
    // Manually tell the calendar to apply maintenance styling
    if (calendarRef.value && calendarRef.value.applyMaintenanceStyling) {
      setTimeout(() => calendarRef.value.applyMaintenanceStyling(), 300);
    }
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
  } else {
    // If calendar ref is not available, use today's date as fallback
    const today = new Date();
    currentDate = formatDateForApi(today);
  }
  
  // Call handleSearch with empty criteria but include the current calendar date
  // This preserves the calendar's date while clearing all search criteria
  handleSearch({ _date: currentDate });
  
  // Emit an event to tell the BookingSearchBar to reset its form fields
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
  // Get the current date from the calendar
  if (calendarRef.value) {
    const currentDate = calendarRef.value.getCurrentCalendarDate();
    
    // Include the date explicitly in the search criteria
    const updatedCriteria = { 
      ...currentSearchCriteria.value,
      _date: currentDate 
    };
    
    // Pass the updated criteria with the date
    handleSearch(updatedCriteria);
  } else {
    // Fallback if calendar ref is not available
    handleSearch(currentSearchCriteria.value);
  }
};

// Handle timeslot selection
const handleTimeslotSelect = (info) => {
  // Check if the resource is under maintenance
  if (isFacilityUnderMaintenance(info.resource.id)) {
    toast.add({
      title: 'Facility Unavailable',
      description: 'This facility is currently under maintenance and cannot be booked.',
      color: 'orange'
    });
    return;
  }
  
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
  // Get the event properties
  const isMaintenance = event.extendedProps?.isMaintenance || false;
  
  // If this is a maintenance event, show a special message
  if (isMaintenance) {
    const resourceName = event.getResources()[0]?.title || 'Unknown Facility';
    
    toast.add({
      title: 'Maintenance Event',
      description: `${resourceName} is under maintenance and cannot be booked.`,
      color: 'orange'
    });
    return; // Return early for maintenance events - don't open any modal
  }
  
  // Otherwise prepare the regular booking details
  Object.assign(selectedBooking, {
    id: event.id,
    title: event.extendedProps?.title || event.title || 'Untitled Booking',
    start: event.start,
    end: event.end,
    resourceId: event.getResources()[0]?.id || '',
    resourceName: event.getResources()[0]?.title || 'Unknown Resource',
    location: event.extendedProps?.location || '',
    status: event.extendedProps?.status || 'Unknown',
    description: event.extendedProps?.originalDescription || event.extendedProps?.description || '',
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
  if (facilities.value && facilities.value.length > 0) {
    facilities.value.forEach(facility => {
      const facilityId = facility.facilityId.toString();
      
      // Get the current date from the calendar or use today's date
      const currentDate = calendarRef.value ? calendarRef.value.getCurrentCalendarDate() : formatDateForApi(new Date());
      
      // Check if this facility is under maintenance for the current date
      const isMaintenanceResource = isUnderMaintenance(facilityId, currentDate);
      
      // If the facility is under maintenance, add maintenance "booking" covering entire day
      if (isMaintenanceResource) {
        try {
          // Get the current date
          let displayDate = currentDate ? new Date(currentDate) : new Date();
          
          // Create an all-day maintenance booking
          const startDateTime = new Date(displayDate);
          startDateTime.setHours(7, 0, 0, 0); // Start at 7 AM
          
          const endDateTime = new Date(displayDate);
          endDateTime.setHours(19, 0, 0, 0); // End at 7 PM
          
          // Add maintenance event
          allBookings.push({
            id: `maintenance-${facilityId}`,
            resourceId: facilityId,
            title: 'Maintenance', 
            start: startDateTime.toISOString(),
            end: endDateTime.toISOString(),
            backgroundColor: '#f6c46a', // Orange-yellow color from legend
            borderColor: '#e0b25e', // Darker border
            textColor: '#000000',  // Make text visible
            editable: false, // Disable all drag and resize for bookings
            durationEditable: false,
            startEditable: false,
            display: 'block',
            className: 'maintenance-event', // Add a class for targeting in CSS
            extendedProps: {
              status: 'MAINTENANCE',
              legendStatus: 'MAINTENANCE',
              description: 'This facility is under maintenance and not available for booking.',
              isPast: false,
              isMaintenance: true
            }
          });
        } catch (err) {
          console.error('Error creating maintenance booking:', err);
        }
      }
      
      // Add regular bookings
      if (facility.bookings && facility.bookings.length > 0) {
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
                } else {
                  // Your approved booking - darker purple
                  backgroundColor = '#b088ff'; // Updated color
                  borderColor = '#9e75ec'; // Darker border
                  legendStatus = 'MY_BOOKING';
                }
              } else {
                if (booking.status === 'PENDING') {
                  // Other's pending booking - darker grey
                  backgroundColor = '#b8b8b8'; // Updated color
                  borderColor = '#a7a7a7'; // Darker border
                  legendStatus = 'PENDING';
                } else {
                  // Other's approved booking - darker red
                  backgroundColor = '#ff9999'; // Updated color
                  borderColor = '#ee8888'; // Darker border
                  legendStatus = 'OCCUPIED';
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
                  title: booking.title || '',
                  originalDescription: booking.description || ''
                }
              });
            }
          } catch (err) {
            console.error('Error processing booking:', err);
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
    // Check if the facility is under maintenance before creating the booking
    const resourceId = booking.resourceId;
    
    if (!resourceId) {
      toast.add({
        title: 'Error',
        description: 'Please select a facility for booking.',
        color: 'red'
      });
      return false;
    }
    
    // Check if facility is under maintenance
    if (isFacilityUnderMaintenance(resourceId)) {
      toast.add({
        title: 'Booking Failed',
        description: 'This facility is currently under maintenance and cannot be booked.',
        color: 'orange'
      });
      return false;
    }
    
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
    
    // Only after options are loaded, do a single search with today's date
    await handleSearch({});
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

// Watch for calendar reference to ensure we can apply maintenance styling
watch(() => calendarRef.value, (newValue) => {
  if (newValue) {
    // Apply maintenance styling when calendar ref becomes available
    setTimeout(() => {
      if (calendarRef.value && calendarRef.value.applyMaintenanceStyling) {
        calendarRef.value.applyMaintenanceStyling();
      }
    }, 500);
  }
});

// Watch for bookings changes if needed
watch(() => bookings.value, (newBookings) => {
  if (calendarRef.value && calendarRef.value.applyMaintenanceStyling) {
    // Apply maintenance styling after bookings update
    setTimeout(() => calendarRef.value.applyMaintenanceStyling(), 200);
  }
}, { deep: true });

// Watch for facilities changes to detect empty results and apply maintenance
watch(() => facilities.value, (newFacilities) => {
  // Apply maintenance styling after facilities update
  if (calendarRef.value && calendarRef.value.applyMaintenanceStyling) {
    setTimeout(() => calendarRef.value.applyMaintenanceStyling(), 200);
  }
}, { deep: true });

// Watch for maintenance status changes
watch(() => facilitiesUnderMaintenance, () => {
  // When maintenance status changes, reprocess bookings to update maintenance events
  processBookings();
  
  // Then apply maintenance styling
  if (calendarRef.value && calendarRef.value.applyMaintenanceStyling) {
    setTimeout(() => calendarRef.value.applyMaintenanceStyling(), 200);
  }
}, { deep: true });
</script>

<template>
  <div class="mx-auto w-3/4 mt-8">
    <h1 class="text-2xl font-bold dark:text-white">Booking Management</h1>
    
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
    <div class="mt-4 p-4 bg-white dark:bg-gray-800 rounded-md shadow dark:shadow-gray-700">
      <div class="flex justify-between items-center mb-3">
        <h3 class="text-sm font-medium dark:text-white">Booking Status Legend:</h3>
        <div class="flex items-center">
          <span class="text-sm font-medium mr-2 dark:text-gray-300">Available Credits:</span>
          <span class="text-sm bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-100 px-3 py-1 rounded font-semibold">
            {{ formatCredits }}
          </span>
        </div>
      </div>
      
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3 mb-3">
        <div v-for="item in legendItems" :key="item.status" 
            class="flex items-center p-2 rounded-md border border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
          <div 
            class="w-7 h-7 rounded-md mr-3 border border-gray-200 dark:border-gray-600" 
            :style="{ backgroundColor: $colorMode.value === 'dark' ? item.darkColor : item.color }"
          ></div>
          <span class="text-sm font-medium dark:text-gray-200">{{ item.label }}</span>
        </div>
      </div>
      
      <!-- Hint about calendar clicking -->
      <div class="text-xs text-gray-600 dark:text-gray-400 mt-3 flex items-center p-2 bg-blue-50 dark:bg-blue-900/30 rounded-md">
        <UIcon name="i-heroicons-information-circle" class="mr-2 text-blue-500 dark:text-blue-400" size="sm" />
        <span>Click on an empty timeslot in the calendar grid to create a new booking. Click on a colored slot to view its details. Facilities under maintenance cannot be booked.</span>
      </div>
    </div>
    
    <!-- Centralized No results message - only shown when search is performed but no facilities found -->
    <div v-if="hasSearched && (!facilities || facilities.length === 0) && !loading && !searchLoading" class="mt-4 p-6 bg-white dark:bg-gray-800 rounded-md shadow dark:shadow-gray-700 text-center">
      <UIcon name="i-heroicons-magnifying-glass" class="mx-auto mb-3 text-gray-400 dark:text-gray-500" size="lg" />
      <h3 class="text-xl font-medium text-gray-700 dark:text-gray-200 mb-2">No Facilities Found</h3>
      <p class="text-gray-500 dark:text-gray-400 mb-4">No resources match your search criteria for the selected date.</p>
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

<style scoped>
/* Maintenance booking indicator animation */
@keyframes wrench-rotate {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-15deg); }
  75% { transform: rotate(15deg); }
}

.maintenance-indicator {
  animation: wrench-rotate 2s infinite ease-in-out;
}

/* Maintenance event styling */
:deep(.maintenance-event) {
  height: 26px !important;
  min-height: 26px !important;
  background-color: #f6c46a !important; /* Match legend color */
  border-color: #e0b25e !important;
  opacity: 0.9 !important;
  cursor: not-allowed !important;
  border-radius: 4px !important;
  box-shadow: 0 1px 3px rgba(0,0,0,0.12) !important;
}

:deep(.dark .maintenance-event) {
  background-color: #ffcc66 !important; /* Brighter color for dark mode */
  border-color: #e6b800 !important;
  box-shadow: 0 1px 3px rgba(0,0,0,0.5) !important;
}

:deep(.maintenance-event .fc-event-main) {
  cursor: not-allowed !important;
}

:deep(.maintenance-event:hover) {
  transform: none !important;
  box-shadow: 0 1px 3px rgba(0,0,0,0.12) !important;
}

:deep(.dark .maintenance-event:hover) {
  box-shadow: 0 1px 3px rgba(0,0,0,0.5) !important;
}

:deep(.maintenance-event .fc-event-title) {
  display: block !important;
  text-align: center;
  font-weight: bold;
  color: #000;
}

:deep(.dark .maintenance-event .fc-event-title) {
  color: #000;
}

/* Resource under maintenance styling */
:deep(.resource-under-maintenance .fc-timeline-slot-lane) {
  background-color: rgba(246, 196, 106, 0.3) !important;
  background-image: repeating-linear-gradient(
    45deg,
    rgba(255, 255, 255, 0.2),
    rgba(255, 255, 255, 0.2) 10px,
    rgba(246, 196, 106, 0.3) 10px,
    rgba(246, 196, 106, 0.3) 20px
  ) !important;
  position: relative;
}

:deep(.dark .resource-under-maintenance .fc-timeline-slot-lane) {
  background-color: rgba(255, 204, 102, 0.3) !important;
  background-image: repeating-linear-gradient(
    45deg,
    rgba(0, 0, 0, 0.2),
    rgba(0, 0, 0, 0.2) 10px,
    rgba(255, 204, 102, 0.3) 10px,
    rgba(255, 204, 102, 0.3) 20px
  ) !important;
}
</style>