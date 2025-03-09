<script setup lang="ts">
const auth = useAuthStore();
const router = useRouter();
const colorMode = useColorMode();
const showManageDropdown = ref(false); // Controls dropdown visibility

const toggleColorMode = () => {
  colorMode.preference = colorMode.value === "dark" ? "light" : "dark";
};

const userRole = computed(() => auth.user.value?.role);

const isAdmin = computed(() => userRole.value === "ADMINISTRATOR");
const isStudent = computed(() => userRole.value === "STUDENT");

console.log("User role:", userRole.value);

const handleLogout = () => {
  auth.logout();
  router.push("/login");
};


</script>

<template>
  <header class="h-16 border-b px-4 flex items-center justify-between">
    <h1 class="font-bold text-xl">
      <NuxtLink to="/">Resource Booking System</NuxtLink>
    </h1>

    <nav class="flex gap-4 relative">
      <template v-if="isAdmin">
        <NuxtLink to="/facility" class="text-sm font-semibold text-blue-500 hover:underline">
          Facility
        </NuxtLink>

        <NuxtLink to="/student-list" class="text-sm font-semibold text-blue-500 hover:underline">
          Manage Students
        </NuxtLink>
      </template>
      <template v-if="isStudent">
        <NuxtLink
        to="/booking"
        class="text-sm font-semibold text-blue-500 hover:underline">
        Booking
        </NuxtLink>
          <NuxtLink
          to="/student-booking"
          class="text-sm font-semibold text-blue-500 hover:underline">
          My Bookings
        </NuxtLink>
      </template>
    </nav>




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

