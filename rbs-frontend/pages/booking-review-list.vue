<script setup lang="ts">
definePageMeta({
    middleware: ['auth', 'admin']
});

import { onMounted } from "vue";
import { bookingRequestManagement } from "@/composables/bookingReqManagement";
import RejectModal from "~/components/booking/RejectModal.vue";
import ConfirmationModal from "~/components/booking/ConfirmationModal.vue";
import axios from "axios";
import { Client } from '@stomp/stompjs';


const rejectModal = ref<InstanceType<typeof RejectModal> | null>(null);
const confirmModal = ref<InstanceType<typeof ConfirmationModal> | null>(null);

const formattedBookings = computed(() => {
    return pendingBookings.value.map(booking => ({
        ...booking,
        bookedDatetime: new Date(booking.bookedDatetime).toLocaleDateString(),
    }));
});

const {
    pendingBookings,
    loading,
    fetchPendingBookings,
    changeSorting
} = bookingRequestManagement();

const connectWebSocket = () => {
    const stompClient = new Client({
        brokerURL: 'ws://localhost:8080/ws',
        reconnectDelay: 5000,
        onConnect: () => {
            console.log('Connected!');
            stompClient.subscribe('/topic/bookings', message => {
                console.log('Received booking update', message.body);
                fetchPendingBookings();
            });
        },
        onDisconnect: () => {
            console.log('Disconnected from WebSocket.');
        },
    });

    stompClient.activate();
};


// Open the modal and pass the dynamic data for the modal
const openRejectModal = (bookingId: number, email: string) => {
    if (rejectModal.value) {
        rejectModal.value.openModal({
            title: "Reject Booking",
            confirmLabel: "Reject",
            cancelLabel: "Cancel",
            onConfirm: (reason: string) => rejectBooking(bookingId, email, reason), // Pass the reason when confirmed
        });
    }
};

const openApproveModal = (bookingId: number, email: string) => {
    if (confirmModal.value) {
        confirmModal.value.openModal({
            title: "Approve Booking",
            confirmLabel: "Confirm",
            cancelLabel: "Cancel",
            onConfirm: () => approveBooking(bookingId, email),
        });
    }
};

const approveBooking = async (bookingId: number, email: string) => {
    console.log(`Rejecting booking ${bookingId}`);
    try {
        const response = await axios.put(
            `http://localhost:8080/api/bookings/update/${bookingId}`,
            null,
            {
                params: {
                    toEmail: email,
                    status: "APPROVED",
                }
            }
        );
        fetchPendingBookings();
        console.log('Booking status updated successfully:', response.data);
    } catch (error) {
        console.error('Error updating booking status:', error);
    }
};

// Handle the rejection logic (e.g., call API to reject the booking)
const rejectBooking = async (bookingId: number, email: string, rejectReason: string) => {
    console.log(`Rejecting booking ${bookingId} with reason: ${rejectReason}`);
    try {
        const response = await axios.put(
            `http://localhost:8080/api/bookings/update/${bookingId}`,
            null, // No body required since we are using query params
            {
                params: {
                    toEmail: email,
                    status: "REJECTED",
                    rejectReason: rejectReason
                }
            }
        );
        fetchPendingBookings();
        console.log('Booking status updated successfully:', response.data);
    } catch (error) {
        console.error('Error updating booking status:', error);
    }
};

onMounted(() => {
    fetchPendingBookings();
    connectWebSocket();
});

</script>

<template>
    <div class="p-8">
        <h1 class="text-2xl font-bold mb-4">Booking Request Management</h1>

        <UCard class="w-full max-w-8xl p-6 shadow-lg bg-white">
            <div>
                <UTable :rows="formattedBookings" :loading="loading" :columns="[
                    { key: 'sn', label: 'S/N', sortable: false },
                    { key: 'facilityName', label: 'Facility', sortable: true },
                    { key: 'location', label: 'Location', sortable: true },
                    { key: 'bookedDatetime', label: 'Booked Date', sortable: true },
                    { key: 'timeslot', label: 'Timeslot', sortable: true },
                    { key: 'status', label: 'Status', sortable: true },
                    { key: 'actions', label: 'Actions', class: 'text-center' }
                ]">
                    <template #sn-data="{ index }">
                        {{ index + 1 }}
                    </template>

                    <template #actions-data="{ row }">
                        <div class="flex justify-center gap-2">
                            <UButton @click="openApproveModal(row.bookingId, row.email)" color="green" variant="solid"
                                icon="i-gridicons-checkmark" label="Approve" />
                            <UButton @click="openRejectModal(row.bookingId, row.email)" color="red" variant="solid"
                                icon="i-fluent-dismiss-20-regular" label="Reject" class="px-3 py-1 gap-2" />
                        </div>
                        <RejectModal ref="rejectModal" />
                        <ConfirmationModal ref="confirmModal" />
                    </template>
                </UTable>
            </div>
        </UCard>
    </div>


</template>
