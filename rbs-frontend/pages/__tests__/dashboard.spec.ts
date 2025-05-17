import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import dashboard from '../dashboard.vue';

describe('dashboard', () => {
  it('renders correctly', () => {
    const wrapper = mount(dashboard, {
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
