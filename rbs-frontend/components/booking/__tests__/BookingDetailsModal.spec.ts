import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import BookingDetailsModal from '../BookingDetailsModal.vue';

describe('BookingDetailsModal', () => {
  it('renders correctly', () => {
    const wrapper = mount(BookingDetailsModal, {
      global: {
        stubs: {
          UButton: true,
          UIcon: true
        }
      }
    });
    expect(wrapper.html()).toMatchSnapshot();
  });
});
