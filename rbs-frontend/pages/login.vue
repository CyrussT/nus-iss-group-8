<script setup lang="ts">
definePageMeta({
  middleware: ['guest'],
  layout: 'unauth'
})

import { z } from "zod";
import type { Form, FormError, FormSubmitEvent } from "#ui/types";
import { useApi } from "~/composables/useApi";

const { apiUrl } = useApi()
const authStore = useAuthStore()
const router = useRouter()

const loginSchema = z.object({
  email: z.string().email("Invalid email"),
  password: z.string(),
});

type LoginData = z.output<typeof loginSchema>;

const state = reactive({
  email: "",
  password: "",
});

const error = ref<string | null>(null)
const loading = ref(false)

const validate = (state: any): FormError[] => {
  const errors = [];
  if (!state.email) errors.push({ path: "email", message: "Email is required" });
  if (!state.password) errors.push({ path: "password", message: "Password is required" });
  return errors;
};

async function onSubmit(event: FormSubmitEvent<LoginData>) {
  event.preventDefault();
  error.value = null
  loading.value = true

  try {
    const response = await fetch(`${apiUrl}/api/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        email: event.data.email,
        password: event.data.password,
      })
    })

    const data = await response.json()

    if (!response.ok) {
      error.value = data.message
      return
    }

    // store jwt token
    authStore.setToken(data.token)

    router.push('/dashboard')
  } catch (e) {
    error.value = "An error has occurred when logging in."
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- Background Image -->
    <img src='./image/index.jpg' alt="Background" class="background-image" />

    <!-- Overlay (optional for darkening the image) -->
    <div class="overlay"></div>

    <!-- Centered Login Card -->
    <div class="form-container">
      <UCard class="w-full max-w-md">
        <template #header>
          <h2>Login to RBS</h2>
        </template>

        <UForm
          :schema="loginSchema"
          :validate="validate"
          :state="state"
          @submit="onSubmit"
          class="space-y-4"
        >
          <UFormGroup label="Email" name="email" :ui="{ wrapper: 'space-y-2' }">
            <UInput v-model="state.email" placeholder="Enter your email" />
          </UFormGroup>

          <UFormGroup label="Password" name="password" :ui="{ wrapper: 'space-y-2' }">
            <UInput v-model="state.password" type="password" placeholder="Enter your password" />
          </UFormGroup>

          <UButton type="submit" block class="mt-4">
            Login <UIcon name="i-heroicons:arrow-right-20-solid" />
          </UButton>
        </UForm>
      </UCard>
    </div>
  </div>
</template>


<script lang="ts" setup></script>

<style scoped>
.login-page {
  position: relative;
  width: 100vw;
  height: 95vh;
  overflow: hidden;
}

.background-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 1;
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5); /* Optional: adds contrast for form */
  z-index: 2;
}

.form-container {
  position: relative;
  z-index: 3;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 1rem;
}
</style>
