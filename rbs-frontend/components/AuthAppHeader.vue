<script setup lang="ts">
const auth = useAuthStore()
const router = useRouter()
const colorMode = useColorMode()
const showManageDropdown = ref(false)

const toggleColorMode = () => {
  colorMode.preference = colorMode.value === "dark" ? "light" : "dark";
};

const userRole = ref<string | null>(null)

watch(
  () => auth.user.value?.role,
  (newRole) => {
    userRole.value = newRole || null
  },
  { immediate: true }
)

const isAdmin = computed(() => userRole.value === 'ADMINISTRATOR')
const isStudent = computed(() => userRole.value === 'STUDENT')

const handleLogout = () => {
  auth.logout()
  router.push('/login')
}

interface NavigationMenuItem {
  label: string
  to?: string
  active?: boolean
  target?: '_blank' | '_self' | '_parent' | '_top'
}

const items = computed<NavigationMenuItem[]>(() => {
  const baseItems: NavigationMenuItem[] = []

  if (isAdmin.value) {
    baseItems.push(
      { label: 'Facility', to: '/facility' },
      { label: 'Manage Students', to: '/student-list' },
      { label: 'Booking Requests', to: '/booking-review-list' }
    )
  }

  if (isStudent.value) {
    baseItems.push({ label: 'Booking', to: '/booking' })
  }

  return baseItems
})
</script>

<template>
  <header class="h-16 border-b px-4 flex items-center justify-between">
    <h1 class="font-bold text-xl">
      <NuxtLink to="/">Resource Booking System</NuxtLink>
    </h1>

    <!-- Navigation -->
    <UHorizontalNavigation :links="items" class="justify-center" />

    <!-- Right controls -->
    <div class="flex items-center gap-4">
      <UButton variant="ghost" @click="toggleColorMode">
        <UIcon
          v-if="colorMode.preference === 'light'"
          name="i-heroicons:moon"
          class="w-5 h-5"
        />
        <UIcon
          v-else
          name="i-heroicons:sun"
          class="w-5 h-5"
        />
      </UButton>

      <UButton :ui="{ rounded: 'rounded-full' }" size="lg" @click="handleLogout">
        Logout
      </UButton>
    </div>
  </header>
</template>
