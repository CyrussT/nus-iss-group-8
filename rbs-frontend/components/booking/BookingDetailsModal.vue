<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  booking: {
    type: Object,
    default: () => ({
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
    })
  }
});

const emit = defineEmits(['update:model-value']);

// Check if the booking is a maintenance event
const isMaintenanceEvent = computed(() => {
  return props.booking.status === 'MAINTENANCE' || props.booking.id?.toString().startsWith('maintenance-');
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

// Helper function to format just the time portion from a date
const formatTime = (date) => {
  if (!date) return '';
  const d = new Date(date);
  return d.toLocaleString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  });
};

// Helper to get status badge color
const getStatusColor = (status) => {
  if (!status) return 'gray';
  
  switch(status.toUpperCase()) {
    case 'APPROVED':
      return 'green';
    case 'CONFIRMED':
      return 'blue';
    case 'PENDING':
      return 'orange';
    case 'REJECTED':
      return 'red';
    case 'CANCELLED':
      return 'gray';
    case 'MAINTENANCE':
      return 'orange';
    default:
      return 'gray';
  }
};

const closeModal = () => {
  emit('update:modelValue', false);
};
</script>

<template>
  <UModal :model-value="modelValue" @update:model-value="closeModal" prevent-close>
    <UCard class="booking-view-modal dark:bg-gray-800">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <UIcon 
              :name="isMaintenanceEvent ? 'i-heroicons-wrench' : (booking.isPast ? 'i-heroicons-lock-closed' : 'i-heroicons-calendar')" 
              class="mr-2 text-gray-500 dark:text-gray-400"
              size="lg" 
            />
            <h2 class="text-xl font-bold dark:text-white">{{ isMaintenanceEvent ? 'Maintenance Information' : 'Booking Details' }}</h2>
          </div>
          <div class="flex items-center">
            <UBadge 
              :color="getStatusColor(booking.status)" 
              class="mr-2"
              variant="subtle"
              size="lg"
            >
              {{ booking.status }}
            </UBadge>
          </div>
        </div>
      </template>
      
      <div class="space-y-4">
        <!-- Title Section -->
        <div class="border-b dark:border-gray-700 pb-3">
          <h3 class="text-xl font-bold mb-1 dark:text-white">{{ isMaintenanceEvent ? 'Facility Maintenance' : (booking.title || 'Untitled Booking') }}</h3>
          <p class="text-gray-600 dark:text-gray-300">
            {{ formatDate(booking.start) }}
            <span v-if="booking.end"> - {{ formatTime(booking.end) }}</span>
          </p>
        </div>
        
        <!-- Booking Details -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">Resource</label>
            <div class="flex items-center">
              <UIcon name="i-heroicons-building-office" class="mr-2 text-gray-400 dark:text-gray-500" />
              <p class="dark:text-gray-200">{{ booking.resourceName }}</p>
            </div>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">Location</label>
            <div class="flex items-center">
              <UIcon name="i-heroicons-map-pin" class="mr-2 text-gray-400 dark:text-gray-500" />
              <p class="dark:text-gray-200">{{ booking.location || 'Not specified' }}</p>
            </div>
          </div>
        </div>
        
        <!-- Maintenance Alert -->
        <div v-if="isMaintenanceEvent" class="bg-orange-50 dark:bg-orange-900/30 p-4 rounded-md border border-orange-200 dark:border-orange-800">
          <div class="flex items-center mb-2">
            <UIcon name="i-heroicons-wrench" class="mr-2 text-orange-500 dark:text-orange-400" />
            <h4 class="font-medium text-orange-700 dark:text-orange-300">Facility Under Maintenance</h4>
          </div>
          <p class="text-orange-600 dark:text-orange-400 text-sm">
            This facility is currently unavailable for booking due to scheduled maintenance. 
            Please check back after the maintenance period or select a different facility.
          </p>
        </div>
        
        <!-- User Info - only show for regular bookings -->
        <div v-if="!isMaintenanceEvent" class="bg-gray-50 dark:bg-gray-700/50 p-3 rounded-md">
          <label class="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">Booked by</label>
          <div class="flex items-center">
            <UIcon name="i-heroicons-user" class="mr-2 text-gray-400 dark:text-gray-500" />
            <p class="dark:text-gray-200">{{ booking.studentName }} ({{ booking.studentId }})</p>
          </div>
        </div>
        
        <!-- Description Section -->
        <div v-if="booking.description">
          <label class="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">Description</label>
          <UCard class="bg-gray-50 dark:bg-gray-700/50 p-3">
            <p class="whitespace-pre-wrap dark:text-gray-200">{{ booking.description }}</p>
          </UCard>
        </div>
        
        <!-- Booking ID for reference -->
        <div class="text-center text-xs text-gray-400 dark:text-gray-500 mt-2">
          {{ isMaintenanceEvent ? 'Maintenance ID' : 'Booking ID' }}: {{ booking.id }}
        </div>
      </div>
      
      <template #footer>
        <div class="flex justify-end gap-3">
          <UButton 
            color="gray" 
            variant="ghost" 
            @click="closeModal"
          >
            Close
          </UButton>
        </div>
      </template>
    </UCard>
  </UModal>
</template>

<style scoped>
.booking-view-modal {
  width: 100%;
  max-width: 550px;
}

:deep(.dark .booking-view-modal) {
  border-color: #4a5568;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3);
}

.booking-view-modal .whitespace-pre-wrap {
  white-space: pre-wrap;
}
</style>