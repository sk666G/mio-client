package dev.schizo.client.integrations;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class MultiplayerCompatibilityPolicyTest {
   @Test
   void warningOnlyIsTheDefaultAndDoesNotBlockReviewedOrUnreviewedModules() {
      MultiplayerCompatibilityPolicy policy = new MultiplayerCompatibilityPolicy();

      assertEquals(MultiplayerCompatibilityPolicy.Mode.WARN_ONLY, policy.getMode());
      assertFalse(policy.blocksWhenConservative(NoFallModule.class));
      assertFalse(policy.blocksWhenConservative(UnreviewedModule.class));
   }

   @Test
   void conservativeModeBlocksServerSensitivePacketAffectingAndUnverified() {
      MultiplayerCompatibilityPolicy policy = new MultiplayerCompatibilityPolicy();
      policy.setMode(MultiplayerCompatibilityPolicy.Mode.CONSERVATIVE);

      assertTrue(policy.blocksWhenConservative(ReachModule.class));
      assertTrue(policy.blocksWhenConservative(NoFallModule.class));
      assertTrue(policy.blocksWhenConservative(UnreviewedModule.class));
   }

   @Test
   void conservativeModeAllowsClientVisualInputAndWorldInteractionClasses() {
      MultiplayerCompatibilityPolicy policy = new MultiplayerCompatibilityPolicy();
      policy.setMode(MultiplayerCompatibilityPolicy.Mode.CONSERVATIVE);

      assertFalse(policy.blocksWhenConservative(SettingsModule.class));
      assertFalse(policy.blocksWhenConservative(FastClimbModule.class));
      assertFalse(policy.blocksWhenConservative(FastPlaceModule.class));
      assertFalse(policy.blocksWhenConservative(null));
   }

   @Test
   void explicitModuleExceptionPermitsAClassUntilTheExceptionIsRemoved() {
      MultiplayerCompatibilityPolicy policy = new MultiplayerCompatibilityPolicy();
      policy.setMode(MultiplayerCompatibilityPolicy.Mode.CONSERVATIVE);

      assertTrue(policy.blocksWhenConservative(ReachModule.class));
      assertTrue(policy.allowModule("ReachModule"));
      assertFalse(policy.allowModule("ReachModule"));
      assertFalse(policy.blocksWhenConservative(ReachModule.class));
      assertTrue(policy.removeAllowedModule("ReachModule"));
      assertTrue(policy.blocksWhenConservative(ReachModule.class));
   }

   @Test
   void persistsPolicyModeAndExceptions() {
      MultiplayerCompatibilityPolicy source = new MultiplayerCompatibilityPolicy();
      source.setMode(MultiplayerCompatibilityPolicy.Mode.CONSERVATIVE);
      source.allowModule("ReachModule");

      MultiplayerCompatibilityPolicy restored = new MultiplayerCompatibilityPolicy();
      restored.fromJson(source.toJson());

      assertEquals(MultiplayerCompatibilityPolicy.Mode.CONSERVATIVE, restored.getMode());
      assertEquals(java.util.List.of("ReachModule"), restored.getAllowedModules());
      assertFalse(restored.blocksWhenConservative(ReachModule.class));
   }

   @Test
   void malformedConfigurationDoesNotPartiallyReplaceCurrentPolicy() {
      MultiplayerCompatibilityPolicy policy = new MultiplayerCompatibilityPolicy();
      policy.setMode(MultiplayerCompatibilityPolicy.Mode.CONSERVATIVE);
      policy.allowModule("ReachModule");

      policy.fromJson(JsonParser.parseString("""
         {"version":1,"mode":"warn_only","allowedModules":["valid","../bad"]}
         """));

      assertEquals(MultiplayerCompatibilityPolicy.Mode.CONSERVATIVE, policy.getMode());
      assertEquals(java.util.List.of("ReachModule"), policy.getAllowedModules());
   }

   @Test
   void rejectsInvalidExceptionNamesAndEnforcesTheExceptionLimit() {
      MultiplayerCompatibilityPolicy policy = new MultiplayerCompatibilityPolicy();
      assertThrows(IllegalArgumentException.class, () -> policy.allowModule("../ReachModule"));
      assertThrows(IllegalArgumentException.class, () -> policy.allowModule("qualified.ReachModule"));
   }

   private static final class NoFallModule {}
   private static final class ReachModule {}
   private static final class SettingsModule {}
   private static final class FastClimbModule {}
   private static final class FastPlaceModule {}
   private static final class UnreviewedModule {}
}
