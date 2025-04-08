<script setup lang="ts">
definePageMeta({
  middleware: ['auth', 'admin']
});

import { useRouter } from "vue-router";
import { computed, onMounted, ref } from "vue";
import type { Facility } from "@/composables/useFacility";
import { useFacility } from "@/composables/useFacility";
import axios from "axios";


const router = useRouter();
const isModalOpen = ref(false);
const isEditing = ref(false);

definePageMeta({
  middleware: ['auth', 'admin']
})

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

const markAsMaintenance = (row: any) => {
  console.log(`Maintenance mode triggered for facility: ${row.facilityId}`);
  alert("Maintenance mode triggered for facility: " + row.facilityId);
};

const closeModal = () => {
  isModalOpen.value = false;
  resetFacility();
};

const viewFacility = (facilityId: number) => {
  router.push(`/facility-details/${facilityId}`);
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



onMounted(async () => {
  await fetchResourceTypes();
  console.log("resourceTypeOptions:", resourceTypeOptions.value); // Add this line
  fetchFacilities();
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


              <UButton @click="markAsMaintenance(row)" color="red" variant="solid" icon="i-heroicons-wrench"
                label="Maintenance" class="px-3 py-1 gap-2" />

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
</template>
