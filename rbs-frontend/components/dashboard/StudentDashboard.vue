<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { useBooking } from "@/composables/useBooking";
import { useAuthStore } from "@/composables/authStore";
import ConfirmationModal from "@/components/booking/ConfirmationModal.vue";
import { Client } from '@stomp/stompjs';
import { useToast } from '#imports'
const toast = useToast();

const { apiUrl } = useApi();
const { upcomingApprovedBookings, pendingBookings, pastBookings, availableCredits, fetchUpcomingApprovedBookings, fetchPendingBookings, fetchPastBookings, fetchAvailableCredits, fetchAccountId } = useBooking();
const auth = useAuthStore();
const accountId = ref<number | null>(null);
const confirmModal = ref<InstanceType<typeof ConfirmationModal> | null>(null);

// Fetch Student Data
const loadStudentData = async () => {
  if (auth.user.value?.email) {
    accountId.value = await fetchAccountId(auth.user.value.email);
    if (accountId.value) {
      fetchPastBookings(accountId.value);
      fetchPendingBookings(accountId.value);
      fetchUpcomingApprovedBookings(accountId.value);
      fetchAvailableCredits(auth.user.value.email);
    }
  }
};

const emergencyMessage = ref();

const connectWebSocket = () => {
  const wsApi = apiUrl.replace('https://', 'wss://').replace('http://', 'ws://').concat('/ws');
  const stompClient = new Client({
    brokerURL: wsApi,
    reconnectDelay: 5000,
    onConnect: () => {
      console.log('Connected to WebSocket!');
      stompClient.subscribe('/topic/emergency', (message) => {
        emergencyMessage.value = message.body;
        setTimeout(() => {
          emergencyMessage.value = null;
        }, 10_000);
      });

    },
    onDisconnect: () => {
      console.log('Disconnected from WebSocket.');
    },
  });

  stompClient.activate();
};

await loadStudentData();

const items = [
  { key: 'upcoming', label: 'Upcoming', icon: 'material-symbols:event-upcoming', description: 'Upcoming Bookings' },
  { key: 'pending', label: 'Pending', icon: 'material-symbols:calendar-clock', description: 'Pending Bookings' },
  { key: 'past', label: 'Past', icon: 'material-symbols:event-repeat', description: 'Past Bookings' }
];

const columns = [
  { key: "facilityName", label: "Resource Name" },
  { key: "bookedDatetime", label: "Date" },
  { key: "timeslot", label: "Time" },
  { key: "status", label: "Status" },
  { key: "bookingId", label: "Booking ID" },
  { key: "actions", label: "Actions" }
];

const formatBookingData = (bookings: any[]) => bookings.map(booking => ({
  facilityName: booking.facilityName,
  bookedDatetime: new Date(booking.bookedDatetime).toLocaleDateString(),
  timeslot: booking.timeslot,
  status: booking.status,
  bookingId: booking.bookingId
}));

const formattedUpcomingBookings = computed(() => formatBookingData(upcomingApprovedBookings.value));
const formattedPendingBookings = computed(() => formatBookingData(pendingBookings.value));
const formattedPastBookings = computed(() => formatBookingData(pastBookings.value));
const formattedCredits = computed(() => {
  if (availableCredits.value === null) return 'Loading...';
  const hours = Math.floor(availableCredits.value / 60);
  const minutes = availableCredits.value % 60;
  return `${hours} hour${hours !== 1 ? 's' : ''}${minutes ? `, ${minutes} min` : ''}`;
});

const openCancelModal = (bookingId: number) => {
  if (confirmModal.value) {
    confirmModal.value.openModal({
      title: "Cancel Booking",
      confirmLabel: "Yes, Cancel",
      cancelLabel: "No, Keep it",
      onConfirm: () => cancelBooking(bookingId),
    });
  }
};

const cancelBooking = async (bookingId: number) => {
  try {
    const email = auth.user.value?.email || '';
    await fetch(`http://localhost:8080/api/bookings/cancel-booking/${bookingId}?toEmail=${encodeURIComponent(email)}`, {
      method: 'DELETE'
    });

    toast.add({
      title: 'Booking Cancelled',
      description: 'Your booking was successfully cancelled.',
      color: 'green'
    });

    if (accountId.value) {
      fetchUpcomingApprovedBookings(accountId.value);
      fetchPendingBookings(accountId.value);
      fetchPastBookings(accountId.value);
      fetchAvailableCredits(email);
    }

  } catch (e) {
    console.error('Error cancelling booking', e);

    toast.add({
      title: 'Error',
      description: 'Failed to cancel the booking. Please try again.',
      color: 'red'
    });
  }
};
onMounted(() => {
  connectWebSocket();
});
</script>

<template>
  <div class="p-8 grid grid-cols-1 lg:grid-cols-[4fr_1fr] gap-6 bg-white dark:bg-gray-900">
    <!-- Bookings Section (80%) -->
    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6">
      <h1 class="text-2xl font-bold mb-4">My Bookings</h1>

      <div v-if="emergencyMessage" class="bg-red-100 p-4 rounded-xl text-red-700 font-semibold shadow mb-4">
        🚨 {{ emergencyMessage }}
      </div>

      <UTabs :items="items">
        <template #item="{ item }">
          <div v-if="item.key === 'upcoming'">
            <div v-if="formattedUpcomingBookings.length > 0">
              <UTable :rows="formattedUpcomingBookings" :columns="columns" class="shadow-lg rounded-lg overflow-hidden">
                <template #actions-data="{ row }">
                  <UButton @click="openCancelModal(row.bookingId)" color="red" variant="solid" label="Cancel" />
                </template>
              </UTable>
            </div>
            <div v-else class="text-center py-8 text-gray-500">
              No upcoming bookings.
            </div>
          </div>

          <div v-else-if="item.key === 'pending'">
            <div v-if="formattedPendingBookings.length > 0">
              <UTable :rows="formattedPendingBookings" :columns="columns" class="shadow-lg rounded-lg overflow-hidden">
                <template #actions-data="{ row }">
                  <UButton @click="openCancelModal(row.bookingId)" color="red" variant="solid" label="Cancel" />
                </template>
              </UTable>
            </div>
            <div v-else class="text-center py-8 text-gray-500">
              No pending bookings.
            </div>
          </div>

          <div v-else-if="item.key === 'past'">
            <UTable :rows="formattedPastBookings" :columns="columns.filter(c => c.key !== 'actions')" />
          </div>
        </template>
      </UTabs>
    </div>

    <!-- Credits Section (20%) -->
    <div class="bg-blue-100 dark:bg-blue-900 text-center rounded-xl shadow-md p-6 text-gray-900 dark:text-white h-fit">
      <h2 class="text-xl font-semibold mb-2">Available Credits</h2>
      <p class="text-3xl font-bold">{{ formattedCredits }}</p>
    </div>
  </div>

  <ConfirmationModal ref="confirmModal" />
</template>
