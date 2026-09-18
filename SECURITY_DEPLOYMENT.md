# Security rollout (Oracle VM + frontend)

## What changed

- Browser authentication is cookie-only. No access/refresh token is returned in JSON or stored in localStorage. Legacy localStorage access tokens are removed on frontend load. Authorization / X-Refresh-Token headers are no longer accepted.
- Access JWT lifetime remains 15 minutes. Login creates a database-backed session with an absolute 7-day expiry; refresh rotation does NOT extend that session forever.
- Only hashes of refresh tokens and the user's password hash fingerprint are stored. Refresh tokens rotate under a database row lock. Reusing a rotated refresh token revokes the entire session. Logout revokes the session, including existing access tokens, for subsequent requests.
- A changed password hash or removed account invalidates existing sessions. Existing JWTs without a session ID are rejected: everyone signs in again after rollout.
- Cookie paths: access /api; refresh /api/auth. Both are HttpOnly; production requires Secure. Legacy root-path auth cookies are cleared on login/refresh/logout.
- GET /api/auth/csrf returns a token in JSON, with a matching HttpOnly XSRF-TOKEN cookie. All state-changing browser requests, including login/refresh/logout and multipart uploads, require X-XSRF-TOKEN. Only the existing POST Cloudinary webhook path is exempt; its Cloudinary signature verification is unchanged.
- Protected API routes require ROLE_OWNER. Both seeded owners still share all memories. Public routes remain GET-only and check public visibility. This is not a multi-tenant account model.
- Missing/expired authentication returns 401; insufficient permission and CSRF rejection return 403. Auth storage outages return 503 in the JWT filter, not a misleading expired-token error.
- Frontend restores /auth/me through refresh, serializes refresh requests, checks whether another tab already refreshed, and coordinates login/refresh/logout with Web Locks where available. BroadcastChannel notifies other same-origin tabs on logout/account switch. Browsers without Web Locks can still hit cross-tab refresh replay detection and must sign in again (fail closed).
- Auth transport has a 15-second timeout (does not impose this limit on video uploads). If the server rotates a refresh token but the response/cookie is lost, a later reuse of the old token revokes the session and requires signing in again; no insecure replay grace period is enabled.
- Logout/session expiry clears Redux and React Query caches. Late responses cannot restore the previous user's data. Transient startup failures show a retry screen.
- Login has a bounded in-process 20 attempts/minute/socket-peer limit. Behind Nginx this may be a shared proxy-IP limit for both owners; it deliberately does not trust client-supplied X-Forwarded-For. Add trusted-proxy/edge rate limiting for larger deployments. Restart resets this in-memory guard.

## Required deployment order

1. Back up the DB using your normal procedure. Apply ONLY V5__auth_sessions.sql to an existing database that already has V1-V4, through the normal migration process or Supabase SQL Editor. Do not rerun the initialization/seed scripts. Production currently skips the manual .env-based migration helper when using Docker --env-file, and spring.flyway.enabled is false, so do not assume this migration will run automatically.
2. If applying V5 manually, record it in your deployment procedure. Do not later enable Flyway or run its automatic repair/baseline against that DB without reconciling migration history. No production schema was changed by this code task.
3. Build and deploy the backend image containing this source and recreate its container with the existing .env and memory limits. Keep VIDEO_CALLBACK_BASE_URL and Cloudinary credentials unchanged. Preserve the configured JDBC URL and credentials; never put passwords in JDBC URLs or logs.
4. Deploy the matching frontend immediately in the same maintenance window. The previous frontend lacks CSRF headers and is not compatible with the new backend. Do not release only one side.
5. Reload the browser (close old tabs if needed) and sign in again. No frontend secret is required.

Production environment (example frontend origin; use the actual one):

```dotenv
COOKIE_SECURE=true
COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGINS=https://ourmem.koictu.id.vn
```

Use Lax when both frontend and backend are HTTPS subdomains of koictu.id.vn. Origins are still different, so CORS must explicitly allow the frontend origin.

If frontend is on *.vercel.app while backend is api.koictu.id.vn, set COOKIE_SAME_SITE=None and COOKIE_SECURE=true. This is cross-site: browsers that block third-party cookies may still prevent authentication. For reliable browser support, host the frontend on a subdomain of koictu.id.vn, or use a properly designed same-site API proxy. Do not restore localStorage tokens to work around cookie restrictions.

Local development ONLY, using localhost for both FE and BE:

```dotenv
COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Do not mix 127.0.0.1 and localhost. Do not set Secure=false on production. CORS accepts exact comma-separated origins, not wildcard origins with credentials.

## Verify before reopening uploads

- Login: response body contains user details, not JWTs. Cookies are HttpOnly and Secure on HTTPS.
- Reload after access expiry: /auth/me -> 401 -> /auth/csrf (if needed) -> /auth/refresh -> 200 -> /auth/me -> 200. Parallel expired requests share a refresh.
- Logout: next protected request is 401; the same old refresh token cannot obtain a new session. Another open tab clears private UI.
- Invalid/missing CSRF token: state-changing request fails with 403 CSRF_INVALID; trusted-origin frontend recovers a stale CSRF token once.
- Wrong password: 401 with generic message; repeated login attempts eventually return 429.
- Public session stays viewable as guest. Private session is not exposed through public endpoints.
- Cloudinary's valid signed callback works without JWT/CSRF; unsigned callbacks still return 403. Test an actual MOV upload through READY.
- Watch Oracle RAM/restarts and DB connectivity. These changes do not repair unrelated VM/network or provider failures.

Tests run locally use mocked persistence/network and a real Spring Security MockMvc filter chain. They do not prove PostgreSQL locking/DDL, production reverse-proxy headers, browser cookie acceptance or real Cloudinary callback delivery.

## Operations

- Periodically delete expired auth_sessions rows after the configured session lifetime, using an approved maintenance job. Missing rows fail authentication closed. No scheduler or production cleanup has been installed.
- Keep DB/JWT/Cloudinary secrets out of logs and source. Rotate any previously exposed database password.
- Retain the old image only for operational rollback, not as a permanent fallback: it lacks session revocation/CSRF. A backend-only rollback re-enables old security behavior and is not recommended; prefer roll forward.
- JWT and refresh revocation cannot cancel a request that was already authorized and executing when logout occurred.

References: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html and https://docs.spring.io/spring-security/reference/features/exploits/csrf.html
