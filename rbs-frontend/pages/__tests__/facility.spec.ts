import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import facility from '../facility.vue';

describe('facility', () => {
  it('renders correctly', () => {
    const wrapper = mount(facility, {
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
