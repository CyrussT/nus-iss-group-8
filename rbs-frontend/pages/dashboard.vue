<script lang="ts" setup>

definePageMeta({
  middleware: ['auth', 'student-or-admin']
})

import { useBooking } from "@/composables/useBooking";  // Import composable
import { useAuthStore } from "@/composables/authStore";
import axios from "axios";
import { computed, onMounted, ref } from "vue";
import ConfirmationModal from "~/components/booking/ConfirmationModal.vue";

const auth = useAuthStore();
const { availableCredits, fetchAvailableCredits, upcomingApprovedBookings, pendingBookings, pastBookings, fetchAccountId, fetchUpcomingApprovedBookings, fetchPendingBookings, fetchPastBookings } = useBooking();
const accountId = ref<number | null>(null);
const confirmModal = ref<InstanceType<typeof ConfirmationModal> | null>(null);

onMounted(async () => {
  if (auth.user.value?.email) {
    accountId.value = await fetchAccountId(auth.user.value.email);
    if (accountId.value) {
      fetchPastBookings(accountId.value);
      fetchPendingBookings(accountId.value);
      fetchUpcomingApprovedBookings(accountId.value);
      fetchAvailableCredits(auth.user.value.email);
    }
  }
});
const currentUserEmail = computed(() => auth.user.value?.email || '');


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

const formattedCredits = computed(() => {
  if (availableCredits.value === null) return 'Loading...';

  const hours = Math.floor(availableCredits.value / 60);
  const minutes = availableCredits.value % 60;

  return `${hours} hour${hours !== 1 ? 's' : ''}${minutes > 0 ? `, ${minutes} minute${minutes !== 1 ? 's' : ''}` : ''}`;
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
    const userEmail: string = auth.user.value?.email ?? '';

    // Make the DELETE request to the backend API to cancel the booking
    const response = await axios.delete(`http://localhost:8080/api/bookings/cancel-booking/${bookingId}?toEmail=${encodeURIComponent(userEmail)}`);

    // Log success message or handle the response accordingly
    console.log('Booking canceled:', bookingId);

    if (accountId.value) {
      fetchUpcomingApprovedBookings(accountId.value);
      fetchPendingBookings(accountId.value);
      fetchPastBookings(accountId.value);
      fetchAvailableCredits(userEmail);
    }
  } catch (error) {
    console.error('Error canceling booking:', error);
    alert('Failed to cancel the booking. Please try again.');
  }
};
</script>


<template>
  <div class="p-8 flex justify-center" v-if="auth.user.value?.role === 'STUDENT'">
    <div class="flex flex-col lg:flex-row w-[90%] mx-auto gap-6">
      <!-- Bookings section (70%) -->
      <div class="w-full lg:w-[80%]">
        <div class="bg-white shadow-xl rounded-2xl p-6 h-full">
          <h1 class="text-2xl font-bold mb-4">My Bookings</h1>

          <!-- Tabs inside the card -->
          <UTabs :items="items" class="w-full">
            <!-- Tab headers -->
            <template #default="{ item, selected }">
              <span class="truncate text-sm font-medium"
                :class="[selected && 'text-primary-600 dark:text-primary-400']">
                {{ item.label }}
              </span>
            </template>

            <!-- Tab content -->
            <template #item="{ item }">
              <div v-if="item.key === 'upcoming'" class="text-sm space-y-2">
                <p class="font-semibold text-gray-700">Upcoming Approved Bookings</p>
                <UTable v-if="upcomingApprovedBookings.length > 0" :rows="formattedUpcomingApprovedBookings"
                  :columns="columns" class="rounded-md shadow-sm text-sm">
                  <template #actions-data="{ row }">
                    <UButton @click="openCancelModal(row.bookingId)" size="sm" color="green" variant="solid"
                      label="Cancel" />
                    <ConfirmationModal ref="confirmModal" />
                  </template>
                </UTable>
                <div v-else class="flex items-center justify-center h-32 text-gray-500 text-center">
                  No upcoming bookings found.
                </div>
              </div>

              <div v-else-if="item.key === 'pending'" class="text-sm space-y-2">
                <p class="font-semibold text-gray-700"> Pending Bookings</p>
                <UTable v-if="pendingBookings.length > 0" :rows="formattedPendingBookings" :columns="columns"
                  class="rounded-md shadow-sm text-sm">
                  <template #actions-data="{ row }">
                    <UButton @click="openCancelModal(row.bookingId)" size="sm" color="green" variant="solid"
                      label="Cancel" />
                    <ConfirmationModal ref="confirmModal" />
                  </template>
                </UTable>
                <p v-else class="text-gray-500">No pending bookings found.</p>
              </div>

              <div v-else-if="item.key === 'past'" class="text-sm space-y-2">
                <p class="font-semibold text-gray-700"> Past Bookings</p>
                <UTable v-if="pastBookings.length > 0" :rows="formattedPastBookings"
                  :columns="columns.filter(col => col.key !== 'actions')" class="rounded-md shadow-sm text-sm" />
                <p v-else class="text-gray-500">No past bookings found.</p>
              </div>
            </template>
          </UTabs>
        </div>
      </div>

      <div class="w-full lg:w-[20%]">
        <div class="bg-blue-50 border border-blue-300 rounded-xl shadow p-6 min-h-[100px]">
          <h2 class="text-lg font-semibold text-blue-700">Available Credits</h2>
          <p class="text-4xl font-bold text-blue-900 mt-4">
            {{ formattedCredits }}
          </p>
        </div>
      </div>
    </div>
  </div>
  <div v-else-if="auth.user.value?.role === 'ADMINISTRATOR'">
    <p>admin dashboard</p>
  </div>
</template>
