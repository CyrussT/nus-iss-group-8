FROM oven/bun:latest AS build

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY bun.lock ./

# Install dependencies
RUN bun install

# Copy the rest of the application code
COPY . .

# Use API URL from environment variable with fallback
ARG API_BASE_URL=http://backend:8080
ENV NUXT_PUBLIC_API_URL=${API_BASE_URL}

# Build the application
RUN bun run build

# Runtime stage
FROM node:20-alpine

WORKDIR /app

# Copy the built files from the previous stage
COPY --from=build /app/.output ./

# Expose the port the app runs on
EXPOSE 3000

# Allow runtime environment variable override
ENV HOST=0.0.0.0
ENV PORT=3000
ENV NODE_ENV=production

# Command to run the application
CMD ["node", "/app/server/index.mjs"]