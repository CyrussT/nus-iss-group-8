<script lang="ts" setup>
definePageMeta({
  middleware: ['auth', 'admin']
});

import { useRouter } from "vue-router";
import { computed, onMounted, ref } from "vue";
import axios from "axios";


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
    <h1 class="text-2xl font-bold mb-4">Student Management</h1>
    <h3 class="text-2xl font-bold mb-4">Search for Students</h3>

    <div class="grid grid-cols-2 gap-4">
      <!-- Student Name -->
      <div>
        <label class="text-sm font-semibold text-gray-700">Student Name</label>
        <UInput v-model="studentName" placeholder="Enter Student Name" />
      </div>

      <!-- Student ID -->
      <div>
        <label class="text-sm font-semibold text-gray-700">Student ID</label>
        <UInput v-model="studentID" placeholder="Enter Student ID" />
      </div>

      <!-- Enrollment Year -->
      <div>
        <label class="text-sm font-semibold text-gray-700">Enrollment Year</label>
        <USelect v-model="selectedYear" :options="yearEnrolled" placeholder="Select Year" />
      </div>

      <!-- Status -->
      <div>
        <label class="text-sm font-semibold text-gray-700">Status</label>
        <USelect v-model="selectedStatus" :options="status" placeholder="Select Status" />
      </div>
    </div>

    <!-- Added mt-6 mb-6 for spacing -->
    <div class="col-span-2 flex justify-end gap-2 mt-6 mb-6">
      <button @click="fetchStudents"
          class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-700 flex items-center gap-2">
          <UIcon name="i-ic:baseline-search" class="w-5 h-5" />
          Search
      </button>
      <button @click="resetSearch"
          class="bg-gray-400 text-white px-4 py-2 rounded hover:bg-red-500 flex items-center gap-2">
          <UIcon name="i-ic:round-restart-alt" class="w-5 h-5" />
          Reset
      </button>
    </div>
  </div>
</template>
