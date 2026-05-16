# Stage 1: Build
FROM node:18-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./

# install ALL dependencies for build
RUN npm ci

COPY . .

# update browserslist (optional)
RUN npx update-browserslist-db@latest

# build the app
RUN npm run build


# Stage 2: Production
FROM node:18-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# copy built app
COPY --from=builder /app ./

ENV NODE_ENV=production

EXPOSE 5001

USER appuser

CMD ["npm", "start"]