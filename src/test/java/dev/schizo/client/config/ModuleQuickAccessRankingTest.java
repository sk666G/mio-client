package dev.schizo.client.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleQuickAccessRankingTest {
   @Test
   void favoritesRankBeforeRecentAndGeneralModules() {
      var favorites = List.of("AutoCrystal", "Surround");
      var recent = List.of("Speed", "AutoCrystal", "Flight");

      assertTrue(
         ModuleQuickAccessRanking.rank("AutoCrystal", favorites, recent)
            .compareTo(ModuleQuickAccessRanking.rank("Speed", favorites, recent)) < 0
      );
      assertTrue(
         ModuleQuickAccessRanking.rank("Speed", favorites, recent)
            .compareTo(ModuleQuickAccessRanking.rank("Scaffold", favorites, recent)) < 0
      );
   }

   @Test
   void preservesFavoriteAndRecentOrdering() {
      var favorites = List.of("Surround", "AutoCrystal");
      var recent = List.of("Flight", "Speed");

      assertTrue(
         ModuleQuickAccessRanking.rank("Surround", favorites, recent)
            .compareTo(ModuleQuickAccessRanking.rank("AutoCrystal", favorites, recent)) < 0
      );
      assertTrue(
         ModuleQuickAccessRanking.rank("Flight", favorites, recent)
            .compareTo(ModuleQuickAccessRanking.rank("Speed", favorites, recent)) < 0
      );
   }

   @Test
   void snapshotMatchesDirectRankingAndHandlesUnknownModules() {
      var favorites = List.of("Surround", "AutoCrystal");
      var recent = List.of("Flight", "Speed", "AutoCrystal");
      var snapshot = ModuleQuickAccessRanking.snapshot(favorites, recent);

      assertTrue(snapshot.rank("Surround").compareTo(snapshot.rank("Flight")) < 0);
      assertTrue(snapshot.rank("Flight").compareTo(snapshot.rank("Unknown")) < 0);
      assertTrue(snapshot.rank("AutoCrystal").compareTo(snapshot.rank("Flight")) < 0);
   }

   @Test
   void favoritesTakePrecedenceOverRecentMembership() {
      var favorites = List.of("Surround");
      var recent = List.of("Flight", "Surround");

      assertTrue(
         ModuleQuickAccessRanking.rank("Surround", favorites, recent)
            .compareTo(ModuleQuickAccessRanking.rank("Flight", favorites, recent)) < 0
      );
   }
}
