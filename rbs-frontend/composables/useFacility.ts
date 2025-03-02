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
  const currentPage = ref(1); 
  const pageSize = ref(10);
  const totalPages = ref(1); 
  const totalItems = ref(0);

  const fetchFacilities = async () => {
    try {
      loading.value = true;
      const response = await axios.get("http://localhost:8080/api/facilities/list", {
        params: {
          page: currentPage.value - 1,  // ✅ Convert UI page (1-based) to API (0-based)
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
    totalItems,
    nextPage,
    prevPage,
  };
};
