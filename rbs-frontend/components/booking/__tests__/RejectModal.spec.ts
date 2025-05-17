import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import RejectModal from '../RejectModal.vue';

describe('RejectModal', () => {
  it('renders correctly', () => {
    const wrapper = mount(RejectModal, {
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
