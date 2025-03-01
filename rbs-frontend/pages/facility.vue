<script setup lang="ts">
import { useRouter } from "vue-router";
import { computed, onMounted, ref } from "vue";
import type { Facility } from "@/composables/useFacility";
import { useFacility } from "@/composables/useFacility";
import axios from "axios";

const auth = useAuthStore();
const router = useRouter();
const isModalOpen = ref(false);
const isEditing = ref(false);

const isAdmin = computed(() => auth.user.value?.role === "ADMINISTRATOR");

onMounted(() => {
  if (!isAdmin.value) {
    router.push("/");
  }
});

const {
  searchQuery,
  fetchFacilities,
  resetSearch,
  facility,
  resetFacility,
  facilities,
  loading,
  currentPage,
  totalPages,
  nextPage,
  prevPage,
  pageSize,
} = useFacility();


const openModal = (facilityData: Partial<Facility> | null = null) => {
  if (facilityData && typeof facilityData === "object") {
    facility.value = {
      facilityId: facilityData.facilityId ?? undefined,
      resourceType: facilityData.resourceType ?? "",
      resourceName: facilityData.resourceName ?? "",
      location: facilityData.location ?? "",
      capacity: facilityData.capacity ?? 1,
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

onMounted(fetchFacilities);
</script>

<template>
  <div class="p-8">
    <h1 class="text-2xl font-bold mb-4">Facility Management</h1>

    <div class="grid grid-cols-2 gap-4">
      <UInput v-model="searchQuery.resourceType" placeholder="Resource Type" />
      <UInput v-model="searchQuery.resourceName" placeholder="Resource Name" />
      <UInput v-model="searchQuery.location" placeholder="Location" />
      <UInput v-model="searchQuery.capacity" type="number" placeholder="Capacity" />

      <div class="col-span-2 flex justify-end gap-2">
        <button @click="fetchFacilities"
          class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-700 flex items-center gap-2">
          <UIcon name="i-ic:baseline-search" class="w-5 h-5" />
          Search
        </button>

        <button @click="resetSearch"
          class="bg-gray-400 text-white px-4 py-2 rounded hover:bg-red-500 flex items-center gap-2">
          <UIcon name="i-ic:round-restart-alt" class="w-5 h-5" />
          Reset
        </button>
        <button @click="openModal()"
          class="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-700 flex items-center gap-2">
          <UIcon name="i-ic:baseline-plus" class="w-5 h-5" />
          Add Facility
        </button>
      </div>
    </div>

    <UCard class="w-full max-w-8xl p-6 shadow-lg bg-white">
      <div class="mt-6 overflow-x-auto">
        <UTable :rows="facilities" :loading="loading" :columns="[
          { key: 'sn', label: 'SN', sortable: true },
          { key: 'resourceType', label: 'Type', sortable: true },
          { key: 'resourceName', label: 'Name', sortable: true },
          { key: 'location', label: 'Location', sortable: true },
          { key: 'capacity', label: 'Capacity', sortable: true },
          { key: 'actions', label: 'Actions', class: 'text-center' }
        ]">
          <template #sn-data="{ index }">
            {{ index + 1 + currentPage * pageSize }}
          </template>

          <template #actions-data="{ row }">
            <div class="flex justify-center gap-2">
              <button @click="openModal(row)"
                class="bg-yellow-500 text-white px-3 py-1 rounded flex items-center gap-2 hover:bg-yellow-700">
                <UIcon name="i-heroicons-pencil" class="w-5 h-5" />
                Edit
              </button>

              <button @click="markAsMaintenance(row)"
                class="bg-red-500 text-white px-3 py-1 rounded flex items-center gap-2 hover:bg-red-700">
                <UIcon name="i-heroicons-wrench" class="w-5 h-5" />
                Maintenance
              </button>
            </div>
          </template>
        </UTable>
      </div>

      <div class="mt-4 flex justify-center items-center">
        <button @click="currentPage > 0 ? (currentPage--, fetchFacilities()) : null" :disabled="currentPage === 0"
          class="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-700 disabled:opacity-50 flex items-center">
          <UIcon name="i-heroicons-arrow-left" class="w-5 h-5 mr-1" />
          Previous
        </button>

        <span class="text-lg">Page {{ currentPage + 1 }} of {{ totalPages }}</span>

        <button @click="nextPage" :disabled="currentPage >= totalPages - 1"
          class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50 flex items-center">
          Next
          <UIcon name="i-heroicons-arrow-right" class="w-5 h-5 ml-1" />
        </button>
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
        <UInput v-model="facility.resourceType" class="col-span-2 w-full" />

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
