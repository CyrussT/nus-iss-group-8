<script setup>
import { ref, reactive, watch, computed } from 'vue';
import { useToast } from '#imports';
import { useMaintenance } from '~/composables/useMaintenance';

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

const isSubmitting = ref(false);

const emit = defineEmits(['update:modelValue', 'save']);

// Use the maintenance composable
const maintenanceModule = useMaintenance();
const {
  checkMaintenanceStatus,
  isUnderMaintenance,
  maintenanceLoading,
  getMaintenanceDetails,
  clearMaintenanceCache
} = maintenanceModule;

// Maintenance related data
const maintenanceDetails = ref(null);
const checkingMaintenance = ref(false);

// Validation states
const validationErrors = ref({
  resourceId: '',
  bookingDate: '',
  bookingTime: '',
  title: '',
  duration: ''
});

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

// Format date for API and comparison (YYYY-MM-DD)
const formatDateForForm = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

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

    // Add end time based on duration
    let endTimeText = '';
    if (props.startTime && bookingForm.duration) {
      const startDate = new Date(props.startTime);
      const endDate = new Date(startDate.getTime() + bookingForm.duration * 60000);
      endTimeText = ` - ${endDate.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      })}`;
    }

    return [resourceText, startTimeText + endTimeText].filter(Boolean).join(' • ');
  } else {
    // For button-initiated bookings, show selected date, time and duration
    const parts = [];

    if (bookingForm.resourceName) {
      parts.push(`Room: ${bookingForm.resourceName}`);
    }

    if (bookingForm.bookingDate && bookingForm.bookingTime) {
      // Format the date
      const dateObj = new Date(bookingForm.bookingDate);
      const formattedDate = dateObj.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });

      // Get start time
      const [hours, minutes] = bookingForm.bookingTime.split(':').map(Number);
      const startTime = new Date(dateObj);
      startTime.setHours(hours, minutes, 0, 0);
      const formattedStartTime = startTime.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });

      // Calculate end time based on duration
      const endTime = new Date(startTime.getTime() + bookingForm.duration * 60000);
      const formattedEndTime = endTime.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });

      parts.push(`${formattedDate} • ${formattedStartTime} - ${formattedEndTime}`);
    }

    return parts.length > 0 ? parts.join(' • ') : 'Select a room and time for your booking';
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
    // If it's today, check if we're in business hours (7 AM - 7 PM)
    if ((now.getHours() < 7) || (now.getHours() >= 19)) {
      // Before business hours - use 7 AM
      startHour = 7;
      startMinute = 0;
    } else {
      // During business hours - use next half-hour slot
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

        // Check if next hour would be after business hours
        if (startHour >= 19) {
          // Cap at the last available slot
          startHour = 18;
          startMinute = 30;
        }
      }
    }
  } else {
    // If it's a future date, use first slot (7:00 AM)
    startHour = 7;
    startMinute = 0;
  }

  return `${startHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}`;
};

// Get the next half hour - fixes the overnight issue
const getNextHalfHour = () => {
  const now = new Date();
  const hours = now.getHours();
  const minutes = now.getMinutes();

  // If current time is outside of business hours (7 AM - 7 PM)
  if (hours < 7 || hours >= 19) {
    // Default to 7 AM
    const result = new Date(now);
    result.setHours(7, 0, 0, 0);
    return result;
  }

  // Normal half-hour rounding within business hours
  const newMinutes = minutes < 30 ? 30 : 0;

  now.setMinutes(newMinutes);
  now.setSeconds(0);
  now.setMilliseconds(0);

  // Add hours and check if we're still within business hours
  const newHour = hours + (minutes < 30 ? 0 : 1);
  if (newHour >= 19) {
    // If we'd end up past 7 PM, set to 7 PM
    now.setHours(18, 30, 0, 0);
  } else {
    now.setHours(newHour);
  }

  return now;
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
    const currentHour = now.getHours();
    const currentMinute = now.getMinutes();

    // Only adjust if current time is within business hours
    if (currentHour >= 7 && currentHour < 19) {
      // Get next half hour time
      if (currentMinute < 30) {
        baselineHour = currentHour;
        baselineMinute = 30;
      } else {
        baselineHour = currentHour + 1;
        baselineMinute = 0;

        // If next hour would be after business hours, cap at last slot
        if (baselineHour >= 19) {
          baselineHour = 18;
          baselineMinute = 30;
        }
      }
    }
  }

  // Generate slots from 7 AM to 7 PM (19:00), or from baseline time if later
  const startHour = Math.max(7, baselineHour);

  for (let hour = startHour; hour <= 18; hour++) {
    // For the first hour, respect the baseline minute
    const startMinute = (hour === baselineHour) ? baselineMinute : 0;

    // Generate 30-minute slots for this hour
    for (let minute of [0, 30]) {
      // Skip slots before baseline for first hour
      if (hour === baselineHour && minute < startMinute) continue;

      const formattedHour = hour.toString().padStart(2, '0');
      const formattedMinute = minute.toString().padStart(2, '0');
      const timeValue = `${formattedHour}:${formattedMinute}`;

      // Create a label time that shows correctly formatted for display
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

// Check if selected resource is under maintenance with date check
const isSelectedResourceUnderMaintenance = computed(() => {
  if (!isFromCalendar.value && !bookingForm.bookingDate) return false;

  const resourceId = isFromCalendar.value ? props.resourceId : bookingForm.resourceId;
  const bookingDate = isFromCalendar.value
    ? formatDateForForm(new Date(props.startTime))
    : bookingForm.bookingDate;

  // If no resource ID or date, can't check maintenance
  if (!resourceId || !bookingDate) return false;

  return isUnderMaintenance(resourceId, bookingDate);
});

// Filter out facilities that are under maintenance
const filteredFacilityOptions = computed(() => {
  if (!bookingForm.bookingDate) return facilityOptions.value;

  return facilityOptions.value.filter(facility =>
    !isUnderMaintenance(facility.value, bookingForm.bookingDate)
  );
});

// Handle resource selection from dropdown
const handleResourceSelection = async (resourceId) => {
  // Reset validation first
  validationErrors.value = {
    resourceId: '',
    bookingDate: '',
    bookingTime: '',
    title: '',
    duration: ''
  };

  // Find the selected resource name from the facilityOptions
  if (!resourceId) {
    bookingForm.resourceName = '';
    maintenanceDetails.value = null;
    return;
  }

  // Get the booking date for maintenance check
  const bookingDate = bookingForm.bookingDate;
  if (!bookingDate) {
    // Can't check maintenance without a date
    const selectedResource = facilityOptions.value.find(option => option.value === resourceId);
    if (selectedResource) {
      bookingForm.resourceName = selectedResource.resourceName;
    }
    return;
  }

  // Check if the facility is under maintenance for this specific date
  if (isUnderMaintenance(resourceId, bookingDate)) {
    checkingMaintenance.value = true;
    try {
      const details = await getMaintenanceDetails(resourceId, bookingDate);
      maintenanceDetails.value = details;

      // Show toast notification
      toast.add({
        title: 'Facility Unavailable',
        description: `This facility is under maintenance and cannot be booked on ${bookingDate}.`,
        color: 'orange'
      });

      // Clear selected resource to prevent booking
      bookingForm.resourceId = '';
      bookingForm.resourceName = '';

    } catch (error) {
      console.error('Error getting maintenance details:', error);
    } finally {
      checkingMaintenance.value = false;
    }
    return;
  }

  // If not under maintenance, set the resource name as usual
  const selectedResource = facilityOptions.value.find(option => option.value === resourceId);
  if (selectedResource) {
    bookingForm.resourceName = selectedResource.resourceName;
    maintenanceDetails.value = null; // Clear any previous maintenance details
  }
};

// Handle date change
const handleDateChange = () => {
  // Reset validation errors for date
  validationErrors.value.bookingDate = '';
  validationErrors.value.bookingTime = '';

  // If changing date, reset time to appropriate value
  if (!isFromCalendar.value) {
    bookingForm.bookingTime = getFirstAvailableTimeSlot(bookingForm.bookingDate);
  }

  // Re-check maintenance status if a resource is selected
  if (bookingForm.resourceId) {
    handleResourceSelection(bookingForm.resourceId);
  }

  // Call updateStartTime to recalculate derived values
  updateStartTime();
};

// Handle time change
const handleTimeChange = () => {
  // Reset validation errors for time
  validationErrors.value.bookingTime = '';

  // Update start time calculations
  updateStartTime();
};

// Helper function to check if a date is in the past
const isInPast = (date) => {
  if (!date) return false;

  const now = new Date();
  const checkDate = date instanceof Date ? date : new Date(date);

  // Compare just the date parts for dates in the future
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

// Determine if the booking was initiated from calendar or button
const isFromCalendar = computed(() => {
  return Boolean(props.resourceId && props.startTime);
});

// Map facilities for dropdown
const facilityOptions = computed(() => {
  return props.facilities.map(facility => ({
    value: facility.facilityId.toString(),
    label: `${facility.resourceName} - ${facility.location || 'No location'} (${facility.capacity || 0} persons)`,
    resourceName: facility.resourceName
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
  start: props.startTime || '',
  end: ''
});

// Update start time method to preserve date in SG timezone
const updateStartTime = () => {
  if (bookingForm.bookingDate && bookingForm.bookingTime) {
    // Explicitly construct the date without relying on timezone conversion
    const [year, month, day] = bookingForm.bookingDate.split('-').map(Number);
    const [hours, minutes] = bookingForm.bookingTime.split(':').map(Number);

    // Create date with explicit components
    const localDate = new Date(year, month - 1, day, hours, minutes, 0, 0);

    // Log the constructed date for debugging
    console.log('Constructed date:', {
      date: bookingForm.bookingDate,
      time: bookingForm.bookingTime,
      localDate: localDate
    });

    // Set as ISO string
    bookingForm.start = localDate.toISOString();

    // Store the local date and time components separately for the API call
    bookingForm.localDate = bookingForm.bookingDate;
    bookingForm.localTime = bookingForm.bookingTime;

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
  bookingForm.start = '';
  bookingForm.end = '';
  maintenanceDetails.value = null;
  validationErrors.value = {
    resourceId: '',
    bookingDate: '',
    bookingTime: '',
    title: '',
    duration: ''
  };
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

// Check if booking overlaps with existing bookings
const isBookingOverlapping = () => {
  return props.facilities.some(facility => {
    // Only check the selected facility
    if (facility.facilityId.toString() !== bookingForm.resourceId) return false;

    // Check each existing booking for this facility
    return facility.bookings && facility.bookings.some(existingBooking => {
      // Parse existing booking times
      let existingStartTime, existingEndTime;

      if (existingBooking.timeslot && existingBooking.timeslot.includes(' - ')) {
        [existingStartTime, existingEndTime] = existingBooking.timeslot.split(' - ');
      } else if (existingBooking.timeslot && existingBooking.timeslot.includes('-')) {
        [existingStartTime, existingEndTime] = existingBooking.timeslot.split('-');
      } else {
        console.error('Invalid timeslot format:', existingBooking.timeslot);
        return false;
      }

      // Parse dates
      if (!existingBooking.bookedDatetime) return false;

      const bookedDate = new Date(existingBooking.bookedDatetime);
      const bookedDateStr = formatDateForForm(bookedDate);

      // Only check bookings on the same day
      if (bookedDateStr !== bookingForm.bookingDate) return false;

      // Check if the status is approved or pending
      if (existingBooking.status !== 'APPROVED' &&
        existingBooking.status !== 'CONFIRMED' &&
        existingBooking.status !== 'PENDING') {
        return false;
      }

      // Parse times
      const [existingStartHour, existingStartMinute] = existingStartTime.trim().split(":").map(Number);
      const [existingEndHour, existingEndMinute] = existingEndTime.trim().split(":").map(Number);

      // Create Date objects for existing booking
      const existingStartDate = new Date(bookedDate);
      existingStartDate.setHours(existingStartHour, existingStartMinute, 0, 0);

      const existingEndDate = new Date(bookedDate);
      existingEndDate.setHours(existingEndHour, existingEndMinute, 0, 0);

      // Create Date objects for new booking
      const [year, month, day] = bookingForm.bookingDate.split('-').map(Number);
      const [hour, minute] = bookingForm.bookingTime.split(':').map(Number);

      const newStartDate = new Date(year, month - 1, day, hour, minute, 0, 0);
      const newEndDate = new Date(newStartDate.getTime() + bookingForm.duration * 60000);

      // Check for overlap
      return (newStartDate < existingEndDate && newEndDate > existingStartDate);
    });
  });
};

// Filter available durations based on credits and time constraints
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

// Watch for duration changes
watch(() => bookingForm.duration, (newDuration) => {
  // Ensure the date remains consistent when changing duration
  if (bookingForm.start) {
    // Recalculate end time while preserving the original date
    bookingForm.end = calculateEndTime(bookingForm.start, newDuration);
  }

  // Clear duration validation error when changed
  validationErrors.value.duration = '';
});

// Watch for props changes
watch(() => [props.resourceId, props.resourceName, props.startTime],
  async ([newResourceId, newResourceName, newStartTime]) => {
    // Update resource info if provided
    if (newResourceId) {
      bookingForm.resourceId = newResourceId;
      bookingForm.resourceName = newResourceName;

      // Check if facility is under maintenance (using cached result)
      const bookingDate = newStartTime ? formatDateForForm(new Date(newStartTime)) : getTodayFormatted();
      if (isUnderMaintenance(newResourceId, bookingDate)) {
        checkingMaintenance.value = true;
        try {
          // Only fetch details if it's in maintenance
          maintenanceDetails.value = await getMaintenanceDetails(newResourceId, bookingDate);
        } finally {
          checkingMaintenance.value = false;
        }
      } else {
        maintenanceDetails.value = null;
      }
    } else {
      bookingForm.resourceId = '';
      bookingForm.resourceName = '';
      maintenanceDetails.value = null;
    }

    // Always ensure default duration is 30 minutes
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
  handleDateChange();
});

// Watch for booking time changes
watch(() => bookingForm.bookingTime, () => {
  handleTimeChange();
});

// Submit booking function
const submitBooking = async () => {
  if (isSubmitting.value) return;

  isSubmitting.value = true;


  try {
    // Reset validation errors
    validationErrors.value = {
      resourceId: '',
      bookingDate: '',
      bookingTime: '',
      title: '',
      duration: ''
    };

    // Validation checks
    if (!bookingForm.resourceId) {
      validationErrors.value.resourceId = 'Please select a facility';
      toast.add({
        title: 'Error',
        description: 'Please select a facility for the booking',
        color: 'red'
      });
      return false;
    }

    // Check if facility is under maintenance
    if (isSelectedResourceUnderMaintenance.value) {
      toast.add({
        title: 'Booking Failed',
        description: 'This facility is under maintenance and cannot be booked on the selected date.',
        color: 'orange'
      });
      return false;
    }

    // Validate required fields
    if (!bookingForm.title) {
      validationErrors.value.title = 'Please enter a title for the booking';
      toast.add({
        title: 'Error',
        description: 'Please enter a title for the booking',
        color: 'red'
      });
      return false;
    }

    // Check if selected time is in the past
    if (isSelectedTimeInPast.value) {
      validationErrors.value.bookingTime = 'Cannot create bookings in the past';
      toast.add({
        title: 'Error',
        description: 'Cannot create bookings in the past. Please select a future time.',
        color: 'red'
      });
      return false;
    }

    // Check if booking would end after 7 PM
    if (wouldEndAfter7PM.value) {
      validationErrors.value.duration = 'Bookings cannot extend beyond 7:00 PM';
      toast.add({
        title: 'Error',
        description: 'Bookings cannot extend beyond 7:00 PM. Please select a shorter duration or an earlier start time.',
        color: 'red'
      });
      return false;
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
      return false;
    }

    // Create booking with local time information
    const newBooking = {
      title: bookingForm.title,
      resourceId: bookingForm.resourceId,
      start: bookingForm.start,
      end: bookingForm.end,
      description: bookingForm.description,
      creditsUsed: bookingForm.duration.toString(), // Convert duration to string for creditsUsed

      // Add these for debugging or potential API use
      localDate: bookingForm.bookingDate,
      localTime: bookingForm.bookingTime
    };

    // Send the booking to the parent and wait for result
    const success = await emit('save', newBooking);

    // Reset form ONLY if we're keeping the modal open (on failure)
    if (!success) {
      // Do not reset the form here - we want to keep the form populated
      // if there's an error, so the user can try again
    }

    return success;
  } catch (err) {
    console.error('Error during booking save:', err);
    toast.add({
      title: 'Error',
      description: 'Failed to create booking. Please try again.',
      color: 'red'
    });
    return false;
  }
};

// Close modal method
const closeModal = () => {
  resetForm();
  emit('update:modelValue', false);
};

// Expose methods and computed properties
defineExpose({
  resetForm,
  isSelectedResourceUnderMaintenance
});
</script>

<template>
  <UModal :model-value="modelValue" @update:model-value="closeModal" prevent-close>
    <UCard class="p-2">
      <div class="mb-4">
        <h2 class="text-xl font-bold mb-2 dark:text-white">Create Booking</h2>
        <p class="text-gray-600 dark:text-gray-300 whitespace-normal break-words">
          {{ formattedTimeDisplay }}
        </p>
      </div>

      <!-- Maintenance Warning Banner (if applicable) -->
      <div v-if="isSelectedResourceUnderMaintenance"
        class="mb-4 p-3 bg-orange-100 dark:bg-orange-900/30 border border-orange-300 dark:border-orange-700 rounded-md">
        <div class="flex items-start">
          <UIcon name="i-heroicons-exclamation-triangle"
            class="text-orange-500 dark:text-orange-400 mr-2 flex-shrink-0 mt-0.5" />
          <div>
            <p class="font-medium text-orange-700 dark:text-orange-300">Facility Under Maintenance</p>
            <p class="text-sm text-orange-600 dark:text-orange-400">
              This facility is currently unavailable for booking due to scheduled maintenance.
            </p>
            <p v-if="maintenanceDetails" class="text-xs mt-1 text-orange-600 dark:text-orange-400">
              Maintenance period: {{ maintenanceDetails.startDate }} to {{ maintenanceDetails.endDate }}
            </p>
          </div>
        </div>
      </div>

      <div class="space-y-4">
        <!-- Room Selection with InputMenu for searchable dropdown -->
        <div v-if="!props.resourceId">
          <label for="room" class="block text-sm font-medium mb-1 dark:text-gray-200">Room *</label>
          <UInputMenu id="room" v-model="bookingForm.resourceId" :options="filteredFacilityOptions" option-attribute="label"
            value-attribute="value" placeholder="Search or select a room" class="w-full" required
            :disabled="checkingMaintenance" :color="validationErrors.resourceId ? 'red' : undefined"
            @update:model-value="handleResourceSelection" />
          <p v-if="validationErrors.resourceId" class="text-red-500 dark:text-red-400 text-sm mt-1">
            {{ validationErrors.resourceId }}
          </p>
          <p v-else-if="maintenanceDetails" class="text-orange-500 dark:text-orange-400 text-sm mt-1">
            The selected facility is under maintenance until {{ maintenanceDetails.endDate }}.
          </p>
        </div>

        <!-- Date and Time Selection (visible only when entering through button) -->
        <div v-if="!isFromCalendar" class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- Date Picker -->
          <div>
            <label for="bookingDate" class="block text-sm font-medium mb-1 dark:text-gray-200">Date *</label>
            <UInput id="bookingDate" v-model="bookingForm.bookingDate" type="date" :min="getTodayFormatted()" class="w-full"
              :color="validationErrors.bookingDate ? 'red' : undefined" :disabled="isSelectedResourceUnderMaintenance"
              required />
            <p v-if="validationErrors.bookingDate" class="text-red-500 dark:text-red-400 text-sm mt-1">
              {{ validationErrors.bookingDate }}
            </p>
          </div>

          <!-- Time Dropdown -->
          <div>
            <label for="selectedTime" class="block text-sm font-medium mb-1 dark:text-gray-200">Time *</label>
            <USelect id="selectedTime" v-model="bookingForm.bookingTime" :options="availableTimeSlots" placeholder="Select time"
              class="w-full" :color="validationErrors.bookingTime ? 'red' : undefined"
              :disabled="isSelectedResourceUnderMaintenance" required />
            <p v-if="validationErrors.bookingTime" class="text-red-500 dark:text-red-400 text-sm mt-1">
              {{ validationErrors.bookingTime }}
            </p>
            <p v-else-if="isSelectedTimeInPast" class="text-red-500 dark:text-red-400 text-sm mt-1">
              This time is in the past. Please select a future time.
            </p>
          </div>
        </div>

        <!-- Title field -->
        <div>
          <label for="title" class="block text-sm font-medium mb-1 dark:text-gray-200">Title *</label>
          <UInput id="title" v-model="bookingForm.title" placeholder="Meeting title" class="w-full" required
            :color="validationErrors.title ? 'red' : undefined" :disabled="isSelectedResourceUnderMaintenance" />
          <p v-if="validationErrors.title" class="text-red-500 dark:text-red-400 text-sm mt-1">
            {{ validationErrors.title }}
          </p>
        </div>

        <!-- Duration dropdown -->
        <div>
          <label for="duration" class="block text-sm font-medium mb-1 dark:text-gray-200">Duration</label>
          <USelect id="duration" v-model="bookingForm.duration" :options="availableDurationOptions" placeholder="Select duration"
            class="w-full" :key="`duration-select-${modelValue}`" :color="validationErrors.duration ? 'red' : undefined"
            :disabled="isSelectedResourceUnderMaintenance" />
          <p v-if="validationErrors.duration" class="text-red-500 dark:text-red-400 text-sm mt-1">
            {{ validationErrors.duration }}
          </p>
          <p v-else-if="props.availableCredits !== undefined && props.availableCredits !== null && availableDurationOptions.length === 0"
            class="text-red-500 dark:text-red-400 text-sm mt-1">
            You don't have enough credits for any booking. Each booking requires at least 30 minutes.
          </p>
          <p v-else-if="wouldEndAfter7PM" class="text-red-500 dark:text-red-400 text-sm mt-1">
            Selected duration would extend beyond 7:00 PM closing time.
          </p>
        </div>

        <!-- Description field -->
        <div>
          <label for="description" class="block text-sm font-medium mb-1 dark:text-gray-200">Description</label>
          <UTextarea id="description" v-model="bookingForm.description" placeholder="Add details about this booking" class="w-full"
            rows="3" :disabled="isSelectedResourceUnderMaintenance" />
        </div>
      </div>

      <div class="flex justify-end gap-3 mt-6">
        <UButton color="gray" variant="ghost" @click="closeModal">
          Cancel
        </UButton>

        <UButton color="primary" @click="submitBooking" :loading="isSubmitting || checkingMaintenance"
          :disabled="isSubmitting || !bookingForm.title || (!bookingForm.resourceId && !isFromCalendar) || isSelectedResourceUnderMaintenance">
          Create Booking
        </UButton>
      </div>
    </UCard>
  </UModal>
</template>

<style scoped>
/* Add animation for error messages */
.text-red-500,
.dark .text-red-400 {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Maintenance indicator styling */
.maintenance-indicator {
  animation: wrench-rotate 2s infinite ease-in-out;
}

@keyframes wrench-rotate {

  0%,
  100% {
    transform: rotate(0deg);
  }

  25% {
    transform: rotate(-15deg);
  }

  75% {
    transform: rotate(15deg);
  }
}

/* Additional dark mode styles for inputs */
:deep(.dark input[type="date"]) {
  color-scheme: dark;
}

:deep(.dark .u-input, .dark .u-textarea, .dark .u-select) {
  background-color: #1e1e2d;
  border-color: #4a5568;
  color: #e2e8f0;
}

:deep(.dark .u-input::placeholder, .dark .u-textarea::placeholder) {
  color: #a0aec0;
}

:deep(.dark .u-select-button) {
  background-color: #1e1e2d;
  border-color: #4a5568;
  color: #e2e8f0;
}

:deep(.dark .u-select-dropdown) {
  background-color: #1e1e2d;
  border-color: #4a5568;
  color: #e2e8f0;
}

:deep(.dark .u-select-option) {
  color: #e2e8f0;
}

:deep(.dark .u-select-option:hover) {
  background-color: #2d3748;
}
</style>