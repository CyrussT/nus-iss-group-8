import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import AdminDashboard from '../AdminDashboard.vue';
import axios from 'axios';

// Mock axios
vi.mock('axios');

// Mock composables
vi.mock('@/composables/useApi', () => ({
  useApi: () => ({
    apiUrl: 'http://localhost:8080'
  })
}));

describe('AdminDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    
    // Set up axios mock response
    (axios.get as any).mockResolvedValue({
      data: {
        todayBookings: 5,
        pendingApprovals: 2,
        facilitiesUnderMaintenance: 1,
        emergencyMessage: null
      }
    });
  });
  
  it('renders correctly with stats', async () => {
    const wrapper = mount(AdminDashboard, {
      global: {
        stubs: {
          UButton: true,
          UCard: true,
          UModal: true,
          UTextarea: true
        }
      }
    });
    
    // Wait for the component to update after API call
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    
    expect(wrapper.html()).toMatchSnapshot();
    
    // Verify stats are displayed
    expect(wrapper.find('p.text-4xl.font-bold.text-center').text()).toBe('5');
    expect(wrapper.findAll('p.text-4xl.font-bold.text-center')[1].text()).toBe('2');
    expect(wrapper.findAll('p.text-4xl.font-bold.text-center')[2].text()).toBe('1');
  });
  
  it('renders emergency message when present', async () => {
    // Mock response with emergency message
    (axios.get as any).mockResolvedValue({
      data: {
        todayBookings: 10,
        pendingApprovals: 3,
        facilitiesUnderMaintenance: 2,
        emergencyMessage: "Emergency: System maintenance at 5PM"
      }
    });
    
    const wrapper = mount(AdminDashboard, {
      global: {
        stubs: {
          UButton: true,
          UCard: true,
          UModal: true,
          UTextarea: true
        }
      }
    });
    
    // Wait for the component to update after API call
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    
    expect(wrapper.html()).toMatchSnapshot();
    
    // Verify emergency message is displayed
    const emergencyCard = wrapper.find('p.text-red-700');
    expect(emergencyCard.exists()).toBe(true);
    expect(emergencyCard.text()).toBe("Emergency: System maintenance at 5PM");
  });
  
  it('opens emergency modal when button is clicked', async () => {
    const wrapper = mount(AdminDashboard, {
      global: {
        stubs: {
          UButton: false,
          UCard: true,
          UModal: {
            template: '<div v-if="modelValue"><slot /></div>',
            props: ['modelValue']
          },
          UTextarea: true
        }
      }
    });
    
    // Emergency modal should be closed initially
    expect(wrapper.vm.isEmergencyModalOpen).toBe(false);
    
    // Click the emergency button
    await wrapper.find('button.px-4.py-2').trigger('click');
    
    // Modal should be open
    expect(wrapper.vm.isEmergencyModalOpen).toBe(true);
    expect(wrapper.html()).toMatchSnapshot();
  });
});
