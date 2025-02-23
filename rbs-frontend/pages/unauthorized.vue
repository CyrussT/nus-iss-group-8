<script lang="ts" setup>
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-screen">
    <div class="p-8 rounded-lg shadow-md">
      <h1 class="text-3xl font-bold mb-4 ml-4">Unauthorized Access</h1>
      <p class="mb-6">You do not have permission to access this page.</p>
      <div class="flex justify-center">
        <NuxtLink 
          to="/login" 
          class="bg-blue-500 hover:bg-blue-600 text-white font-semibold py-2 px-4 rounded"
        >
          Back to Login
        </NuxtLink>
      </div>
    </div>
  </div>
</template>
