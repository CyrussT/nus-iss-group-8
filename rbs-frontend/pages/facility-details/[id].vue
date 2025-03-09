<script lang="ts" setup>
definePageMeta({
  middleware: ['auth', 'admin']
});

import { onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useFacility } from "@/composables/useFacility";

const route = useRoute();
const router = useRouter();
const { facility, fetchFacilityDetails } = useFacility();

const goBack = () => {
  router.push("/facility");
};

// ✅ Fetch facility details on component mount
onMounted(() => {
  const facilityId = Number(route.params.id);
  if (!isNaN(facilityId)) {
    fetchFacilityDetails(facilityId);
  } else {
    router.push("/facility"); // Redirect if ID is invalid
  }
});
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
            <td class="font-semibold text-gray-700 p-4">Type:</td>
            <td class="p-4">{{ facility.resourceType }}</td>
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

    <div class="mt-6">
      <UButton @click="goBack" color="blue" variant="solid" class="px-6 py-2 text-lg">
        <UIcon name="i-heroicons-arrow-left" class="w-5 h-5 mr-2" />
        Back to Facilities
      </UButton>
    </div>
  </div>
</template>