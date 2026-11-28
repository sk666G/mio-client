package dev.schizo.client.modules.hud;

import dev.schizo.client.media.MediaSnapshot;
import java.util.Objects;

/** Pure display formatting for one immutable media snapshot. */
final class MediaDisplayText {
   private MediaDisplayText() {
   }

   static String format(MediaSnapshot snapshot) {
      MediaSnapshot state = Objects.requireNonNull(snapshot, "snapshot");
      return state.artist() + " - " + state.title();
   }
}
