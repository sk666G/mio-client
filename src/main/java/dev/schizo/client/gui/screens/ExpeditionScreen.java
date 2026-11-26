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
   private List<Row> profileSnapshot;
   private EditBox search;
   private String query = "";
   private int tab, offset, selected = -1;
   private int left, panelWidth, visibleRows;
   private int rowsTop, rowsBottom, statusY, titleY;
   private boolean localOnly = true;
   private boolean showCoordinates;
   private boolean privacyAtRefresh;
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
      ExpeditionLayout layout = ExpeditionLayout.forViewportHeight(this.height);
      this.visibleRows = layout.visibleRows();
      this.rowsTop = layout.rowsTop();
      this.rowsBottom = layout.rowsBottom();
      this.statusY = layout.statusY();
      this.titleY = layout.titleY();

      this.search = new EditBox(this.font, this.left, layout.searchY(), this.panelWidth, 18, Component.literal("Filter"));
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
         this.button(this.fit(TABS[index], width - 6), this.left + index * (width + 2), layout.tabsY(), width, () -> {
            this.tab = target;
            this.profileSnapshot = null;
            this.offset = 0;
            this.selected = -1;
            this.rebuildWidgets();
         });
      }

      int half = Math.max(1, (this.panelWidth - 2) / 2);
      if (layout.showSecondaryControls()) {
         this.button(this.tab == 0 ? "Server filter" : "Refresh view", this.left, layout.secondaryY(), half, () -> {
            if (this.tab == 0) this.localOnly = !this.localOnly;
            this.profileSnapshot = null;
            this.offset = 0;
            this.selected = -1;
            this.refresh();
         });
         this.button("Show / hide coordinates", this.left + half + 2, layout.secondaryY(), half, () -> {
            this.showCoordinates = !this.showCoordinates;
            this.refresh();
         });
      }

      this.copyButton = this.button("Copy selected", this.left, layout.actionY(), half, this::copySelected);
      this.waypointButton = this.button("Use as waypoint", this.left + half + 2, layout.actionY(), half, this::createWaypoint);
      this.copyButton.visible = layout.showActions();
      this.waypointButton.visible = layout.showActions();

      if (layout.showPagination()) {
         this.button("Previous", this.left, layout.pageY(), half, () -> this.movePage(-1));
         this.button("Next", this.left + half + 2, layout.pageY(), half, () -> this.movePage(1));
      }

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
      this.privacyAtRefresh = dev.schizo.client.utils.CoordinatePrivacy.isEnabled();
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
      var chunks = ClientContext.getModule(dev.schizo.client.modules.exploit.NewChunksModule.class);
      if (chunks != null && this.minecraft.level != null) {
         for (var chunk : chunks.getVisibleChunks()) {
            entries.add(new DiscoveryEntry("Chunk", "NewChunks " + chunk.kind().getName(), this.server(), chunk.dimension().getName(),
               chunk.chunkPosition().getMiddleBlockX(), null, chunk.chunkPosition().getMiddleBlockZ(), 0, 0));
         }
      }
      entries.sort(Comparator.comparing(DiscoveryEntry::server, String.CASE_INSENSITIVE_ORDER)
         .thenComparing(DiscoveryEntry::dimension).thenComparing(DiscoveryEntry::name));
      for (DiscoveryEntry entry : entries) {
         if (!entry.matches(this.query, this.server(), this.localOnly)) continue;
         String observed = entry.lastSeen() == 0 ? "" : " | last " + java.time.Instant.ofEpochMilli(entry.lastSeen());
         String location = this.showCoordinates && !dev.schizo.client.utils.CoordinatePrivacy.isEnabled() ? entry.coordinates() : "Coordinates hidden";
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
      if (this.profileSnapshot != null) {
         rows.addAll(this.profileSnapshot);
         return;
      }
      var profiles = ClientContext.contextProfileRegistry;
      rows.add(new Row(profiles.isEnabled() ? "Context profiles enabled" : "Context profiles disabled",
         "Rules in application order. JSON differences compare each preset with current state.", null));
      for (var rule : profiles.previewCurrentRules()) {
         var preset = ClientContext.configurationManager.getPresetRegistry(rule.category()).getPreset(rule.presetName());
         rows.add(new Row(rule.category().getName() + " -> " + rule.presetName() + (preset == null ? " [missing]" : ""),
            rule.server() + " | " + rule.dimension() + " | " + rule.gameMode(), null));
         if (preset != null) {
            var differences = dev.schizo.client.config.JsonChangePreview.compare(rule.category().toJson(), preset.copyContents());
            for (String path : differences.paths()) rows.add(new Row(rule.category().getName() + " JSON field difference", path, null));
            if (differences.truncated()) rows.add(new Row("Additional differences omitted", "Preview is limited to 64 paths per category.", null));
         }
      }
      String failure = profiles.getFailedContextDescription();
      if (failure != null && !failure.isBlank()) rows.add(new Row("Profile recovery pending", failure, null));
      this.profileSnapshot = List.copyOf(rows);
   }

   private void copySelected() {
      if (this.selected < 0 || this.selected >= this.rows.size()) return;
      if (dev.schizo.client.utils.CoordinatePrivacy.isEnabled()) {
         this.message = "Disable CoordinatePrivacy before exporting data.";
         return;
      }
      Row row = this.rows.get(this.selected);
      String text = row.discovery() == null ? row.title() + "\n" + row.detail()
         : dev.schizo.client.ClientAccess.gson.toJson(row.discovery());
      this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
         if (confirmed && !dev.schizo.client.utils.CoordinatePrivacy.isEnabled()) this.minecraft.keyboardHandler.setClipboard(text);
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
      Waypoint waypoint = ClientContext.waypointRegistry.registerDiscoveryWaypoint(new Waypoint(
         entry.name(), entry.x(), y, entry.z(), WorldUtils.getDimension().getName(), entry.server()));
      this.destination = entry;
      this.message = waypoint == null ? "Waypoint could not be added." : "Waypoint selected: " + waypoint.getName()
         + (entry.y() == null ? ". Stashes use your current altitude." : "");
      this.selected = -1;
      this.refresh();
   }

   @Override
   public void tick() {
      if ((this.tab == 0 || this.tab == 1)
         && System.nanoTime() - this.refreshedAt >= 500_000_000L) {
         this.refresh();
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() == 0 && event.x() >= this.left && event.x() < this.left + this.panelWidth) {
         int index = (int)((event.y() - this.rowsTop) / 34);
         if (event.y() >= this.rowsTop && event.y() < this.rowsBottom
            && index >= 0 && index < this.visibleRows && this.offset + index < this.rows.size()) {
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
      if (x >= this.left && x < this.left + this.panelWidth
         && y >= this.rowsTop && y < this.rowsBottom && vertical != 0) {
         this.movePage(vertical < 0 ? 1 : -1);
         return true;
      }
      return super.mouseScrolled(x, y, horizontal, vertical);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.privacyAtRefresh != dev.schizo.client.utils.CoordinatePrivacy.isEnabled()) this.refresh();
      graphics.fill(0, 0, this.width, this.height, 0xee080c12);
      graphics.drawString(this.font, this.fit("Schizo Expedition / " + TABS[this.tab], this.panelWidth), this.left, this.titleY, 0xffa4d5ff);
      int visible = Math.min(this.visibleRows, this.rows.size() - this.offset);
      for (int i = 0; i < visible; i++) {
         int index = this.offset + i, y = this.rowsTop + i * 34;
         Row row = this.rows.get(index);
         graphics.fill(this.left, y, this.left + this.panelWidth, y + 32, index == this.selected ? 0xff25435e : 0xff131e2a);
         graphics.drawString(this.font, this.fit(row.title(), this.panelWidth - 8), this.left + 4, y + 4, 0xffffffff);
         graphics.drawString(this.font, this.fit(row.detail(), this.panelWidth - 8), this.left + 4, y + 17, 0xffacb9c9);
      }
      String status = this.message.isEmpty() ? (this.rows.isEmpty() ? "No matching entries" : this.rows.size() + " entries") : this.message;
      status += this.localOnly ? " | current server" : " | all servers";
      graphics.drawString(this.font, this.fit(status, this.panelWidth), this.left, this.statusY, 0xffacb9c9);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() { return false; }
}
