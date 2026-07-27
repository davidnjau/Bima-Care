# Deploying BimaCare to a VPS

Single-node deployment matching `BIMA CARE.md` §9's baseline production node spec (8 vCPU /
12 GB RAM / 240 GB SSD RAID 10 - the Namecheap Magnetar tier fits this). Everything runs on
one box for now; the three-cluster split described in §9 is a later step once this needs to
scale, and shouldn't require re-architecting anything here.

Fully secrets-driven: once the secrets below are set, every merge to `main` deploys with no
manual steps on the VPS. There's no provisioning script to run by hand - the workflow
installs Docker if it's missing, renders `infra/.env` from GitHub Secrets, and brings up both
compose layers, every single run. Re-running it is always safe.

Fronted by Caddy for automatic HTTPS - `bimacare.online` (web app), `api.bimacare.online`
(gateway), and `auth.bimacare.online` (Keycloak) each get their own Let's Encrypt cert with
zero manual cert management. Nothing else publishes a host port at all.

## How it fits together

- `infra/docker-compose.infra.yml` - Postgres, Kafka, MinIO, the HAPI FHIR server, Keycloak,
  and Caddy. Caddy is the only container publishing a host port (80/443).
- `infra/docker-compose.apps.yml` - the gateway, all 13 microservices, and the web frontend.
  Pulls prebuilt images from Docker Hub; never builds from source on the VPS.
- `infra/caddy/Caddyfile` - routes each of the three hostnames to its container by name.
- `.github/workflows/ci.yml`'s `deploy` job (runs after `build-images`/`build-web-image`
  succeed, only on merge to `main`):
  1. Renders `infra/.env` from GitHub Secrets (nothing is hand-edited on the VPS).
  2. Bundles both compose files, the Postgres init script, the Keycloak realm export, and
     the Caddyfile into one tarball and copies it to the VPS over SCP.
  3. SSHes in, installs Docker if it isn't already there, brings up
     `docker-compose.infra.yml`, then pulls the latest images and brings up
     `docker-compose.apps.yml`.

## DNS

Four A records, all pointed at the VPS IP, before the first deploy (Caddy requests each
cert on first request to that hostname - if DNS hasn't propagated yet, that one request just
retries in the background rather than failing the whole deploy, but nothing will be reachable
on that hostname until it resolves):

| Host | Points to |
|---|---|
| `@` (bimacare.online) | VPS IP |
| `www` | VPS IP (redirects to the apex over HTTPS, doesn't serve content itself) |
| `api` | VPS IP |
| `auth` | VPS IP |

## The one thing that can't be automated away

GitHub Actions needs to SSH into the VPS, and that first connection requires the matching
public key to already be in the VPS's `~/.ssh/authorized_keys` - there's no API call that
gets around placing that key once. Concretely:

1. Generate a deploy keypair (don't reuse your personal one):
   ```bash
   ssh-keygen -t ed25519 -f bima-deploy-key -N ""
   ```
2. Add `bima-deploy-key.pub` to the VPS - either paste it into Namecheap's panel when
   creating the VM, or `ssh-copy-id -i bima-deploy-key.pub user@your-vps-ip` once it exists.
3. Add `bima-deploy-key` (the private half) as the `VPS_SSH_KEY` secret below.

Everything past this point is secrets only.

## Required GitHub secrets

Settings → Secrets and variables → Actions:

| Secret | What it is |
|---|---|
| `VPS_HOST` | VPS IP or hostname |
| `VPS_USER` | SSH user - either `root`, or a user with passwordless sudo (the Docker install step needs it) |
| `VPS_SSH_KEY` | The private half of the deploy keypair above |
| `VPS_SSH_PORT` | Optional, defaults to 22 |
| `DOCKERHUB_USERNAME` | Your Docker Hub username - images are pushed as `docker.io/<username>/bima-care-<service>` |
| `DOCKERHUB_TOKEN` | A Docker Hub Personal Access Token (Account Settings → Security → New Access Token, Read & Write) - not your account password |
| `POSTGRES_PASSWORD` | |
| `KEYCLOAK_ADMIN_PASSWORD` | |
| `MINIO_ROOT_PASSWORD` | |
| `BIMA_GATEWAY_CLIENT_SECRET` | Must match the `bima-gateway` client secret in the Keycloak realm |
| `IAM_PROVISIONER_CLIENT_SECRET` | Must match the `bima-iam-provisioner` client secret in the Keycloak realm |
| `PUBLIC_KEYCLOAK_URL` | `https://auth.bimacare.online` - also becomes Keycloak's own `--hostname` (bare hostname, derived by stripping the scheme) |
| `PUBLIC_WEB_ORIGIN` | `bimacare.online` - passed to the gateway's CORS check |
| `PUBLIC_API_BASE_URL` | `https://api.bimacare.online` |
| `CADDY_EMAIL` | Optional - Let's Encrypt renewal-notice email, defaults to `webmaster@bimacare.online` if unset |
| `DEMO_PROVIDER_IDENTITIES` | Optional, empty until you've onboarded real providers on this deployment |
| `DEMO_INSURER_IDENTITIES` | Optional, same |

`PUBLIC_KEYCLOAK_URL` and `PUBLIC_API_BASE_URL` get baked into the web build at image-build
time (Vite env vars aren't runtime-configurable) via `build-web-image`; `PUBLIC_KEYCLOAK_URL`
also becomes `PUBLIC_KEYCLOAK_ISSUER` in the rendered `.env` (with `/realms/bima-care`
appended), which every backend service uses to validate the JWTs Keycloak hands the browser,
and `KEYCLOAK_HOSTNAME` (bare hostname only), which Keycloak's own `--hostname` flag uses so
every issuer/link it generates matches what the browser actually used. All of these have to
describe the *same* deployment - a mismatch means every login silently 401s.

## First deploy

1. Complete the SSH key step above.
2. Add all the secrets.
3. Merge to `main`. CI builds every image, renders `.env`, copies config, and deploys.
4. Watch the `deploy` job's SSH step output for the final `docker compose ... ps` - all
   containers should show `healthy` or `running`.

To force a redeploy without a new commit (e.g. after fixing a secret), re-run the workflow
from the Actions tab - same commit, same images, freshly rendered `.env`.

## Known gaps to close later

- **Rollback**: every push also tags the image with the commit SHA, so the previous image is
  still on the VPS as `:<previous-sha>` even after a bad deploy pulls `:latest` - a manual
  rollback is `IMAGE_TAG=<previous-sha> docker compose --env-file .env -f docker-compose.infra.yml -f docker-compose.apps.yml up -d`
  run on the VPS (both files must always be passed together - see the comment in
  `ci.yml`'s deploy job for why). Not automated yet.
- **Secrets rotation**: if you change a Keycloak client secret in the admin console, update
  the matching GitHub secret and redeploy - there's no automation tying the two together.
