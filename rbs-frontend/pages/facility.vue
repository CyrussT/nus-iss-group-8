<script setup lang="ts">
import { useRouter } from "vue-router";
import { computed } from "vue";

const auth = useAuthStore();
const router = useRouter();

// Redirect non-admin users back to home
const isAdmin = computed(() => auth.user.value?.role === "ADMINISTRATOR");

if (!isAdmin.value) {
  router.push("/");
}
</script>

<template>
  <div class="p-8">
    <h1 class="text-2xl font-bold">Facility Management</h1>
    <p>Welcome, Admin! You have access to this page.</p>
  </div>
</template>
