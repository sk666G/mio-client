# PingBypass companion — stage 1

This is a new, independent connectivity diagnostics implementation inspired by the supplied PingBypass 1.20.4 project's separate client/server architecture (3arthqu4ke, MIT, archive revision 159065494e9f31a970c1792b95fcf56035fa86f5). No upstream source code is copied. The upstream README explicitly states that the archive has no actual PingBypass functionality yet.

## Current functionality

The Client-category PingBypass module samples an authenticated HTTPS health endpoint using the JDK HTTP client. It permits one pending sample, with a configurable interval (5–60 seconds), four-second request timeout, default certificate validation, no redirects and no stored tokens. Disable cancels pending work and shuts down the HTTP client. The displayed time measures the HTTPS request (potentially including DNS, connection establishment, TLS and server response), not Minecraft ping or pure network RTT.

## Companion setup

Run companion/health.py using Python 3 on a host you control. Generate a random token using a cryptographically secure generator (at least 32 URL-safe characters). Set SCHIZO_COMPANION_TOKEN in the companion's protected process environment. The service listens on 127.0.0.1:8765 only.

Use an operator-managed TLS reverse proxy with a trusted certificate to expose only the HEAD /health endpoint, with rate limits. Do not expose the raw loopback HTTP service. Do not record Authorization headers. The reverse proxy must forward the Authorization header and must not cache the health response.

Set SCHIZO_COMPANION_URL to the exact HTTPS /health endpoint and SCHIZO_COMPANION_TOKEN to the same token in the environment of the Minecraft launcher before launching Schizo. Enable PingBypass in the Client category. No endpoint or credential is stored in module configuration. Restart the launcher to change environment values.

## Scope and limitations

This is not a working remote gameplay client. No Minecraft login, world synchronization, packet relay, gameplay execution, spoofing, account authentication, anti-cheat evasion or server-side latency modification is implemented. No VPS is provisioned. Use only endpoints you control. The HTTPS measurement is not a claimed reduction in game latency. Deployment and actual TLS connectivity require operator-side verification.
