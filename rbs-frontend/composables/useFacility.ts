import { ref } from "vue";
import axios from "axios";

export interface Booking {
  bookingId: number;
  studentId: string;
  studentName: string;
  bookedDatetime: string;
  timeslot: string;
  status: string;
}

export interface Facility {
  facilityId?: number; // ✅ Make id optional
  resourceType: string;
  resourceName: string;
  location: string;
  capacity: number;
  bookings: Booking[]; // Include bookings
}

export const useFacility = () => {
  const facility = ref<Facility>({
    resourceType: "",
    resourceName: "",
    location: "",
    capacity: 1,
    bookings: [],
  });


  const resetFacility = () => {
    facility.value = {
      resourceType: "",
      resourceName: "",
      location: "",
      capacity: 1,
      bookings: [],
    };
  };

  const searchQuery = ref({
    resourceType: "",
    resourceName: "",
    location: "",
    capacity: "",
  });
  const facilities = ref([]);
  const loading = ref(true);
  const currentPage = ref(1);
  const pageSize = ref(10);
  const totalPages = ref(1);
  const totalItems = ref(0);

  const fetchFacilities = async () => {
    try {
      loading.value = true;
      const response = await axios.get("http://localhost:8080/api/facilities/search", {
        params: {
          ...searchQuery.value,
          page: currentPage.value - 1,
          size: pageSize.value,
        },
      });

      facilities.value = response.data.content;
      totalPages.value = response.data.totalPages;
      totalItems.value = response.data.totalElements;
    } catch (error) {
      console.error("Error fetching facilities:", error);
    } finally {
      loading.value = false;
    }
  };





  const resetSearch = () => {
    searchQuery.value = {
      resourceType: "",
      resourceName: "",
      location: "",
      capacity: "",
    };
    fetchFacilities();
  };


  const fetchFacilityDetails = async (facilityId: number) => {
    try {
      // const response = await axios.get(`http://localhost:8080/api/facilities/details/${facilityId}`);
      const response = await axios.get(`http://localhost:8080/api/facilities/${facilityId}/details`);
      facility.value = response.data;
    } catch (error) {
      console.error("Error fetching facility details:", error);
    }
  };


  const resourceTypeOptions = ref([])

  const fetchResourceTypes = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/facility-types/all');
      resourceTypeOptions.value = response.data;

      console.log(resourceTypeOptions.value); // ✅
    } catch (error) {
      console.error('Failed to fetch resource types:', error);
    }
  };


  return {
    facility,
    resetFacility,
    searchQuery,
    fetchFacilities,
    resetSearch,
    fetchFacilityDetails,
    facilities,
    loading,
    currentPage,
    pageSize,
    totalItems,
    fetchResourceTypes,
    resourceTypeOptions,
  };
};
