import { ref } from "vue";
import axios from "axios";

export interface Facility {
  facilityId?: number; // ✅ Make id optional
  resourceType: string;
  resourceName: string;
  location: string;
  capacity: number;
}

export const useFacility = () => {
  const facility = ref<Facility>({
    resourceType: "",
    resourceName: "",
    location: "",
    capacity: 1,
  });


  const resetFacility = () => {
    facility.value = {
      resourceType: "",
      resourceName: "",
      location: "",
      capacity: 1,
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
  const currentPage = ref(0);
  const pageSize = ref(10);
  const totalPages = ref(1); // Track total pages from backend

  const fetchFacilities = async () => {
    try {
      loading.value = true;
      const response = await axios.get("http://localhost:8080/api/facilities/search", {
        params: {
          ...searchQuery.value, // Spread search parameters
          page: currentPage.value,
          size: pageSize.value,
        },
      });
  
      facilities.value = response.data.content; // Extract paginated content
      totalPages.value = response.data.totalPages; // Get total pages
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

  const nextPage = () => {
    if (currentPage.value < totalPages.value - 1) {
      currentPage.value++;
      fetchFacilities();
    }
  };

  const prevPage = () => {
    if (currentPage.value > 0) {
      currentPage.value--;
      fetchFacilities();
    }
  };


  return {
    facility,
    resetFacility,
    searchQuery,
    fetchFacilities,
    resetSearch,
    facilities,
    loading,
    currentPage,
    pageSize,
    totalPages,
    nextPage,
    prevPage,
  };
};
