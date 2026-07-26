# Vite bakes VITE_* env vars into the built JS at build time, not at container runtime -
# so the public Keycloak/gateway URLs must be passed as build args here, not as env vars
# on the running container.
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .

ARG VITE_KEYCLOAK_URL
ARG VITE_KEYCLOAK_REALM=bima-care
ARG VITE_KEYCLOAK_CLIENT_ID=bima-admin-web
ARG VITE_API_BASE_URL
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL \
    VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM \
    VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID \
    VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
