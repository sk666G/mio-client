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

    static {
        MEDIA.start();
        Runtime.getRuntime().addShutdownHook(new Thread(MEDIA::close, "schizo-media-stop"));
    }

    private MediaSessionBridge() {}

    private static void update(MediaSnapshot state) {
        latestSnapshot = state;
        artist = state.artist();
        title = state.title();
        positionMillis = state.positionMillis();
        durationMillis = state.durationMillis();
        playbackStatus = state.playbackStatus();
    }

    public static MediaSnapshot snapshot() {
        return latestSnapshot;
    }

    public static String getDisplayText() {
        return getDisplayText(latestSnapshot);
    }

    static String getDisplayText(MediaSnapshot snapshot) {
        return MediaDisplayText.format(snapshot);
    }

    public static void stopMediaUpdates() {
        MEDIA.close();
    }
}
