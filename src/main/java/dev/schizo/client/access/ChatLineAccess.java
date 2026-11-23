/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.access;

import dev.schizo.client.utils.ClampedInterpolatedFloat;

import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;

public interface ChatLineAccess {
   void setContent(FormattedCharSequence content);

   void setSignature(MessageSignature signature);

   void schizo$setAddTime(long addTime);

   long schizo$getAddTime();

   ClampedInterpolatedFloat getProgress();

   MessageSignature getSignature();

   FormattedCharSequence getContent();
}
