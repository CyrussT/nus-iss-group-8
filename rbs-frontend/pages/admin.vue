<template>
  <div>
    <p>Admin Page</p>
  </div>
</template>

<script lang="ts" setup>
definePageMeta({
  middleware: ['auth', 'admin']
})
</script>

<style>

</style>