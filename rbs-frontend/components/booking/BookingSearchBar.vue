<script setup>
import { ref, defineEmits, onMounted, onBeforeUnmount } from 'vue';

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  resourceTypeOptions: {
    type: Array,
    default: () => []
  },
  resourceNameOptions: {
    type: Array,
    default: () => []
  },
  locationOptions: {
    type: Array,
    default: () => []
  },
  optionsLoading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['search', 'reset']);

const searchQuery = ref({
  resourceType: "",
  resourceName: "",
  location: "",
  capacity: ""
});

const handleSearch = () => {
  emit('search', { ...searchQuery.value });
};

const resetSearchFields = () => {
  // Reset search form fields
  searchQuery.value = {
    resourceType: "",
    resourceName: "",
    location: "",
    capacity: ""
  };
  console.log("Search fields have been reset");
};

const handleReset = () => {
  // Reset local search fields
  resetSearchFields();
  
  // Emit the reset event to parent
  emit('reset');
};

// Listen for external reset events 
const handleExternalReset = () => {
  resetSearchFields();
};

// Set up event listener for external reset
onMounted(() => {
  document.addEventListener('reset-search-fields', handleExternalReset);
});

// Clean up event listener
onBeforeUnmount(() => {
  document.removeEventListener('reset-search-fields', handleExternalReset);
});
</script>

<template>
  <UCard class="booking-search-bar p-4">
    <template #header>
      <div class="flex items-center">
        <UIcon name="i-heroicons-magnifying-glass" class="mr-2 text-gray-500" />
        <h3 class="text-lg font-medium">Find Available Resources</h3>
      </div>
    </template>

    <div class="mb-4 grid grid-cols-1 md:grid-cols-2 gap-4">
      <!-- Resource Type with UInputMenu -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Resource Type</label>
        <UInputMenu
          v-model="searchQuery.resourceType"
          :options="resourceTypeOptions"
          option-attribute="facilityType"
          value-attribute="facilityTypeId"
          placeholder="Type or select resource type"
          size="md"
          class="w-full"
          :loading="optionsLoading"
          clearable
        />
      </div>

      <!-- Resource Name with regular UInput - no autocomplete -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Resource Name</label>
        <UInput
          v-model="searchQuery.resourceName"
          placeholder="Enter resource name"
          size="md"
          class="w-full"
          icon="i-heroicons-building-office"
          clearable
        />
      </div>

      <!-- Location with UInputMenu -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Location</label>
        <UInputMenu
          v-model="searchQuery.location"
          :options="locationOptions"
          placeholder="Type or select location"
          size="md"
          class="w-full"
          :loading="optionsLoading"
          clearable
        />
      </div>

      <!-- Capacity -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Minimum Capacity</label>
        <UInput 
          v-model="searchQuery.capacity" 
          type="number" 
          placeholder="Min. capacity needed"
          size="md"
          icon="i-heroicons-user-group"
        />
      </div>
    </div>

    <div class="flex justify-end gap-3">
      <UButton
        color="gray"
        variant="soft"
        icon="i-heroicons-arrow-path"
        @click="handleReset"
      >
        Reset
      </UButton>
      
      <UButton
        color="primary"
        icon="i-heroicons-magnifying-glass"
        @click="handleSearch"
        :loading="loading"
      >
        Search
      </UButton>
    </div>
  </UCard>
</template>