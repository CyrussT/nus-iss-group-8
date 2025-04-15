<script lang="ts" setup>
definePageMeta({
  middleware: ['auth', 'admin']
});

import { onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useFacility } from "@/composables/useFacility";

const route = useRoute();
const router = useRouter();
const { facility, fetchFacilityDetails, getResourceTypeName } = useFacility();
const resourceTypeName = ref("");

const goBack = () => {
  router.push("/facility");
};

// Fetch facility details on component mount
onMounted(() => {
  const facilityId = Number(route.params.id);
  if (!isNaN(facilityId)) {
    fetchFacilityDetails(facilityId);
  } else {
    router.push("/facility"); // Redirect if ID is invalid
  }
});

watch (
  () => facility.value.resourceTypeId,
  (newResourceTypeId) => {
    resourceTypeName.value = getResourceTypeName(Number(newResourceTypeId));
  }
);

// Pagination state
const currentPage = ref(1);
const pageSize = ref(5); // Number of rows per page

// Computed property to handle pagination
const paginatedBookings = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return facility.value.bookings.slice(start, end);
});

// Sorting state
const sortKey = ref<keyof typeof facility.value.bookings[0] | null>(null);
const sortOrder = ref<"asc" | "desc">("asc");

// Sorting function
</script>

<template>
  <div class="p-8 bg-gray-100 min-h-screen">
    <h1 class="text-3xl font-bold text-gray-800 mb-6">Facility Details</h1>

    <div class="bg-white shadow-md rounded-lg p-6">
      <table class="w-full border-collapse">
        <tbody>
          <tr class="border-b">
            <td class="font-semibold text-gray-700 p-4">Facility ID:</td>
            <td class="p-4">{{ facility.facilityId }}</td>
          </tr>
          <tr class="border-b">
            <td class="font-semibold text-gray-700 p-4">Resource Type Name:</td>
            <td class="p-4">{{ resourceTypeName }}</td>
          </tr>
          <tr class="border-b">
            <td class="font-semibold text-gray-700 p-4">Name:</td>
            <td class="p-4">{{ facility.resourceName }}</td>
          </tr>
          <tr class="border-b">
            <td class="font-semibold text-gray-700 p-4">Location:</td>
            <td class="p-4">{{ facility.location }}</td>
          </tr>
          <tr>
            <td class="font-semibold text-gray-700 p-4">Capacity:</td>
            <td class="p-4">{{ facility.capacity }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 📌 Booking History Table -->
    <div class="mt-8 bg-white shadow-md rounded-lg p-6">
      <h2 class="text-2xl font-bold mb-4">Booking History</h2>

      <!-- ✅ Show message if no bookings -->
      <p v-if="!facility.bookings || facility.bookings.length === 0" class="text-gray-500 text-center py-4">
        No bookings for this facility yet.
      </p>

      <!-- ✅ Show table only if bookings exist -->
      <UTable v-if="facility.bookings.length > 0" :rows="paginatedBookings" :columns="[
        { key: 'sn', label: 'S/N' },
        { key: 'email', label: 'Email', sortable: true },
        { key: 'studentName', label: 'Student Name', sortable: true },
        { key: 'purpose', label: 'Purpose' },
        { key: 'bookedDatetime', label: 'Booked Date', sortable: true },
        { key: 'timeslot', label: 'Timeslot' },
        { key: 'status', label: 'Status', sortable: true }
      ]" :loading="!facility.bookings.length">

        <!-- Serial Number -->
      <template #sn-data="{ index }">
          {{ index + 1 + (currentPage - 1) * pageSize }}
      </template>
      
        <!-- Format Date -->
        <template #bookedDatetime-data="{ row }">
          {{ new Date(row.bookedDatetime).toLocaleDateString('en-GB').replace(/\//g, '-') }}
        </template>

        <!-- Status Styling -->
        <template #status-data="{ row }">
          <span :class="{
            'text-green-600 font-bold': row.status === 'CONFIRMED',
            'text-yellow-600 font-bold': row.status === 'PENDING',
            'text-red-600 font-bold': row.status === 'CANCELLED'
          }">
            {{ row.status }}
          </span>
        </template> 

      </UTable>

      <!-- Pagination Controls -->
      <div class="mt-4 flex justify-between items-center">
        <UButton @click="currentPage--" :disabled="currentPage === 1" color="gray">Previous</UButton>
        <span class="text-gray-700">Page {{ currentPage }} of {{ Math.ceil(facility.bookings.length / pageSize) }}</span>
        <UButton @click="currentPage++" :disabled="currentPage * pageSize >= facility.bookings.length" color="gray">Next</UButton>
      </div>
    </div>

    <div class="mt-6">
      <UButton @click="goBack" color="blue" variant="solid" class="px-6 py-2 text-lg">
        <UIcon name="i-heroicons-arrow-left" class="w-5 h-5 mr-2" />
        Back to Facilities
      </UButton>
    </div>
  </div>
</template>