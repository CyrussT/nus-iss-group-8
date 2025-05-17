import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import AuthAppHeader from '../AuthAppHeader.vue';

describe('AuthAppHeader', () => {
  it('renders correctly', () => {
    const wrapper = mount(AuthAppHeader, {
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
