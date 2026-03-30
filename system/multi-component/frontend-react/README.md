# Frontend Service

This is the frontend UI service for the Starter, built with React.

## Features

- React-based single-page application
- React Router for navigation
- Home page with welcome message
- Todo Fetcher page to query and display todos
- Responsive design

## Pages

- `/` - Home page with welcome message
- `/todos` - Todo Fetcher page

## Instructions

```shell
cd frontend
```

Check that you have Powershell 7

```shell
$PSVersionTable.PSVersion
```

Check that you have Node.js 22 installed

```shell
node -v
```

## Running Locally

First, install dependencies:

```shell
npm install
```

Then start the development server:

```shell
npm start
```

The app will open at http://localhost:8080.

**Note:** For local development, make sure the backend service is running on port 8081, or update the proxy setting in `package.json`.

## Building for Production

```shell
npm run build
```

This creates an optimized production build in the `build` folder.

## Docker

Build the Docker image:

```shell
docker build -t frontend .
```

Run the container on same network as the 'backend' image (in the folder 'backend' > README.md, follow the steps to create the network and run the 'backend' image):

```shell
docker run -d --name frontend --network hero-network -p 8080:8080 frontend
```

## Technology Stack

- React 18
- React Router DOM 6
- React Scripts 5