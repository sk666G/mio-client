/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.hud;

import dev.schizo.client.media.MediaSnapshot;
import dev.schizo.client.media.PortableMediaSessionProvider;


public final class MediaSessionBridge {
    public static volatile int playbackStatus;
    public static volatile String artist = "";
    public static volatile String title = "";
    public static volatile long positionMillis;
    public static volatile long durationMillis;
    private static volatile MediaSnapshot latestSnapshot = MediaSnapshot.empty();

    private static final PortableMediaSessionProvider MEDIA = new PortableMediaSessionProvider(MediaSessionBridge::update);
    private static boolean started;

    private MediaSessionBridge() {}

    private static synchronized void ensureStarted() {
        if (started) return;
        started = true;
        MEDIA.start();
        Runtime.getRuntime().addShutdownHook(new Thread(MEDIA::close, "schizo-media-stop"));
    }

    private static void update(MediaSnapshot state) {
        latestSnapshot = state;
        artist = state.artist();
        title = state.title();
        positionMillis = state.positionMillis();
        durationMillis = state.durationMillis();
        playbackStatus = state.playbackStatus();
    }

    public static MediaSnapshot snapshot() {
        ensureStarted();
        return latestSnapshot;
    }

    public static String getDisplayText() {
        ensureStarted();
        return getDisplayText(latestSnapshot);
    }

    static String getDisplayText(MediaSnapshot snapshot) {
        return MediaDisplayText.format(snapshot);
    }

    public static synchronized void stopMediaUpdates() {
        if (!started) return;
        MEDIA.close();
    }
}
