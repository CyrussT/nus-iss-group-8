<template>
  <div>
    <p>Student Page</p>
  </div>
<!-- 
  <div v-if="emergencyMessage" class="bg-red-100 p-4 rounded-xl text-red-700 font-semibold shadow mb-4">
    🚨 {{ emergencyMessage }}
  </div> -->
</template>

<script lang="ts" setup>
definePageMeta({
  middleware: ['auth', 'student']
})

// const emergencyMessage = ref<string | null>(null);

// onMounted(() => {
//   const socket = new SockJS('http://localhost:8080/ws');
//   const stompClient = Stomp.over(socket);

//   stompClient.connect({}, () => {
//     stompClient.subscribe('/topic/emergency', (message) => {
//       emergencyMessage.value = message.body;
//       alert(`🚨 Emergency Notice: ${message.body}`);  // You can make a custom popup later
//     });
//   });
// });
</script>