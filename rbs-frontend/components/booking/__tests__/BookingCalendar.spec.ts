import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import BookingCalendar from '../BookingCalendar.vue';
import { setupNuxtMocks, setupNuxtUIStubs } from '~/test/utils';

// Store original Date constructor
const OriginalDate = global.Date;

// Define DateArgument type to match possible Date constructor arguments
type DateArgument = number | string | Date | undefined;

// Define CalendarEventData interface for proper typing
interface CalendarEventData {
  id: string;
  resourceId?: string;
  title?: string;
  start?: string | Date;
  end?: string | Date;
  backgroundColor?: string;
  borderColor?: string;
  textColor?: string;
  editable?: boolean;
  durationEditable?: boolean;
  startEditable?: boolean;
  display?: string;
  className?: string;
  extendedProps?: {
    status?: string;
    legendStatus?: string;
    description?: string;
    isPast?: boolean;
    isMaintenance?: boolean;
    [key: string]: any;
  };
  [key: string]: any;
}

// Define a consistent mock API to use throughout tests
const createMockCalendarApi = () => {
  const mockEl = document.createElement('div');
  
  // Create a mock prev button
  const mockPrevButton = document.createElement('button');
  mockPrevButton.className = 'fc-prev-button';
  mockEl.appendChild(mockPrevButton);
  
  return {
    el: mockEl,
    getDate: vi.fn(() => new OriginalDate('2025-05-17T12:00:00Z')),
    today: vi.fn(),
    prev: vi.fn(),
    next: vi.fn(),
    getEvents: vi.fn(() => []),
    addEvent: vi.fn(),
    removeAllEvents: vi.fn(),
    render: vi.fn(),
    refetchResources: vi.fn(),
    setOption: vi.fn(),
    // Ensure querySelector always returns a valid element to avoid null issues
    querySelector: (selector: string) => {
      if (selector === '.fc-prev-button') {
        return mockPrevButton;
      }
      // Return a generic element for other selectors to avoid null errors
      return document.createElement('div');
    }
  };
};

// Mock FullCalendar and its plugins with proper default exports
vi.mock('@fullcalendar/vue3', () => {
  return {
    default: {
      name: 'FullCalendar',
      props: ['options'],
      template: '<div class="mock-calendar"><slot></slot></div>',
      methods: {
        getApi: () => createMockCalendarApi()
      }
    }
  };
});

vi.mock('@fullcalendar/resource-timeline', () => ({
  default: {} // Mock default export
}));

vi.mock('@fullcalendar/interaction', () => ({
  default: {} // Mock default export
}));

// Create mock functions with consistent reference for composables
const mockMaintenanceValues = {
  isUnderMaintenance: vi.fn().mockReturnValue(false),
  facilitiesUnderMaintenance: {},
  checkMaintenanceStatus: vi.fn()
};

// Mock composables consistently
vi.mock('~/composables/useMaintenance', () => ({
  useMaintenance: () => mockMaintenanceValues
}));

// Mock for colorMode
const mockColorMode = {
  preference: 'light',
  value: 'light'
};

vi.mock('#imports', async () => {
  const actual = await vi.importActual('#imports');
  return {
    ...actual as object,
    useColorMode: () => mockColorMode,
    useRouter: () => ({
      push: vi.fn(),
      replace: vi.fn()
    }),
    useRoute: () => ({
      params: {},
      query: {},
      path: '/'
    }),
    useNuxtApp: () => ({
      $router: {
        push: vi.fn(),
        replace: vi.fn()
      }
    }),
    navigateTo: vi.fn(),
    definePageMeta: vi.fn()
  };
});

describe('BookingCalendar', () => {
  // Setup mocks before each test
  beforeEach(() => {
    setupNuxtMocks();
    
    // Reset the mock values to default 
    mockMaintenanceValues.isUnderMaintenance = vi.fn().mockReturnValue(false);
    mockMaintenanceValues.facilitiesUnderMaintenance = {};
    
    // Reset colorMode to default
    mockColorMode.preference = 'light';
    mockColorMode.value = 'light';
    
    // Fix Date mock to avoid infinite recursion by using OriginalDate
    vi.spyOn(global, 'Date').mockImplementation(function(this: unknown, arg?: DateArgument): Date {
      if (this instanceof global.Date) {
        return arg === undefined 
          ? new OriginalDate('2025-05-17T12:00:00Z') 
          : new OriginalDate(arg);
      }
      return arg === undefined 
        ? new OriginalDate('2025-05-17T12:00:00Z')
        : new OriginalDate(arg);
    } as any);

    // Silence console.error for cleaner test output
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders correctly', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
          // Don't stub FullCalendar
        }
      }
    });
    
    await flushPromises();
    expect(wrapper.find('.calendar-wrapper').exists()).toBe(true);
    expect(wrapper.find('.mock-calendar').exists()).toBe(true); // Use the class from our mock
  });

  it('shows loading state when loading prop is true', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: true
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    expect(wrapper.find('.calendar-wrapper').exists()).toBe(false);
    expect(wrapper.text()).toContain('Loading resources...');
  });

  it('emits date-change event when calendar date changes', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Call the datesSet handler manually
    const dateInfo = {
      start: new OriginalDate('2025-05-20'),
      end: new OriginalDate('2025-05-21')
    };
    
    wrapper.vm.calendarOptions.datesSet(dateInfo);
    
    expect(wrapper.emitted('date-change')).toBeTruthy();
    expect(wrapper.emitted('date-change')[0]).toEqual([dateInfo]);
  });

  it('transforms facilities into calendar resources correctly', async () => {
    const facilities = [
      {
        facilityId: 1,
        resourceName: 'Test Room',
        location: 'Building-A',
        capacity: 10
      },
      {
        facilityId: 2,
        resourceName: 'Conference Hall',
        location: 'Building-B',
        capacity: 50
      }
    ];
    
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities,
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Test that the computed resources property transforms facilities correctly
    const resources = wrapper.vm.resources;
    expect(resources.length).toBe(2);
    expect(resources[0].id).toBe('1');
    expect(resources[0].building).toBe('Building');
    expect(resources[0].title).toBe('Test Room');
    expect(resources[1].id).toBe('2');
    expect(resources[1].building).toBe('Building');
    expect(resources[1].title).toBe('Conference Hall');
  });

  it('emits select-timeslot event when a time slot is selected', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Mock selection info
    const selectInfo = {
      start: new OriginalDate('2025-05-20T10:00:00'),
      end: new OriginalDate('2025-05-20T10:30:00'),
      startStr: '2025-05-20T10:00:00',
      endStr: '2025-05-20T10:30:00',
      resource: {
        id: '1',
        title: 'Test Room'
      }
    };
    
    // Call the select handler manually
    wrapper.vm.calendarOptions.select(selectInfo);
    
    expect(wrapper.emitted('select-timeslot')).toBeTruthy();
    expect(wrapper.emitted('select-timeslot')[0]).toEqual([selectInfo]);
  });

  it('prevents selecting time slots in the past', async () => {
    // Set up a spy on window.alert
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Mock selection info with a past date
    const pastSelectInfo = {
      start: new OriginalDate('2024-05-20T10:00:00'), // Past date
      end: new OriginalDate('2024-05-20T10:30:00'),
      startStr: '2024-05-20T10:00:00',
      endStr: '2024-05-20T10:30:00',
      resource: {
        id: '1',
        title: 'Test Room'
      }
    };
    
    // Call the select handler manually
    wrapper.vm.calendarOptions.select(pastSelectInfo);
    
    // Should not emit select-timeslot event
    expect(wrapper.emitted('select-timeslot')).toBeFalsy();
    
    // Should show an alert
    expect(alertSpy).toHaveBeenCalledWith('Cannot create bookings in the past');
    
    // Clean up
    alertSpy.mockRestore();
  });

  it('prevents selecting time slots for resources under maintenance', async () => {
    // Update the mock implementation for this specific test
    mockMaintenanceValues.facilitiesUnderMaintenance = { '1': true };
    
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    // Create a spy for isResourceUnderMaintenance method
    const isResourceSpy = vi.spyOn(wrapper.vm, 'isResourceUnderMaintenance');
    isResourceSpy.mockImplementation((resourceId: string) => {
      return resourceId === '1';
    });
    
    await flushPromises();
    
    // Mock selection info
    const selectInfo = {
      start: new OriginalDate('2025-05-20T10:00:00'),
      end: new OriginalDate('2025-05-20T10:30:00'),
      startStr: '2025-05-20T10:00:00',
      endStr: '2025-05-20T10:30:00',
      resource: {
        id: '1', // Under maintenance
        title: 'Test Room'
      }
    };
    
    // Call the select handler manually
    wrapper.vm.calendarOptions.select(selectInfo);
    
    // Should not emit select-timeslot event
    expect(wrapper.emitted('select-timeslot')).toBeFalsy();
    
    // Should show an alert
    expect(alertSpy).toHaveBeenCalledWith(
      'This facility is currently under maintenance and cannot be booked.'
    );
    
    // Clean up
    alertSpy.mockRestore();
  });

  it('emits click-event when an event is clicked', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Mock event info
    const mockEvent = {
      id: '123',
      title: 'Test Booking',
      start: new OriginalDate('2025-05-20T10:00:00'),
      end: new OriginalDate('2025-05-20T10:30:00'),
      extendedProps: { 
        status: 'APPROVED'
      }
    };
    
    // Call the eventClick handler manually
    wrapper.vm.calendarOptions.eventClick({ event: mockEvent });
    
    expect(wrapper.emitted('click-event')).toBeTruthy();
    expect(wrapper.emitted('click-event')[0]).toEqual([mockEvent]);
  });

  it('prevents selecting time slots that end after 7 PM', async () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Mock selection info with time that would end after 7 PM
    const lateSelectInfo = {
      start: new OriginalDate('2025-05-20T19:00:00'), // 7 PM
      end: new OriginalDate('2025-05-20T19:30:00'),   // 7:30 PM
      startStr: '2025-05-20T19:00:00',
      endStr: '2025-05-20T19:30:00',
      resource: {
        id: '1',
        title: 'Test Room'
      }
    };
    
    // Call the wouldEndAfter7PM method directly to test it
    const wouldEnd = wrapper.vm.wouldEndAfter7PM(lateSelectInfo.start, 30 * 60000);
    expect(wouldEnd).toBe(true);
    
    // Call the select handler manually
    wrapper.vm.calendarOptions.select(lateSelectInfo);
    
    // Should not emit select-timeslot event
    expect(wrapper.emitted('select-timeslot')).toBeFalsy();
    
    // Should show an alert
    expect(alertSpy).toHaveBeenCalledWith('Bookings cannot extend beyond 7:00 PM');
    
    // Clean up
    alertSpy.mockRestore();
  });

  it('formats date correctly for API', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    // Test formatDateForApi method with OriginalDate to avoid mock issues
    const testDate = new OriginalDate('2025-05-20T12:34:56');
    const formattedDate = wrapper.vm.formatDateForApi(testDate);
    expect(formattedDate).toBe('2025-05-20');
  });

  // Test isInPast method directly
  it('correctly identifies past dates using isInPast', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Test with date in past year
    const pastYear = new OriginalDate('2024-05-17T12:00:00Z'); 
    expect(wrapper.vm.isInPast(pastYear)).toBe(true);
    
    // Test with date in past month
    const pastMonth = new OriginalDate('2025-04-17T12:00:00Z');
    expect(wrapper.vm.isInPast(pastMonth)).toBe(true);
    
    // Test with date in past day
    const pastDay = new OriginalDate('2025-05-16T12:00:00Z');
    expect(wrapper.vm.isInPast(pastDay)).toBe(true);
    
    // Test with date in future
    const future = new OriginalDate('2025-05-18T12:00:00Z');
    expect(wrapper.vm.isInPast(future)).toBe(false);
    
    // Test with today but earlier hours
    const todayEarlier = new OriginalDate('2025-05-17T10:00:00Z');
    expect(wrapper.vm.isInPast(todayEarlier)).toBe(true);
    
    // Test with today but later hours
    const todayLater = new OriginalDate('2025-05-17T14:00:00Z');
    expect(wrapper.vm.isInPast(todayLater)).toBe(false);
    
    // Test with null input
    expect(wrapper.vm.isInPast(null)).toBe(false);
  });



  // Test getNextBusinessDay method
  it('correctly gets the next business day', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Using our fixed date (May 17, 2025), which is a Saturday
    const nextDay = wrapper.vm.getNextBusinessDay();
    
    // Should be May 18, 2025
    expect(nextDay.getDate()).toBe(18);
    expect(nextDay.getMonth()).toBe(4); // May is month 4 (0-indexed)
    expect(nextDay.getFullYear()).toBe(2025);
  });

  // Test isCellBooked method
  it('correctly identifies if a cell is already booked', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Create a mock selection that doesn't overlap with any events
    const selectInfo = {
      start: new OriginalDate('2025-05-20T10:00:00'),
      end: new OriginalDate('2025-05-20T10:30:00'),
      resource: { id: '1' }
    };
    
    // Set up mock calendar with no events
    const mockApi = createMockCalendarApi();
    mockApi.getEvents = vi.fn().mockReturnValue([]);
    
    wrapper.vm.calendarRef = { getApi: () => mockApi };
    
    // Should return false (cell not booked)
    expect(wrapper.vm.isCellBooked(selectInfo)).toBe(false);
    
    // Now create a mock event that overlaps with the selection
    const overlappingEvents = [
      {
        start: new OriginalDate('2025-05-20T10:15:00'),
        end: new OriginalDate('2025-05-20T10:45:00'),
        getResources: () => [{ id: '1' }]
      }
    ];
    
    // Update the mock API to return our overlapping event
    mockApi.getEvents = vi.fn().mockReturnValue(overlappingEvents);
    
    // Should return true (cell is booked)
    expect(wrapper.vm.isCellBooked(selectInfo)).toBe(true);
    
    // Test with event in different resource
    const differentResourceEvents = [
      {
        start: new OriginalDate('2025-05-20T10:15:00'),
        end: new OriginalDate('2025-05-20T10:45:00'),
        getResources: () => [{ id: '2' }] // Different resource
      }
    ];
    
    mockApi.getEvents = vi.fn().mockReturnValue(differentResourceEvents);
    
    // Should return false (cell not booked in this resource)
    expect(wrapper.vm.isCellBooked(selectInfo)).toBe(false);
  });

  // Test getCurrentCalendarDate method
  it('correctly gets the current calendar date', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Test when calendarRef is null
    wrapper.vm.calendarRef = null;
    expect(wrapper.vm.getCurrentCalendarDate()).toBe('2025-05-17');
    
    // Test with valid calendarRef
    const mockDate = new OriginalDate('2025-06-01');
    const mockApi = createMockCalendarApi();
    mockApi.getDate = vi.fn().mockReturnValue(mockDate);
    
    wrapper.vm.calendarRef = { getApi: () => mockApi };
    
    expect(wrapper.vm.getCurrentCalendarDate()).toBe('2025-06-01');
    
    // Test error handling
    const errorApi = {
      getDate: vi.fn().mockImplementation(() => { throw new Error('Test error'); })
    };
    
    wrapper.vm.calendarRef = { getApi: () => errorApi };
    
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(wrapper.vm.getCurrentCalendarDate()).toBe('2025-05-17'); // Should fall back to today
    expect(consoleSpy).toHaveBeenCalled();
  });

  // Test the datesSet handler's behavior when selecting a date before today
  it('resets to today when attempting to navigate to a date before today', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Create mock calendar API with today method
    const mockApi = createMockCalendarApi();
    
    wrapper.vm.calendarRef = { getApi: () => mockApi };
    
    // Create dateInfo with a past date
    const dateInfo = {
      start: new OriginalDate('2024-05-20'), // Past date
      end: new OriginalDate('2024-05-21')
    };
    
    // Call datesSet handler
    wrapper.vm.calendarOptions.datesSet(dateInfo);
    
    // Should have called today to reset the date
    expect(mockApi.today).toHaveBeenCalled();
    
    // Should not have emitted date-change
    expect(wrapper.emitted('date-change')).toBeFalsy();
  });

  // Test the eventDidMount handler
  it('applies correct styling in eventDidMount handler', async () => {
    const wrapper = mount(BookingCalendar, {
      props: {
        facilities: [],
        bookings: [],
        loading: false
      },
      global: {
        stubs: {
          ...setupNuxtUIStubs(),
        }
      }
    });
    
    await flushPromises();
    
    // Test past event styling
    const pastEventEl = document.createElement('div');
    const pastEventInfo = {
      event: {
        extendedProps: { isPast: true },
        end: new OriginalDate('2024-05-20')
      },
      el: pastEventEl
    };
    
    // Call eventDidMount handler
    wrapper.vm.calendarOptions.eventDidMount(pastEventInfo);
    
    // Check that past-event class was added
    expect(pastEventEl.classList.contains('past-event')).toBe(true);
    expect(pastEventEl.style.opacity).toBe('0.6');
    
    // Test maintenance event styling
    const maintenanceEventEl = document.createElement('div');
    const eventTitle = document.createElement('div');
    eventTitle.className = 'fc-event-title';
    maintenanceEventEl.appendChild(eventTitle);
    
    const maintenanceEventInfo = {
      event: {
        extendedProps: { isMaintenance: true },
        end: new OriginalDate('2025-05-20')
      },
      el: maintenanceEventEl
    };
    
    // Call eventDidMount handler
    wrapper.vm.calendarOptions.eventDidMount(maintenanceEventInfo);
    
    // Check that maintenance-event class was added
    expect(maintenanceEventEl.classList.contains('maintenance-event')).toBe(true);
    
    // Check that title was modified
    expect(eventTitle.innerHTML).toBe('Maintenance');
    expect(eventTitle.style.display).toBe('block');
    expect(eventTitle.style.textAlign).toBe('center');
    
    // Test normal event styling
    const normalEventEl = document.createElement('div');
    const normalEventTitle = document.createElement('div');
    normalEventTitle.className = 'fc-event-title';
    normalEventEl.appendChild(normalEventTitle);
    
    const normalEventInfo = {
      event: {
        extendedProps: { },
        end: new OriginalDate('2025-05-20')
      },
      el: normalEventEl
    };
    
    // Call eventDidMount handler
    wrapper.vm.calendarOptions.eventDidMount(normalEventInfo);
    
    // Check that title was hidden
    expect(normalEventTitle.style.display).toBe('none');
  });
});