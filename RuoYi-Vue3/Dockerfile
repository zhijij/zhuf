FROM node:22-alpine AS build
WORKDIR /app
ARG VITE_AMAP_JS_API_KEY=
ARG VITE_AMAP_SECURITY_JS_CODE=
ENV VITE_AMAP_JS_API_KEY=$VITE_AMAP_JS_API_KEY
ENV VITE_AMAP_SECURITY_JS_CODE=$VITE_AMAP_SECURITY_JS_CODE
COPY package*.json ./
RUN npm ci --fetch-retries=5 --fetch-retry-mintimeout=20000 --fetch-retry-maxtimeout=120000
COPY . .
RUN npm run build:prod

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
