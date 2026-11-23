/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {
   @Accessor("environmentAttributes")
   @Mutable
   void schizo$setEnvironmentAttributes(EnvironmentAttributeSystem attributes);

   @Invoker("addEnvironmentAttributeLayers")
   EnvironmentAttributeSystem.Builder schizo$addEnvironmentAttributeLayers(EnvironmentAttributeSystem.Builder builder);

   @Accessor("blockStatePredictionHandler")
   BlockStatePredictionHandler getPendingUpdateManager();
}
