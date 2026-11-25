package dev.schizo.client.gui.screens;

import dev.schizo.client.ClientContext;
import dev.schizo.client.integrations.ModuleCompatibility;
import dev.schizo.client.managers.TravelMonitor;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.misc.StashEntry;
import dev.schizo.client.utils.world.Waypoint;
import dev.schizo.client.utils.world.WorldUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Client-thread snapshots and explicit actions over existing discovery/profile registries. */
public final class ExpeditionScreen extends Screen {
   private static final String[] TABS = {"Discoveries", "Travel", "Profiles", "Compatibility"};
   private record Row(String title, String detail, DiscoveryEntry discovery) {}
   private List<Row> rows = List.of();
   private EditBox search;
   private String query = "";
   private int tab, offset, selected = -1;
   private int left, panelWidth, visibleRows;
   private boolean localOnly = true;
   private boolean showCoordinates;
   private long refreshedAt;
   private DiscoveryEntry destination;
   private String message = "";
   private Button copyButton, waypointButton;

   public ExpeditionScreen() {
      super(Component.literal("Schizo Expedition"));
   }

   @Override
   protected void init() {
      this.panelWidth = Math.max(1, Math.min(720, this.width - 16));
      this.left = (this.width - this.panelWidth) / 2;
      this.visibleRows = Math.max(0, (this.height - 164) / 34);
      this.search = new EditBox(this.font, this.left, 27, this.panelWidth, 18, Component.literal("Filter"));
      this.search.setMaxLength(128);
      this.search.setValue(this.query);
      this.search.setResponder(value -> {
         this.query = value;
         this.offset = 0;
         this.selected = -1;
         this.refresh();
      });
      this.addRenderableWidget(this.search);
      int width = Math.max(1, (this.panelWidth - 6) / TABS.length);
      for (int index = 0; index < TABS.length; index++) {
         int target = index;
         this.button(this.fit(TABS[index], width - 6), this.left + index * (width + 2), 49, width, () -> {
            this.tab = target;
            this.offset = 0;
            this.selected = -1;
            this.refresh();
         });
      }
      int half = Math.max(1, (this.panelWidth - 2) / 2);
      this.button("Server filter", this.left, 72, half, () -> {
         this.localOnly = !this.localOnly;
         this.offset = 0;
         this.selected = -1;
         this.refresh();
      });
      this.button("Show / hide coordinates", this.left + half + 2, 72, half, () -> {
         this.showCoordinates = !this.showCoordinates;
         this.refresh();
      });
      this.copyButton = this.button("Copy selected", this.left, this.height - 49, half, this::copySelected);
      this.waypointButton = this.button("Use as waypoint", this.left + half + 2, this.height - 49, half, this::createWaypoint);
      this.button("Previous", this.left, this.height - 26, half, () -> this.movePage(-1));
      this.button("Next", this.left + half + 2, this.height - 26, half, () -> this.movePage(1));
      this.refresh();
      this.setInitialFocus(this.search);
   }

   private Button button(String title, int x, int y, int width, Runnable action) {
      return this.addRenderableWidget(Button.builder(Component.literal(title), button -> action.run())
         .bounds(x, y, width, 20).build());
   }

   private String server() {
      var connection = this.minecraft.getConnection();
      return connection == null || connection.getServerData() == null ? "singleplayer" : connection.getServerData().ip;
   }

   private String fit(String value, int width) {
      return this.font.plainSubstrByWidth(value, Math.max(0, width));
   }

   private void movePage(int direction) {
      this.offset = Math.max(0, Math.min(Math.max(0, this.rows.size() - this.visibleRows),
         this.offset + direction * Math.max(1, this.visibleRows)));
   }

   private void refresh() {
      Row previousSelection = this.selected >= 0 && this.selected < this.rows.size() ? this.rows.get(this.selected) : null;
      this.refreshedAt = System.nanoTime();
      List<Row> result = new ArrayList<>();
      switch (this.tab) {
         case 0 -> this.discoveries(result);
         case 1 -> this.travel(result);
         case 2 -> this.profiles(result);
         case 3 -> {
            for (Object item : ClientContext.moduleRegistry.getRegistry()) {
               Module module = (Module)item;
               var assessment = ModuleCompatibility.assess(module.getClass());
               result.add(new Row(module.getName() + " - " + assessment.classification(), assessment.reason(), null));
            }
         }
         default -> throw new IllegalStateException("Unknown expedition tab");
      }
      if (this.tab != 0) {
         String normalized = this.query.toLowerCase(Locale.ROOT);
         result.removeIf(row -> !(row.title() + " " + row.detail()).toLowerCase(Locale.ROOT).contains(normalized));
      }
      this.rows = List.copyOf(result);
      this.offset = Math.min(this.offset, Math.max(0, this.rows.size() - this.visibleRows));
      this.selected = previousSelection == null ? -1 : this.rows.indexOf(previousSelection);
      this.copyButton.active = this.selected >= 0;
      this.waypointButton.active = this.selected >= 0 && this.rows.get(this.selected).discovery() != null;
   }

   private void discoveries(List<Row> rows) {
      List<DiscoveryEntry> entries = new ArrayList<>();
      for (Object value : ClientContext.stashRegistry.getRegistry()) {
         StashEntry stash = (StashEntry)value;
         if (stash == null || !stash.isPersistable()) continue;
         entries.add(new DiscoveryEntry("Stash", stash.getName(), stash.getServer(), stash.getDimension(),
            stash.getX(), null, stash.getZ(), stash.firstSeenMillis(), stash.lastSeenMillis()));
      }
      for (Object value : ClientContext.waypointRegistry.getRegistry()) {
         Waypoint waypoint = (Waypoint)value;
         if (waypoint == null || waypoint.getName() == null || waypoint.getServer() == null || waypoint.getDimension() == null) continue;
         entries.add(new DiscoveryEntry("Waypoint", waypoint.getName(), waypoint.getServer(), waypoint.getDimension(),
            waypoint.getX(), waypoint.getY(), waypoint.getZ(), 0, 0));
      }
      entries.sort(Comparator.comparing(DiscoveryEntry::server, String.CASE_INSENSITIVE_ORDER)
         .thenComparing(DiscoveryEntry::dimension).thenComparing(DiscoveryEntry::name));
      for (DiscoveryEntry entry : entries) {
         if (!entry.matches(this.query, this.server(), this.localOnly)) continue;
         String observed = entry.lastSeen() == 0 ? "" : " | last " + java.time.Instant.ofEpochMilli(entry.lastSeen());
         String location = this.showCoordinates ? entry.coordinates() : "Coordinates hidden";
         rows.add(new Row(entry.kind() + " - " + entry.name(), location + " | " + entry.server() + " | " + entry.dimension() + observed, entry));
      }
   }

   private void travel(List<Row> rows) {
      var player = this.minecraft.player;
      if (player == null || this.minecraft.level == null) {
         rows.add(new Row("Join a world to view travel resources", "Travel resets on player or dimension replacement.", null));
         return;
      }
      var session = TravelMonitor.SESSION.snapshot(System.nanoTime());
      rows.add(new Row(String.format(Locale.ROOT, "Distance %.1f blocks | duration %.0f s", session.distance(), session.seconds()),
         "Discontinuities over 64 blocks per sample excluded: " + session.discontinuities(), null));
      int rockets = 0, food = 0;
      for (var stack : player.getInventory().getNonEquipmentItems()) {
         if (stack.is(Items.FIREWORK_ROCKET)) rockets += stack.getCount();
         if (stack.has(DataComponents.FOOD)) food += stack.getCount();
      }
      var chest = player.getItemBySlot(EquipmentSlot.CHEST);
      rows.add(new Row("Rockets " + rockets + " | food items " + food + " | hunger " + player.getFoodData().getFoodLevel(),
         chest.is(Items.ELYTRA) ? "Elytra durability " + (chest.getMaxDamage() - chest.getDamageValue()) + "/" + chest.getMaxDamage() : "No elytra equipped", null));
      if (this.destination != null && this.destination.server().equalsIgnoreCase(this.server())
         && DiscoveryEntry.canonicalDimension(this.destination.dimension()).equals(WorldUtils.getDimension().getName())) {
         double distance = Math.hypot(player.getX() - this.destination.x(), player.getZ() - this.destination.z());
         double speed = session.seconds() > 0 ? session.distance() / session.seconds() : 0;
         String eta = speed > 0.1 ? String.format(Locale.ROOT, "Estimate %.0f s at session average speed", distance / speed) : "Move to estimate arrival time";
         rows.add(new Row(String.format(Locale.ROOT, "Destination %s | horizontal distance %.1f", this.destination.name(), distance), eta, null));
      }
      for (Object item : ClientContext.moduleRegistry.getRegistry()) {
         Module module = (Module)item;
         if (module.isToggled() && List.of("AutoWalk", "ElytraFly", "AutoEat", "Fireworks", "Baritone").contains(module.getName())) {
            rows.add(new Row(module.getName(), module.getRuntimeStatus(), null));
         }
      }
   }

   private void profiles(List<Row> rows) {
      var profiles = ClientContext.contextProfileRegistry;
      rows.add(new Row(profiles.isEnabled() ? "Context profiles enabled" : "Context profiles disabled",
         "Preview of matching rules in application order. No settings are applied by this view.", null));
      for (var rule : profiles.previewCurrentRules()) {
         var preset = ClientContext.configurationManager.getPresetRegistry(rule.category()).getPreset(rule.presetName());
         rows.add(new Row(rule.category().getName() + " -> " + rule.presetName() + (preset == null ? " [missing]" : ""),
            rule.server() + " | " + rule.dimension() + " | " + rule.gameMode(), null));
      }
      String failure = profiles.getFailedContextDescription();
      if (failure != null && !failure.isBlank()) rows.add(new Row("Profile recovery pending", failure, null));
   }

   private void copySelected() {
      if (this.selected < 0 || this.selected >= this.rows.size()) return;
      Row row = this.rows.get(this.selected);
      String text = row.discovery() == null ? row.title() + "\n" + row.detail()
         : dev.schizo.client.ClientAccess.gson.toJson(row.discovery());
      this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
         if (confirmed) this.minecraft.keyboardHandler.setClipboard(text);
         this.minecraft.setScreen(this);
      }, Component.literal("Copy selected data?"), Component.literal("Discovery exports include exact coordinates and server identity.")));
   }

   private void createWaypoint() {
      if (this.selected < 0 || this.selected >= this.rows.size()) return;
      DiscoveryEntry entry = this.rows.get(this.selected).discovery();
      var player = this.minecraft.player;
      if (entry == null || player == null || !entry.server().equalsIgnoreCase(this.server())
         || !DiscoveryEntry.canonicalDimension(entry.dimension()).equals(WorldUtils.getDimension().getName())) {
         this.message = "Join the discovery's server and dimension first.";
         return;
      }
      double y = entry.y() == null ? player.getY() : entry.y();
      boolean added = ClientContext.waypointRegistry.registerWaypoint(new Waypoint(
         entry.name(), entry.x(), y, entry.z(), WorldUtils.getDimension().getName(), entry.server()));
      this.destination = entry;
      this.message = added ? "Waypoint added. Stashes use your current altitude." : "Waypoint already exists. Travel destination selected.";
      this.selected = -1;
      this.refresh();
   }

   @Override
   public void tick() {
      if (System.nanoTime() - this.refreshedAt >= 500_000_000L) this.refresh();
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() == 0 && event.x() >= this.left && event.x() < this.left + this.panelWidth) {
         int index = (int)((event.y() - 100) / 34);
         if (event.y() >= 100 && index < this.visibleRows && this.offset + index < this.rows.size()) {
            this.selected = this.offset + index;
            this.copyButton.active = true;
            this.waypointButton.active = this.rows.get(this.selected).discovery() != null;
            return true;
         }
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.search.isFocused()) return super.keyPressed(event);
      if (event.key() == GLFW.GLFW_KEY_PAGE_DOWN || event.key() == GLFW.GLFW_KEY_PAGE_UP) {
         this.movePage(event.key() == GLFW.GLFW_KEY_PAGE_DOWN ? 1 : -1);
         return true;
      }
      return super.keyPressed(event);
   }

   @Override
   public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      if (x >= this.left && x < this.left + this.panelWidth && y >= 100 && y < this.height - 60 && vertical != 0) {
         this.movePage(vertical < 0 ? 1 : -1);
         return true;
      }
      return super.mouseScrolled(x, y, horizontal, vertical);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, this.width, this.height, 0xee080c12);
      graphics.drawString(this.font, this.fit("Schizo Expedition / " + TABS[this.tab], this.panelWidth), this.left, 10, 0xffa4d5ff);
      int visible = Math.min(this.visibleRows, this.rows.size() - this.offset);
      for (int i = 0; i < visible; i++) {
         int index = this.offset + i, y = 100 + i * 34;
         Row row = this.rows.get(index);
         graphics.fill(this.left, y, this.left + this.panelWidth, y + 32, index == this.selected ? 0xff25435e : 0xff131e2a);
         graphics.drawString(this.font, this.fit(row.title(), this.panelWidth - 8), this.left + 4, y + 4, 0xffffffff);
         graphics.drawString(this.font, this.fit(row.detail(), this.panelWidth - 8), this.left + 4, y + 17, 0xffacb9c9);
      }
      String status = this.message.isEmpty() ? (this.rows.isEmpty() ? "No matching entries" : this.rows.size() + " entries") : this.message;
      status += this.localOnly ? " | current server" : " | all servers";
      graphics.drawString(this.font, this.fit(status, this.panelWidth), this.left, this.height - 61, 0xffacb9c9);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() { return false; }
}
