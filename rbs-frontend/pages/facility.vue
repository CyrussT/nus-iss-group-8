<script setup lang="ts">
definePageMeta({
  middleware: ['auth', 'admin']
});

import { useRouter } from "vue-router";
import { computed, onMounted, ref, watch } from "vue";
import type { Facility } from "@/composables/useFacility";
import { useFacility } from "@/composables/useFacility";
import { useMaintenance } from "~/composables/useMaintenance";
import axios from "axios";

const auth = useAuthStore();
const router = useRouter();
const isModalOpen = ref(false);
const isEditing = ref(false);
const isMaintenanceModalOpen = ref(false);
const isReleaseConfirmModalOpen = ref(false);
const selectedFacility = ref<Partial<Facility> | null>(null);
const currentMaintenanceDetails = ref<any>(null);

// Maintenance composable
const maintenanceModule = useMaintenance();
const { 
  checkMultipleFacilities, 
  isUnderMaintenance, 
  maintenanceLoading,
  facilitiesUnderMaintenance,
  getMaintenanceDetails,
  checkAffectedBookings,
  affectedBookings,
  affectedBookingsCount
} = maintenanceModule;

// Computed property for today's date in YYYY-MM-DD format for min attribute
const minDate = computed(() => {
  return formatDateForInput(new Date());
});

// Maintenance data
const maintenanceData = ref({
  facilityId: null as number | null,
  startDate: '',
  endDate: '',
  description: '',
  createdBy: ''
});

// Store facility maintenance history
const facilityUpcomingMaintenance = ref<any[]>([]);
const facilityMaintenanceHistoryLoading = ref(false);

// Validation states
const validationErrors = ref({
  startDate: '',
  endDate: '',
  description: ''
});

// Number of rows for the textarea
const textareaRows = 3;

// Flag to show when we're checking for affected bookings
const checkingAffectedBookings = ref(false);

// Maintenance status tracking
const facilityMaintenanceStatus = ref<Record<number, boolean>>({});
const releasingMaintenance = ref(false);

const {
  searchQuery,
  fetchFacilities,
  resetSearch,
  facility,
  resetFacility,
  facilities,
  loading,
  currentPage,
  totalItems,
  pageSize,
  fetchResourceTypes,
  resourceTypeOptions,
  getResourceTypeName
} = useFacility();

const openModal = (facilityData: Partial<Facility> | null = null) => {
  if (facilityData && typeof facilityData === "object") {
    facility.value = {
      facilityId: facilityData.facilityId ?? undefined,
      resourceTypeId: facilityData.resourceTypeId ?? undefined,
      resourceName: facilityData.resourceName ?? "",
      location: facilityData.location ?? "",
      capacity: facilityData.capacity ?? 1,
      bookings: facilityData.bookings ?? [],
    };
    isEditing.value = true;
  } else {
    resetFacility();
    isEditing.value = false;
  }
  isModalOpen.value = true;
};

const openMaintenanceModal = async (row: any) => {
  selectedFacility.value = row;
  
  // Reset validation errors
  validationErrors.value = {
    startDate: '',
    endDate: '',
    description: ''
  };
  
  // Get user email safely
  let userEmail = 'admin'; // Default value
  try {
    userEmail = auth.user?.value?.email || userEmail;
  } catch (e) {
    console.error('Error accessing user email:', e);
  }
  
  // Reset maintenance data and affected bookings
  maintenanceData.value = {
    facilityId: row.facilityId,
    startDate: formatDateForInput(new Date()),
    endDate: formatDateForInput(new Date(Date.now() + 24 * 60 * 60 * 1000)), // Default to next day
    description: '',
    createdBy: userEmail
  };
  
  // Check for affected bookings with initial date values
  await checkForAffectedBookings();
  
  // Get upcoming maintenance for this facility
  await fetchFacilityMaintenanceSchedules(row.facilityId);
  
  isMaintenanceModalOpen.value = true;
};

// Function to fetch upcoming maintenance for a specific facility
const fetchFacilityMaintenanceSchedules = async (facilityId: number) => {
  try {
    facilityMaintenanceHistoryLoading.value = true;
    
    // Call the API to get maintenance schedules for this facility
    const response = await axios.get(`http://localhost:8080/api/maintenance/facility/${facilityId}`);
    
    if (response.data && Array.isArray(response.data)) {
      // Sort by start date - first upcoming (future), then past
      const now = new Date();
      const today = formatDateForInput(now);
      
      // Filter to get only upcoming maintenance (start date >= today)
      facilityUpcomingMaintenance.value = response.data
        .filter((m: any) => m.startDate >= today && m.startDate !== currentMaintenanceDetails.value?.startDate)
        .sort((a: any, b: any) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime())
        .slice(0, 3); // Just show the next 3 upcoming maintenance periods
    } else {
      facilityUpcomingMaintenance.value = [];
    }
  } catch (error) {
    console.error("Error fetching facility maintenance schedules:", error);
    facilityUpcomingMaintenance.value = [];
  } finally {
    facilityMaintenanceHistoryLoading.value = false;
  }
};

// Function to check for affected bookings when maintenance dates change
const checkForAffectedBookings = async () => {
  if (!maintenanceData.value.facilityId || 
      !maintenanceData.value.startDate || 
      !maintenanceData.value.endDate) {
    return;
  }
  
  try {
    checkingAffectedBookings.value = true;
    
    // Use the composable function to check affected bookings
    await checkAffectedBookings(
      maintenanceData.value.facilityId,
      maintenanceData.value.startDate,
      maintenanceData.value.endDate
    );
  } catch (error) {
    console.error('Error checking for affected bookings:', error);
  } finally {
    checkingAffectedBookings.value = false;
  }
};

// Function to handle maintenance button click
const handleMaintenanceButtonClick = async (row: any) => {
  const isInMaintenance = isUnderMaintenance(row.facilityId);
  
  if (isInMaintenance) {
    // If under maintenance, fetch current maintenance details and show release modal
    await fetchCurrentMaintenanceDetails(row.facilityId);
    selectedFacility.value = row;
    isReleaseConfirmModalOpen.value = true;
  } else {
    // If not under maintenance, show maintenance creation modal
    openMaintenanceModal(row);
  }
};

// Release facility from maintenance early
const releaseFacilityFromMaintenance = async () => {
  if (!selectedFacility.value?.facilityId) return;
  
  try {
    releasingMaintenance.value = true;
    
    // Call API to release facility
    const response = await axios.post(`http://localhost:8080/api/maintenance/release/${selectedFacility.value.facilityId}`);
    
    // Close the release modal
    isReleaseConfirmModalOpen.value = false;
    
    // Refresh data
    await fetchFacilities();
    await updateAllMaintenanceStatus();
    
    // Success message
    alert(`${selectedFacility.value.resourceName} has been successfully released from maintenance.`);
  } catch (error: any) {
    console.error("Error releasing facility from maintenance:", error);
    if (error.response && error.response.data && error.response.data.error) {
      alert(error.response.data.error);
    } else {
      alert("Failed to release facility from maintenance. Please try again.");
    }
  } finally {
    releasingMaintenance.value = false;
  }
};

// Fetch current maintenance details for a facility
const fetchCurrentMaintenanceDetails = async (facilityId: number) => {
  try {
    const response = await axios.get(`http://localhost:8080/api/maintenance/current/${facilityId}`);
    currentMaintenanceDetails.value = response.data;
    return response.data;
  } catch (error) {
    console.error("Error fetching current maintenance details:", error);
    currentMaintenanceDetails.value = null;
    return null;
  }
};

// Format date for display (YYYY-MM-DD to DD MMM YYYY)
const formatDateForDisplay = (dateString: string) => {
  if (!dateString) return '';
  
  const date = new Date(dateString);
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  
  return `${date.getDate()} ${months[date.getMonth()]} ${date.getFullYear()}`;
};

// Helper function to format date for input field
const formatDateForInput = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const closeMaintenanceModal = () => {
  isMaintenanceModalOpen.value = false;
  selectedFacility.value = null;
  facilityUpcomingMaintenance.value = [];
};

const closeReleaseConfirmModal = () => {
  isReleaseConfirmModalOpen.value = false;
  selectedFacility.value = null;
  currentMaintenanceDetails.value = null;
};

const closeModal = () => {
  isModalOpen.value = false;
  resetFacility();
};

const viewFacility = (facilityId: number) => {
  router.push(`/facility-details/${facilityId}`);
};

// Update maintenance status for all facilities using batch API
const updateAllMaintenanceStatus = async () => {
  if (!facilities.value || facilities.value.length === 0) return;

  try {
    // Extract facility IDs from all facilities
    const facilityIds = facilities.value.map((facility: any) => facility.facilityId).filter(Boolean);
    
    if (facilityIds.length === 0) return;
    
    // Use the batch API to check maintenance status for all facilities at once
    await checkMultipleFacilities(facilityIds);
    
    // Update facilityMaintenanceStatus from the shared state
    facilityIds.forEach((id: number) => {
      facilityMaintenanceStatus.value[id] = isUnderMaintenance(id);
    });
  } catch (error) {
    console.error('Error updating maintenance status:', error);
  }
};

// Validate the form and return true if valid
const validateMaintenanceForm = (): boolean => {
  // Reset validation errors
  validationErrors.value = {
    startDate: '',
    endDate: '',
    description: ''
  };
  
  let isValid = true;
  
  // Validate dates
  const startDate = new Date(maintenanceData.value.startDate);
  const endDate = new Date(maintenanceData.value.endDate);
  const today = new Date();
  today.setHours(0, 0, 0, 0); // Set to beginning of today
  
  // Check if start date is provided
  if (!maintenanceData.value.startDate) {
    validationErrors.value.startDate = 'Start date is required';
    isValid = false;
  }
  // Check if start date is in the past
  else if (startDate < today) {
    validationErrors.value.startDate = 'Start date cannot be in the past';
    isValid = false;
  }
  
  // Check if end date is provided
  if (!maintenanceData.value.endDate) {
    validationErrors.value.endDate = 'End date is required';
    isValid = false;
  }
  // Check if start date is after end date
  else if (startDate > endDate) {
    validationErrors.value.endDate = 'End date must be after start date';
    isValid = false;
  }
  
  // Check if description is provided
  if (!maintenanceData.value.description.trim()) {
    validationErrors.value.description = 'Description is required';
    isValid = false;
  }
  
  return isValid;
};

// Watch for date changes to provide real-time validation
watch(() => maintenanceData.value.startDate, (newValue) => {
  if (newValue) {
    const startDate = new Date(newValue);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    if (startDate < today) {
      validationErrors.value.startDate = 'Start date cannot be in the past';
    } else {
      validationErrors.value.startDate = '';
    }
    
    // Also validate end date if it exists
    if (maintenanceData.value.endDate) {
      const endDate = new Date(maintenanceData.value.endDate);
      if (startDate > endDate) {
        validationErrors.value.endDate = 'End date must be after start date';
      } else {
        validationErrors.value.endDate = '';
      }
    }
    
    // Check for affected bookings when date changes
    checkForAffectedBookings();
  }
});

watch(() => maintenanceData.value.endDate, (newValue) => {
  if (newValue && maintenanceData.value.startDate) {
    const startDate = new Date(maintenanceData.value.startDate);
    const endDate = new Date(newValue);
    
    if (startDate > endDate) {
      validationErrors.value.endDate = 'End date must be after start date';
    } else {
      validationErrors.value.endDate = '';
    }
    
    // Check for affected bookings when date changes
    checkForAffectedBookings();
  }
});

watch(() => maintenanceData.value.description, (newValue) => {
  if (!newValue.trim()) {
    validationErrors.value.description = 'Description is required';
  } else {
    validationErrors.value.description = '';
  }
});

const saveMaintenance = async () => {
  try {
    // Validate the form
    if (!validateMaintenanceForm()) {
      return; // Don't proceed if validation fails
    }
    
    // Send data to the API
    await axios.post("http://localhost:8080/api/maintenance/schedule", maintenanceData.value);
    
    // Show success message including cancelled bookings information
    if (affectedBookingsCount.value > 0) {
      alert(`Facility scheduled for maintenance successfully! ${affectedBookingsCount.value} existing bookings have been cancelled, and notifications have been sent to the affected users.`);
    } else {
      alert("Facility scheduled for maintenance successfully!");
    }
    
    closeMaintenanceModal();
    await fetchFacilities();
    
    // Update maintenance status for all facilities
    await updateAllMaintenanceStatus();
  } catch (error: any) {
    console.error("Error scheduling maintenance:", error);
    if (error.response && error.response.data && error.response.data.error) {
      // If the API returns a specific error message
      validationErrors.value.startDate = error.response.data.error;
    } else {
      alert("Failed to schedule maintenance. Please try again.");
    }
  }
};

const saveFacility = async () => {
  try {
    if (isEditing.value) {
      await axios.put(`http://localhost:8080/api/facilities/update/${facility.value.facilityId}`, facility.value);
      alert("Facility Updated Successfully!");
    } else {
      await axios.post("http://localhost:8080/api/facilities/create", facility.value);
      alert("Facility Created Successfully!");
    }
    closeModal();
    fetchFacilities();
  } catch (error) {
    console.error("Error saving facility:", error);
    alert("Failed to save facility. Please try again.");
  }
};

// Watch for facilities change to update maintenance statuses
watch(() => facilities.value, async (newFacilities) => {
  if (newFacilities && newFacilities.length > 0) {
    await updateAllMaintenanceStatus();
  }
}, { deep: true });

onMounted(async () => {
  await fetchFacilities();
});
</script>

<template>
  <div class="p-8">
    <h1 class="text-2xl font-bold mb-4">Facility Management</h1>

    <div class="grid grid-cols-2 gap-4">
      <UInputMenu v-model="searchQuery.resourceTypeId" :options="resourceTypeOptions" option-attribute="name"
        value-attribute="id" placeholder="Type or select resource type" size="md" class="w-full"
        clearable />

      <UInput v-model="searchQuery.resourceName" placeholder="Resource Name" />
      <UInput v-model="searchQuery.location" placeholder="Location" />
      <UInput v-model="searchQuery.capacity" type="number" placeholder="Capacity" />

      <div class="col-span-2 flex justify-end gap-2">
        <UButton @click="() => { currentPage = 1; fetchFacilities(); }" color="blue" variant="solid"
          icon="i-ic:baseline-search" label="Search" class="px-4 py-2 gap-2" />

        <UButton @click="resetSearch" color="gray" variant="solid" icon="i-ic:round-restart-alt" label="Reset"
          class="px-4 py-2 gap-2 hover:bg-red-500" />

        <UButton @click="openModal()" color="green" variant="solid" icon="i-ic:baseline-plus" label="Add Facility"
          class="px-4 py-2 gap-2" />
      </div>
    </div>

    <UCard class="w-full max-w-8xl p-6 shadow-lg bg-white">
      <div class="mt-6 overflow-x-auto">
        <UTable :rows="facilities" :loading="loading" :columns="[
          { key: 'sn', label: 'SN', sortable: false },
          { key: 'resourceTypeName', label: 'Resource Type', sortable: true },
          { key: 'resourceName', label: 'Name', sortable: true },
          { key: 'location', label: 'Location', sortable: true },
          { key: 'capacity', label: 'Capacity', sortable: true },
          { key: 'actions', label: 'Actions', class: 'text-center' }
        ]">
          <template #sn-data="{ index }">
            {{ index + 1 + (currentPage - 1) * pageSize }}
          </template>

          <template #actions-data="{ row }">
            <div class="flex justify-center gap-2">
              <UButton @click="viewFacility(row.facilityId)" color="blue" variant="solid" icon="i-heroicons-eye"
                label="View" />

              <UButton @click="openModal(row)" color="yellow" variant="solid" icon="i-heroicons-pencil" label="Edit"
                class="px-3 py-1 gap-2" />

              <UButton 
                @click="handleMaintenanceButtonClick(row)" 
                :color="isUnderMaintenance(row.facilityId) ? 'orange' : 'red'" 
                variant="solid" 
                :icon="isUnderMaintenance(row.facilityId) ? 'i-heroicons-check-circle' : 'i-heroicons-wrench'"
                :loading="maintenanceLoading"
                :label="isUnderMaintenance(row.facilityId) ? 'Release' : 'Maintenance'" 
                class="px-3 py-1 gap-2"
              />
            </div>
          </template>
        </UTable>
      </div>

      <div class="mt-4 flex justify-center">
        <UPagination v-model="currentPage" :max="5" :total="totalItems" @update:model-value="fetchFacilities" show-last
          show-first />
      </div>
    </UCard>
  </div>
  
  <!-- Facility Modal -->
  <UModal v-model="isModalOpen">
    <UCard class="p-9 max-w-lg ">
      <button @click="closeModal" class="absolute top-4 right-4 text-gray-500 hover:text-gray-700">
        <UIcon name="i-heroicons-x-mark" class="w-6 h-6" />
      </button>

      <h2 class="text-xl font-bold mb-4 text-center">
        {{ isEditing ? "Edit Facility" : "Add Facility" }}
      </h2>
      <div class="grid grid-cols-3 gap-4 items-center">
        <label class="text-gray-700 font-medium col-span-1">Resource Type:</label>
          
      <UInputMenu v-model="facility.resourceTypeId" :options="resourceTypeOptions" option-attribute="name"
        value-attribute="id" placeholder="Type or select resource type" size="md" class="col-span-2 w-full"
        clearable />

        <label class="text-gray-700 font-medium col-span-1">Resource Name:</label>
        <UInput v-model="facility.resourceName" class="col-span-2 w-full" />

        <label class="text-gray-700 font-medium col-span-1">Location:</label>
        <UInput v-model="facility.location" class="col-span-2 w-full" />

        <label class="text-gray-700 font-medium col-span-1">Capacity:</label>
        <UInput v-model.number="facility.capacity" type="number" class="col-span-2 w-full" />
      </div>

      <div class="mt-9 flex justify-end gap-3">
        <button @click="closeModal" class="bg-gray-400 text-white px-4 py-2 rounded hover:bg-gray-600">
          Cancel
        </button>
        <button @click="saveFacility" class="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-700">
          {{ isEditing ? "Update Facility" : "Save Facility" }}
        </button>
      </div>
    </UCard>
  </UModal>
  
  <!-- Maintenance Modal -->
  <UModal v-model="isMaintenanceModalOpen">
    <UCard class="p-9 max-w-lg">
      <button @click="closeMaintenanceModal" class="absolute top-4 right-4 text-gray-500 hover:text-gray-700">
        <UIcon name="i-heroicons-x-mark" class="w-6 h-6" />
      </button>

      <h2 class="text-xl font-bold mb-4 text-center">
        Schedule Maintenance
      </h2>
      
      <div v-if="selectedFacility" class="mb-6 p-4 bg-gray-50 rounded-lg">
        <p class="font-medium">Facility: {{ selectedFacility.resourceName }}</p>
        <p class="text-sm text-gray-600">Resource Type: {{ getResourceTypeName(Number(selectedFacility.resourceTypeId)) }}</p>
        <p class="text-sm text-gray-600">Location: {{ selectedFacility.location }}</p>
      </div>
      
      <!-- Upcoming maintenance for this facility -->
      <div v-if="facilityUpcomingMaintenance.length > 0" class="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <div class="flex items-start mb-2">
          <UIcon name="i-heroicons-calendar" class="text-blue-500 mr-2 flex-shrink-0" />
          <h3 class="font-medium text-blue-800">Upcoming Maintenance for this Facility</h3>
        </div>
        
        <div class="space-y-3 mt-2">
          <div v-for="maintenance in facilityUpcomingMaintenance" :key="maintenance.maintenanceId" 
              class="border-l-4 border-blue-400 pl-3 py-1 bg-white rounded-md">
            <div class="text-sm">
              <p class="font-medium text-blue-700">{{ formatDateForDisplay(maintenance.startDate) }} to {{ formatDateForDisplay(maintenance.endDate) }}</p>
              <p class="text-gray-600 line-clamp-2">{{ maintenance.description }}</p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Affected Bookings Warning -->
      <div v-if="affectedBookingsCount > 0" class="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
        <div class="flex items-start">
          <UIcon name="i-heroicons-exclamation-triangle" class="text-red-500 mr-3 flex-shrink-0 mt-0.5" />
          <div>
            <h3 class="font-medium text-red-700 mb-1">Warning: {{ affectedBookingsCount }} Booking{{ affectedBookingsCount > 1 ? 's' : '' }} Will Be Cancelled</h3>
            <p class="text-sm text-red-600 mb-2">
              Scheduling maintenance during this period will automatically cancel {{ affectedBookingsCount }} 
              existing booking{{ affectedBookingsCount > 1 ? 's' : '' }} and send notification emails to affected users.
            </p>
            <div v-if="affectedBookings.length > 0" class="mt-3 bg-white p-3 rounded border border-red-100 text-sm max-h-40 overflow-y-auto">
              <p class="font-medium mb-2">Affected bookings:</p>
              <ul class="list-disc pl-5 space-y-1">
                <li v-for="booking in affectedBookings" :key="booking.bookingId">
                  {{ booking.studentName }} ({{ booking.email }}) - {{ new Date(booking.bookedDatetime).toLocaleDateString() }} / {{ booking.timeslot }}
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      
      <div class="grid grid-cols-3 gap-4 items-start">
        <label class="text-gray-700 font-medium col-span-1 mt-2.5">Start Date:<span class="text-red-500">*</span></label>
        <div class="col-span-2 w-full">
          <UInput v-model="maintenanceData.startDate" type="date" class="w-full" :min="minDate" 
            :color="validationErrors.startDate ? 'red' : undefined" />
          <p v-if="validationErrors.startDate" class="text-red-500 text-sm mt-1">
            {{ validationErrors.startDate }}
          </p>
        </div>

        <label class="text-gray-700 font-medium col-span-1 mt-2.5">End Date:<span class="text-red-500">*</span></label>
        <div class="col-span-2 w-full">
          <UInput v-model="maintenanceData.endDate" type="date" class="w-full" :min="maintenanceData.startDate || minDate" 
            :color="validationErrors.endDate ? 'red' : undefined" />
          <p v-if="validationErrors.endDate" class="text-red-500 text-sm mt-1">
            {{ validationErrors.endDate }}
          </p>
        </div>

        <label class="text-gray-700 font-medium col-span-1 mt-2.5">Description:<span class="text-red-500">*</span></label>
        <div class="col-span-2 w-full">
          <UTextarea v-model="maintenanceData.description" class="w-full" :rows="textareaRows" 
            placeholder="Enter maintenance details here..." :color="validationErrors.description ? 'red' : undefined" />
          <p v-if="validationErrors.description" class="text-red-500 text-sm mt-1">
            {{ validationErrors.description }}
          </p>
        </div>
      </div>

      <div class="mt-9 flex justify-end gap-3">
        <button @click="closeMaintenanceModal" class="bg-gray-400 text-white px-4 py-2 rounded hover:bg-gray-600">
          Cancel
        </button>
        <button @click="saveMaintenance" class="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-700">
          Schedule Maintenance
        </button>
      </div>
    </UCard>
  </UModal>
  
  <!-- Early Release Confirmation Modal -->
  <UModal v-model="isReleaseConfirmModalOpen">
    <UCard class="p-9 max-w-lg">
      <button @click="closeReleaseConfirmModal" class="absolute top-4 right-4 text-gray-500 hover:text-gray-700">
        <UIcon name="i-heroicons-x-mark" class="w-6 h-6" />
      </button>

      <h2 class="text-xl font-bold mb-4 text-center">
        Release Facility from Maintenance
      </h2>
      
      <div v-if="selectedFacility" class="mb-6 p-4 bg-gray-100 rounded-lg">
        <p class="font-medium">Facility: {{ selectedFacility.resourceName }}</p>
        <p class="text-sm text-gray-600">Resource Type: {{ getResourceTypeName(Number(selectedFacility.resourceTypeId)) }}</p>
        <p class="text-sm text-gray-600">Location: {{ selectedFacility.location }}</p>
      </div>
      
      <div v-if="currentMaintenanceDetails" class="mb-6 p-4 bg-orange-50 border border-orange-200 rounded-lg">
        <h3 class="font-medium text-orange-700 mb-2">Current Maintenance Information</h3>
        <div class="grid grid-cols-2 gap-2 text-sm">
          <p class="text-gray-600">Start Date:</p>
          <p class="font-medium">{{ formatDateForDisplay(currentMaintenanceDetails.startDate) }}</p>
          
          <p class="text-gray-600">Scheduled End Date:</p>
          <p class="font-medium">{{ formatDateForDisplay(currentMaintenanceDetails.endDate) }}</p>
          
          <p class="text-gray-600">Description:</p>
          <p class="font-medium">{{ currentMaintenanceDetails.description }}</p>
        </div>
      </div>
      
      <div class="bg-yellow-50 p-4 rounded-lg mb-6 border border-yellow-200">
        <div class="flex items-start">
          <UIcon name="i-heroicons-exclamation-triangle" class="text-yellow-500 mr-3 flex-shrink-0 mt-0.5" />
          <p class="text-sm text-gray-700">
            You are about to release this facility from maintenance earlier than scheduled. 
            The maintenance end date will be set to today, and the facility will be available for booking immediately.
          </p>
        </div>
      </div>

      <div class="mt-6 flex justify-end gap-3">
        <button @click="closeReleaseConfirmModal" class="bg-gray-400 text-white px-4 py-2 rounded hover:bg-gray-600">
          Cancel
        </button>
        <button 
          @click="releaseFacilityFromMaintenance" 
          class="bg-orange-500 text-white px-4 py-2 rounded hover:bg-orange-700 flex items-center"
          :disabled="releasingMaintenance"
        >
          <UIcon v-if="releasingMaintenance" name="i-heroicons-arrow-path" class="animate-spin mr-2" />
          <span>{{ releasingMaintenance ? 'Releasing...' : 'Release Facility' }}</span>
        </button>
      </div>
    </UCard>
  </UModal>
</template>

<style scoped>
/* Add animation for error messages */
.text-red-500 {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-5px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Loading animation */
.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>