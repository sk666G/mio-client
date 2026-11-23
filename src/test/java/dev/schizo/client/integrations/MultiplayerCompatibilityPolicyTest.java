package dev.schizo.client.integrations;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import dev.schizo.client.modules.client.SettingsModule;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.modules.exploit.ReachModule;
import dev.schizo.client.modules.movement.FastClimbModule;
import dev.schizo.client.modules.movement.NoFallModule;
import dev.schizo.client.modules.player.FastPlaceModule;
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
   void leavesPermanentServiceModulesWarningOnlyUntilSettingsCanBeControlled() {
      MultiplayerCompatibilityPolicy policy = new MultiplayerCompatibilityPolicy();
      policy.setMode(MultiplayerCompatibilityPolicy.Mode.CONSERVATIVE);

      assertEquals(ModuleCompatibility.Classification.PACKET_AFFECTING,
         ModuleCompatibility.assess(AntiCheatModule.class).classification());
      assertFalse(policy.blocksWhenConservative(AntiCheatModule.class));
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

      for (int index = 0; index < MultiplayerCompatibilityPolicy.MAX_ALLOWED_MODULES; index++) {
         policy.allowModule("Module" + index);
      }
      assertThrows(IllegalStateException.class, () -> policy.allowModule("ModuleOverflow"));
   }

   private static final class UnreviewedModule {}
}
