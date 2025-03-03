<script lang="ts" setup>
definePageMeta({
  middleware: ['auth', 'student']
})

import { useRouter } from "vue-router";
import { computed, onMounted, ref } from "vue";
import axios from "axios";


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

// change and add backend later
const yearEnrolled = ['All', '2020', '2021', '2022', '2023', '2024', '2025'];
const selectedYear = ref(yearEnrolled[0]);

const status = ['All','Active', 'Inactive'];
const selectedStatus  = ref(status[0]);


const studentName = ref("");
const studentID = ref("");

// Fetch students (Backend will be implemented later)
const fetchStudents = () => {
  console.log("Fetching students with filters:", {
    name: studentName.value,
    studentID: studentID.value,
    year: selectedYear.value,
    status: selectedStatus.value
  });
  // Placeholder for API call
  // axios.get('/api/students', { params: { name, studentID, year, status } })
  //   .then(response => { console.log(response.data); })
  //   .catch(error => { console.error(error); });
};

// Reset search filters
const resetSearch = () => {
  studentName.value = "";
  studentID.value = "";
  selectedYear.value = yearEnrolled[0];  // Reset to 'All'
  selectedStatus.value = status[0];  // Reset to 'All'
};
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
                    <p>Upcoming Approve Bookings are shown here</p>
                </div>
                <div v-else-if="item.key === 'pending'">
                    <p>Pending Bookings not approve are shown here</p>
                </div>
                <div v-else-if="item.key === 'past'">
                    <p>Past Bookings</p>
                </div>
            </template>
        </UTabs>
    </div>
</template>
