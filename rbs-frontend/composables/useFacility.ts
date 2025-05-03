import { ref, onMounted } from "vue";
import axios from "axios";

const { apiUrl } = useApi();

export interface Booking {
  bookingId: number;
  studentId: string;
  studentName: string;
  bookedDatetime: string;
  timeslot: string;
  status: string;
}

export interface Facility {
  facilityId?: number;
  resourceTypeId: number | undefined;
  resourceName: string;
  location: string;
  capacity: number;
  bookings: Booking[];
}

export const useFacility = () => {
  const facility = ref<Facility>({
    resourceTypeId: undefined,
    resourceName: "",
    location: "",
    capacity: 1,
    bookings: [],
  });

  const resetFacility = () => {
    facility.value = {
      resourceTypeId: undefined,
      resourceName: "",
      location: "",
      capacity: 1,
      bookings: [],
    };
  };

  const searchQuery = ref({
    resourceTypeId: undefined,
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

  const resourceTypeOptions = ref<{ id: number; name: string }[]>([]);

  const fetchResourceTypes = async () => {
    try {
      const { data } = await axios.get(`${apiUrl}/api/facility-types/all`);
      if (Array.isArray(data)) {
        resourceTypeOptions.value = data.map(item => ({
          id: item.id,
          name: item.name,
        }));
        console.log("Loaded resource types:", resourceTypeOptions.value);
      } else {
        console.warn("Unexpected data format for resource types:", data);
      }
    } catch (error) {
      console.error("Failed to fetch resource types:", error);
      resourceTypeOptions.value = [];
    }
  };

  const getResourceTypeName = (id: number | string) => {
    const numericId = Number(id);
    const match = resourceTypeOptions.value.find(type => type.id === numericId);
    return match ? match.name : "-";
  };

  const fetchFacilities = async () => {
    try {
      loading.value = true;
      const response = await axios.get(`${apiUrl}/api/facilities/search`, {
        params: {
          ...searchQuery.value,
          page: currentPage.value - 1,
          size: pageSize.value,
        },
      });

      facilities.value = response.data.content.map((f: any) => {
        return {
          ...f,
          resourceTypeName: getResourceTypeName(f.resourceTypeId),
        };
      });

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
      resourceTypeId: undefined,
      resourceName: "",
      location: "",
      capacity: "",
    };
    fetchFacilities();
  };

  const fetchFacilityDetails = async (facilityId: number) => {
    try {
      const response = await axios.get(`${apiUrl}/api/facilities/${facilityId}/details`);
      facility.value = response.data;
    } catch (error) {
      console.error("Error fetching facility details:", error);
    }
  };

  // You can optionally call fetchResourceTypes & fetchFacilities in onMounted if used in setup
  onMounted(async () => {
    await fetchResourceTypes();
    await fetchFacilities();
  });

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
    getResourceTypeName,
  };
};
