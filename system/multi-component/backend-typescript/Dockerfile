# Stage 1: Build the app
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci

# Copy source code and build
COPY . .
RUN npm run build

# Stage 2: Run the app
FROM node:22-alpine
WORKDIR /app

# Copy built files and dependencies from build stage
COPY --from=build /app ./

# Expose port (adjust if your app uses a different port)
EXPOSE 8081

# Start the app (adjust if your entry point is different)
CMD ["npm", "start"]