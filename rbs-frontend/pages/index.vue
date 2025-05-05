<script setup lang="ts">
import indexImage from './image/index2.jpg'

definePageMeta({
  middleware: ['guest'],
  layout: 'unauth'
})

// Declare imageUrl as a constant
const imageUrl = indexImage
</script>

<template>
  <div class="full-page">
    <img :src="imageUrl" alt="Background" class="background-image" />
  </div>
</template>


<style scoped>
.full-page {
  width: 100vw;
  height: 95vh;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.background-image {
  width: 100%;
  height: 100%;
  object-fit: cover; /* cover fills while preserving aspect ratio */
  display: block;
}
</style>