import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import BookingCreateModal from '../BookingCreateModal.vue';

describe('BookingCreateModal', () => {
  it('renders correctly', () => {
    const wrapper = mount(BookingCreateModal, {
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
