<script lang="ts" setup>
definePageMeta({
  middleware: ['auth', 'student']
})

import { useBooking } from "@/composables/useBooking";  // Import composable
import { useAuthStore } from "@/composables/authStore";
import axios from "axios";
import { computed, onMounted, ref } from "vue";
import ConfirmationModal from "~/components/booking/ConfirmationModal.vue";

const auth = useAuthStore();
const { upcomingApprovedBookings, pendingBookings, pastBookings, fetchAccountId, fetchUpcomingApprovedBookings, fetchPendingBookings, fetchPastBookings } = useBooking();

const authStore = useAuthStore();
const accountId = ref<number | null>(null);
const confirmModal = ref<InstanceType<typeof ConfirmationModal> | null>(null);

onMounted(async () => {
  if (auth.user.value?.email) {
    accountId.value = await fetchAccountId(auth.user.value.email);
    if (accountId.value) {
      fetchPastBookings(accountId.value);
      fetchPendingBookings(accountId.value);
      fetchUpcomingApprovedBookings(accountId.value);
    }
  }
});

const items = [{
  key: 'upcoming',
  label: 'Upcoming',
  icon: 'material-symbols:event-upcoming',
  description: 'Upcoming Bookings are shown here'
}, {
  key: 'pending',
  label: 'Pending',
  icon: 'material-symbols:calendar-clock',
  description: 'Bookings not approved shown here'
}, {
  key: 'past',
  label: 'Past',
  icon: 'material-symbols:event-repeat',
  description: 'Past bookings are shown here'
}]

const columns = [
  { key: "facilityName", label: "Resource Name", sortable: true },
  { key: "bookedDatetime", label: "Date", sortable: true },
  { key: "timeslot", label: "Time", sortable: true },
  { key: "supported", label: "Supporter", sortable: false },
  { key: "purpose", label: "Purpose", sortable: false },
  { key: "status", label: "Status", sortable: true },
  { key: "bookingId", label: "Booking ID", sortable: true },
  { key: 'actions', label: 'Actions' }
];

const formatBookingData = (bookings: any[]) => {
  return bookings.map(booking => ({
    facilityName: booking.facilityName,
    bookedDatetime: new Date(booking.bookedDatetime).toLocaleDateString(),
    timeslot: booking.timeslot,
    supported: "N/A", // Placeholder — can be enhanced later
    purpose: "N/A",   // Placeholder — can be enhanced later
    status: booking.status,
    bookingId: booking.bookingId,
  }));
};

const formattedUpcomingApprovedBookings = computed(() => {
  return formatBookingData(upcomingApprovedBookings.value);
});

const formattedPendingBookings = computed(() => {
  return formatBookingData(pendingBookings.value);
});

const formattedPastBookings = computed(() => {
  return formatBookingData(pastBookings.value);
});

// Open the modal and pass the dynamic data for the modal
const openCancelModal = (bookingId: number) => {
  if (confirmModal.value) {
    confirmModal.value.openModal({
      title: "Cancel Booking",
      confirmLabel: "Yes, Cancel",
      cancelLabel: "No, Keep it",
      onConfirm: () => cancelBooking(bookingId), // Pass the method to be executed
    });
  }
};

const cancelBooking = async (bookingId: number) => {
  try {
    const userEmail: string = authStore.user.value?.email ?? '';

    // Make the DELETE request to the backend API to cancel the booking
    const response = await axios.delete(`http://localhost:8080/api/bookings/cancel-booking/${bookingId}?toEmail=${encodeURIComponent(userEmail)}`);

    // Log success message or handle the response accordingly
    console.log('Booking canceled:', bookingId);

    if (accountId.value) {
      fetchUpcomingApprovedBookings(accountId.value);
      fetchPendingBookings(accountId.value);
      fetchPastBookings(accountId.value);
    }
  } catch (error) {
    console.error('Error canceling booking:', error);
    alert('Failed to cancel the booking. Please try again.');
  }
};
</script>


<template>
  <div class="p-8">
    <h1 class="text-2xl font-bold mb-4">Booking Management</h1>
    <h3 class="text-2xl font-bold mb-4">My Bookings</h3>

    <UTabs :items="items" class="w-full">
      <template #default="{ item, index, selected }">
        <span class="truncate" :class="[selected && 'text-primary-500 dark:text-primary-400']">{{ item.label }}</span>
      </template>

      <template #item="{ item }">
        <div v-if="item.key === 'upcoming'">
          <p class="font-bold mb-2">Upcoming Approved Bookings are shown here</p>
          <UTable v-if="upcomingApprovedBookings.length > 0" :rows="formattedUpcomingApprovedBookings"
            :columns="columns" class="shadow-lg rounded-lg overflow-hidden">
            <template #actions-data="{ row }">
              <UButton @click="openCancelModal(row.bookingId)" color="green" variant="solid" label="Cancel" />
              <ConfirmationModal ref="confirmModal" />
            </template>
          </UTable>


          <p v-else>No upcoming bookings found.</p>

        </div>

        <div v-else-if="item.key === 'pending'">
          <p class="font-bold mb-2">Pending Bookings are shown here</p>
          <UTable v-if="pendingBookings.length > 0" :rows="formattedPendingBookings" :columns="columns"
            class="shadow-lg rounded-lg overflow-hidden">
            <template #actions-data="{ row }">
              <UButton @click="openCancelModal(row.bookingId)" color="green" variant="solid" label="Cancel" />
              <ConfirmationModal ref="confirmModal" />
            </template>
          </UTable>

          <p v-else>No pending bookings found.</p>
        </div>


        <div v-else-if="item.key === 'past'">
          <p class="font-bold mb-2">Past Bookings</p>

          <UTable v-if="pastBookings.length > 0" :rows="formattedPastBookings"
            :columns="item.key === 'past' ? columns.filter(col => col.key !== 'actions') : columns"
            class="shadow-lg rounded-lg overflow-hidden" />

          <p v-else>No past bookings found.</p>

        </div>
      </template>
    </UTabs>
  </div>
</template>
