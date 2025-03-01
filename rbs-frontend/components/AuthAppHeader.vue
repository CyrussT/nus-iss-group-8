<script setup lang="ts">
const auth = useAuthStore();
const router = useRouter();
const colorMode = useColorMode();


const toggleColorMode = () => {
  colorMode.preference = colorMode.value === "dark" ? "light" : "dark";
};
const isAdmin = computed(() => auth.user.value?.role === "ADMINISTRATOR");

console.log("user role:"+auth.user.value?.role);

const handleLogout = () => {
  auth.logout();
  router.push("/login");
};
</script>

<template>
  <header class="h-16 border-b px-4 flex items-center justify-between">
    <h1 class="font-bold text-xl"><NuxtLink to="/">Resource Booking System</NuxtLink></h1>

    <div> 
      <NuxtLink
        v-if="isAdmin"
        to="/facility"
        class="text-sm font-semibold text-blue-500 hover:underline"
      >
        Facility
      </NuxtLink>
    </div>
          


    <div class="flex items-center gap-4">
      <UButton variant="ghost" @click="toggleColorMode">
        <UIcon
          v-if="colorMode.preference === 'light'"
          name="i-heroicons:moon"
          class="w-5 h-5"
        />
        <UIcon v-else name="i-heroicons:sun" class="w-5 h-5" />
      </UButton>

      <UButton :ui="{ rounded: 'rounded-full' }" size="lg" @click="handleLogout">
        <a> Logout </a>
      </UButton>
    </div>
  </header>
</template>
