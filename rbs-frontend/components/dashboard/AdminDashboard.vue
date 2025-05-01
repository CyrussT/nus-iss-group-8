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

const isEmergencyModalOpen = ref(false);
const emergencyInput = ref('');

const pushEmergency = async () => {
  if (!emergencyInput.value.trim()) {
    alert('Message cannot be empty!');
    return;
  }

  try {
    await axios.post('http://localhost:8080/api/emergency/push', {
      message: emergencyInput.value
    });
    alert('Emergency Notice sent successfully!');
    emergencyInput.value = ''; // Clear after send
  } catch (error) {
    console.error('Failed to send emergency notice:', error);
    alert('Failed to send emergency notice. Try again.');
  }
};

onMounted(fetchAdminStats);
</script>
<template>
  <div class="p-8 space-y-6">

    <!-- Header and Emergency Button -->
    <div class="flex justify-between items-center">
      <h1 class="text-2xl font-bold">Admin Dashboard</h1>
      <UButton @click="isEmergencyModalOpen = true" color="red" variant="solid" 
      class="px-4 py-2 gap-2" 
      label="Push Emergency Notice" />
    </div>

    <!-- Stat Cards -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <UCard>
        <template #header>
          <p class="text-gray-500">Today's Bookings</p>
        </template>
        <p class="text-4xl font-bold text-center">{{ todayBookingsCount }}</p>
      </UCard>

      <UCard>
        <template #header>
          <p class="text-gray-500">Pending Approvals</p>
        </template>
        <p class="text-4xl font-bold text-center">{{ pendingApprovalsCount }}</p>
      </UCard>

      <UCard>
        <template #header>
          <p class="text-gray-500">Facilities Under Maintenance</p>
        </template>
        <p class="text-4xl font-bold text-center">{{ facilitiesUnderMaintenanceCount }}</p>
      </UCard>
    </div>

    <!-- Emergency Message Display -->
    <UCard v-if="emergencyMessage" class="border border-red-300 bg-red-50">
      <template #header>
        <h2 class="text-lg font-bold text-red-700">🚨 Emergency Message</h2>
      </template>
      <p class="text-red-700">{{ emergencyMessage }}</p>
    </UCard>

    <!-- Emergency Modal -->
    <UModal v-model="isEmergencyModalOpen">
      <div class="relative p-6 bg-red-50 border border-red-300 rounded-xl shadow-lg">
        <!-- Close Button -->
        <button @click="isEmergencyModalOpen = false"
          class="absolute top-3 right-3 text-red-600 hover:text-red-800 text-xl font-bold">
          &times;
        </button>

        <h2 class="text-xl font-bold text-red-700 mb-4">Emergency Notice</h2>

        <UTextarea v-model="emergencyInput" placeholder="Type emergency message here..." class="mb-4" :rows="3" />

        <UButton @click="pushEmergency" color="red" variant="solid" label="Push Notice" />
      </div>
    </UModal>
  </div>
</template>

