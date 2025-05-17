import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import student from '../student.vue';

describe('student', () => {
  it('renders correctly', () => {
    const wrapper = mount(student, {
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
