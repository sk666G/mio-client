/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.integrations;

public interface ProtocolVersionAccess {
   boolean olderEq_1_20_6();

   boolean olderEq_1_21_4();

   boolean olderEq_1_21_5();
}
