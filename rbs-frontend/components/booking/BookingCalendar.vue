<template>
  <UCard>
    <div v-if="loading" class="py-8 text-center">
      <p>Loading resources...</p>
    </div>
    <div v-else class="calendar-wrapper">
      <FullCalendar 
        ref="calendarRef"
        :options="calendarOptions"
        class="resource-timeline"
        key="booking-calendar"
      />
    </div>
  </UCard>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import FullCalendar from '@fullcalendar/vue3';
import resourceTimelinePlugin from '@fullcalendar/resource-timeline';
import interactionPlugin from '@fullcalendar/interaction';

const props = defineProps({
  facilities: {
    type: Array,
    default: () => []
  },
  bookings: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  'select-timeslot', 
  'click-event', 
  'date-change',
  'reset-search'  // Keep the emit for reset search button for parent component
]);

const calendarRef = ref(null);

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

// Function to check if a time slot would end after 7 PM
const wouldEndAfter7PM = (startTime, duration) => {
  const start = new Date(startTime);
  const end = new Date(start.getTime() + duration);
  
  // Create a 7 PM reference for the same day
  const sevenPM = new Date(start);
  sevenPM.setHours(19, 0, 0, 0);
  
  return end > sevenPM;
};

// Format date for API (YYYY-MM-DD)
const formatDateForApi = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

// Computed properties for calendar
const resources = computed(() => {
  // Map facilities to resources preserving order
  const mappedResources = props.facilities.map(facility => {
    const building = facility.location ? facility.location.split('-')[0] : 'Unknown';
    
    return {
      id: facility.facilityId.toString(),
      building: building,
      title: facility.resourceName || 'Unnamed Resource',
      // Include original position to help with sorting
      extendedProps: {
        resourceType: facility.resourceType,
        fullLocation: facility.location,
        capacity: facility.capacity
      }
    };
  });
  
  return mappedResources;
});

// Function to check if a cell is already booked (used for selection validation)
const isCellBooked = (selectInfo) => {
  if (!calendarRef.value) return false;
  
  const calendarApi = calendarRef.value.getApi();
  const events = calendarApi.getEvents();
  const resourceId = selectInfo.resource.id;
  
  return events.some(event => {
    // Only check events in the same resource
    if (event.getResources()[0]?.id !== resourceId) return false;
    
    // Check for overlap
    const selectStart = selectInfo.start;
    const selectEnd = selectInfo.end;
    const eventStart = event.start;
    const eventEnd = event.end;
    
    return (selectStart < eventEnd && selectEnd > eventStart);
  });
};

// Get the current calendar date
const getCurrentCalendarDate = () => {
  if (!calendarRef.value) return formatDateForApi(new Date());
  
  const calendarApi = calendarRef.value.getApi();
  return formatDateForApi(calendarApi.getDate());
};

// Force disable/enable the prev button based on the current calendar date
const forceDisablePrevButton = () => {
  if (!calendarRef.value) return;
  
  const calendarApi = calendarRef.value.getApi();
  if (!calendarApi) return;
  
  // Get today's date with time set to midnight
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  
  // Get the current calendar date with time set to midnight
  const calendarDate = new Date(calendarApi.getDate());
  calendarDate.setHours(0, 0, 0, 0);
  
  // Get the prev button
  const prevButton = calendarApi.el.querySelector('.fc-prev-button');
  if (!prevButton) return;
  
  // Compare dates properly
  if (calendarDate.getTime() <= today.getTime()) {
    // If calendar date is today or earlier, disable the prev button
    prevButton.setAttribute('disabled', 'disabled');
    prevButton.classList.add('fc-button-disabled');
  } else {
    // If calendar date is in the future, enable the prev button
    prevButton.removeAttribute('disabled');
    prevButton.classList.remove('fc-button-disabled');
  }
};

// Determine initial date based on time of day
const getInitialDate = () => {
  // If it's late in the day (after 5 PM), default to tomorrow
  if (isEndOfDay()) {
    const nextDay = getNextBusinessDay();
    return nextDay.toISOString().split('T')[0]; // Format as YYYY-MM-DD
  }
  
  // Otherwise, use today's date
  return new Date().toISOString().split('T')[0];
};

// Calendar options
const calendarOptions = ref({
  plugins: [resourceTimelinePlugin, interactionPlugin],
  initialView: 'resourceTimelineDay',
  initialDate: getInitialDate(), // Use the function to determine initial date
  schedulerLicenseKey: 'GPL-My-Project-Is-Open-Source',
  headerToolbar: {
    left: 'prev',
    center: 'title',
    right: 'next'
  },
  customButtons: {
    prev: {
      text: 'prev',
      click: function() {
        const calendarApi = calendarRef.value.getApi();
        
        const currentDate = new Date(calendarApi.getDate());
        currentDate.setHours(0, 0, 0, 0);
        
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        // Only go to previous date if current date is after today
        if (currentDate.getTime() > today.getTime()) {
          calendarApi.prev();
          // Update button state after navigation
          setTimeout(forceDisablePrevButton, 0);
        }
      }
    }
  },
  resources: [],
  resourceGroupField: 'building',
  resourceOrder: 'title', // Sort resources by title within groups
  events: [],
  editable: false, // Set to false to disable all drag/resize by default
  selectable: true,
  selectAllow: (selectInfo) => {
    // Prevent selecting time slots in the past
    if (isInPast(selectInfo.start)) return false;
    
    // Prevent selecting time slots that are already booked
    if (isCellBooked(selectInfo)) return false;
    
    // Prevent selecting if end time would be after 7 PM
    // Default selection is usually 30 minutes
    if (wouldEndAfter7PM(selectInfo.start, 30 * 60000)) return false;
    
    return true;
  },
  slotDuration: '00:30:00',
  slotMinTime: '07:00:00',
  slotMaxTime: '19:00:00',
  resourceAreaWidth: '20%',
  height: 'auto',
  contentHeight: "auto",
  nowIndicator: true,
  scrollTime: '07:00:00',
  
  // Run when calendar is done loading
  loading: (isLoading) => {
    if (!isLoading) {
      // This runs after the calendar has fully loaded
      setTimeout(forceDisablePrevButton, 0);
    }
  },
  
  // Handle date changes in the calendar
  datesSet: (dateInfo) => {
    // If the date is set to before today, reset it to today
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const startDate = new Date(dateInfo.start);
    startDate.setHours(0, 0, 0, 0);
    
    if (startDate.getTime() < today.getTime()) {
      // Navigate to today instead
      calendarRef.value.getApi().today();
      return; // The event will fire again with today's date
    }
    
    emit('date-change', dateInfo);
    // Update button states after date change
    setTimeout(forceDisablePrevButton, 0);
  },
  
  // Override default booking modal
  select: (info) => {
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
      alert('Cannot create bookings in the past');
      return;
    }
    
    // Double-check that the selected time slot is not already booked
    if (isCellBooked(info)) {
      alert('This time slot is already booked');
      return;
    }
    
    // Check if selected time would result in booking ending after 7 PM
    // Using default duration of 30 minutes
    if (wouldEndAfter7PM(info.start, 30 * 60000)) {
      alert('Bookings cannot extend beyond 7:00 PM');
      return;
    }
    
    emit('select-timeslot', info);
  },
  
  eventClick: (info) => {
    emit('click-event', info.event);
  },
  
  eventDidMount: (info) => {
    const event = info.event;
    const isPastEvent = event.extendedProps.isPast || isInPast(event.end);
    
    // Apply visual styling to past events to indicate they can't be edited
    if (isPastEvent) {
      // Add a "past-event" class to the event element
      info.el.classList.add('past-event');
      
      // Make past events more visually distinct by adding opacity
      info.el.style.opacity = '0.6';
    }
    
    // Remove any text content from the event to just show the color
    const eventTitle = info.el.querySelector('.fc-event-title');
    if (eventTitle) {
      eventTitle.style.display = 'none';
    }
  },
  
  // Runs when the view is fully rendered
  viewDidMount: () => {
    // Update button states when the view is mounted
    forceDisablePrevButton();
  }
});

// Watch for changes in resources and bookings
watch(() => resources.value, (newResources) => {
  updateCalendarResources(newResources);
}, { deep: true });

watch(() => props.bookings, (newBookings) => {
  updateCalendarEvents(newBookings);
}, { deep: true });

// Watch for calendar reference to be available
watch(() => calendarRef.value, (newCalendarRef) => {
  if (newCalendarRef) {
    // Wait for the calendar to be fully rendered
    setTimeout(forceDisablePrevButton, 50);
  }
});

// Update calendar resources
const updateCalendarResources = (resourcesList) => {
  if (calendarRef.value) {
    const calendarApi = calendarRef.value.getApi();
    
    // Set resource group field first
    calendarApi.setOption('resourceGroupField', 'building');
    
    // Set resource order option
    calendarApi.setOption('resourceOrder', 'title');
    
    // Apply resources to calendar
    calendarApi.setOption('resources', resourcesList);
    
    // Also update button states whenever resources are updated
    setTimeout(forceDisablePrevButton, 0);
    
    // Force a refetch to ensure proper rendering
    calendarApi.refetchResources();
  }
};

// Update calendar events
const updateCalendarEvents = (eventsList) => {
  if (calendarRef.value) {
    const calendarApi = calendarRef.value.getApi();
    
    // Remove existing events
    calendarApi.removeAllEvents();
    
    // Add new events
    eventsList.forEach(event => {
      try {
        calendarApi.addEvent(event);
      } catch (err) {
        console.error('Error adding event to calendar:', err, event);
      }
    });
    
    // Force calendar to redraw
    calendarApi.render();
    
    // Update button states after events are updated
    setTimeout(forceDisablePrevButton, 0);
  }
};

// Add CSS to override FullCalendar's default cursor behavior
const addPointerCursorToEvents = () => {
  if (!calendarRef.value) return;
  
  setTimeout(() => {
    const calendarEl = calendarRef.value.getApi().el;
    
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
      
      /* Make all empty cells show the pointer cursor to indicate clickability */
      .fc-timeline-slot-frame {
        cursor: pointer;
      }
      
      /* Hide event titles to just show color blocks */
      .fc-event-title {
        display: none !important;
      }
    `;
    
    calendarEl.appendChild(styleEl);
  }, 200);
};

// Force a refresh of the calendar
const forceRefresh = () => {
  if (calendarRef.value) {
    const currentDate = getCurrentCalendarDate();
    console.log("Force refreshing with date:", currentDate);
    emit('date-change', { 
      start: new Date(currentDate),
      end: new Date(currentDate)
    });
  }
};

// Handle reset search button click
const handleResetSearch = () => {
  if (calendarRef.value) {
    const currentDate = getCurrentCalendarDate();
    console.log("Reset search with current calendar date:", currentDate);
    
    // Pass the current date explicitly to ensure it's preserved
    emit('reset-search', { 
      date: currentDate
    });
  } else {
    // Just emit the reset event if calendar isn't available
    emit('reset-search');
  }
};

// Expose methods to parent
defineExpose({
  getCurrentCalendarDate,
  updateCalendarResources,
  updateCalendarEvents,
  addPointerCursorToEvents,
  forceRefresh,
  handleResetSearch
});

onMounted(() => {
  // Initial setup of resources
  updateCalendarResources(resources.value);
  
  // Initial setup of events
  updateCalendarEvents(props.bookings);
  
  // Simplified approach to ensure the prev button state is correct
  // Two attempts with different delays should be sufficient
  setTimeout(() => {
    if (calendarRef.value) {
      forceDisablePrevButton();
      addPointerCursorToEvents();
    }
  }, 100);
  
  setTimeout(() => {
    if (calendarRef.value) {
      forceDisablePrevButton();
      addPointerCursorToEvents();
    }
  }, 300);
});
</script>

<style scoped>
.calendar-wrapper {
  width: 100%;
  overflow-x: auto;
}

.resource-timeline {
  min-height: 400px;
  max-height: 600px;
}

:deep(.fc) {
  width: 100% !important;
  max-width: 100%;
}

:deep(.fc-view) {
  width: 100% !important;
  overflow: visible;
}

:deep(.fc-timeline-slot-frame) {
  height: 100%;
  cursor: pointer !important;
}

:deep(.fc-timeline-slot) {
  border-right: 1px solid #ddd;
}

:deep(.fc-resource-timeline-divider) {
  background: #f5f5f5;
}

:deep(.fc-timeline-now-indicator-line) {
  border-color: #ff0000;
  border-width: 1px;
}

:deep(.fc-timeline-now-indicator-arrow) {
  border-color: #ff0000;
}

:deep(.fc-resource-group) {
  font-weight: bold;
  background-color: #f0f0f0;
}

/* Enhanced event styling */
:deep(.fc-event) {
  height: 26px !important; /* Increased event height */
  min-height: 26px !important;
  border-radius: 4px !important; /* Rounded corners */
  border-width: 1px !important; /* Consistent border width */
  box-shadow: 0 1px 3px rgba(0,0,0,0.08) !important; /* Subtle shadow for depth */
  cursor: pointer !important;
  margin-top: 3px !important; /* Slightly more spacing */
  margin-bottom: 3px !important;
  transition: transform 0.1s ease-in-out, box-shadow 0.1s ease-in-out;
}

/* Subtle hover effect for events */
:deep(.fc-event:hover) {
  transform: translateY(-1px) !important;
  box-shadow: 0 3px 5px rgba(0,0,0,0.12) !important;
}

:deep(.fc-event-main) {
  padding: 3px 5px !important; /* Increased inner padding */
  cursor: pointer !important;
}

/* Override any potential highlighting cursor for future events */
:deep(.fc-event-draggable),
:deep(.fc-event-resizable) {
  cursor: pointer !important;
}

/* Styles for past events */
:deep(.past-event) {
  opacity: 0.65; /* Slightly more opaque */
  pointer-events: auto !important;
  cursor: pointer !important;
  background-image: repeating-linear-gradient(
    45deg,
    rgba(255, 255, 255, 0.1),
    rgba(255, 255, 255, 0.1) 10px,
    rgba(255, 255, 255, 0.2) 10px,
    rgba(255, 255, 255, 0.2) 20px
  ) !important;
}

:deep(.past-event) .fc-event-main {
  cursor: pointer !important;
}

/* Hide event titles to just show the color block */
:deep(.fc-event-title) {
  display: none !important;
}

/* Disable the resize handles on past events */
:deep(.past-event) .fc-event-resizer {
  display: none !important;
}

/* Styles for disabled navigation buttons */
:deep(.fc-button-disabled) {
  opacity: 0.4 !important;
  cursor: not-allowed !important;
  pointer-events: none !important;
}
</style>