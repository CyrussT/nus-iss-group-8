import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import bookingReviewList from '../booking-review-list.vue';
// import { WebSocketServer } from "ws"

vi.mock('@stomp/stompjs', () => {
  return {
    Client: vi.fn().mockImplementation(() => ({
      activate: vi.fn(),
      deactivate: vi.fn(),
      subscribe: vi.fn(),
      publish: vi.fn()
    }))
  };
});

describe('bookingReviewList', () => {
  it('renders correctly', () => {
    const wrapper = mount(bookingReviewList, {
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
