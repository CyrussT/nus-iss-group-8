// pages/__tests__/booking.spec.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import bookingPage from '../booking.vue';
import { setupNuxtUIStubs } from '~/test/utils';

// Mock the STOMP client to prevent "WebSocket is not a constructor" error
vi.mock('@stomp/stompjs', () => {
  return {
    Client: vi.fn().mockImplementation(() => ({
      activate: vi.fn(),
      deactivate: vi.fn(),
      subscribe: vi.fn(),
      publish: vi.fn()
    }))
  };
}); 

// Mock the booking related composables
vi.mock('#imports', async () => {
  const actual = await vi.importActual('#imports');
  return {
    ...actual as object,
    useToast: () => ({
      add: vi.fn()
    }),
    useBooking: () => ({
      availableCredits: { value: 240 }, // 4 hours in minutes
      fetchAvailableCredits: vi.fn().mockResolvedValue(240),
      searchFacilities: vi.fn().mockResolvedValue([]),
      fetchDropdownOptions: vi.fn().mockResolvedValue({
        resourceTypes: ['Classroom', 'Lab'],
        resourceNames: ['Room 101', 'Room 102'],
        locations: ['Building A', 'Building B']
      }),
      createBooking: vi.fn().mockResolvedValue(true),
      facilities: { value: [] },
      searchLoading: { value: false }
    }),
    useAuthStore: () => ({
      user: { value: { email: 'test@example.com', role: 'STUDENT' } },
      token: { value: 'fake-token' },
      isAuthenticated: { value: true }
    }),
    // Add useColorMode mock here
    useColorMode: () => ({
      preference: 'light',
      value: 'light'
    })
  };
});

// Mock other specific composables needed for this component
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({
    apiUrl: 'https://api.example.com'
  })
}));

vi.mock('~/composables/useMaintenance', () => ({
  useMaintenance: () => ({
    checkMultipleFacilities: vi.fn().mockResolvedValue([]),
    isUnderMaintenance: vi.fn().mockReturnValue(false),
    maintenanceLoading: { value: false },
    facilitiesUnderMaintenance: { value: [] }
  })
}));

// Mock booking components
const mockBookingComponents = {
  BookingCalendar: {
    template: '<div class="booking-calendar" data-testid="booking-calendar"></div>',
    methods: {
      getCurrentCalendarDate: () => '2025-05-17',
      forceRefresh: vi.fn(),
      applyMaintenanceStyling: vi.fn()
    }
  },
  BookingSearchBar: {
    template: '<div class="booking-search-bar" data-testid="booking-search-bar"></div>'
  },
  BookingCreateModal: {
    template: '<div class="booking-create-modal" data-testid="booking-create-modal"></div>'
  },
  BookingDetailsModal: {
    template: '<div class="booking-details-modal" data-testid="booking-details-modal"></div>'
  }
};

describe('booking page', () => {
  let wrapper: any;

  beforeEach(async () => {
    // Reset any component mocks before each test
    vi.clearAllMocks();
    
    // Create the wrapper with necessary stubs
    wrapper = mount(bookingPage, {
      global: {
        components: mockBookingComponents,
        stubs: {
          ...setupNuxtUIStubs(),
          BookingCalendar: mockBookingComponents.BookingCalendar,
          BookingSearchBar: mockBookingComponents.BookingSearchBar,
          BookingCreateModal: mockBookingComponents.BookingCreateModal,
          BookingDetailsModal: mockBookingComponents.BookingDetailsModal
        },
        // Add global mocks for $colorMode
        mocks: {
          $colorMode: {
            value: 'light'
          }
        }
      }
    });

    // Wait for component to finish all async operations
    await flushPromises();
  });

  it('renders correctly', async () => {
    // Check that the page title is rendered
    expect(wrapper.find('h1').text()).toBe('Booking Management');
    
    // Check that the create booking button exists
    expect(wrapper.find('button').text()).toContain('Create New Booking');
  });

  it('displays booking legend correctly', async () => {
    // Check that the legend title is rendered
    expect(wrapper.html()).toContain('Booking Status Legend');
    
    // Check that all legend items are displayed
    const legendItems = [
      'Occupied', 'Pending', 'My Booking (Approved)', 
      'My Booking (Pending)', 'Maintenance'
    ];
    
    legendItems.forEach(item => {
      expect(wrapper.html()).toContain(item);
    });
  });

  it('displays available credits section', async () => {
    // Check that the available credits section is displayed
    expect(wrapper.html()).toContain('Available Credits');
  });
});