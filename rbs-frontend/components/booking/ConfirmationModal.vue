<template>
    <UModal v-model="isModalOpen" @close="closeModal">
        <UCard class="p-6">
            <h2 class="text-xl font-semibold text-center mb-4">{{ title }}</h2>
            <div class="flex justify-center gap-4">
                <UButton @click="handleConfirm" color="red" variant="solid" :label="confirmLabel" />
                <UButton @click="closeModal" color="gray" variant="solid" :label="cancelLabel" />
            </div>
        </UCard>
    </UModal>
</template>

<script setup>
import { ref } from "vue";

const isModalOpen = ref(false);
const title = ref(""); // Modal title
const confirmLabel = ref(""); // Label for confirm button
const cancelLabel = ref(""); // Label for cancel button
const methodToCall = ref(null); // Method to call upon confirmation

// Expose `openModal` method to parent
defineExpose({
    openModal({ title: modalTitle, confirmLabel: buttonConfirmLabel, cancelLabel: buttonCancelLabel, onConfirm }) {
        title.value = modalTitle;
        confirmLabel.value = buttonConfirmLabel;
        cancelLabel.value = buttonCancelLabel;
        methodToCall.value = onConfirm;
        isModalOpen.value = true;
    }
});

// Close the modal
const closeModal = () => {
    isModalOpen.value = false;
};

// Call the passed method when confirmed
const handleConfirm = () => {
    if (methodToCall.value) {
        methodToCall.value(); // Execute the passed method
        closeModal(); // Close the modal after confirmation
    }
};
</script>
