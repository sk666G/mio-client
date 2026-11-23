/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import java.util.function.Supplier;
import net.minecraft.network.chat.Style;

public interface DynamicColorStyle {
   Style schizo$withColor(Supplier colorSupplier);
}
