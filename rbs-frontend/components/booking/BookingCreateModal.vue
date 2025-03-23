<script setup>
import { ref, reactive, watch } from 'vue';

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
  }
});

const emit = defineEmits(['update:modelValue', 'save']);

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

// Helper function to check if a date is in the past
const isInPast = (date) => {
  const now = new Date();
  const checkDate = date instanceof Date ? date : new Date(date);
  return checkDate < now;
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

const bookingForm = reactive({
  title: '',
  description: '',
  resourceId: props.resourceId,
  resourceName: props.resourceName,
  start: props.startTime,
  end: '',
  duration: 60, // Default 60 minutes
  attendees: ''
});

// When props change, update the form
watch(() => [props.resourceId, props.resourceName, props.startTime], 
  ([newResourceId, newResourceName, newStartTime]) => {
    bookingForm.resourceId = newResourceId;
    bookingForm.resourceName = newResourceName;
    bookingForm.start = newStartTime;
    handleDurationChange();
  }
);

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
  
  // Check if the booking time is in the past
  if (isInPast(new Date(bookingForm.start))) {
    alert('Cannot create bookings in the past');
    return;
  }
  
  const newEnd = calculateEndTime(bookingForm.start, bookingForm.duration);
  
  const newBooking = {
    title: bookingForm.title,
    resourceId: bookingForm.resourceId,
    start: bookingForm.start,
    end: newEnd,
    description: bookingForm.description,
    attendees: bookingForm.attendees
  };
  
  emit('save', newBooking);
  emit('update:modelValue', false);
};

const closeModal = () => {
  emit('update:modelValue', false);
};
</script>

<template>
  <UModal :model-value="modelValue" @update:model-value="closeModal" prevent-close>
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
          @click="closeModal"
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
</template>

<style scoped>
.booking-modal {
  width: 100%;
  max-width: 500px;
}
</style>