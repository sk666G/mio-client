package dev.schizo.client.utils.inventory;

/** Policy for the main hand. Offhand food can be used while the main hand protects a totem. */
public final class InventoryActionPriority {
   private InventoryActionPriority() {}

   public static boolean protectMainhand(boolean enabled, boolean autoSelect, boolean hasTotem,
                                         boolean predictedLethal, double health, double threshold) {
      return enabled && autoSelect && hasTotem && (predictedLethal || health <= threshold);
   }

   public static boolean pauseFood(boolean protectedMainhand, boolean offhandFood) {
      return protectedMainhand && !offhandFood;
   }
}
