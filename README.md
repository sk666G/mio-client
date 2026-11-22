# Mio Client — Static Analysis

Static security review of the Mio Client Minecraft mod. This repository documents observations from **static analysis only**; the client was not executed.

![Mio Client logo](https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ5F1K8mGGc_9b82X-zjlDGGnt0bjuYV-ChsA&s)

![Additional image](https://hackvshack.net/attachments/1757260471994-png.11478/)

## Files

- `final-assessment.txt` — concise assessment and SHA-256 hashes.
- Original JARs are not included in this initial commit.

## SHA-256

| File | SHA-256 |
|---|---|
| `mio-loader.jar` | `e728ab4e52c25dbcbeac1b0f095c98333daf2c65e1c92fc7f6987b16aba1c7c4` |
| `mio.jar` | `4eb2b23917e2db37cfa990245544c349bfc6301c0fa607347c9442767539c38d` |

These hashes matched the GitHub API release digests for Mio Client release `mio2.2.1` (published 2025-12-19).

## Key observations

- Custom classloader, reflection, and use of `sun.misc.Unsafe`.
- Country lookup via `https://api.country.is/`.
- Screenshot upload functionality using Imgur; behavior is mode/state-dependent.
- TCP connection probe to the current Minecraft server for latency measurement.
- Explorer file-reveal behavior.
- Reconstructed-source references to optional Discord webhook notifications and other external APIs require bytecode corroboration.

## Assessment

**Verdict: untrusted; not certified safe.** No definitive credential-theft or destructive-malware behavior was established in this review. Obfuscation and conditional behavior limit confidence; absence of a finding is not proof of absence.

## Scope and limitations

This is a static-analysis report, not a full audit. Some observations come from reconstructed third-party source and are explicitly distinguished from findings confirmed in the original bytecode. Do not treat this assessment as a guarantee of safety.

## Attribution and redistribution

Mio Client is third-party software. This repository's analysis does not imply ownership or endorsement. Obtain permission before redistributing the original JARs or third-party images.
