<script setup lang="ts">
const colorMode = useColorMode();

const toggleColorMode = () => {
  colorMode.preference = colorMode.value === "dark" ? "light" : "dark";
};
</script>

<template>
  <header class="h-16 border-b px-4 flex items-center justify-between">
    <h1 class="font-bold text-xl"><NuxtLink to="/">Resource Booking System</NuxtLink></h1>
    <div class="flex items-center gap-4">
      <UButton variant="ghost" @click="toggleColorMode">
        <UIcon
          v-if="colorMode.preference === 'light'"
          name="i-heroicons:moon"
          class="w-5 h-5"
        />
        <UIcon v-else name="i-heroicons:sun" class="w-5 h-5" />
      </UButton>
      <UButton :ui="{ rounded: 'rounded-full' }" size="lg">
        <NuxtLink to="/login"> Login </NuxtLink>
      </UButton>
    </div>
  </header>
</template>
