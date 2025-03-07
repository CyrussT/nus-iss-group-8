<script lang="ts" setup>
definePageMeta({
  middleware: ['auth', 'student']
})

import { useBooking } from "@/composables/useBooking";  // Import composable
import { useAuthStore } from "@/composables/authStore";
import axios from "axios";
import { useRouter } from "vue-router";
import { computed, onMounted, ref } from "vue";

const auth = useAuthStore();
const {upcomingApprovedBookings, pendingBookings, pastBookings, fetchAccountId, fetchUpcomingApprovedBookings, fetchPendingBookings, fetchPastBookings } = useBooking();

const authStore = useAuthStore();
const accountId = ref<number | null>(null);

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
  { key: "bookingId", label: "Booking ID", sortable: true }
];

// Format Upcoming bookings
const formattedUpcomingApprovedBookings = computed(() => {
  return upcomingApprovedBookings.value.map(upcomingApprovedBookings => ({
    facilityName: upcomingApprovedBookings.facilityName,
    bookedDatetime: new Date(upcomingApprovedBookings.bookedDatetime).toLocaleDateString(),
    timeslot: upcomingApprovedBookings.timeslot,
    supported: "N/A",
    purpose: "N/A",
    status: upcomingApprovedBookings.status,
    bookingId: upcomingApprovedBookings.bookingId
  }));
});

// Format Pending bookings
const formattedPendingBookings = computed(() => {
  return pendingBookings.value.map(pendingBookings => ({
    facilityName: pendingBookings.facilityName,
    bookedDatetime: new Date(pendingBookings.bookedDatetime).toLocaleDateString(),
    timeslot: pendingBookings.timeslot,
    supported: "N/A",
    purpose: "N/A",
    status: pendingBookings.status,
    bookingId: pendingBookings.bookingId
  }));
});


// Format Past bookings 
const formattedPastBookings = computed(() => {
  return pastBookings.value.map(pastBookings => ({
    facilityName: pastBookings.facilityName,
    bookedDatetime: new Date(pastBookings.bookedDatetime).toLocaleDateString(),  // Format date
    timeslot: pastBookings.timeslot,
    supported: "N/A",  // Placeholder
    purpose: "N/A",  // Placeholder
    status: pastBookings.status,
    bookingId: pastBookings.bookingId
  }));
});

</script>


<template>
    <div class="p-8">
        <h1 class="text-2xl font-bold mb-4">Booking Management</h1>
        <h3 class="text-2xl font-bold mb-4">My Bookings</h3>

        <UTabs 
            :items="items" 
            class="w-full">
            <template #default="{ item, index, selected }">
                <span class="truncate" :class="[selected && 'text-primary-500 dark:text-primary-400']">{{ item.label }}</span>
            </template>
     
        
            <template #item="{ item }">
                <div v-if="item.key === 'upcoming'">
                    <p class="font-bold mb-2">Upcoming Approve Bookings are shown here</p>
                    <UTable 
                        v-if="upcomingApprovedBookings.length > 0"
                        :rows="upcomingApprovedBookings"
                        :columns="columns"
                        class="shadow-lg rounded-lg overflow-hidden"  
                    />

                    <p v-else>No upcoming bookings found.</p>

                </div>

                <div v-else-if="item.key === 'pending'">
                    <p class="font-bold mb-2">Pending Bookings not approve are shown here</p>
                    <UTable 
                      v-if="pendingBookings.length > 0"
                      :rows="pendingBookings"
                      :columns="columns"
                      class="shadow-lg rounded-lg overflow-hidden"
                    />

                    <p v-else>No pending bookings found.</p>
                </div>

                
                <div v-else-if="item.key === 'past'">
                    <p class="font-bold mb-2">Past Bookings</p>

                    <UTable 
                        v-if="pastBookings.length > 0"
                        :rows="pastBookings"
                        :columns="columns"
                        class="shadow-lg rounded-lg overflow-hidden"
                    />

                    <p v-else>No past bookings found.</p>

                </div>
            </template>
        </UTabs>
    </div>
</template>
