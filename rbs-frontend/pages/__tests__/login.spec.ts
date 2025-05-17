import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import login from '../login.vue';

describe('login', () => {
  it('renders correctly', () => {
    const wrapper = mount(login, {
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
