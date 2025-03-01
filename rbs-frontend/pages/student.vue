<template>
  <div>
    <p>Student Page</p>
  </div>
</template>

<script lang="ts" setup>
definePageMeta({
  middleware: ['auth', 'student']
})
</script>

<style>

</style>