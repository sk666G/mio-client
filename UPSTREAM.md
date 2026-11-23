# Upstream source and migration notes

- Project: https://git.spectrum.best/SpectrumDevs/open-zio
- Pinned source snapshot: 2fec97aa0c52860f9252b61543dce8fc9825e081, matching the commit comment on the supplied archive.
- Project description: an optimized recreation of Mio 2.4.4 for Minecraft 1.21.11.
- The supplied archive contains 1,199 Java files; 1,179 carry the upstream MIT-license header. The supplied archive has no separate root LICENSE file. Original headers are retained.
- Rebrand: dev.zio becomes dev.schizo; mod ID, resource namespace, artifact and Gradle root name become schizo; ZioClient becomes SchizoClient.
- The previous hand-built Schizo scaffold is replaced by the upstream application architecture.
- GrimDisablerModule.java and RaytraceBypassModule.java are retained in the active source tree with their original source notices.
- The upstream project URL remains intact in retained source headers and attribution.

This file supplements rather than replaces upstream notices.
