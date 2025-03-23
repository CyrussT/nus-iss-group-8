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
  'drop-event', 
  'resize-event',
  'date-change'
]);

const calendarRef = ref(null);

// Helper function to check if a date is in the past
const isInPast = (date) => {
  const now = new Date();
  const checkDate = date instanceof Date ? date : new Date(date);
  return checkDate < now;
};

// Format date for API (YYYY-MM-DD)
const formatDateForApi = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

// Computed properties for calendar
const resources = computed(() => {
  return props.facilities.map(facility => {
    const building = facility.location ? facility.location.split('-')[0] : 'Unknown';
    
    return {
      id: facility.facilityId.toString(),
      building: building,
      title: facility.resourceName || 'Unnamed Resource',
      extendedProps: {
        resourceType: facility.resourceType,
        fullLocation: facility.location,
        capacity: facility.capacity
      }
    };
  });
});

// Function to check if a time slot is available
const isSlotAvailable = (startTime, endTime, resourceId, excludeEventId = null) => {
  if (!calendarRef.value) return true;
  
  const calendarApi = calendarRef.value.getApi();
  const events = calendarApi.getEvents();
  
  return !events.some(event => {
    // Skip the event being dragged
    if (event.id === excludeEventId) return false;
    
    // Only check events in the same resource
    if (event.getResources()[0]?.id !== resourceId) return false;
    
    // Check for overlap
    const eventStart = event.start;
    const eventEnd = event.end;
    
    return (startTime < eventEnd && endTime > eventStart);
  });
};

// Get the current calendar date
const getCurrentCalendarDate = () => {
  if (!calendarRef.value) return formatDateForApi(new Date());
  
  const calendarApi = calendarRef.value.getApi();
  return formatDateForApi(calendarApi.getDate());
};

// Force disable the prev button when calendar is at today's date
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
  
  // If the calendar is showing today's date, forcibly disable the prev button
  if (calendarDate.getTime() === today.getTime()) {
    const prevButton = calendarApi.el.querySelector('.fc-prev-button');
    if (prevButton) {
      prevButton.setAttribute('disabled', 'disabled');
      prevButton.classList.add('fc-button-disabled');
    }
  }
};

// Calendar options
const calendarOptions = ref({
  plugins: [resourceTimelinePlugin, interactionPlugin],
  initialView: 'resourceTimelineDay',
  initialDate: new Date().toISOString().split('T')[0], // Format as YYYY-MM-DD string
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
        const currentDate = calendarApi.getDate();
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        // Only go to previous date if current date is after today
        if (currentDate > today) {
          calendarApi.prev();
        }
      }
    }
  },
  resources: [],
  resourceGroupField: 'building',
  events: [],
  editable: true,
  selectable: true,
  selectAllow: (selectInfo) => {
    // Prevent selecting time slots in the past
    return !isInPast(selectInfo.start);
  },
  eventAllow: (dropInfo, draggedEvent) => {
    // Prevent dragging events to time slots in the past
    return !isInPast(dropInfo.start);
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
    
    if (dateInfo.start < today) {
      // Navigate to today instead
      calendarRef.value.getApi().today();
      return; // The event will fire again with today's date
    }
    
    emit('date-change', dateInfo);
    // Update button states after date change
    forceDisablePrevButton();
  },
  
  // Override default booking modal
  select: (info) => {
    // Double-check that the selected time slot is not in the past
    if (isInPast(info.start)) {
      alert('Cannot create bookings in the past');
      return;
    }
    
    emit('select-timeslot', info);
  },
  
  eventDrop: (info) => {
    const { event } = info;
    const newStart = event.start;
    const newEnd = event.end;
    const newResourceId = event.getResources()[0].id;
    
    // Check if the new time is in the past
    if (isInPast(newStart)) {
      info.revert();
      alert('Cannot move bookings to times in the past');
      return;
    }
    
    if (!isSlotAvailable(newStart, newEnd, newResourceId, event.id)) {
      info.revert();
      alert('This time slot is already booked for this room');
    } else {
      emit('drop-event', {
        eventId: event.id,
        newStart,
        newEnd,
        newResourceId
      });
    }
  },
  
  eventResize: (info) => {
    const { event } = info;
    
    // Check if the new start time would be in the past
    if (isInPast(event.start)) {
      info.revert();
      alert('Cannot resize bookings to start in the past');
      return;
    }
    
    if (!isSlotAvailable(event.start, event.end, event.getResources()[0].id, event.id)) {
      info.revert();
      alert('Cannot resize: This would overlap with another booking');
    } else {
      emit('resize-event', {
        eventId: event.id,
        newStart: event.start,
        newEnd: event.end
      });
    }
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
      
      // You could also add a small indicator icon or text
      const eventTitle = info.el.querySelector('.fc-event-title');
      if (eventTitle) {
        // Add a small lock icon or text to indicate it's locked/past
        const lockIcon = document.createElement('span');
        lockIcon.className = 'past-event-indicator';
        lockIcon.innerHTML = ' 🔒'; // Simple lock emoji
        eventTitle.appendChild(lockIcon);
      }
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
    calendarApi.setOption('resourceGroupField', 'building');
    calendarApi.setOption('resources', resourcesList);
    
    // Also update button states whenever resources are updated
    forceDisablePrevButton();
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
    forceDisablePrevButton();
  }
};

// Expose methods to parent
defineExpose({
  getCurrentCalendarDate,
  isSlotAvailable
});

onMounted(() => {
  // Initial setup of resources
  updateCalendarResources(resources.value);
  
  // Initial setup of events
  updateCalendarEvents(props.bookings);
  
  // Run multiple strategies to ensure the prev button is disabled on initial load
  // Strategy 1: Immediate attempt
  setTimeout(() => {
    if (calendarRef.value) {
      const calendarApi = calendarRef.value.getApi();
      calendarApi.today();
      forceDisablePrevButton();
    }
  }, 0);
  
  // Strategy 2: Multiple delayed attempts
  setTimeout(() => forceDisablePrevButton(), 100);
  setTimeout(() => forceDisablePrevButton(), 300);
  setTimeout(() => forceDisablePrevButton(), 500);
  
  // Strategy 3: Interval check for the first few seconds
  const checkInterval = setInterval(() => {
    if (calendarRef.value) {
      forceDisablePrevButton();
    }
  }, 200);
  
  // Clear the interval after 2 seconds
  setTimeout(() => clearInterval(checkInterval), 2000);
});
</script>

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

<style scoped>
.calendar-wrapper {
  width: 100%;
  overflow-x: auto; /* Allow horizontal scrolling if needed */
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

/* Styles for past events */
:deep(.past-event) {
  opacity: 0.7;
  pointer-events: auto !important; /* Override FullCalendar's pointer-events: none */
  cursor: default !important; /* Show default cursor instead of move cursor */
  background-image: repeating-linear-gradient(
    45deg,
    rgba(255, 255, 255, 0.1),
    rgba(255, 255, 255, 0.1) 10px,
    rgba(255, 255, 255, 0.2) 10px,
    rgba(255, 255, 255, 0.2) 20px
  ) !important; /* Add subtle pattern to indicate past status */
}

:deep(.past-event) .fc-event-main {
  cursor: pointer !important; /* Allow clicking to view details */
}

:deep(.past-event-indicator) {
  font-size: 0.8em;
  margin-left: 4px;
  opacity: 0.8;
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