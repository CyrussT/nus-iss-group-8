import { describe, it, expect, vi, beforeEach } from 'vitest';
import { shallowMount } from '@vue/test-utils';
import StudentDashboard from '../StudentDashboard.vue';
import { useToast } from '#imports';

// Mock the imports
vi.mock('#imports', async () => {
  const actual = await vi.importActual('#imports');
  return {
    ...actual as object,
    useToast: vi.fn().mockReturnValue({
      add: vi.fn()
    })
  };
});

// Mock the composables
vi.mock('@/composables/useBooking', () => ({
  useBooking: () => ({
    upcomingApprovedBookings: { value: [] },
    pendingBookings: { value: [] },
    pastBookings: { value: [] },
    availableCredits: { value: 120 },
    fetchUpcomingApprovedBookings: vi.fn(),
    fetchPendingBookings: vi.fn(),
    fetchPastBookings: vi.fn(),
    fetchAvailableCredits: vi.fn(),
    fetchAccountId: vi.fn().mockResolvedValue(1)
  })
}));

vi.mock('@/composables/authStore', () => ({
  useAuthStore: () => ({
    user: { value: { email: 'test@example.com', role: 'STUDENT' } }
  })
}));

vi.mock('@/composables/useApi', () => ({
  useApi: () => ({
    apiUrl: 'http://localhost:8080'
  })
}));

// Mock fetch
global.fetch = vi.fn().mockResolvedValue({
  ok: true,
  json: vi.fn().mockResolvedValue({})
});

// Mock stomp client
vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation(() => ({
    activate: vi.fn(),
    subscribe: vi.fn()
  }))
}));

describe('StudentDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });
  
  it('renders correctly with empty bookings', () => {
    const wrapper = shallowMount(StudentDashboard, {
      global: {
        stubs: {
          UTabs: true,
          UTable: true,
          UButton: true,
          ConfirmationModal: true
        }
      }
    });
    
    expect(wrapper.html()).toMatchSnapshot();
  });
  
  it('renders correctly with bookings data', async () => {
    // Mock booking data
    vi.mock('@/composables/useBooking', () => ({
      useBooking: () => ({
        upcomingApprovedBookings: { 
          value: [
            { 
              facilityName: 'Room 101', 
              bookedDatetime: '2025-05-20T14:00:00', 
              timeslot: '2:00 PM - 3:00 PM', 
              status: 'APPROVED', 
              bookingId: 123 
            }
          ] 
        },
        pendingBookings: { 
          value: [
            { 
              facilityName: 'Lab 201', 
              bookedDatetime: '2025-05-22T10:00:00', 
              timeslot: '10:00 AM - 11:00 AM', 
              status: 'PENDING', 
              bookingId: 124 
            }
          ] 
        },
        pastBookings: { 
          value: [
            { 
              facilityName: 'Auditorium', 
              bookedDatetime: '2025-05-10T13:00:00', 
              timeslot: '1:00 PM - 3:00 PM', 
              status: 'COMPLETED', 
              bookingId: 122 
            }
          ] 
        },
        availableCredits: { value: 180 },
        fetchUpcomingApprovedBookings: vi.fn(),
        fetchPendingBookings: vi.fn(),
        fetchPastBookings: vi.fn(),
        fetchAvailableCredits: vi.fn(),
        fetchAccountId: vi.fn().mockResolvedValue(1)
      })
    }));
    
    const wrapper = shallowMount(StudentDashboard, {
      global: {
        stubs: {
          UTabs: true,
          UTable: true,
          UButton: true,
          ConfirmationModal: true
        }
      }
    });
    
    await wrapper.vm.$nextTick();
    expect(wrapper.html()).toMatchSnapshot();
  });
});
