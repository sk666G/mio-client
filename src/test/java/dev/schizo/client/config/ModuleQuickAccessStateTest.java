package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleQuickAccessStateTest {
   @Test
   void togglesFavoritesAndPreservesInsertionOrder() {
      ModuleQuickAccessState state = new ModuleQuickAccessState();

      assertTrue(state.addFavorite("AutoCrystal"));
      assertTrue(state.addFavorite("Surround"));
      assertFalse(state.addFavorite("AutoCrystal"));
      assertEquals(List.of("AutoCrystal", "Surround"), state.getFavorites());

      assertFalse(state.toggleFavorite("AutoCrystal"));
      assertTrue(state.toggleFavorite("Speed"));
      assertEquals(List.of("Surround", "Speed"), state.getFavorites());
   }

   @Test
   void keepsRecentlyEnabledModulesNewestFirstAndMovesDuplicatesToFront() {
      ModuleQuickAccessState state = new ModuleQuickAccessState();
      state.recordRecentlyUsed("Speed");
      state.recordRecentlyUsed("Surround");
      state.recordRecentlyUsed("Speed");

      assertEquals(List.of("Speed", "Surround"), state.getRecentlyUsed());
      assertEquals(0, state.getRecentIndex("Speed"));
      assertEquals(1, state.getRecentIndex("Surround"));
   }

   @Test
   void enforcesFavoriteAndRecentCaps() {
      ModuleQuickAccessState state = new ModuleQuickAccessState();
      for (int index = 0; index < 140; index++) {
         state.addFavorite("Favorite" + index);
         state.recordRecentlyUsed("Module" + index);
      }

      assertEquals(ModuleQuickAccessState.MAX_FAVORITES, state.getFavorites().size());
      assertEquals(ModuleQuickAccessState.MAX_RECENT, state.getRecentlyUsed().size());
      assertEquals(-1, state.getFavoriteIndex("Favorite0"));
      assertEquals(-1, state.getRecentIndex("Module0"));
      assertEquals("Favorite139", state.getFavorites().getLast());
      assertEquals("Module139", state.getRecentlyUsed().getFirst());
   }

   @Test
   void replacesStateWithBoundedAndValidatedCollections() {
      ModuleQuickAccessState state = new ModuleQuickAccessState();
      state.replace(
         List.of("A", "B", "A", "", "C"),
         List.of("RecentA", "RecentB", "RecentA", " ")
      );

      assertEquals(List.of("A", "B", "C"), state.getFavorites());
      assertEquals(List.of("RecentB", "RecentA"), state.getRecentlyUsed());
   }

   @Test
   void clearOperationsDoNotAffectTheOtherCollection() {
      ModuleQuickAccessState state = new ModuleQuickAccessState();
      state.addFavorite("Favorite");
      state.recordRecentlyUsed("Recent");

      state.clearFavorites();
      assertTrue(state.getFavorites().isEmpty());
      assertEquals(List.of("Recent"), state.getRecentlyUsed());

      state.clearRecentlyUsed();
      assertTrue(state.getRecentlyUsed().isEmpty());
   }
}
