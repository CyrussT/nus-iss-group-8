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

const emit = defineEmits(['update:modelValue']);

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
    <UCard class="booking-view-modal">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <UIcon 
              :name="booking.isPast ? 'i-heroicons-lock-closed' : 'i-heroicons-calendar'" 
              class="mr-2 text-gray-500"
              size="lg" 
            />
            <h2 class="text-xl font-bold">Booking Details</h2>
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
        <div class="border-b pb-3">
          <h3 class="text-xl font-bold mb-1">{{ booking.title || 'Untitled Booking' }}</h3>
          <p class="text-gray-600">
            {{ formatDate(booking.start) }}
            <span v-if="booking.end"> - {{ formatTime(booking.end) }}</span>
          </p>
        </div>
        
        <!-- Booking Details -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-500 mb-1">Resource</label>
            <div class="flex items-center">
              <UIcon name="i-heroicons-building-office" class="mr-2 text-gray-400" />
              <p>{{ booking.resourceName }}</p>
            </div>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-500 mb-1">Location</label>
            <div class="flex items-center">
              <UIcon name="i-heroicons-map-pin" class="mr-2 text-gray-400" />
              <p>{{ booking.location || 'Not specified' }}</p>
            </div>
          </div>
        </div>
        
        <!-- User Info -->
        <div class="bg-gray-50 p-3 rounded-md">
          <label class="block text-sm font-medium text-gray-500 mb-1">Booked by</label>
          <div class="flex items-center">
            <UIcon name="i-heroicons-user" class="mr-2 text-gray-400" />
            <p>{{ booking.studentName }} ({{ booking.studentId }})</p>
          </div>
        </div>
        
        <!-- Description Section -->
        <div v-if="booking.description">
          <label class="block text-sm font-medium text-gray-500 mb-1">Description</label>
          <UCard class="bg-gray-50 p-3">
            <p class="whitespace-pre-wrap">{{ booking.description }}</p>
          </UCard>
        </div>
        
        <!-- Booking ID for reference -->
        <div class="text-center text-xs text-gray-400 mt-2">
          Booking ID: {{ booking.id }}
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

.booking-view-modal .whitespace-pre-wrap {
  white-space: pre-wrap;
}
</style>