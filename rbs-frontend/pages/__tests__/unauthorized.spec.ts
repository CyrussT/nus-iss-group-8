import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import unauthorized from '../unauthorized.vue';

describe('unauthorized', () => {
  it('renders correctly', () => {
    const wrapper = mount(unauthorized, {
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
