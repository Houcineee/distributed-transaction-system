# Stage 1: Build the React application
FROM --platform=$BUILDPLATFORM node:20-alpine AS build

WORKDIR /app

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies cleanly
RUN npm ci --legacy-peer-deps

# Copy the rest of the frontend source code
COPY . .

# Build the Vite project with specified mode (defaults to staging)
ARG MODE=staging
RUN npm run build -- --mode ${MODE}

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Copy the built assets from the build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copy our custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80 to the Docker network
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
