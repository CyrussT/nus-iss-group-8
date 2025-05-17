import { vi } from 'vitest';
import { config } from '@vue/test-utils';

// Mock Nuxt composables
vi.mock('#imports', () => ({
  useColorMode: () => ({
    preference: 'light',
    value: 'light'
  }),
  useNuxtApp: () => ({
    $router: {
      push: vi.fn()
    }
  }),
  navigateTo: vi.fn(),
  definePageMeta: vi.fn()
}));

// Mock NuxtLink component
config.global.stubs = {
  NuxtLink: {
    template: '<a :href="to"><slot /></a>',
    props: ['to']
  }
};
