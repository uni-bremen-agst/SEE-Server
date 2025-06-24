# SEE Managed Server

## Table of Contents

- [About this Project](#about-this-project)
- [Built with](#built-with)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Environment Variables](#environment-variables)
    - [Run Locally](#run-locally)
    - [Security Considerations](#security-considerations)
- [Container Deployment](#container-deployment)
- [Usage](#usage)
- [Documentation](#documentation)

## About This Project

This is the management server for [SEE](https://github.com/uni-bremen-agst/SEE), consisting of a backend and a frontend
project.

The management server can be used to configure, run, and stop SEE game server instances.
Additionally, it provides an API to store and retrieve files for the use in multiple connected SEE clients.

## Built With

* [![React][React.js-badge]][React-url]
* [![Vite][Vite-badge]][Vite-url]
* [![Spring Boot][Springboot-badge]][Springboot-url]
* [![Traefik][Traefik-badge]][Traefik-url]

[React.js-badge]: https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB
[React-url]: https://reactjs.org/

[Springboot-badge]: https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&style=for-the-badge&logoColor=fff
[Springboot-url]: https://spring.io/projects/spring-boot

[Traefik-badge]: https://img.shields.io/badge/Traefik-24A1C1?style=for-the-badge&logo=traefikproxy&logoColor=black
[Traefik-url]: https://traefik.io/

[Vite-badge]: https://img.shields.io/badge/Vite-B73BFE?style=for-the-badge&logo=vite&logoColor=FFD62E
[Vite-url]: https://vitejs.dev/

## Getting Started

### Prerequisites

+ A Linux based machine (or WSL2)
+ [Docker](https://www.docker.com/get-started/) or [Podman](https://podman.io/get-started) need to be installed

### Environment Variables

To run this project, you will need to configure the following environment variables in your local `.env` file.

In `.env-example` we provide you with an example configuration.
Simply copy the content in you own file and adjust it to your needs.

Note that dotfiles, filenames starting with a `.`, might be hidden depending on your operating system and configuration.

> [!IMPORTANT]<br/>
> Remember to change the admin username and password as well as the JWT secret.

| Variable                 | Description                                                   |
|--------------------------|---------------------------------------------------------------|
| `DOMAIN_NAME`            | Domain under which the frontend/backend should be served      |
| `EXTERNAL_PORT`          | Port to expose the compose stack (default: 80)                |
| `DOCKER_SOCKET`          | Docker/Podman socket used to spawn new server instances       |
| `DOCKER_EXTERNAL_HOST`   | Public IPv4 address to register the game server               |
| `DOCKER_IMAGE_NAME`      | Docker image of the game server                               |
| `JWT_SECRET`             | Secret used to sign auth tokens                               |
| `JWT_EXPIRATION`         | Duration of token validity                                    |
| `ADD_ADMIN_USERNAME`     | Creates a new admin user account with this username           |
| `ADD_ADMIN_PASSWORD`     | Creates a new admin user account with this password           |

You can generate a new 64-bit random JWT secret using the following command:

```sh
openssl rand -base64 64 | tr -d '\n' && echo
```

**Important:**
Make sure to update the JWT secret before using the server in production.
Also note that with this configuration the backend will automatically find your Docker (or Podman) instance under `/var/run/docker.sock` and use it to spawn game server containers. You might want to use a different instance.

**Environment Setup Tips:**

+ `DOMAIN_NAME` must resolve to the machine hosting the server.
+ `EXTERNAL_PORT` must be accessible over the network.
+ `DOCKER_HOST_EXTERNAL` must be a valid, reachable IPv4 address.
+ For Podman, ensure `DOCKER_HOST` points to the socket (usually under `/var/run/user/${UID}/podman/podman.sock` for rootless Podman).

### Security Considerations

The backend – by default – has complete access to the Docker server it is running on.
Being able to launch any container means, it can potentially manipulate anything on the server.
If the backend itself is compromised, the complete server is also.

This can be mitigated by either:

- **Use a VM**: Running the backend or the complete stack in a VM.
- **Use a separate Docker host:** Giving the backend access to a separate Docker instance.
    - Set up a separate machine or VM to provide a Docker host for the game servers and configure the backend to use
      this instead.
- **[Use Podman in rootless mode](https://wiki.archlinux.org/title/Podman#Rootless_Podman)**.
  - Running containers in rootless setup will reduce the attack vector to the scope of the user executing them.
  - Full access to the rootless Podman socket means a compromised container can do anything the user can do on the host system.
  - Be advised that in production environments it is still a good idea to use a separate dedicated user for running the containers or even another separate user for the game server instances as well.
  - This will usually prevent the frontend to open port 80, which should be no problem as a reverse proxy with HTTPS should be used, anyway.

### Run Locally

See the `README.md` files in the Frontend and Backend directories for detailed local setup instructions if you want to run the server without Docker or Podman.

### Container Deployment

This section describes how to download/build and run the container images via Docker or Podman.<br/>
Make sure you have read the security considerations, especially when exposing the server to the Internet.

#### 1. Clone the Project

```sh
git clone https://github.com/uni-bremen-agst/SEE-Server.git
# or git clone git@github.com:uni-bremen-agst/SEE-Server.git
cd SEE-Server
```

#### 2a. Pull Container Images

To pull the Backend and Frontend images as well as the Traefik reverse proxy, execute:

```sh
docker compose pull
# or using Podman Compose
podman-compose pull
```

You also need to pull the actual SEE game server image which is not part of the compose file as it is indirectly spawned by the back-end container.
This image needs to be available on the Docker/Podman instance that is made available to the Backend (cmp. `DOCKER_SOCKET`).

```sh
docker pull ghcr.io/uni-bremen-agst/see-gameserver:latest
# or using Podman
podman pull ghcr.io/uni-bremen-agst/see-gameserver:latest
```

The tags as defined in `compose.yaml` and the above command can be replaced to use specific tags in place of `latest`. Available images and tags are listed in our [container registry](https://github.com/orgs/uni-bremen-agst/packages).

#### 2b. Build Container Images

Alternatively, you can build the container images locally using the following commands:

```sh
docker compose build
# or
podman-compose build
```

Please note that the images will be created with the names defined in the compose file.
By default, this will replace the images you might have downloaded earlier.
(Actually, any downloaded images will stick around but the newly built images will get tagged so that they are used in place of them.)

#### 3. Create Data Directory

The data directory will be mounted into the Backend container to store persistent data.

From the repository root, execute:

```sh
mkdir backend-data
```

#### 4. Start the Server

You need to create a `.env` file with your configuration needed according to section [Environment Variables](#environment-variables).

This project uses [Traefik](https://traefik.io/traefik/) as a reverse proxy.
By default, it exposes services on port 80.

**Note:**
This can be a problem when using rootless Podman because by default system ports (<1024) are not available for users.
Typically, you would set `EXTERNAL_PORT` to `8080` or similar in the `.env` file and use your firewall to redirect port `80` if necessary.

To enable HTTPS:

- Uncomment the relevant sections in `compose.yaml`.
- Refer to [Traefik TLS Docs](https://doc.traefik.io/traefik/https/tls/).
- For automatic certs, use [Let's Encrypt](https://doc.traefik.io/traefik/https/acme/).

To start the server:

```sh
docker compose up -d
# or
podman-compose up -d
```

You can add parameter `--env-file <YOUR_ENV_FILE>` to select a specific environment file different from the default `.env`.
Replace `<YOUR_ENV_FILE>` with the env file you have created.

Now you can visit the configured domain in your browser.

**Note:**
It is important to use exactly the domain and port that is configured via `DOMAIN_NAME` and `EXTERNAL_PORT` in your environment.
For example, if you configured `DOMAIN_NAME=127.0.0.1` and `EXTERNAL_PORT=8082`, the URL is `http://127.0.0.1:8082/` and `http://localhost:8082/` won't work!

#### 4. Stop the Server

```sh
docker compose down
```

--------------------------------------------------------------------------------

## Usage

Using the Frontend, you can create and manage servers on the Backend.

When creating a new server, you can attach several Code City archives that are uploaded to the backend.
Each Code City type has a dedicated table in the virtual world.
Clients will automatically download the archives from the backend when connecting to the server instance and try to
instantiate them on the appropriate table.

### Prepare Code Cities

The Code Cities are stored in the `Assets/StreamingAssets/Multiplayer/` directory.

**Heads up:** The `Multiplayer` directory will get cleared if you connect to a server. Keep that in mind while you
prepare your Code Cities for upload!

Prepare your Code Cities depending on their type in the respective subdirectories:

- `SEECity`
- `DiffCity`
- `SEECityEvolution`
- `SEEJlgCity`
- `SEEReflexionCity`

This is necessary so that all relative paths are correctly referenced, e.g., in the `.cfg` files.

Each of the Code Cities should contain a valid configuration file (`.cfg`),
which is automatically loaded by the SEE clients after connecting to the server and downloading the files from the
backend.

Each of the above Code City directories should be individually zipped.
You can either zip the directories' contents or the folder itself.

--------------------------------------------------------------------------------

## Documentation

Both subprojects have their own README file in their respective subfolders, which comes with additional information.

Keep the information collected here and in the READMEs of the subprojects up-to-date, and update them in a timely
fashion, at the latest before merging the development branch back into the main branch.

Please feel free to add additional info whenever you change anything that is covered in this document, or whenever you
overcome hurdles and would have liked to have found the information here in the first place.
