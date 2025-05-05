<script setup lang="ts">
import { useAuthStore } from "@/composables/authStore";
import StudentDashboard from "@/components/dashboard/StudentDashboard.vue";
import AdminDashboard from "@/components/dashboard/AdminDashboard.vue";

const auth = useAuthStore();
</script>

<template>
  <div>
    <StudentDashboard v-if="auth.user.value?.role === 'STUDENT'" />
    <AdminDashboard v-else-if="auth.user.value?.role === 'ADMINISTRATOR'" />
    <div v-else class="text-center p-10">Unauthorized</div>
  </div>
</template>
