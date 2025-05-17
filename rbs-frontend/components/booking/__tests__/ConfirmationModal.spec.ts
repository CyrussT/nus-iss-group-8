import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import ConfirmationModal from '../ConfirmationModal.vue';

describe('ConfirmationModal', () => {
  it('renders correctly', () => {
    const wrapper = mount(ConfirmationModal, {
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
