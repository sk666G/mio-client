/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.components;

import net.minecraft.resources.ResourceKey;

public record EnchantmentListEntry(
   ResourceKey enchantmentKey,
   int level,
   String name
) {}
