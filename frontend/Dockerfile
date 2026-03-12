# Stage 1: Build the React app
FROM node:25-alpine3.22 AS build
WORKDIR /app

COPY package.json ./
RUN npm install

COPY . ./
RUN npm run build

# Stage 2: Development environment
FROM node:25-alpine3.22 AS development
WORKDIR /app

COPY package.json ./
RUN npm install

COPY . ./

EXPOSE 3000
CMD ["npm", "run", "dev"]

# Stage 3: Production environment
FROM nginx:alpine AS production

# Fix pid path
RUN sed -i 's|pid\s*/[a-z/]*nginx.pid;|pid /tmp/nginx.pid;|g' /etc/nginx/nginx.conf

# Redirect all nginx temp dirs to /var/cache/nginx (mounted as emptyDir in K8s)
RUN printf 'client_body_temp_path /var/cache/nginx/client_temp;\n\
proxy_temp_path /var/cache/nginx/proxy_temp;\n\
fastcgi_temp_path /var/cache/nginx/fastcgi_temp;\n\
uwsgi_temp_path /var/cache/nginx/uwsgi_temp;\n\
scgi_temp_path /var/cache/nginx/scgi_temp;\n' \
    > /etc/nginx/conf.d/temp-paths.conf

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]