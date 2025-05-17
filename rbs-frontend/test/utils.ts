import { vi } from 'vitest';
import { config } from '@vue/test-utils';

/**
 * Common mocks for testing Vue components with Nuxt
 */
export const setupNuxtMocks = () => {
  vi.mock('#imports', async () => {
    const actual = await vi.importActual('#imports');
    return {
      ...actual as object,
      useRouter: () => ({
        push: vi.fn(),
        replace: vi.fn()
      }),
      useRoute: () => ({
        params: {},
        query: {},
        path: '/'
      }),
      useNuxtApp: () => ({
        $router: {
          push: vi.fn(),
          replace: vi.fn()
        }
      }),
      useColorMode: () => ({
        preference: 'light',
        value: 'light'
      }),
      useAuthStore: () => ({
        user: {
          value: { role: 'STUDENT' }
        },
        isAuthenticated: { value: true },
        logout: vi.fn()
      }),
      navigateTo: vi.fn(),
      definePageMeta: vi.fn()
    };
  });
};

/**
 * Setup common component stubs for NuxtUI
 */
export const setupNuxtUIStubs = () => {
  return {
    UButton: true,
    UIcon: true,
    UCard: true,
    UInput: true,
    UInputMenu: true,
    NuxtLink: {
      template: '<a :href="to"><slot /></a>',
      props: ['to']
    },
    UHorizontalNavigation: true,
    UModal: true,
    UForm: true,
    UFormGroup: true,
    USelect: true,
    UAlert: true,
    UBadge: true,
    UAvatar: true,
    UTooltip: true,
    UPopover: true
  };
};

/**
 * Configure Vue Test Utils globally
 */
export const configureVueTestUtils = () => {
  config.global.stubs = {
    NuxtLink: {
      template: '<a :href="to"><slot /></a>',
      props: ['to']
    }
  };
};
