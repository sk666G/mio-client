package dev.schizo.client.config;

import dev.schizo.client.managers.ContextProfileRegistry;
import dev.schizo.client.managers.ContextProfileRegistry.Rule;
import dev.schizo.client.settings.options.PresetCategory;
import java.util.ArrayList;
import java.util.List;

/** Pure rule-list simulation. Never loads or applies a preset. */
public final class ContextRulePreview {
   private ContextRulePreview() {}

   public static boolean sameKey(Rule a, Rule b) {
      return a.category() == b.category() && a.server().equals(b.server())
         && a.dimension().equals(b.dimension()) && a.gameMode().equals(b.gameMode());
   }

   public static Result simulate(List<Rule> original, Rule proposed) {
      List<Rule> edited = new ArrayList<>(original);
      int replace = -1;
      for (int i = 0; i < edited.size(); i++) {
         if (sameKey(edited.get(i), proposed)) { replace = i; break; }
      }
      if (replace >= 0) edited.set(replace, proposed);
      else {
         if (edited.size() >= ContextProfileRegistry.MAX_RULES) throw new IllegalStateException("Rule limit reached");
         edited.add(proposed);
      }
      // Concrete representative context, not a claim about every wildcard match.
      String server = proposed.server().equals("*") ? "singleplayer" : proposed.server();
      String dimension = proposed.dimension().equals("any") ? "overworld" : proposed.dimension();
      String mode = proposed.gameMode().equals("any") ? "survival" : proposed.gameMode();
      List<Rule> before = ContextProfileRegistry.selectApplicationRules(original, server, dimension, mode);
      List<Rule> after = ContextProfileRegistry.selectApplicationRules(edited, server, dimension, mode);
      List<Rule> matching = ContextProfileRegistry.selectMatchingRules(edited, server, dimension, mode);
      boolean overlap = after.stream().anyMatch(r -> r.category() == PresetCategory.ALL)
         && after.stream().anyMatch(r -> r.category() != PresetCategory.ALL && r.category() != PresetCategory.MACRO);
      return new Result(List.copyOf(original), proposed, server, dimension, mode, replace >= 0,
         before, after, matching.size() - after.size(), overlap);
   }

   public record Result(List<Rule> original, Rule proposed, String server, String dimension, String gameMode,
                        boolean replaces, List<Rule> before, List<Rule> after,
                        int shadowed, boolean allOverlap) {}
}
