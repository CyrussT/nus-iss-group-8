import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import BookingSearchBar from '../BookingSearchBar.vue';

describe('BookingSearchBar', () => {
  it('renders correctly', () => {
    const wrapper = mount(BookingSearchBar, {
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
