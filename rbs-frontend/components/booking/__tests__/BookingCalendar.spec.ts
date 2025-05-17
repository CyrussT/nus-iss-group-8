import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import BookingCalendar from '../BookingCalendar.vue';

describe('BookingCalendar', () => {
  it('renders correctly', () => {
    const wrapper = mount(BookingCalendar, {
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
