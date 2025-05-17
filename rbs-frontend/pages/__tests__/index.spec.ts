import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import index from '../index.vue';

describe('index', () => {
  it('renders correctly', () => {
    const wrapper = mount(index, {
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
