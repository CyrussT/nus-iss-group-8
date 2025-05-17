// pages/facility-details/__tests__/facility-details.spec.ts
import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";
import Id from "../[id].vue";
import { setupNuxtUIStubs } from "~/test/utils";
import { ref } from "vue";

vi.mock("@/composables/useFacility", () => ({
  useFacility: () => ({
    facility: ref({
      facilityId: 1,
      resourceTypeId: 2,
      resourceName: "Test Facility",
      location: "Test Location",
      capacity: 30,
      bookings: [
        {
          bookingId: 101,
          email: "student@example.com",
          studentName: "Test Student",
          purpose: "Group Study",
          bookedDatetime: "2025-05-17T10:00:00",
          timeslot: "10:00 - 11:00",
          status: "CONFIRMED",
        },
        {
          bookingId: 102,
          email: "student2@example.com",
          studentName: "Another Student",
          purpose: "Project Meeting",
          bookedDatetime: "2025-05-18T14:00:00",
          timeslot: "14:00 - 15:00",
          status: "PENDING",
        },
      ],
    }),
    fetchFacilityDetails: vi.fn(),
    getResourceTypeName: vi.fn().mockReturnValue("Classroom"),
  }),
}));

vi.mock("vue-router", () => ({
  useRoute: () => ({
    params: { id: "1" },
  }),
  useRouter: () => ({
    push: vi.fn(),
  }),
}));

vi.mock("vue", async () => {
  const actual = await vi.importActual("vue");
  return {
    ...actual,
    ref: (val: any) => ({
      value: val,
      __v_isRef: true,
    }),
    watch: vi.fn((source, callback) => {
      if (typeof source === "function") {
        try {
          callback(source());
        } catch (e) {
        }
      }
    }),
    computed: (fn: () => any) => {
      if (typeof fn === "function") {
        try {
          return fn();
        } catch (e) {
          return ref(undefined);
        }
      }
      return fn;
    },
    onMounted: (fn: () => void) => {
      if (typeof fn === "function") {
        try {
          fn();
        } catch (e) {
        }
      }
    },
  };
});

vi.mock('@nuxt/icon', () => {
  return {
    default: {
      name: 'NuxtIcon',
      props: ['name', 'size', 'filled', 'class'],
      template: '<div class="nuxt-icon" />',
    }
  };
});

const stubs = {
  ...setupNuxtUIStubs(),
  UTable: true, 
  UButton: true,
  UIcon: true,
  NuxtIcon: true,
  'nuxt-icon': true,
};

describe("facility-details", () => {
  let wrapper: any;

  beforeEach(async () => {
    vi.clearAllMocks();

    // Mount the component with all UI components stubbed
    wrapper = mount(Id, {
      global: {
        stubs: stubs,
        mocks: {
          $route: {
            params: { id: '1' }
          },
          $router: {
            push: vi.fn()
          }
        }
      }
    });

    await flushPromises();
  });

  it("renders correctly", async () => {
    // Check that the page title is rendered
    expect(wrapper.find("h1").text()).toBe("Facility Details");
  });

  it("displays facility information", async () => {
    // Check facility details are displayed correctly
    const facilityDetails = [
      { label: "Facility ID:", value: "1" },
      { label: "Resource Type Name:", value: "Classroom" },
      { label: "Name:", value: "Test Facility" },
      { label: "Location:", value: "Test Location" },
      { label: "Capacity:", value: "30" },
    ];

    // Check each detail is present
    const rows = wrapper.findAll("tbody tr");

    facilityDetails.forEach((detail, index) => {
      const row = rows[index];
      expect(row.findAll("td")[0].text()).toBe(detail.label);
      expect(row.findAll("td")[1].text()).toContain(detail.value);
    });
  });

  it("displays booking history section", async () => {
    // Check that the booking history section exists
    expect(wrapper.find("h2").text()).toBe("Booking History");
  });
});