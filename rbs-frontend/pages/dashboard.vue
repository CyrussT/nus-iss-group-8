<script lang="ts" setup>

const auth = useAuthStore();
const user = auth.user.value;

definePageMeta({
  middleware: ['auth']
})
</script>

<template>
  <div>
    <h1>Welcome to Dashboard Page!</h1>
    <br/>
    <h1>{{ user?.email }}</h1>

    <p>You have {{ user?.role }} role.</p>
  </div>
</template>


<style></style>
