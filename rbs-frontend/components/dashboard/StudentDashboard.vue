<script setup lang="ts">
import { ref, computed } from "vue";
import { useBooking } from "@/composables/useBooking";
import { useAuthStore } from "@/composables/authStore";
import ConfirmationModal from "@/components/booking/ConfirmationModal.vue";

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
    await fetch(`http://localhost:8080/api/bookings/cancel-booking/${bookingId}?toEmail=${encodeURIComponent(email)}`, { method: 'DELETE' });
    if (accountId.value) {
      fetchUpcomingApprovedBookings(accountId.value);
      fetchPendingBookings(accountId.value);
      fetchPastBookings(accountId.value);
      fetchAvailableCredits(email);
    }
  } catch (e) {
    console.error('Error cancelling booking', e);
  }
};
</script>

<template>
  <div class="p-8 flex flex-col gap-6">
    <div class="bg-white rounded-xl shadow-md p-6">
      <h1 class="text-2xl font-bold mb-4">My Bookings</h1>
      <UTabs :items="items">
        <template #item="{ item }">
          <div v-if="item.key === 'upcoming'">
            <UTable :rows="formattedUpcomingBookings" :columns="columns" />
          </div>
          <div v-else-if="item.key === 'pending'">
            <UTable :rows="formattedPendingBookings" :columns="columns" />
          </div>
          <div v-else-if="item.key === 'past'">
            <UTable :rows="formattedPastBookings" :columns="columns.filter(c => c.key !== 'actions')" />
          </div>
        </template>
      </UTabs>
    </div>
    <div class="bg-blue-100 rounded-xl shadow-md p-6 text-center">
      <h2 class="text-xl font-semibold mb-2">Available Credits</h2>
      <p class="text-3xl font-bold">{{ formattedCredits }}</p>
    </div>
  </div>
</template>
