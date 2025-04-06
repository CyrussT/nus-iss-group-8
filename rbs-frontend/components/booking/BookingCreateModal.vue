<script setup>
import { ref, reactive, watch, computed } from 'vue';
import { useToast } from '#imports';

const toast = useToast();

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

// Display formatted time range for the modal header
const formattedTimeDisplay = computed(() => {
  if (isFromCalendar.value) {
    // If selected from calendar, show resource and time
    const resourceText = props.resourceName ? `Room: ${props.resourceName}` : '';
    const startTimeText = props.startTime ? `Time: ${new Date(props.startTime).toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    })}` : '';
    
    return [resourceText, startTimeText].filter(Boolean).join(' • ');
  } else {
    // Otherwise, show a generic message
    return 'Select a room and time for your booking';
  }
});

// Get the first available time slot based on the selected date
const getFirstAvailableTimeSlot = (date) => {
  const now = new Date();
  const selectedDate = date ? new Date(date) : new Date();
  
  // Check if the selected date is today
  const isToday = selectedDate.getDate() === now.getDate() && 
                 selectedDate.getMonth() === now.getMonth() && 
                 selectedDate.getFullYear() === now.getFullYear();
  
  let startHour, startMinute;
  
  if (isToday) {
    // If it's today, use the next half-hour slot
    const currentHour = now.getHours();
    const currentMinute = now.getMinutes();
    
    if (currentMinute < 30) {
      // Use current hour and 30 minutes
      startHour = currentHour;
      startMinute = 30;
    } else {
      // Use next hour and 0 minutes
      startHour = currentHour + 1;
      startMinute = 0;
    }
    
    // Check if we're past business hours (7 AM to 7 PM)
    if (startHour < 7) {
      startHour = 7;
      startMinute = 0;
    } else if (startHour >= 19) {
      // If after business hours, return the first slot of the next day
      // (this case should be handled by the calendar's end of day logic,
      // but included here for completeness)
      startHour = 7;
      startMinute = 0;
    }
  } else {
    // If it's a future date, use first slot (7:00 AM)
    startHour = 7;
    startMinute = 0;
  }
  
  return `${startHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}`;
};

// Time slot options for dropdown
const availableTimeSlots = computed(() => {
  const options = [];
  const now = new Date();
  const selectedDate = bookingForm.bookingDate ? new Date(bookingForm.bookingDate) : new Date();
  
  // Extract date parts only from the selected date (avoid timezone issues)
  const selectedYear = selectedDate.getFullYear();
  const selectedMonth = selectedDate.getMonth();
  const selectedDay = selectedDate.getDate();
  
  // Determine the baseline start time
  let baselineHour = 7; // Default to 7 AM
  let baselineMinute = 0;
  
  // Check if the selected date is today or a future date
  const isToday = selectedDay === now.getDate() && 
                  selectedMonth === now.getMonth() && 
                  selectedYear === now.getFullYear();
  
  // If it's today, adjust baseline based on current time
  if (isToday) {
    // Get next half hour rounded time as baseline
    const nextHalfHour = getNextHalfHour();
    baselineHour = nextHalfHour.getHours();
    baselineMinute = nextHalfHour.getMinutes();
  }
  
  // Generate slots from baseline time to 19:00 (7 PM)
  for (let hour = baselineHour; hour <= 18; hour++) {
    for (let minute of [0, 30]) {
      // Skip slots before baseline for today
      if (isToday && hour === baselineHour && minute < baselineMinute) continue;
      
      const formattedHour = hour.toString().padStart(2, '0');
      const formattedMinute = minute.toString().padStart(2, '0');
      const timeValue = `${formattedHour}:${formattedMinute}`;
      
      const timeForLabel = new Date(selectedYear, selectedMonth, selectedDay, hour, minute, 0, 0);
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

// Handle resource selection from dropdown
const handleResourceSelection = (resourceId) => {
  // Find the selected resource name from the facilityOptions
  if (!resourceId) {
    bookingForm.resourceName = '';
    return;
  }
  
  const selectedResource = facilityOptions.value.find(option => option.value === resourceId);
  if (selectedResource) {
    bookingForm.resourceName = selectedResource.resourceName;
  }
};

// Handle date change
const handleDateChange = () => {
  // Call updateStartTime to recalculate derived values
  updateStartTime();
};

// Handle time change
const handleTimeChange = () => {
  // Call updateStartTime to recalculate derived values
  updateStartTime();
};

// Handle duration change
const handleDurationChange = () => {
  // The watcher for duration should handle this, but we can add extra logic here if needed
};

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
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
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

// Calculate end time based on start time and duration
const calculateEndTime = (startTime, durationMinutes) => {
  if (!startTime) return '';
  
  const start = new Date(startTime);
  const end = new Date(start.getTime() + durationMinutes * 60000);
  
  return end.toISOString();
};

// Check if booking would end after 7 PM
const wouldEndAfter7PM = computed(() => {
  if (!bookingForm.bookingDate || !bookingForm.bookingTime || !bookingForm.duration) return false;
  
  // Create a proper Date object with the local date and time
  const [year, month, day] = bookingForm.bookingDate.split('-').map(Number);
  const [hours, minutes] = bookingForm.bookingTime.split(':').map(Number);
  
  const startTime = new Date(year, month - 1, day, hours, minutes, 0, 0);
  const endTime = new Date(startTime.getTime() + bookingForm.duration * 60000);
  
  // Create a reference date for 7 PM on the same day
  const sevenPM = new Date(year, month - 1, day, 19, 0, 0, 0);
  
  return endTime > sevenPM;
});

// Form state with explicit date and time fields
const bookingForm = reactive({
  title: '',
  description: '',
  resourceId: props.resourceId || '',
  resourceName: props.resourceName || '',
  bookingDate: props.startTime ? new Date(props.startTime).toISOString().split('T')[0] : getTodayFormatted(),
  bookingTime: props.startTime && props.startTime.includes('T') 
    ? `${new Date(props.startTime).getHours().toString().padStart(2, '0')}:${new Date(props.startTime).getMinutes().toString().padStart(2, '0')}` 
    : getNextHalfHourFormatted(),
  duration: 30, // Default to 0.5 hours
  attendees: '',
  start: props.startTime || '',
  end: ''
});

// Update start time method to preserve date
const updateStartTime = () => {
  if (bookingForm.bookingDate && bookingForm.bookingTime) {
    // Explicitly construct the date without relying on timezone conversion
    const [year, month, day] = bookingForm.bookingDate.split('-').map(Number);
    const [hours, minutes] = bookingForm.bookingTime.split(':').map(Number);
    
    // Create date with explicit components to avoid timezone shifts
    const localDate = new Date(year, month - 1, day, hours, minutes, 0, 0);
    
    // Set as ISO string, keeping the local time
    bookingForm.start = localDate.toISOString();
    
    // Calculate end time preserving the same date
    bookingForm.end = calculateEndTime(bookingForm.start, bookingForm.duration);
  }
};

// Reset form values when dialog is closed
const resetForm = () => {
  bookingForm.title = '';
  bookingForm.description = '';
  bookingForm.resourceId = '';
  bookingForm.resourceName = '';
  bookingForm.bookingDate = getTodayFormatted();
  bookingForm.bookingTime = getNextHalfHourFormatted();
  bookingForm.duration = 30;
  bookingForm.attendees = '';
  bookingForm.start = '';
  bookingForm.end = '';
};

// Check if selected date/time is in the past
const isSelectedTimeInPast = computed(() => {
  if (!bookingForm.bookingDate || !bookingForm.bookingTime) return false;
  
  // Create a proper Date object with the local date and time
  const [year, month, day] = bookingForm.bookingDate.split('-').map(Number);
  const [hours, minutes] = bookingForm.bookingTime.split(':').map(Number);
  
  const selectedDateTime = new Date(year, month - 1, day, hours, minutes, 0, 0);
  
  return isInPast(selectedDateTime);
});

// Comprehensive overlap check
const isBookingOverlapping = () => {
  return props.facilities.some(facility => {
    // Only check the selected facility
    if (facility.facilityId.toString() !== bookingForm.resourceId) return false;
    
    // Check each existing booking for this facility
    return facility.bookings.some(existingBooking => {
      // Parse existing booking times
      let existingStartTime, existingEndTime;
      
      if (existingBooking.timeslot.includes(' - ')) {
        [existingStartTime, existingEndTime] = existingBooking.timeslot.split(' - ');
      } else if (existingBooking.timeslot.includes('-')) {
        [existingStartTime, existingEndTime] = existingBooking.timeslot.split('-');
      } else {
        console.error('Invalid timeslot format:', existingBooking.timeslot);
        return false;
      }
      
      // Convert booking times to Date objects
      const bookedDate = new Date(existingBooking.bookedDatetime);
      const [existingStartHour, existingStartMinute] = existingStartTime.trim().split(':').map(Number);
      const [existingEndHour, existingEndMinute] = existingEndTime.trim().split(':').map(Number);
      
      const existingStart = new Date(bookedDate);
      existingStart.setHours(existingStartHour, existingStartMinute, 0, 0);
      
      const existingEnd = new Date(bookedDate);
      existingEnd.setHours(existingEndHour, existingEndMinute, 0, 0);
      
      // Parse current booking times
      const currentStart = new Date(bookingForm.start);
      const currentEnd = new Date(bookingForm.end);
      
      // Check for overlap
      return (currentStart < existingEnd && currentEnd > existingStart);
    });
  });
};

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
    const [year, month, day] = bookingForm.bookingDate.split('-').map(Number);
    const [hours, minutes] = bookingForm.bookingTime.split(':').map(Number);
    
    const startTime = new Date(year, month - 1, day, hours, minutes, 0, 0);
    
    // Create a reference date for 7 PM on the same day
    const sevenPM = new Date(year, month - 1, day, 19, 0, 0, 0);
    
    // Calculate maximum possible duration in minutes
    const maxDurationMinutes = Math.floor((sevenPM - startTime) / 60000);
    
    // Filter out durations that would go beyond 7 PM
    options = options.filter(option => option.value <= maxDurationMinutes);
  }
  
  return options;
});

// Watches and event handlers
watch(() => bookingForm.duration, (newDuration, oldDuration) => {
  // Ensure the date remains consistent when changing duration
  if (bookingForm.start) {
    const originalStart = new Date(bookingForm.start);
    
    // Recalculate end time while preserving the original date
    bookingForm.end = calculateEndTime(bookingForm.start, newDuration);
    
    // Verify and correct any unexpected date changes
    const newEnd = new Date(bookingForm.end);
    if (newEnd.getDate() !== originalStart.getDate()) {
      // If date changed, adjust the end time to stay on the same day
      const correctedEnd = new Date(
        originalStart.getFullYear(), 
        originalStart.getMonth(), 
        originalStart.getDate(), 
        originalStart.getHours(), 
        originalStart.getMinutes() + newDuration
      );
      
      bookingForm.end = correctedEnd.toISOString();
    }
  }
});

// Props watch to handle initialization
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
      
      // Use explicit date extraction to avoid timezone issues
      const year = startDate.getFullYear();
      const month = String(startDate.getMonth() + 1).padStart(2, '0');
      const day = String(startDate.getDate()).padStart(2, '0');
      
      bookingForm.bookingDate = `${year}-${month}-${day}`;
      
      const hours = String(startDate.getHours()).padStart(2, '0');
      const minutes = String(startDate.getMinutes()).padStart(2, '0');
      bookingForm.bookingTime = `${hours}:${minutes}`;
      
      // Full ISO string for API
      bookingForm.start = newStartTime;
      
      // Update end time
      updateStartTime();
    } else if (!newStartTime) {
      // If startTime is empty (from create button), set defaults
      bookingForm.bookingDate = getTodayFormatted();
      
      // Use first available time slot instead of a fixed time
      bookingForm.bookingTime = getFirstAvailableTimeSlot(bookingForm.bookingDate);
      
      updateStartTime(); // Calculate and set start time from date/time components
    }
  },
  { immediate: true }
);

// Watch for booking date changes
watch(() => bookingForm.bookingDate, () => {
  // If changing date, reset time to appropriate value
  if (isFromCalendar.value) return;
  
  // Use the first available time slot function
  bookingForm.bookingTime = getFirstAvailableTimeSlot(bookingForm.bookingDate);
  
  updateStartTime();
});

// Submit booking function
const submitBooking = async () => {
  // Validate required fields
  if (!bookingForm.title) {
    toast.add({
      title: 'Error',
      description: 'Please enter a title for the booking',
      color: 'red'
    });
    return;
  }
  
  if (!isFromCalendar.value && !bookingForm.resourceId) {
    toast.add({
      title: 'Error',
      description: 'Please select a room for the booking',
      color: 'red'
    });
    return;
  }
  
  // Check if selected time is in the past
  if (isSelectedTimeInPast.value) {
    toast.add({
      title: 'Error',
      description: 'Cannot create bookings in the past. Please select a future time.',
      color: 'red'
    });
    return;
  }
  
  // Check if booking would end after 7 PM
  if (wouldEndAfter7PM.value) {
    toast.add({
      title: 'Error',
      description: 'Bookings cannot extend beyond 7:00 PM. Please select a shorter duration or an earlier start time.',
      color: 'red'
    });
    return;
  }
  
  // Ensure start time is calculated
  updateStartTime();
  
  // Check for booking overlaps
  if (isBookingOverlapping()) {
    toast.add({
      title: 'Error',
      description: 'This time slot is already booked for the selected room.',
      color: 'red'
    });
    return;
  }
  
  // Create booking with local time information
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
  
  try {
    // Send the booking to the parent and wait for result
    const success = await emit('save', newBooking);
    
    // Reset form ONLY if we're keeping the modal open (on failure)
    if (!success) {
      // Do not reset the form here - we want to keep the form populated
      // if there's an error, so the user can try again
    }
  } catch (err) {
    console.error('Error during booking save:', err);
    toast.add({
      title: 'Error',
      description: 'Failed to create booking. Please try again.',
      color: 'red'
    });
  }
};

// Close modal method
const closeModal = () => {
  resetForm();
  emit('update:modelValue', false);
};

// Expose methods and computed properties
defineExpose({
  resetForm
});
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