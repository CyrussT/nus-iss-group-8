<script setup>
import { ref, reactive, watch, computed } from 'vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  resourceName: {
    type: String,
    default: ''
  },
  startTime: {
    type: String,
    default: ''
  },
  resourceId: {
    type: String,
    default: ''
  },
  facilities: {
    type: Array,
    default: () => []
  },
  availableCredits: {
    type: Number,
    default: 240 // Default to 4 hours (240 minutes)
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue', 'save']);

// Duration options in minutes
const durationOptions = [
  { label: '0.5 hours', value: 30 },
  { label: '1 hour', value: 60 },
  { label: '1.5 hours', value: 90 },
  { label: '2 hours', value: 120 },
  { label: '2.5 hours', value: 150 },
  { label: '3 hours', value: 180 },
  { label: '3.5 hours', value: 210 },
  { label: '4 hours', value: 240 }
];

// Time slot options for dropdown
const timeSlotOptions = [
  { label: '7:00 AM', value: '07:00' },
  { label: '7:30 AM', value: '07:30' },
  { label: '8:00 AM', value: '08:00' },
  { label: '8:30 AM', value: '08:30' },
  { label: '9:00 AM', value: '09:00' },
  { label: '9:30 AM', value: '09:30' },
  { label: '10:00 AM', value: '10:00' },
  { label: '10:30 AM', value: '10:30' },
  { label: '11:00 AM', value: '11:00' },
  { label: '11:30 AM', value: '11:30' },
  { label: '12:00 PM', value: '12:00' },
  { label: '12:30 PM', value: '12:30' },
  { label: '1:00 PM', value: '13:00' },
  { label: '1:30 PM', value: '13:30' },
  { label: '2:00 PM', value: '14:00' },
  { label: '2:30 PM', value: '14:30' },
  { label: '3:00 PM', value: '15:00' },
  { label: '3:30 PM', value: '15:30' },
  { label: '4:00 PM', value: '16:00' },
  { label: '4:30 PM', value: '16:30' },
  { label: '5:00 PM', value: '17:00' },
  { label: '5:30 PM', value: '17:30' },
  { label: '6:00 PM', value: '18:00' },
  { label: '6:30 PM', value: '18:30' }
];

// Helper function to check if a date is in the past
const isInPast = (date) => {
  if (!date) return false;
  
  const now = new Date();
  const checkDate = date instanceof Date ? date : new Date(date);
  
  // Compare just the date parts for dates in the future
  // This avoids timezone issues with future dates
  if (checkDate.getFullYear() > now.getFullYear()) {
    return false; // Future year
  }
  
  if (checkDate.getFullYear() === now.getFullYear() && 
      checkDate.getMonth() > now.getMonth()) {
    return false; // Future month in same year
  }
  
  if (checkDate.getFullYear() === now.getFullYear() && 
      checkDate.getMonth() === now.getMonth() && 
      checkDate.getDate() > now.getDate()) {
    return false; // Future day in same month and year
  }
  
  // For today, compare with time
  if (checkDate.getFullYear() === now.getFullYear() && 
      checkDate.getMonth() === now.getMonth() && 
      checkDate.getDate() === now.getDate()) {
    // Same day - compare time
    return (checkDate.getHours() < now.getHours() || 
            (checkDate.getHours() === now.getHours() && 
             checkDate.getMinutes() < now.getMinutes()));
  }
  
  // If we get here, it's a date in the past
  return true;
};

// Format date for display
const formatDate = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return d.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  });
};

// Calculate end time based on start time and duration
const calculateEndTime = (startTime, durationMinutes) => {
  if (!startTime) return '';
  const start = new Date(startTime);
  const end = new Date(start.getTime() + durationMinutes * 60000);
  return end.toISOString();
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

// Get today's date formatted for input[type="date"]
const getTodayFormatted = () => {
  const today = new Date();
  return today.toISOString().split('T')[0];
};

// Get the next half hour formatted for HH:MM
const getNextHalfHourFormatted = () => {
  const time = getNextHalfHour();
  const hours = time.getHours().toString().padStart(2, '0');
  const minutes = time.getMinutes().toString().padStart(2, '0');
  return `${hours}:${minutes}`;
};

// Determine if booking was initiated from calendar or button
const isFromCalendar = computed(() => {
  return Boolean(props.resourceId && props.startTime && props.startTime.includes('T'));
});

// Map facilities for input menu
const facilityOptions = computed(() => {
  return props.facilities.map(f => ({
    value: f.facilityId.toString(),
    label: `${f.resourceName} - ${f.location || 'No location'} (Capacity: ${f.capacity || 'N/A'})`,
    resourceName: f.resourceName
  }));
});

// Generate time slot options based on current time and selected duration
const availableTimeSlots = computed(() => {
  const options = [];
  const now = new Date();
  const selectedDate = bookingForm.bookingDate ? new Date(bookingForm.bookingDate) : new Date();
  
  // Extract date parts only from the selected date (avoid timezone issues)
  const selectedYear = selectedDate.getFullYear();
  const selectedMonth = selectedDate.getMonth();
  const selectedDay = selectedDate.getDate();
  
  // Check if selected date is today
  const isToday = selectedDay === now.getDate() && 
                  selectedMonth === now.getMonth() && 
                  selectedYear === now.getFullYear();
  
  // Get next half hour rounded time as baseline for today
  const nextHalfHour = getNextHalfHour();
  const baselineHour = isToday ? nextHalfHour.getHours() : 7;
  const baselineMinute = isToday && nextHalfHour.getHours() === now.getHours() ? nextHalfHour.getMinutes() : 0;
  
  // Get current booking duration in minutes
  const durationMinutes = bookingForm.duration || 30;
  
  // Generate slots from baseline time to 19:00 (7 PM)
  for (let hour = baselineHour; hour <= 18; hour++) {
    for (let minute = 0; minute < 60; minute += 30) {
      // Skip slots before baseline for today
      if (isToday && hour === baselineHour && minute < baselineMinute) continue;
      
      // Calculate if this time slot would end after 7 PM with the current duration
      const startTime = new Date(selectedYear, selectedMonth, selectedDay, hour, minute, 0, 0);
      const endTime = new Date(startTime.getTime() + durationMinutes * 60000);
      
      // Create 7 PM reference time
      const sevenPM = new Date(selectedYear, selectedMonth, selectedDay, 19, 0, 0, 0);
      
      // Skip this time slot if it would end after 7 PM
      if (endTime > sevenPM) continue;
      
      const formattedHour = hour.toString().padStart(2, '0');
      const formattedMinute = minute.toString().padStart(2, '0');
      const timeValue = `${formattedHour}:${formattedMinute}`;
      
      const timeForLabel = new Date();
      timeForLabel.setHours(hour, minute, 0, 0);
      const timeLabel = timeForLabel.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });
      
      options.push({
        label: timeLabel,
        value: timeValue
      });
    }
  }
  
  return options;
});

// Make sure the initial default duration is set to 30 minutes (0.5 hours)
// when the component is first initialized
const initialDuration = ref(30); 

// Form state with explicit date and time fields (instead of combined)
const bookingForm = reactive({
  title: '',
  description: '',
  resourceId: props.resourceId || '',
  resourceName: props.resourceName || '',
  bookingDate: props.startTime ? new Date(props.startTime).toISOString().split('T')[0] : getTodayFormatted(), // YYYY-MM-DD
  bookingTime: props.startTime && props.startTime.includes('T') 
    ? `${new Date(props.startTime).getHours().toString().padStart(2, '0')}:${new Date(props.startTime).getMinutes().toString().padStart(2, '0')}` 
    : getNextHalfHourFormatted(), // HH:MM
  duration: initialDuration.value, // Default to 0.5 hours (30 minutes)
  attendees: '',
  // Combined start date/time for API calls (calculated from bookingDate and bookingTime)
  start: props.startTime || '',
  end: ''
});

// Reset form values when dialog is closed
const resetForm = () => {
  bookingForm.title = '';
  bookingForm.description = '';
  bookingForm.resourceId = '';
  bookingForm.resourceName = '';
  bookingForm.bookingDate = getTodayFormatted();
  bookingForm.bookingTime = getNextHalfHourFormatted();
  bookingForm.duration = 30; // Always reset to 30 minutes (0.5 hours)
  bookingForm.attendees = '';
  bookingForm.start = '';
  bookingForm.end = '';
  updateStartTime();
};

// Check if the booking would end after 7 PM
const wouldEndAfter7PM = computed(() => {
  if (!bookingForm.bookingDate || !bookingForm.bookingTime || !bookingForm.duration) return false;
  
  // Create a proper Date object with the local date and time
  const year = parseInt(bookingForm.bookingDate.split('-')[0], 10);
  const month = parseInt(bookingForm.bookingDate.split('-')[1], 10) - 1; // JS months are 0-indexed
  const day = parseInt(bookingForm.bookingDate.split('-')[2], 10);
  
  const [hours, minutes] = bookingForm.bookingTime.split(':').map(Number);
  
  const startTime = new Date(year, month, day, hours, minutes, 0, 0);
  const endTime = new Date(startTime.getTime() + bookingForm.duration * 60000);
  
  // Create a reference date for 7 PM on the same day
  const sevenPM = new Date(year, month, day, 19, 0, 0, 0);
  
  return endTime > sevenPM;
});

// Filter available durations based on credits and end time restrictions
const availableDurationOptions = computed(() => {
  // First filter by available credits
  let options = durationOptions;
  
  // Check if props.availableCredits is defined
  if (props.availableCredits !== undefined && props.availableCredits !== null) {
    options = options.filter(option => option.value <= props.availableCredits);
  }
  
  // Then filter by time restrictions - only if date and time are selected
  if (bookingForm.bookingDate && bookingForm.bookingTime) {
    const year = parseInt(bookingForm.bookingDate.split('-')[0], 10);
    const month = parseInt(bookingForm.bookingDate.split('-')[1], 10) - 1; // JS months are 0-indexed
    const day = parseInt(bookingForm.bookingDate.split('-')[2], 10);
    
    const [hours, minutes] = bookingForm.bookingTime.split(':').map(Number);
    
    const startTime = new Date(year, month, day, hours, minutes, 0, 0);
    
    // Create a reference date for 7 PM on the same day
    const sevenPM = new Date(year, month, day, 19, 0, 0, 0);
    
    // Calculate maximum possible duration in minutes
    const maxDurationMinutes = Math.floor((sevenPM - startTime) / 60000);
    
    // Filter out durations that would go beyond 7 PM
    options = options.filter(option => option.value <= maxDurationMinutes);
  }
  
  return options;
});

// Create the combined start time from date and time parts
const updateStartTime = () => {
  if (bookingForm.bookingDate && bookingForm.bookingTime) {
    // Important: We need to manually construct the date to avoid timezone issues
    const year = parseInt(bookingForm.bookingDate.split('-')[0], 10);
    const month = parseInt(bookingForm.bookingDate.split('-')[1], 10) - 1; // JS months are 0-indexed
    const day = parseInt(bookingForm.bookingDate.split('-')[2], 10);
    
    const [hours, minutes] = bookingForm.bookingTime.split(':').map(Number);
    
    // Create date without any timezone conversion
    const localDate = new Date(year, month, day, hours, minutes, 0, 0);
    
    // Set as ISO string but keep track that this is local time
    bookingForm.start = localDate.toISOString();
    
    // Also update end time
    bookingForm.end = calculateEndTime(bookingForm.start, bookingForm.duration);
  }
};

// Initialize start time
updateStartTime();

// Create formatted display string with start AND end time + room
const formattedTimeDisplay = computed(() => {
  if (!bookingForm.start) return '';
  
  const startDate = new Date(bookingForm.start);
  let displayStr = formatDate(startDate);
  
  if (bookingForm.end) {
    const endDate = new Date(bookingForm.end);
    // Only show time portion of end time
    displayStr += ` - ${endDate.toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    })}`;
  }
  
  if (bookingForm.resourceName) {
    displayStr += ` | ${bookingForm.resourceName}`;
  }
  
  return displayStr;
});

// Check if selected date/time is in the past
const isSelectedTimeInPast = computed(() => {
  if (!bookingForm.bookingDate || !bookingForm.bookingTime) return false;
  
  // Create a proper Date object with the local date and time
  const year = parseInt(bookingForm.bookingDate.split('-')[0], 10);
  const month = parseInt(bookingForm.bookingDate.split('-')[1], 10) - 1; // JS months are 0-indexed
  const day = parseInt(bookingForm.bookingDate.split('-')[2], 10);
  
  const [hours, minutes] = bookingForm.bookingTime.split(':').map(Number);
  
  const selectedDateTime = new Date(year, month, day, hours, minutes, 0, 0);
  
  return isInPast(selectedDateTime);
});

// Handle duration change
const handleDurationChange = () => {
  if (bookingForm.start) {
    bookingForm.end = calculateEndTime(bookingForm.start, bookingForm.duration);
  }
};

// Handle date change
const handleDateChange = () => {
  // Reset time when date changes
  if (isFromCalendar.value) return;
  
  // If changing to today, ensure time is after current time
  const now = new Date();
  const selectedDate = new Date(bookingForm.bookingDate);
  const isSelectedToday = selectedDate.getDate() === now.getDate() && 
                          selectedDate.getMonth() === now.getMonth() && 
                          selectedDate.getFullYear() === now.getFullYear();
  
  if (isSelectedToday) {
    const nextHalf = getNextHalfHourFormatted();
    bookingForm.bookingTime = nextHalf;
  } else {
    // If not today, default to 9:00 AM
    bookingForm.bookingTime = '09:00';
  }
  
  updateStartTime();
};

// Handle time change
const handleTimeChange = () => {
  updateStartTime();
};

// Watch for changes in props
watch(() => [props.resourceId, props.resourceName, props.startTime], 
  ([newResourceId, newResourceName, newStartTime]) => {
    // Update resource info if provided
    if (newResourceId) {
      bookingForm.resourceId = newResourceId;
      bookingForm.resourceName = newResourceName;
    } else {
      bookingForm.resourceId = '';
      bookingForm.resourceName = '';
    }
    
    // Always ensure default duration is 30 minutes (0.5 hours)
    bookingForm.duration = 30;
    
    // Update date/time if provided as ISO string from calendar
    if (newStartTime && newStartTime.includes('T')) {
      const startDate = new Date(newStartTime);
      
      // Set date and time separately
      bookingForm.bookingDate = startDate.toISOString().split('T')[0];
      
      const hours = startDate.getHours().toString().padStart(2, '0');
      const minutes = startDate.getMinutes().toString().padStart(2, '0');
      bookingForm.bookingTime = `${hours}:${minutes}`;
      
      // Full ISO string for API
      bookingForm.start = newStartTime;
      
      // Update end time
      handleDurationChange();
    } else if (!newStartTime) {
      // If startTime is empty (from create button), set defaults
      bookingForm.bookingDate = getTodayFormatted();
      bookingForm.bookingTime = getNextHalfHourFormatted();
      updateStartTime(); // Calculate and set start time from date/time components
    }
  },
  { immediate: true }
);

// Handle resource selection
const handleResourceSelection = (value) => {
  const facility = props.facilities.find(f => f.facilityId.toString() === value);
  if (facility) {
    bookingForm.resourceName = facility.resourceName;
  }
};

// Watch for booking date changes
watch(() => bookingForm.bookingDate, () => {
  // If changing date, reset time to appropriate value
  handleDateChange();
});

// Watch for duration changes to update available time slots
watch(() => bookingForm.duration, () => {
  // This will trigger a recalculation of available time slots based on the new duration
  updateStartTime();
});

// Submit booking
const submitBooking = () => {
  // Validate required fields
  if (!bookingForm.title) {
    alert('Please enter a title for the booking');
    return;
  }
  
  if (!isFromCalendar.value && !bookingForm.resourceId) {
    alert('Please select a room for the booking');
    return;
  }
  
  // Check if selected time is in the past
  if (isSelectedTimeInPast.value) {
    alert('Cannot create bookings in the past. Please select a future time.');
    return;
  }
  
  // Check if booking would end after 7 PM
  if (wouldEndAfter7PM.value) {
    alert('Bookings cannot extend beyond 7:00 PM. Please select a shorter duration or an earlier start time.');
    return;
  }
  
  // Ensure start time is calculated
  updateStartTime();
  
  // Create booking with local time information
  const bookingStart = new Date(bookingForm.start);
  
  // Format the date and time for the API in a way that preserves the local date/time
  // Create a new object with local YYYY-MM-DD and HH:MM fields
  const localStartTime = {
    date: bookingForm.bookingDate,
    time: bookingForm.bookingTime,
    iso: bookingForm.start
  };
  
  console.log("Submitting booking with local time info:", localStartTime);
  
  const newBooking = {
    title: bookingForm.title,
    resourceId: bookingForm.resourceId,
    start: bookingForm.start,
    end: bookingForm.end,
    description: bookingForm.description,
    attendees: bookingForm.attendees,
    creditsUsed: bookingForm.duration.toString(), // Convert duration to string for creditsUsed
    
    // Add these for debugging or potential API use
    localDate: bookingForm.bookingDate,
    localTime: bookingForm.bookingTime
  };
  
  emit('save', newBooking);
  emit('update:modelValue', false);
};

const closeModal = () => {
  resetForm();
  emit('update:modelValue', false);
};
</script>

<template>
  <UModal :model-value="modelValue" @update:model-value="closeModal" prevent-close>
      <UCard class="p-2">
        <div class="mb-4">
          <h2 class="text-xl font-bold mb-2">Create Booking</h2>
          <p class="text-gray-600">
            {{ formattedTimeDisplay }}
          </p>
        </div>
        
        <div class="space-y-4">
          <!-- Room Selection with InputMenu for searchable dropdown -->
          <div v-if="!props.resourceId">
            <label class="block text-sm font-medium mb-1">Room *</label>
            <UInputMenu
              v-model="bookingForm.resourceId"
              :options="facilityOptions"
              option-attribute="label"
              value-attribute="value"
              placeholder="Search or select a room"
              class="w-full"
              required
              @update:model-value="handleResourceSelection"
            />
          </div>
          
          <!-- Date and Time Selection (visible only when entering through button) -->
          <div v-if="!isFromCalendar" class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- Date Picker -->
            <div>
              <label class="block text-sm font-medium mb-1">Date *</label>
              <UInput
                v-model="bookingForm.bookingDate"
                type="date"
                :min="getTodayFormatted()"
                class="w-full"
                @update:model-value="handleDateChange"
                required
              />
            </div>
            
            <!-- Time Dropdown -->
            <div>
              <label class="block text-sm font-medium mb-1">Time *</label>
              <USelect
                v-model="bookingForm.bookingTime"
                :options="availableTimeSlots"
                placeholder="Select time"
                class="w-full"
                @update:model-value="handleTimeChange"
                required
              />
              <p v-if="isSelectedTimeInPast" class="text-red-500 text-sm mt-1">
                This time is in the past. Please select a future time.
              </p>
            </div>
          </div>
          
          <!-- Title field -->
          <div>
            <label class="block text-sm font-medium mb-1">Title *</label>
            <UInput 
              v-model="bookingForm.title" 
              placeholder="Meeting title" 
              class="w-full"
              required
            />
          </div>
          
          <!-- Duration dropdown -->
          <div>
            <label class="block text-sm font-medium mb-1">Duration</label>
            <USelect
              v-model="bookingForm.duration"
              :options="availableDurationOptions"
              placeholder="Select duration"
              class="w-full"
              @update:model-value="handleDurationChange"
              :key="`duration-select-${modelValue}`"
            />
            <p v-if="props.availableCredits !== undefined && props.availableCredits !== null && availableDurationOptions.length === 0" class="text-red-500 text-sm mt-1">
              You don't have enough credits for any booking. Each booking requires at least 30 minutes.
            </p>
            <p v-else-if="wouldEndAfter7PM" class="text-red-500 text-sm mt-1">
              Selected duration would extend beyond 7:00 PM closing time.
            </p>
          </div>
          
          <!-- Description field -->
          <div>
            <label class="block text-sm font-medium mb-1">Description</label>
            <UTextarea
              v-model="bookingForm.description"
              placeholder="Add details about this booking"
              class="w-full"
              rows="3"
            />
          </div>
          
          <!-- Attendees field -->
          <div>
            <label class="block text-sm font-medium mb-1">Attendees</label>
            <UTextarea
              v-model="bookingForm.attendees"
              placeholder="Add attendees (one per line)"
              class="w-full"
              rows="2"
            />
          </div>
        </div>
        
        <div class="flex justify-end gap-3 mt-6">
          <UButton 
            color="gray" 
            variant="ghost" 
            @click="closeModal"
          >
            Cancel
          </UButton>
          
          <UButton 
            color="primary" 
            @click="submitBooking"
            :loading="props.loading"
            :disabled="!bookingForm.title || (!bookingForm.resourceId && !isFromCalendar)"
          >
            Create Booking
          </UButton>
        </div>
      </UCard>
  </UModal>
</template>