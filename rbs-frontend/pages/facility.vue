<script setup lang="ts">
import { useRouter } from "vue-router";
import { computed, onMounted, ref } from "vue";
import axios from "axios";

const auth = useAuthStore();
const router = useRouter();

const isAdmin = computed(() => auth.user.value?.role === "ADMINISTRATOR");
if (!isAdmin.value) {
  router.push("/");
}


const facilities = ref([]);
const loading = ref(true);
const currentPage = ref(0);
const pageSize = ref(10);
const totalPages = ref(1); // Track total pages from backend

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
  currentPage.value = 0;
  fetchFacilities(); // Reload all facilities
};

const fetchFacilities = async () => {
  try {
    loading.value = true;
    const response = await axios.get("http://localhost:8080/api/facilities/search", {
      params: {
        ...searchQuery.value, // Spread search parameters
        page: currentPage.value,
        size: pageSize.value,
      },
    });

    facilities.value = response.data.content; // Extract paginated content
    totalPages.value = response.data.totalPages; // Get total pages
  } catch (error) {
    console.error("Error fetching facilities:", error);
  } finally {
    loading.value = false;
  }
};

const nextPage = () => {
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++;
    fetchFacilities(); 
  }
};


const goToFacilityDetails = (facilityId: number) => {
  router.push(`/facility/${facilityId}`);
};


onMounted(fetchFacilities);
</script>

<template>
  <div class="p-8">
    <h1 class="text-2xl font-bold mb-4">Facility Management</h1>
    
          <div class="mb-4 grid grid-cols-2 gap-4">
        <UInput v-model="searchQuery.resourceType" placeholder="Resource Type" />
        <UInput v-model="searchQuery.resourceName" placeholder="Resource Name" />
        <UInput v-model="searchQuery.location" placeholder="Location" />
        <UInput v-model="searchQuery.capacity" type="number" placeholder="Capacity" />
        <div class="col-span-2 flex justify-end gap-2">
          <button
            @click="resetSearch"
            class="bg-gray-400 text-white px-4 py-2 rounded hover:bg-gray-600"
          >
            Reset
          </button>

          <button
            @click="fetchFacilities"
            class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-700"
          >
            Search
          </button>
        </div>
      </div>

    <UCard class="w-full max-w-8xl p-6 shadow-lg bg-white"> 

          <div class="mt-6 overflow-x-auto">
      <UTable
        :rows="facilities"
        :loading="loading"
        :columns="[
          { key: 'sn', label: 'SN', sortable: true },
          { key: 'resourceType', label: 'Type', sortable: true },
          { key: 'resourceName', label: 'Name', sortable: true },
          { key: 'location', label: 'Location', sortable: true },
          { key: 'capacity', label: 'Capacity', sortable: true },
          { key: 'actions', label: 'Actions' }
        ]"
      >
        <template #sn-data="{ row, index }">
          {{ index + 1 + currentPage * pageSize }}
        </template>

        <template #actions-data="{ row }">
          <button
            @click="goToFacilityDetails(row.facilityId)"
            class="bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-700"
          >
            View Details
          </button>
        </template>
      </UTable>
    </div>


    <div class="mt-4 flex justify-center items-center">
    <button 
      @click="currentPage > 0 ? (currentPage--, fetchFacilities()) : null" 
      :disabled="currentPage === 0"
      class="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-700 disabled:opacity-50 flex items-center"
    >
      <UIcon name="i-heroicons-arrow-left" class="w-5 h-5 mr-1" /> 
      Previous
    </button>

    <span class="text-lg">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
    <button 
      @click="nextPage"
      :disabled="currentPage >= totalPages - 1"
      class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50 flex items-center"
    >
      Next
      <UIcon name="i-heroicons-arrow-right" class="w-5 h-5 ml-1" /> 
    </button>
  </div>
    </UCard>

  </div>
</template>
