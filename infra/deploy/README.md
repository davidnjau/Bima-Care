# Deploying BimaCare to a VPS

Single-node deployment matching `BIMA CARE.md` §9's baseline production node spec (8 vCPU /
12 GB RAM / 240 GB SSD RAID 10 - the Namecheap Magnetar tier fits this). Everything runs on
one box for now; the three-cluster split described in §9 is a later step once this needs to
scale, and shouldn't require re-architecting anything here.

Fully secrets-driven: once the secrets below are set, every merge to `main` deploys with no
manual steps on the VPS. There's no provisioning script to run by hand - the workflow
installs Docker if it's missing, renders `infra/.env` from GitHub Secrets, and brings up both
compose layers, every single run. Re-running it is always safe.

No TLS/reverse proxy yet - everything is plain HTTP behind the VPS's public IP. Add a domain
+ Caddy/nginx + Let's Encrypt as a follow-up (tracked as a gap below), not blocking the first
deploy.

## How it fits together

- `infra/docker-compose.infra.yml` - Postgres, Kafka, MinIO, the HAPI FHIR server, Keycloak.
- `infra/docker-compose.apps.yml` - the gateway, all 13 microservices, and the web frontend.
  Pulls prebuilt images from Docker Hub; never builds from source on the VPS.
- `.github/workflows/ci.yml`'s `deploy` job (runs after `build-images`/`build-web-image`
  succeed, only on merge to `main`):
  1. Renders `infra/.env` from GitHub Secrets (nothing is hand-edited on the VPS).
  2. Copies the two compose files, the Postgres init script, and the Keycloak realm export
     to the VPS over SCP.
  3. SSHes in, installs Docker if it isn't already there, brings up
     `docker-compose.infra.yml`, then pulls the latest images and brings up
     `docker-compose.apps.yml`.

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
| `PUBLIC_KEYCLOAK_URL` | e.g. `http://YOUR_VPS_IP:8180` - the origin the browser reaches Keycloak at |
| `PUBLIC_WEB_ORIGIN` | e.g. `YOUR_VPS_IP` - passed to the gateway's CORS check |
| `PUBLIC_API_BASE_URL` | e.g. `http://YOUR_VPS_IP:8080` - the gateway's public address |
| `DEMO_PROVIDER_IDENTITIES` | Optional, empty until you've onboarded real providers on this deployment |
| `DEMO_INSURER_IDENTITIES` | Optional, same |

`PUBLIC_KEYCLOAK_URL` and `PUBLIC_API_BASE_URL` get baked into the web build at image-build
time (Vite env vars aren't runtime-configurable) via `build-web-image`; `PUBLIC_KEYCLOAK_URL`
also becomes `PUBLIC_KEYCLOAK_ISSUER` in the rendered `.env` (with `/realms/bima-care`
appended), which every backend service uses to validate the JWTs Keycloak hands the browser.
All of these have to describe the *same* VPS - a mismatch means every login silently 401s.

## First deploy

1. Complete the SSH key step above.
2. Add all the secrets.
3. Merge to `main`. CI builds every image, renders `.env`, copies config, and deploys.
4. Watch the `deploy` job's SSH step output for the final `docker compose ... ps` - all
   containers should show `healthy` or `running`.

To force a redeploy without a new commit (e.g. after fixing a secret), re-run the workflow
from the Actions tab - same commit, same images, freshly rendered `.env`.

## Known gaps to close later

- **TLS**: once you have a domain pointed at the VPS, add a reverse proxy (Caddy is the
  least fuss - automatic Let's Encrypt certs) in front of the gateway, Keycloak, and the web
  container, then update `PUBLIC_KEYCLOAK_URL`/`PUBLIC_WEB_ORIGIN`/`PUBLIC_API_BASE_URL` to
  the `https://` domain form and drop the raw ports.
- **Rollback**: every push also tags the image with the commit SHA, so the previous image is
  still on the VPS as `:<previous-sha>` even after a bad deploy pulls `:latest` - a manual
  rollback is `IMAGE_TAG=<previous-sha> docker compose --env-file .env -f docker-compose.infra.yml -f docker-compose.apps.yml up -d`
  run on the VPS (both files must always be passed together - see the comment in
  `ci.yml`'s deploy job for why). Not automated yet.
- **Secrets rotation**: if you change a Keycloak client secret in the admin console, update
  the matching GitHub secret and redeploy - there's no automation tying the two together.
