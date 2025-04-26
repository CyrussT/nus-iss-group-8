<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import FullCalendar from '@fullcalendar/vue3';
import resourceTimelinePlugin from '@fullcalendar/resource-timeline';
import interactionPlugin from '@fullcalendar/interaction';
import { useMaintenance } from '~/composables/useMaintenance';

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
  'reset-search'
]);

const calendarRef = ref(null);

// Use the maintenance composable
const maintenanceModule = useMaintenance();
const { 
  isUnderMaintenance, 
  facilitiesUnderMaintenance
} = maintenanceModule;

// Get the color mode (light/dark)
const colorMode = ref(null);
try {
  // Check if the colorMode composable is available
  const useColorMode = useColorMode();
  colorMode.value = useColorMode;
} catch (e) {
  // Fallback if colorMode composable isn't available
  colorMode.value = 'light';
}

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

const isEndOfDay = () => {
  const now = new Date();
  return now.getHours() >= 19;
};

const getNextBusinessDay = () => {
  const now = new Date();
  const nextDay = new Date(now);
  nextDay.setDate(now.getDate() + 1);
  
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
  const formattedDate = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  return formattedDate;
};

// Computed properties for calendar
const resources = computed(() => {
  // Map facilities to resources preserving order
  const mappedResources = props.facilities.map(facility => {
    const building = facility.location ? facility.location.split('-')[0] : 'Unknown';
    const facilityId = facility.facilityId.toString();
    
    // Check if this facility is under maintenance
    const currentDate = getCurrentCalendarDate();
    const underMaintenance = isResourceUnderMaintenance(facilityId);
    
    return {
      id: facilityId,
      building: building,
      title: facility.resourceName || 'Unnamed Resource',
      // Include maintenance status in extendedProps
      extendedProps: {
        resourceType: facility.resourceType,
        fullLocation: facility.location,
        capacity: facility.capacity,
        underMaintenance: underMaintenance
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

// Check if a resource is under maintenance
const isResourceUnderMaintenance = (resourceId) => {
  if (!resourceId) return false;
  
  // Get the current calendar date for maintenance check
  const currentDate = getCurrentCalendarDate();
  
  // Check both with and without date format
  const maintenanceKey = currentDate ? `${resourceId}_${currentDate}` : resourceId;
  
  // First check the date-specific key, then fall back to the simple ID key
  return facilitiesUnderMaintenance[maintenanceKey] === true || 
         facilitiesUnderMaintenance[resourceId] === true;
};

// Get the current calendar date
const getCurrentCalendarDate = () => {
  if (!calendarRef.value) {
    return formatDateForApi(new Date());
  }
  
  try {
    const calendarApi = calendarRef.value.getApi();
    const calendarDate = calendarApi.getDate();
    const formattedDate = formatDateForApi(calendarDate);
    return formattedDate;
  } catch (err) {
    console.error('Error getting calendar date:', err);
    return formatDateForApi(new Date());
  }
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
  const today = new Date();
  
  // If it's late in the day (after 7 PM), default to tomorrow
  if (isEndOfDay()) {
    const nextDay = getNextBusinessDay();
    return nextDay.toISOString().split('T')[0]; // Format as YYYY-MM-DD
  }
  
  // Otherwise, use today's date
  return today.toISOString().split('T')[0];
};

// Calendar options
const calendarOptions = ref({
  plugins: [resourceTimelinePlugin, interactionPlugin],
  initialView: 'resourceTimelineDay',
  initialDate: getInitialDate(),
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
    // Get the resource ID from the selection
    const resourceId = selectInfo.resource?.id;
    
    // Prevent selecting resources under maintenance
    if (resourceId && isResourceUnderMaintenance(resourceId)) return false;
    
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
      setTimeout(() => {
        forceDisablePrevButton();
        applyMaintenanceStyling();
        applyDarkModeIfNeeded();
      }, 0);
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
    setTimeout(() => {
      forceDisablePrevButton();
      applyMaintenanceStyling();
      applyDarkModeIfNeeded();
    }, 0);
  },
  
  // Override default booking modal
  select: (info) => {
    // Check again if resource is under maintenance (safeguard)
    if (isResourceUnderMaintenance(info.resource.id)) {
      // Show toast notification
      alert('This facility is currently under maintenance and cannot be booked.');
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
    const isMaintenance = event.extendedProps.isMaintenance || false;
    
    // Apply visual styling to maintenance events
    if (isMaintenance) {
      info.el.classList.add('maintenance-event');
      
      // Add maintenance title - but check if it already has text first
      const eventTitle = info.el.querySelector('.fc-event-title');
      if (eventTitle) {
        if (!eventTitle.textContent || eventTitle.textContent === '') {
          eventTitle.innerHTML = 'Maintenance';
        } else if (eventTitle.textContent === 'MaintenanceMaintenance') {
          // Fix double text if it already happened
          eventTitle.innerHTML = 'Maintenance';
        }
        eventTitle.style.display = 'block';
        eventTitle.style.textAlign = 'center';
        eventTitle.style.fontSize = '12px';
        eventTitle.style.fontWeight = 'bold';
      }
    }
    // Apply visual styling to past events to indicate they can't be edited
    else if (isPastEvent) {
      // Add a "past-event" class to the event element
      info.el.classList.add('past-event');
      
      // Make past events more visually distinct by adding opacity
      info.el.style.opacity = '0.6';
    }
    
    // For non-maintenance events, remove any text content to just show the color
    if (!isMaintenance) {
      const eventTitle = info.el.querySelector('.fc-event-title');
      if (eventTitle) {
        eventTitle.style.display = 'none';
      }
    }
  },
  
  resourceDidMount: (info) => {
    // Apply styling to resources under maintenance
    if (info.resource.extendedProps.underMaintenance) {
      // Find the row element for this resource
      const resourceRow = info.el.closest('.fc-resource');
      
      if (resourceRow) {
        resourceRow.classList.add('resource-under-maintenance');
        
        // Add maintenance indicator
        const titleCell = resourceRow.querySelector('.fc-datagrid-cell-main');
        if (titleCell && !titleCell.querySelector('.maintenance-indicator')) {
          const maintenanceIndicator = document.createElement('span');
          maintenanceIndicator.className = 'maintenance-indicator';
          maintenanceIndicator.innerHTML = ' (MAINTENANCE)';
          maintenanceIndicator.title = 'Under Maintenance';
          titleCell.appendChild(maintenanceIndicator);
        }
      }
    }
  },
  
  // Runs when the view is fully rendered
  viewDidMount: () => {
    // Update button states when the view is mounted
    setTimeout(() => {
      forceDisablePrevButton();
      applyMaintenanceStyling();
      applyDarkModeIfNeeded();
    }, 200);
  }
});

// Dark mode detection
const isDarkMode = computed(() => {
  // Handle different ways dark mode might be stored
  if (typeof colorMode.value === 'object' && colorMode.value && colorMode.value.value) {
    return colorMode.value.value === 'dark';
  }
  
  if (typeof colorMode.value === 'string') {
    return colorMode.value === 'dark';
  }
  
  // Fallback to checking system preference or DOM
  if (typeof window !== 'undefined') {
    // Check data-theme attribute if it exists
    const htmlEl = document.documentElement;
    if (htmlEl.getAttribute('data-theme') === 'dark') {
      return true;
    }
    
    // Check for dark class on html or body
    if (htmlEl.classList.contains('dark') || document.body.classList.contains('dark')) {
      return true;
    }
    
    // Check media query as last resort
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
  
  return false;
});

// Apply dark mode styling to the calendar
const applyDarkModeIfNeeded = () => {
  if (!calendarRef.value) return;
  
  setTimeout(() => {
    try {
      const calendarApi = calendarRef.value.getApi();
      const calendarEl = calendarApi.el;
      
      // First, remove any existing dark mode styles
      const existingDarkStyles = calendarEl.querySelector('.dark-mode-styles');
      if (existingDarkStyles) {
        existingDarkStyles.remove();
      }
      
      // If we're in dark mode, add the styles
      if (isDarkMode.value) {
        const styleEl = document.createElement('style');
        styleEl.className = 'dark-mode-styles';
        styleEl.textContent = `
          /* Calendar container */
          .fc {
            background-color: #1e1e2d !important;
            color: #e2e8f0 !important;
          }
          
          /* Header toolbar */
          .fc .fc-toolbar {
            background-color: #1e1e2d !important;
            color: #e2e8f0 !important;
          }
          
          /* Calendar buttons */
          .fc .fc-button {
            background-color: #2d3748 !important;
            border-color: #4a5568 !important;
            color: #e2e8f0 !important;
          }
          
          .fc .fc-button:hover {
            background-color: #4a5568 !important;
          }
          
          .fc .fc-button-primary:not(:disabled).fc-button-active,
          .fc .fc-button-primary:not(:disabled):active {
            background-color: #553c9a !important;
            border-color: #6b46c1 !important;
          }
          
          /* Disabled button */
          .fc .fc-button-disabled {
            background-color: #2d3748 !important;
            border-color: #4a5568 !important;
            color: #a0aec0 !important;
            opacity: 0.5 !important;
          }
          
          /* Calendar title */
          .fc .fc-toolbar-title {
            color: #e2e8f0 !important;
          }
          
          /* Resource area (left side) */
          .fc .fc-resource-timeline-divider,
          .fc .fc-resource-area,
          .fc .fc-resource-area th,
          .fc .fc-resource-area td {
            background-color: #2d3748 !important;
            color: #e2e8f0 !important;
            border-color: #4a5568 !important;
          }
          
          /* Day header cells */
          .fc .fc-col-header,
          .fc .fc-col-header-cell {
            background-color: #2d3748 !important;
            color: #e2e8f0 !important;
            border-color: #4a5568 !important;
          }
          
          /* Time grid slots */
          .fc .fc-timeline-slot,
          .fc .fc-timeline-slot-lane {
            border-color: #4a5568 !important;
          }
          
          /* Time grid slots - alternating colors */
          .fc .fc-timeline-slot:nth-child(even) .fc-timeline-slot-lane {
            background-color: #2d3748 !important;
          }
          
          .fc .fc-timeline-slot:nth-child(odd) .fc-timeline-slot-lane {
            background-color: #1e1e2d !important;
          }
          
          /* Now indicator */
          .fc .fc-timeline-now-indicator-line {
            border-color: #e53e3e !important;
          }
          
          .fc .fc-timeline-now-indicator-arrow {
            border-color: #e53e3e !important;
            border-bottom-color: transparent !important;
            border-top-color: transparent !important;
          }
          
          /* Resource groups */
          .fc .fc-resource-group {
            background-color: #2d3748 !important;
            color: #e2e8f0 !important;
            border-color: #4a5568 !important;
          }
          
          /* Cell hover state */
          .fc .fc-timeline-slot-lane:hover {
            background-color: #2d3748 !important;
          }
          
          /* Event styles for dark mode */
          .fc-event.past-event {
            opacity: 0.5 !important;
            background-image: repeating-linear-gradient(
              45deg,
              rgba(255, 255, 255, 0.1),
              rgba(255, 255, 255, 0.1) 10px,
              rgba(0, 0, 0, 0.1) 10px,
              rgba(0, 0, 0, 0.1) 20px
            ) !important;
          }
        `;
        
        calendarEl.appendChild(styleEl);
      }
    } catch (error) {
      console.error('Error applying dark mode styling:', error);
    }
  }, 100);
};

// Function to apply maintenance styling
const applyMaintenanceStyling = () => {
  if (!calendarRef.value) return;
  
  setTimeout(() => {
    try {
      const calendarApi = calendarRef.value.getApi();
      const calendarEl = calendarApi.el;
      
      // Find all resources under maintenance based on the cached data
      const resourcesUnderMaintenance = [];
      
      resources.value.forEach(resource => {
        const resourceId = resource.id;
        // Check if this resource is under maintenance for the current date
        const underMaintenance = isResourceUnderMaintenance(resourceId);
        if (underMaintenance) {
          resourcesUnderMaintenance.push(resourceId);
        }
      });
      
      // Add a style element with our custom CSS if it doesn't exist yet
      if (!calendarEl.querySelector('.maintenance-styles')) {
        const styleEl = document.createElement('style');
        styleEl.className = 'maintenance-styles';
        styleEl.textContent = `
          /* Resource row styling */
          .resource-under-maintenance .fc-timeline-lane {
            position: relative;
          }
          
          /* Timeline lane styling */
          .resource-under-maintenance .fc-timeline-slot-lane {
            background-color: #f6c46a !important;
            background-image: repeating-linear-gradient(
              45deg,
              rgba(255, 255, 255, 0.2),
              rgba(255, 255, 255, 0.2) 10px,
              rgba(246, 196, 106, 0.3) 10px,
              rgba(246, 196, 106, 0.3) 20px
            ) !important;
            cursor: not-allowed !important;
          }
          
          /* Dark mode maintenance styling */
          .dark .resource-under-maintenance .fc-timeline-slot-lane,
          html[data-theme="dark"] .resource-under-maintenance .fc-timeline-slot-lane {
            background-color: #ffcc66 !important;
            background-image: repeating-linear-gradient(
              45deg,
              rgba(0, 0, 0, 0.2),
              rgba(0, 0, 0, 0.2) 10px,
              rgba(255, 204, 102, 0.3) 10px,
              rgba(255, 204, 102, 0.3) 20px
            ) !important;
          }
          
          /* Maintenance indicator in the resource title */
          .maintenance-indicator {
            margin-left: 5px;
            font-size: 10px;
            color: #e67e22;
            font-weight: bold;
            animation: wrench-rotate 2s infinite ease-in-out;
          }
          
          /* Dark mode maintenance indicator */
          .dark .maintenance-indicator,
          html[data-theme="dark"] .maintenance-indicator {
            color: #f39c12;
          }
          
          @keyframes wrench-rotate {
            0%, 100% { transform: rotate(0deg); }
            25% { transform: rotate(-15deg); }
            75% { transform: rotate(15deg); }
          }
          
          /* Maintenance event styling */
          .maintenance-event {
            background-color: #f6c46a !important;
            border-color: #e0b25e !important;
            cursor: not-allowed !important;
          }
          
          /* Dark mode maintenance event */
          .dark .maintenance-event,
          html[data-theme="dark"] .maintenance-event {
            background-color: #ffcc66 !important;
            border-color: #e6b800 !important;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.5) !important;
          }
          
          .maintenance-event .fc-event-title {
            display: block !important;
            text-align: center;
            font-size: 12px;
            font-weight: bold;
            color: #000;
          }
          
          /* Maintenance overlay for cells */
          .maintenance-overlay {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            color: #91580d;
            font-size: 14px;
            font-weight: bold;
            opacity: 0.7;
            pointer-events: none;
            white-space: nowrap;
          }
          
          /* Dark mode maintenance overlay */
          .dark .maintenance-overlay,
          html[data-theme="dark"] .maintenance-overlay {
            color: #ffcc66;
          }
        `;
        
        calendarEl.appendChild(styleEl);
      }
      
      // Apply maintenance styling to resources
      resourcesUnderMaintenance.forEach(resourceId => {
        // Apply class to the resource row
        const resourceEl = calendarEl.querySelector(`.fc-resource[data-resource-id="${resourceId}"]`);
        if (resourceEl) {
          resourceEl.classList.add('resource-under-maintenance');
          
          // Add maintenance indicator to resource title if not already present
          const titleCell = resourceEl.querySelector('.fc-datagrid-cell-main');
          if (titleCell && !titleCell.querySelector('.maintenance-indicator')) {
            const maintenanceIndicator = document.createElement('span');
            maintenanceIndicator.className = 'maintenance-indicator';
            maintenanceIndicator.innerHTML = ' (MAINTENANCE)';
            maintenanceIndicator.title = 'Under Maintenance';
            titleCell.appendChild(maintenanceIndicator);
          }
        }
        
        // Apply styling to timeline slots for this resource
        const slots = calendarEl.querySelectorAll(`.fc-timeline-lane[data-resource-id="${resourceId}"] .fc-timeline-slot-lane`);
        slots.forEach(slot => {
          slot.classList.add('maintenance-slot');
          slot.style.cursor = 'not-allowed';
          
          // Add overlay text if not already present
          if (!slot.querySelector('.maintenance-overlay')) {
            const overlayDiv = document.createElement('div');
            overlayDiv.className = 'maintenance-overlay';
            overlayDiv.textContent = 'Under Maintenance';
            slot.appendChild(overlayDiv);
          }
        });
        
        // Create a full-day maintenance event for this resource if not already present
        const existingMaintenanceEvent = calendarApi.getEventById(`maintenance-${resourceId}`);
        if (!existingMaintenanceEvent) {
          // Get the calendar date
          const viewDate = calendarApi.getDate();
          
          // Create start and end times for the maintenance event (full day)
          const startDateTime = new Date(viewDate);
          startDateTime.setHours(7, 0, 0, 0); // 7 AM
          
          const endDateTime = new Date(viewDate);
          endDateTime.setHours(19, 0, 0, 0); // 7 PM
          
          // Add the maintenance event
          calendarApi.addEvent({
            id: `maintenance-${resourceId}`,
            resourceId: resourceId,
            title: '', // Use empty title to prevent duplicate text
            start: startDateTime.toISOString(),
            end: endDateTime.toISOString(),
            backgroundColor: '#f6c46a',
            borderColor: '#e0b25e',
            textColor: '#000000',
            editable: false,
            durationEditable: false,
            startEditable: false,
            display: 'block',
            className: 'maintenance-event',
            extendedProps: {
              status: 'MAINTENANCE',
              legendStatus: 'MAINTENANCE',
              description: 'This facility is under maintenance and not available for booking.',
              isPast: false,
              isMaintenance: true
            }
          });
        }
      });
      
    } catch (error) {
      console.error('Error applying maintenance styling:', error);
    }
  }, 200);
};

// Watch for changes in resources and bookings
watch(() => resources.value, (newResources) => {
  updateCalendarResources(newResources);
}, { deep: true });

watch(() => props.bookings, (newBookings) => {
  updateCalendarEvents(newBookings);
}, { deep: true });

// Watch for changes in the maintenance status
watch(() => facilitiesUnderMaintenance, () => {
  if (calendarRef.value) {
    applyMaintenanceStyling();
  }
}, { deep: true });

// Watch for calendar reference to be available
watch(() => calendarRef.value, (newCalendarRef) => {
  if (newCalendarRef) {
    // Wait for the calendar to be fully rendered
    setTimeout(() => {
      forceDisablePrevButton();
      applyMaintenanceStyling();
      applyDarkModeIfNeeded();
    }, 200);
  }
});

// Watch for color mode changes
watch(() => colorMode.value, () => {
  if (calendarRef.value) {
    applyDarkModeIfNeeded();
  }
}, { deep: true });

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
    setTimeout(() => {
      forceDisablePrevButton();
      
      // Apply maintenance styling
      applyMaintenanceStyling();
      
      // Apply dark mode styling if needed
      applyDarkModeIfNeeded();
      
      // Force a refetch to ensure proper rendering
      calendarApi.refetchResources();
    }, 200);
  }
};

// Update calendar events
const updateCalendarEvents = (eventsList) => {
  if (calendarRef.value) {
    const calendarApi = calendarRef.value.getApi();
    
    // Remove all events first
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
    setTimeout(() => {
      forceDisablePrevButton();
      applyMaintenanceStyling();
      applyDarkModeIfNeeded();
    }, 200);
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
      
      /* Resource under maintenance styling */
      .resource-under-maintenance .fc-timeline-slot-frame {
        cursor: not-allowed !important;
      }
      
      /* Maintenance events should show title */
      .maintenance-event .fc-event-title {
        display: block !important;
      }
    `;
    
    calendarEl.appendChild(styleEl);
  }, 200);
};

// Force a refresh of the calendar
const forceRefresh = () => {
  if (calendarRef.value) {
    const currentDate = getCurrentCalendarDate();
    emit('date-change', { 
      start: new Date(currentDate),
      end: new Date(currentDate)
    });
    
    // Apply maintenance styling after refresh
    setTimeout(() => {
      applyMaintenanceStyling();
      applyDarkModeIfNeeded();
    }, 300);
  }
};

// Handle reset search button click
const handleResetSearch = () => {
  if (calendarRef.value) {
    const currentDate = getCurrentCalendarDate();
    
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
  handleResetSearch,
  isResourceUnderMaintenance,
  applyMaintenanceStyling,
  applyDarkModeIfNeeded
});

onMounted(() => {
  // Initial setup of resources
  updateCalendarResources(resources.value);
  
  // Initial setup of events
  updateCalendarEvents(props.bookings);
  
  // Setup maintenance styling
  setTimeout(() => {
    if (calendarRef.value) {
      forceDisablePrevButton();
      addPointerCursorToEvents();
      applyMaintenanceStyling();
      applyDarkModeIfNeeded();
    }
  }, 300);
});
</script>

<template>
  <UCard class="calendar-wrapper-card">
    <div v-if="loading" class="py-8 text-center dark:text-gray-300">
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
.calendar-wrapper-card {
  /* Apply global dark mode styling */
  background-color: var(--card-bg, #ffffff);
  color: var(--card-text, #1e293b);
}

:global(.dark) .calendar-wrapper-card {
  --card-bg: #1e1e2d;
  --card-text: #e2e8f0;
  border-color: #4a5568;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3);
}

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

:deep(.dark .fc-timeline-slot) {
  border-right: 1px solid #4a5568;
}

:deep(.fc-resource-timeline-divider) {
  background: #f5f5f5;
}

:deep(.dark .fc-resource-timeline-divider) {
  background: #2d3748;
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

:deep(.dark .fc-resource-group) {
  background-color: #2d3748;
  color: #e2e8f0;
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

:deep(.dark .fc-event) {
  box-shadow: 0 1px 3px rgba(0,0,0,0.2) !important;
}

/* Subtle hover effect for events */
:deep(.fc-event:hover) {
  transform: translateY(-1px) !important;
  box-shadow: 0 3px 5px rgba(0,0,0,0.12) !important;
}

:deep(.dark .fc-event:hover) {
  box-shadow: 0 3px 5px rgba(0,0,0,0.3) !important;
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

:deep(.dark .past-event) {
  background-image: repeating-linear-gradient(
    45deg,
    rgba(0, 0, 0, 0.1),
    rgba(0, 0, 0, 0.1) 10px,
    rgba(0, 0, 0, 0.2) 10px,
    rgba(0, 0, 0, 0.2) 20px
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

/* Special styling for maintenance events */
:deep(.maintenance-event) {
  background-color: #f6c46a !important;
  border-color: #e0b25e !important;
  cursor: not-allowed !important;
}

:deep(.dark .maintenance-event) {
  background-color: #ffcc66 !important;
  border-color: #e6b800 !important;
}

:deep(.maintenance-event .fc-event-title) {
  display: block !important;
  text-align: center;
  font-weight: bold;
  color: #000;
}

/* Style for resources under maintenance */
:deep(.resource-under-maintenance .fc-timeline-slot-lane) {
  background-color: #f6c46a !important;
  background-image: repeating-linear-gradient(
    45deg,
    rgba(255, 255, 255, 0.2),
    rgba(255, 255, 255, 0.2) 10px,
    rgba(255, 255, 255, 0.3) 10px,
    rgba(255, 255, 255, 0.3) 20px
  ) !important;
  cursor: not-allowed !important;
}

:deep(.dark .resource-under-maintenance .fc-timeline-slot-lane) {
  background-color: #ffcc66 !important;
  background-image: repeating-linear-gradient(
    45deg,
    rgba(0, 0, 0, 0.2),
    rgba(0, 0, 0, 0.2) 10px,
    rgba(0, 0, 0, 0.3) 10px,
    rgba(0, 0, 0, 0.3) 20px
  ) !important;
}

/* Animation for maintenance indicator */
@keyframes maintenanceBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.maintenance-indicator {
  animation: maintenanceBlink 2s infinite;
}
</style>