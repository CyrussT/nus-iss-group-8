import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import studentList from '../student-list.vue';

describe('studentList', () => {
  it('renders correctly', () => {
    const wrapper = mount(studentList, {
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
