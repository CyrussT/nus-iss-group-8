import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import studentBooking from '../student-booking.vue';

describe('studentBooking', () => {
  it('renders correctly', () => {
    const wrapper = mount(studentBooking, {
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
