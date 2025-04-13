import { ref, reactive } from "vue";
import axios from "axios";

// Define types for maintenance status
interface MaintenanceStatusMap {
  [key: string]: boolean;
}

// Define types for maintenance details
interface MaintenanceDetails {
  maintenanceId: number;
  description: string;
  startDate: string;
  endDate: string;
  createdBy: number;
  facilityId: number;
}

export function useMaintenance() {
  const maintenanceLoading = ref<boolean>(false);
  const facilitiesUnderMaintenance = reactive<MaintenanceStatusMap>({});
  
  /**
   * Check if a facility is under maintenance for a specific date
   * @param facilityId The ID of the facility to check
   * @param date The date to check (optional, defaults to current date)
   * @returns A boolean indicating if the facility is under maintenance
   */
  const checkMaintenanceStatus = async (facilityId: string | number, date?: string): Promise<boolean> => {
    if (!facilityId) return false;
    
    try {
      maintenanceLoading.value = true;
      
      // Use the date-specific endpoint if date is provided
      const url = date 
        ? `http://localhost:8080/api/maintenance/check/${facilityId}?date=${date}`
        : `http://localhost:8080/api/maintenance/check/${facilityId}`;
      
      const response = await axios.get<boolean>(url);
      
      // Convert response to boolean
      const isUnderMaintenance: boolean = Boolean(response.data);
      
      // Store the result in our lookup object
      const cacheKey: string = date 
        ? `${facilityId}_${date}` 
        : String(facilityId);
        
      facilitiesUnderMaintenance[cacheKey] = isUnderMaintenance;
      
      return isUnderMaintenance;
    } catch (error) {
      console.error(`Error checking maintenance status for facility ${facilityId}:`, error);
      return false;
    } finally {
      maintenanceLoading.value = false;
    }
  };
  
  /**
   * Check if multiple facilities are under maintenance using a batch API call
   * @param facilityIds Array of facility IDs to check
   * @param date The date to check maintenance for (optional)
   */
  const checkMultipleFacilities = async (facilityIds: (string | number)[], date?: string): Promise<Record<string, boolean> | undefined> => {
    if (!facilityIds || facilityIds.length === 0) return;
    
    try {
      maintenanceLoading.value = true;
      
      // Get unique IDs and convert to numbers
      const uniqueIds: number[] = [...new Set(facilityIds)].map(id => {
        // Convert to number and ensure it's treated as a long
        const numId = typeof id === 'string' ? parseInt(id) : id;
        return numId;
      });
      
      // Prepare request payload with date
      const payload = {
        facilityIds: uniqueIds,
        date: date || ''
      };
      
      // Make a single API call with all facility IDs and date
      const response = await axios.post<Record<string, boolean>>(
        'http://localhost:8080/api/maintenance/check-maintenance-status',
        payload
      );
      
      // Update our lookup object with the batch results
      if (response.data) {
        // If date was provided, store with date-specific keys
        if (date) {
          Object.entries(response.data).forEach(([facilityId, status]) => {
            // Use Boolean() to ensure it's a boolean value
            facilitiesUnderMaintenance[`${facilityId}_${date}`] = Boolean(status);
          });
        } else {
          // Convert all values to boolean for type safety
          Object.entries(response.data).forEach(([key, value]) => {
            facilitiesUnderMaintenance[key] = Boolean(value);
          });
        }
      }
      
      return response.data;
    } catch (error) {
      console.error('Error checking maintenance status for multiple facilities:', error);
      return undefined;
    } finally {
      maintenanceLoading.value = false;
    }
  };
  
  /**
   * Check if a facility is under maintenance (from cached results)
   * @param facilityId The ID of the facility to check
   * @param date The date to check (optional)
   */
  const isUnderMaintenance = (facilityId: string | number, date?: string): boolean => {
    if (!facilityId) return false;
    
    const cacheKey: string = date 
      ? `${facilityId}_${date}` 
      : String(facilityId);
    
    return Boolean(facilitiesUnderMaintenance[cacheKey]) || false;
  };
  
  /**
   * Get current maintenance details for a facility on a specific date
   * @param facilityId The ID of the facility to check
   * @param date The date to check (optional)
   */
  const getMaintenanceDetails = async (facilityId: string | number, date?: string): Promise<MaintenanceDetails | null> => {
    if (!facilityId) return null;
    
    try {
      maintenanceLoading.value = true;
      
      // Use date-specific endpoint if date is provided
      const url = date
        ? `http://localhost:8080/api/maintenance/current/${facilityId}?date=${date}`
        : `http://localhost:8080/api/maintenance/current/${facilityId}`;
        
      const response = await axios.get<MaintenanceDetails>(url);
      return response.data;
    } catch (error) {
      console.error(`Error getting maintenance details for facility ${facilityId}:`, error);
      return null;
    } finally {
      maintenanceLoading.value = false;
    }
  };
  
  return {
    maintenanceLoading,
    facilitiesUnderMaintenance,
    checkMaintenanceStatus,
    checkMultipleFacilities,
    isUnderMaintenance,
    getMaintenanceDetails
  };
}