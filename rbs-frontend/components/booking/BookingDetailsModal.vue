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
      attendees: '',
      isPast: false,
      studentId: '',
      studentName: ''
    })
  }
});

const emit = defineEmits(['update:modelValue', 'edit']);

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

const editBooking = () => {
  emit('edit', props.booking);
  closeModal();
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
            <h2 class="text-xl font-bold">{{ "View Booking Details" }}</h2>
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
        <div>
          <h3 class="text-lg font-medium mb-1">
            {{ "Booked by: " + booking.studentId + " - " + booking.studentName}}
          </h3>
          <p class="text-gray-600">
            {{ formatDate(booking.start) }}
            <span v-if="booking.end"> - {{ formatTime(booking.end) }}</span>
          </p>
        </div>
        
        <UDivider />
        
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
        
        <div v-if="booking.description">
          <label class="block text-sm font-medium text-gray-500 mb-1">Description</label>
          <UCard class="bg-gray-50 p-3">
            <p class="whitespace-pre-wrap">{{ booking.description }}</p>
          </UCard>
        </div>
        
        <div v-if="booking.attendees">
          <label class="block text-sm font-medium text-gray-500 mb-1">Attendees</label>
          <UCard class="bg-gray-50 p-3">
            <p class="whitespace-pre-wrap">{{ booking.attendees }}</p>
          </UCard>
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
          
          <UButton 
            v-if="!booking.isPast"
            color="primary"
            icon="i-heroicons-pencil"
            @click="editBooking"
          >
            Edit
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