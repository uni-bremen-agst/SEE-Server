FROM ghcr.io/pnpm/pnpm:12 AS base
RUN pnpm runtime set node 26.4 -g
ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
COPY . /app
WORKDIR /app

FROM base AS prod-deps
RUN --mount=type=cache,id=pnpm,target=/var/cache/pnpm \
    pnpm install --store-dir /var/cache/pnpm --prod --frozen-lockfile


FROM base AS build
RUN --mount=type=cache,id=pnpm,target=/var/cache/pnpm \
    pnpm install --store-dir /var/cache/pnpm --frozen-lockfile
RUN pnpm run build

# ----------------------------------------------------------

FROM docker.io/nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
RUN sed -i /etc/nginx/conf.d/default.conf \
        -e 's|\(listen.*\)80\(.*\)|\18080\2|g;'
EXPOSE 8080
