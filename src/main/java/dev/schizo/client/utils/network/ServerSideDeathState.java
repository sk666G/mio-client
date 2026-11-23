/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.network;

public interface ServerSideDeathState {
   boolean isServerSideDead();

   void setServerSideDead(boolean serverSideDead);
}
