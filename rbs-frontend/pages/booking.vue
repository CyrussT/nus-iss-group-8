<script setup>
const auth = useAuthStore();

// definePageMeta({
//   middleware: ['auth']
// })

import { ref, reactive, onMounted, computed } from 'vue';
import FullCalendar from '@fullcalendar/vue3';
import resourceTimelinePlugin from '@fullcalendar/resource-timeline';
import interactionPlugin from '@fullcalendar/interaction';
import { UCard, UModal, UInput, USelect, UButton, UTextarea } from '#components';
import { useApi } from '~/composables/useApi';

const { apiUrl } = useApi();

const calendarRef = ref(null);
const loading = ref(true);
const facilities = ref([]);
const resourceGroups = ref([]);
const resources = ref([]);

// Modal state
const isModalOpen = ref(false);
const bookingForm = reactive({
  title: '',
  description: '',
  resourceId: '',
  resourceName: '',
  start: '',
  end: '',
  duration: 60, // Default 60 minutes
  attendees: ''
});

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

// Calculated end time based on start time and duration
const calculateEndTime = (startTime, durationMinutes) => {
  if (!startTime) return '';
  const start = new Date(startTime);
  const end = new Date(start.getTime() + durationMinutes * 60000);
  return end.toISOString();
};

// Duration options in minutes
const durationOptions = [
  { label: '30 minutes', value: 30 },
  { label: '1 hour', value: 60 },
  { label: '1.5 hours', value: 90 },
  { label: '2 hours', value: 120 },
  { label: '2.5 hours', value: 150 },
  { label: '3 hours', value: 180 },
  { label: '3.5 hours', value: 210 },
  { label: '4 hours', value: 240 }
];

const searchQuery = ref({
  resourceType: "",
  resourceName: "",
  location: "",
  capacity: "",
});

const resetSearch = () => {
  searchQuery.value = {
    resourceType: "",
    resourceName: "",
    location: "",
    capacity: "",
  };
  
  // Reset to show all resources
  if (calendarRef.value) {
    updateCalendarResources(resources.value, resourceGroups.value);
  }
};

const bookings = reactive([
  {
    id: '1',
    resourceId: '1', // This will be matched with facilityId
    title: 'Project Discussion',
    start: '2025-02-23T08:00:00',
    end: '2025-02-23T10:00:00',
    backgroundColor: '#2e7d32'
  },
  {
    id: '2',
    resourceId: '1', // This will be matched with facilityId
    title: 'Project',
    start: '2025-02-23T10:00:00',
    end: '2025-02-23T13:00:00',
    backgroundColor: '#2e7d32'
  }
]);

// Function to check if a time slot is available
const isSlotAvailable = (startTime, endTime, resourceId, excludeEventId = null) => {
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

// Open modal when selecting a time slot
const openBookingModal = (info) => {
  const selectedResource = facilities.value.find(f => f.facilityId.toString() === info.resource.id);
  
  bookingForm.resourceId = info.resource.id;
  bookingForm.resourceName = selectedResource ? selectedResource.resourceName : info.resource.title;
  bookingForm.start = info.startStr;
  bookingForm.duration = 60; // Default to 1 hour
  bookingForm.end = calculateEndTime(info.startStr, 60);
  bookingForm.title = '';
  bookingForm.description = '';
  bookingForm.attendees = '';
  
  isModalOpen.value = true;
};

// Handle duration change
const handleDurationChange = () => {
  if (bookingForm.start) {
    bookingForm.end = calculateEndTime(bookingForm.start, bookingForm.duration);
  }
};

// Submit booking
const submitBooking = () => {
  if (!bookingForm.title) {
    alert('Please enter a title for the booking');
    return;
  }
  
  const newEnd = calculateEndTime(bookingForm.start, bookingForm.duration);
  
  if (!isSlotAvailable(new Date(bookingForm.start), new Date(newEnd), bookingForm.resourceId)) {
    alert('This time slot is already booked or conflicts with another booking');
    return;
  }
  
  const newBooking = {
    id: `booking-${Date.now()}`,
    resourceId: bookingForm.resourceId,
    title: bookingForm.title,
    start: bookingForm.start,
    end: newEnd,
    backgroundColor: '#2e7d32',
    extendedProps: {
      description: bookingForm.description,
      attendees: bookingForm.attendees
    }
  };
  
  bookings.push(newBooking);
  isModalOpen.value = false;
  
  // TODO: Add API call to save booking to backend
  // saveBookingToBackend(newBooking);
};

const calendarOptions = ref({
  plugins: [resourceTimelinePlugin, interactionPlugin],
  initialView: 'resourceTimelineDay',
  schedulerLicenseKey: 'CC-Attribution-NonCommercial-NoDerivatives',
  headerToolbar: {
    left: 'prev',
    center: 'title',
    right: 'next'
  },
  resources: [],
  resourceGroupField: 'building', // Group by building
  events: bookings,
  editable: true,
  selectable: true,
  slotDuration: '00:30:00',
  slotMinTime: '07:00:00',
  slotMaxTime: '19:00:00',
  resourceAreaWidth: '20%', // Increased width for better display of grouped resources
  height: 'auto',
  contentHeight: "auto", // This ensures content fits inside the container
  nowIndicator: true,
  scrollTime: '07:00:00',
  
  // Override default booking modal
  select: (info) => {
    openBookingModal(info);
  },
  
  eventDrop: (info) => {
    const { event } = info;
    const newStart = event.start;
    const newEnd = event.end;
    const newResourceId = event.getResources()[0].id;
    
    if (!isSlotAvailable(newStart, newEnd, newResourceId, event.id)) {
      info.revert();
      alert('This time slot is already booked for this room');
    } else {
      // Update the event in our reactive bookings array
      const bookingIndex = bookings.findIndex(b => b.id === event.id);
      if (bookingIndex !== -1) {
        bookings[bookingIndex] = {
          ...bookings[bookingIndex],
          start: newStart.toISOString(),
          end: newEnd.toISOString(),
          resourceId: newResourceId
        };
      }
    }
  },
  
  eventResize: (info) => {
    const { event } = info;
    if (!isSlotAvailable(event.start, event.end, event.getResources()[0].id, event.id)) {
      info.revert();
      alert('Cannot resize: This would overlap with another booking');
    } else {
      // Add confirmation dialog
      if (confirm(`Confirm booking update:\nTitle: ${event.title}\nNew time: ${formatDate(event.start)} - ${formatDate(event.end)}`)) {
        const bookingIndex = bookings.findIndex(b => b.id === event.id);
        if (bookingIndex !== -1) {
          bookings[bookingIndex] = {
            ...bookings[bookingIndex],
            start: event.start.toISOString(),
            end: event.end.toISOString()
          };
        }
        // API Call to update booking
      } else {
        info.revert(); // If user cancels, revert the resize
      }
    }
  },
  
  eventClick: (info) => {
    // Show booking details when an event is clicked
    console.log('Event clicked:', info.event.title);
    alert(`Booking: ${info.event.title}\nStart: ${formatDate(info.event.start)}\nEnd: ${formatDate(info.event.end)}`);
  }
});

// Update calendar with resources and resource groups
function updateCalendarResources(resourcesList, groupsList) {
  if (calendarRef.value) {
    const calendarApi = calendarRef.value.getApi();
    calendarApi.setOption('resourceGroupField', 'building');
    calendarApi.setOption('resources', resourcesList);
  }
}

async function fetchFacilities() {
  try {
    loading.value = true;
    
    // Fetch facilities from the server
    const response = await fetch(`${apiUrl}/api/bookings/facilities`, {
      headers: {
        Authorization: "Bearer " + auth.token.value
      }
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch facilities');
    }
    
    const data = await response.json();
    facilities.value = data;
    
    // Create unique building list for resource groups
    const uniqueBuildings = [...new Set(facilities.value.map(facility => facility.location.split('-')[0]))];
    
    resourceGroups.value = uniqueBuildings.map(building => ({
      id: building,
      title: `Building ${building}`
    }));
    
    // Convert facilities to FullCalendar resource format
    resources.value = facilities.value.map(facility => {
      // Extract building code (e.g., E2 from E2-03-20-DR216)
      const building = facility.location.split('-')[0];
      
      return {
        id: facility.facilityId.toString(),
        building: building, // Used for grouping
        title: facility.resourceName, // Display room name
        extendedProps: {
          resourceType: facility.resourceType,
          fullLocation: facility.location,
          capacity: facility.capacity
        }
      };
    });
    
    // Update calendar options with the fetched resources
    calendarOptions.value = {
      ...calendarOptions.value,
      resources: resources.value
    };
    
    // Handle any existing bookings from the backend
    const allBookings = [];
    
    facilities.value.forEach(facility => {
      if (facility.bookings && facility.bookings.length > 0) {
        facility.bookings.forEach(booking => {
          allBookings.push({
            id: booking.id.toString(),
            resourceId: facility.facilityId.toString(),
            title: booking.title || 'Booked',
            start: booking.startTime,
            end: booking.endTime,
            backgroundColor: '#2e7d32',
            extendedProps: booking.description ? { description: booking.description } : {}
          });
        });
      }
    });
    
    // Combine existing bookings with any new ones created in the UI
    bookings.splice(0, bookings.length, ...allBookings);
    
  } catch (error) {
    console.error('Error fetching facilities:', error);
  } finally {
    loading.value = false;
  }
}

function searchFacilities() {
  // Filter resources based on search criteria
  const filteredResources = facilities.value
    .filter(facility => {
      return (
        (!searchQuery.value.resourceType || facility.resourceType.toLowerCase().includes(searchQuery.value.resourceType.toLowerCase())) &&
        (!searchQuery.value.resourceName || facility.resourceName.toLowerCase().includes(searchQuery.value.resourceName.toLowerCase())) &&
        (!searchQuery.value.location || facility.location.toLowerCase().includes(searchQuery.value.location.toLowerCase())) &&
        (!searchQuery.value.capacity || facility.capacity >= parseInt(searchQuery.value.capacity))
      );
    })
    .map(facility => {
      const building = facility.location.split('-')[0];
      
      return {
        id: facility.facilityId.toString(),
        building: building,
        title: facility.resourceName,
        extendedProps: {
          resourceType: facility.resourceType,
          fullLocation: facility.location,
          capacity: facility.capacity
        }
      };
    });

  // Update calendar with filtered resources
  if (calendarRef.value) {
    const calendarApi = calendarRef.value.getApi();
    calendarApi.setOption('resources', filteredResources);
  }
}

onMounted(() => {
  fetchFacilities();
});

</script>

<template>
  <div class="mx-auto w-3/4 mt-8">
    <h1 class="text-2xl font-bold">Booking Management</h1>
    
    <UCard class="mt-4">
      <div class="mb-4 grid grid-cols-2 gap-4">
        <UInput v-model="searchQuery.resourceType" placeholder="Resource Type" />
        <UInput v-model="searchQuery.resourceName" placeholder="Resource Name" />
        <UInput v-model="searchQuery.location" placeholder="Location" />
        <UInput v-model="searchQuery.capacity" type="number" placeholder="Capacity" />
      </div>
      <div class="col-span-2 flex justify-end gap-2">
        <button
          @click="resetSearch"
          class="bg-gray-400 text-white px-4 py-2 rounded hover:bg-gray-600"
        >
          Reset
        </button>
        
        <button
          @click="searchFacilities"
          class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          Search
        </button>
      </div>
    </UCard>
    
    <UCard class="mt-4">
      <div v-if="loading" class="py-8 text-center">
        <p>Loading resources...</p>
      </div>
      <div v-else class="calendar-wrapper">
        <FullCalendar 
          ref="calendarRef"
          :options="calendarOptions"
          class="resource-timeline"
        />
      </div>
    </UCard>
    
    <!-- Booking Modal -->
    <UModal v-model="isModalOpen" prevent-close>
      <UCard class="booking-modal">
        <div class="mb-4">
          <h2 class="text-xl font-bold mb-2">Create Booking</h2>
          <p class="text-gray-600">
            {{ formatDate(bookingForm.start) }}
            <span v-if="bookingForm.resourceName"> | {{ bookingForm.resourceName }}</span>
          </p>
        </div>
        
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium mb-1">Title *</label>
            <UInput 
              v-model="bookingForm.title" 
              placeholder="Meeting title" 
              class="w-full"
              required
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium mb-1">Duration</label>
            <USelect
              v-model="bookingForm.duration"
              :options="durationOptions"
              placeholder="Select duration"
              class="w-full"
              @update:model-value="handleDurationChange"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium mb-1">Description</label>
            <UTextarea
              v-model="bookingForm.description"
              placeholder="Add details about this booking"
              class="w-full"
              rows="3"
            />
          </div>
          
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
            @click="isModalOpen = false"
          >
            Cancel
          </UButton>
          
          <UButton 
            color="primary" 
            @click="submitBooking"
            :disabled="!bookingForm.title"
          >
            Create Booking
          </UButton>
        </div>
      </UCard>
    </UModal>
  </div>
</template>

<style scoped>
.calendar-container {
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.calendar-wrapper {
  width: 100%;
  overflow-x: auto; /* Allow horizontal scrolling if needed */
}

.resource-timeline {
  min-height: 400px;
  max-height: 600px;
}

.booking-modal {
  width: 100%;
  max-width: 500px;
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
</style>