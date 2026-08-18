FROM node:24-alpine AS build

WORKDIR /workspace

COPY radar-frontend/package.json radar-frontend/package-lock.json ./

RUN npm ci

COPY radar-frontend/ ./

RUN npm run build

FROM nginx:stable-alpine

COPY radar-frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80

