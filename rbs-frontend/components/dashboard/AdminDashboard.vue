<script setup lang="ts">
import { ref, onMounted } from "vue";
import axios from "axios";

const todayBookingsCount = ref(0);
const pendingApprovalsCount = ref(0);
const facilitiesUnderMaintenanceCount = ref(0);
const emergencyMessage = ref<string | null>(null);

const fetchAdminStats = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/bookings/admin/dashboard-stats');
    const data = res.data;
    todayBookingsCount.value = data.todayBookings;
    pendingApprovalsCount.value = data.pendingApprovals;
    facilitiesUnderMaintenanceCount.value = data.facilitiesUnderMaintenance;
    emergencyMessage.value = data.emergencyMessage || null;
  } catch (error) {
    console.error('Error fetching admin stats', error);
  }
};

onMounted(fetchAdminStats);
</script>

<template>
  <div class="p-8 flex flex-col gap-6">
    <h1 class="text-2xl font-bold">Admin Dashboard</h1>
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="bg-white p-6 rounded-lg shadow-md text-center">
        <p class="text-gray-600">Today's Bookings</p>
        <p class="text-4xl font-bold">{{ todayBookingsCount }}</p>
      </div>
      <div class="bg-white p-6 rounded-lg shadow-md text-center">
        <p class="text-gray-600">Pending Approvals</p>
        <p class="text-4xl font-bold">{{ pendingApprovalsCount }}</p>
      </div>
      <div class="bg-white p-6 rounded-lg shadow-md text-center">
        <p class="text-gray-600">Facilities Under Maintenance</p>
        <p class="text-4xl font-bold">{{ facilitiesUnderMaintenanceCount }}</p>
      </div>
    </div>
    <div v-if="emergencyMessage" class="bg-red-100 border border-red-300 p-4 rounded-lg mt-6">
      <h2 class="font-bold text-lg mb-2">🚨 Emergency Message</h2>
      <p>{{ emergencyMessage }}</p>
    </div>
  </div>
</template>
