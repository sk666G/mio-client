package dev.schizo.mixin;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only selection endpoint for Schizo's custom text rendering. */
@Mixin(EditBox.class)
public interface EditBoxSelectionAccessor {
   @Accessor("highlightPos")
   int schizo$getSelectionAnchor();
}
