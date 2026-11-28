# GUI release acceptance — consolidated PR #301

## Candidate and evidence

- Source checkpoint for this document: `2d79cc697fa358862936a826a6ea15050a9b718a` (24 commits, 21 files relative to the verified baseline).
- Verified baseline: `da272aebe89e2f60822e9f5130e198655175d298`.
- [Build #3496](https://github.com/sk666G/mio-client/actions/runs/46151336623) passed all workflow steps, including rebuild security checks, Gradle build, remapped-artifact verification/provenance and upload.
- Baseline artifact: `schizo-client`, GitHub artifact ID `15266062972`, archive digest `sha256:62ee2f2782691de6414604e2e9b115f34255d652bd33644747861487bb08ccc1`. This is **not** the new candidate artifact or the individual JAR's digest.
- New source checkpoint workflow [#3539](https://github.com/sk666G/mio-client/actions/runs/46154716515) was in progress before this documentation commit. Use the final PR head's workflow to identify the actual candidate artifact.
- Minecraft 1.21.11, Fabric loader 0.19.5, Fabric API 0.141.6+1.21.11 (from gradle.properties).
- Java 21 standalone harness compiled exact copies of LegacyTextDraft and ButtonLabelLines, with Git blob hashes matching repository source. Passed 100,000 randomized code-point editing operations against an independent integer-code-point model and 10,000 label cache/split checks.
- Nine added JUnit methods in this pass, not yet verified by final-head CI.
- No live Minecraft session or measured runtime performance result is available.

## Completion matrix

| Area | Evidence/status | Remaining acceptance |
|---|---|---|
| HUD exact position and scale | Baseline CI verified | Live layout fit, anchor behavior, history |
| Single and group snapping | Baseline CI verified; cancellation guide clearing changed in this pass | Drag/release, cancellation, resizing, dynamic peers |
| Widget locking | Explicitly excluded from this candidate | Shared HUD margin changes and anchor reflow can move anchored widgets. Comprehensive locks require a defined policy for these paths and whole-layout restores. Do not advertise a lock. |
| Layer ordering | Excluded from candidate scope | No independent editor-only order model introduced |
| HUD layout preview | Source-reviewed; compact cancellation and failure feedback implemented; new tests pending | Keep, cancel, replace while open, small viewport, layout rollback |
| ClickGUI input lifecycle | Baseline CI verified; exclusive editing ownership and atomic string character events changed | Keybind capture, focus transfer, callbacks, text rendering |
| String editor | Detached draft, stale rejection and row clipping implemented; pure draft tested independently | In-game clipboard, Unicode input, stale changes and cancellation |
| Numeric/enum controls | Baseline CI verified | In-game endpoints, invalid input, focus, conditional visibility |
| Registry/sound selectors | Baseline CI verified | Actual registry reload, search, resource removal, click/scroll boundaries |
| Enchantment and item lists | Catalog reuse and bounded fitting implemented; stale selection check added | World/language/resource reload and dynamic filters; exact-head build |
| Preset import | Snapshot isolation, changed-source rejection and local status implemented | Exact-head tests, import valid/invalid/changed/duplicate files and refresh failure |
| Preset manager and trade configuration | Targeted source review; create-new, confirmation, rename/duplicate, stale trade editing and panel cleanup implemented in finalization pass | New exact-head CI, live input and configuration workflow tests, wider lifecycle review |
| Theme Studio and Gallery | Baseline CI verified | Actual color editing, persistence, dynamic colors and readability |
| Quick Access/Inspector/Visited Settings | Baseline CI verified | Live keyboard/navigation and stale edit checks |
| Health/Benchmark/Performance/Context/Expedition | Baseline CI verified | Actual viewport, interaction and telemetry checks |
| Final visual consistency and resource ownership | Partial source review | Full GUI inventory and live review outstanding |
| Inherited stack integration | PR mergeable; not exhaustive audit | Review inherited changes before any merge |
| Release readiness | Not signed off | Exact final-head CI plus live acceptance and integration review |

## Changes in this pass

- Detached string draft with original-value comparison; UTF-16 cursor moves by code point and complete character events reach the focused control. Clip the editor to its row and discard transient state on ownership change.
- Cache immutable button line splitting without changing legacy split semantics; preserve dynamic suppliers.
- Reuse enchantment entries for unchanged registry and language identities, evaluate dynamic filters each update, release catalog references on cancellation, avoid per-label holder lookup.
- Reuse bounded Unicode-safe measured text fitting for item lists and reject stale selected entries.
- Reject a dropped preset if its parsed content changed after inspection; preserve create-new storage safeguards; report import errors and distinguish save from registry refresh failure.
- Preserve cancellation in a compact HUD layout preview; disable obsolete Keep action after stale rejection and avoid claiming successful rollback without evidence.
- Clear peer guides on every HUD gesture cancellation.

## Manual acceptance (use a backed-up local configuration, single-player or an authorized test environment)

1. On PR #301 record the exact current head SHA. Open its successful Build workflow and download the matching uploaded artifact. Check the workflow provenance record; do not substitute the older baseline artifact when testing the new head. Install only the candidate JAR and compatible dependencies in a clean test profile.
2. Launch Minecraft 1.21.11. Check startup, ClickGUI open/close, category navigation, scroll, search, expanded controls, and configuration saving.
3. Exercise Boolean, numeric and enum controls. Test both endpoints and negative numbers, reject invalid drafts, collapse during drag, and ensure hidden settings cannot receive input.
4. Edit a string containing ASCII, accented and supplementary characters. Move left/right, Home/End, Backspace/Delete and paste. Escape must leave the original setting unchanged. Change the same value externally during an edit where practical; stale Enter must not overwrite it. Check the caret and row clipping with a very long value.
5. Start a keybind capture, then cancel with Escape and confirm old binding remains; Delete clears explicitly. Change categories and close/reopen while capturing. Confirm no unintended shortcut fires.
6. Open HUD Studio via its existing navigation entry. Test exact X/Y and 50–200% scale on supported widgets, unsupported scaling, single/group drag, grid priority, magnetic alignment, release, Escape, resize mid-gesture, undo/redo and anchor transitions. Verify group spacing and actual guide placement.
7. Preview a HUD layout. Test Keep, Cancel, resize to compact viewport, and stale preview rejection where reproducible. Cancellation must remain reachable and the original layout must be restored when no other layout supersedes it.
8. Open Theme Studio, edit RGBA/HSV and hex values, switch models without changing color, select recent colors, preview, cancel, apply, save and reload. Check the Gallery's advisory scores and dynamic themes.
9. Open available resource selectors. Search labels and namespaced identifiers, scroll to boundaries, change filters, reload language/resources, change worlds and confirm enchantments and selections remain correct. Preview sounds only by explicit action.
10. Drop one valid JSON preset, inspect category, change file contents before importing and verify rejection. Try invalid JSON, duplicate name and normal successful import; verify no overwrite and clear feedback. Back up configuration before testing destructive actions.
11. Open Quick Access, Inspector, Visited Settings, Command Palette, Health Center, Benchmark Lab, Performance Center, Context Profiles and Expedition. Check keyboard browsing, clipped labels, confirmation targets, parent navigation, and responsive behavior.
12. Repeat representative actions at multiple GUI scales and short/narrow window sizes; restart the client and check saved state. Capture actual screenshots for defects, exact candidate SHA, action sequence, logical GUI size and latest.log (redact private information).
13. For performance, use the same world, resolution, GUI scale, configuration and warmed-up workload on baseline and candidate. Capture multiple 30-second frame-time runs in Benchmark Lab, including idle UI, expanded categories, large selector and HUD editing. Report medians and percentile frame times; do not infer FPS from source changes.

## PR-derived source inventory

This is a list of GUI files present in the PR changed-filename response, not an exhaustive repository tree. Unchanged files may be missing. The available connector does not expose a complete directory tree listing in this session, so a full source inventory remains an explicit acceptance blocker.

### Changed GUI source paths (106)

- `src/main/java/dev/schizo/client/gui/AppliedPaletteHistory.java`
- `src/main/java/dev/schizo/client/gui/GuiElement.java`
- `src/main/java/dev/schizo/client/gui/InspectorEnumOptions.java`
- `src/main/java/dev/schizo/client/gui/PaletteChannels.java`
- `src/main/java/dev/schizo/client/gui/PaletteHex.java`
- `src/main/java/dev/schizo/client/gui/PaletteHsv.java`
- `src/main/java/dev/schizo/client/gui/PaletteRecentColors.java`
- `src/main/java/dev/schizo/client/gui/RecentVisits.java`
- `src/main/java/dev/schizo/client/gui/ScissorBounds.java`
- `src/main/java/dev/schizo/client/gui/ScissorStack.java`
- `src/main/java/dev/schizo/client/gui/SettingEditHistory.java`
- `src/main/java/dev/schizo/client/gui/ThemePreviewDraft.java`
- `src/main/java/dev/schizo/client/gui/ThemeShareCode.java`
- `src/main/java/dev/schizo/client/gui/UiMotionPolicy.java`
- `src/main/java/dev/schizo/client/gui/components/AbstractItemListComponent.java`
- `src/main/java/dev/schizo/client/gui/components/BooleanSettingComponent.java`
- `src/main/java/dev/schizo/client/gui/components/ButtonComponent.java`
- `src/main/java/dev/schizo/client/gui/components/ButtonLabelLines.java`
- `src/main/java/dev/schizo/client/gui/components/ColorSettingComponent.java`
- `src/main/java/dev/schizo/client/gui/components/EnchantmentListComponent.java`
- `src/main/java/dev/schizo/client/gui/components/EnumOptionRow.java`
- `src/main/java/dev/schizo/client/gui/components/EnumSettingComponent.java`
- `src/main/java/dev/schizo/client/gui/components/ItemListLayout.java`
- `src/main/java/dev/schizo/client/gui/components/KeybindComponent.java`
- `src/main/java/dev/schizo/client/gui/components/LegacyTextDraft.java`
- `src/main/java/dev/schizo/client/gui/components/MeasuredControlText.java`
- `src/main/java/dev/schizo/client/gui/components/ModuleComponent.java`
- `src/main/java/dev/schizo/client/gui/components/NumericControlInput.java`
- `src/main/java/dev/schizo/client/gui/components/NumericSettingComponent.java`
- `src/main/java/dev/schizo/client/gui/components/PresetPanel.java`
- `src/main/java/dev/schizo/client/gui/components/SettingComponent.java`
- `src/main/java/dev/schizo/client/gui/components/StringSettingComponent.java`
- `src/main/java/dev/schizo/client/gui/components/StudioControlGeometry.java`
- `src/main/java/dev/schizo/client/gui/components/StudioControlStyle.java`
- `src/main/java/dev/schizo/client/gui/components/TradeActionsPanel.java`
- `src/main/java/dev/schizo/client/gui/components/TradeEditorPanel.java`
- `src/main/java/dev/schizo/client/gui/components/TradeItemsComponent.java`
- `src/main/java/dev/schizo/client/gui/components/TradeSelectorPanel.java`
- `src/main/java/dev/schizo/client/gui/panels/ModuleCategoryPanel.java`
- `src/main/java/dev/schizo/client/gui/panels/Panel.java`
- `src/main/java/dev/schizo/client/gui/panels/PanelBounds.java`
- `src/main/java/dev/schizo/client/gui/panels/QueryMatchCache.java`
- `src/main/java/dev/schizo/client/gui/registry/RegistryEntryCatalog.java`
- `src/main/java/dev/schizo/client/gui/registry/RegistryEntryComponent.java`
- `src/main/java/dev/schizo/client/gui/registry/RegistrySearchText.java`
- `src/main/java/dev/schizo/client/gui/registry/RegistrySetScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/BenchmarkCenterScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/ClickGuiLayout.java`
- `src/main/java/dev/schizo/client/gui/screens/ClickGuiScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/ClickGuiSearchText.java`
- `src/main/java/dev/schizo/client/gui/screens/ClientScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/CommandPaletteLayout.java`
- `src/main/java/dev/schizo/client/gui/screens/CommandPaletteScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/CommandPaletteShortcut.java`
- `src/main/java/dev/schizo/client/gui/screens/ContextProfileEditorScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/ContextRuleReviewScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/CustomPaletteScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/DashboardText.java`
- `src/main/java/dev/schizo/client/gui/screens/DiscoveryEntry.java`
- `src/main/java/dev/schizo/client/gui/screens/ExpeditionLayout.java`
- `src/main/java/dev/schizo/client/gui/screens/ExpeditionScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/HealthCenterScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/HudEditorScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/HudLayoutHistory.java`
- `src/main/java/dev/schizo/client/gui/screens/HudLayoutPreview.java`
- `src/main/java/dev/schizo/client/gui/screens/HudLayoutPreviewScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/HudMarginController.java`
- `src/main/java/dev/schizo/client/gui/screens/HudPositionInput.java`
- `src/main/java/dev/schizo/client/gui/screens/HudPositionScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/HudToolbar.java`
- `src/main/java/dev/schizo/client/gui/screens/InspectorRows.java`
- `src/main/java/dev/schizo/client/gui/screens/ModuleInspectorScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/PaletteMatcher.java`
- `src/main/java/dev/schizo/client/gui/screens/PalettePickerScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/PerformanceCenterScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/PerformanceViewport.java`
- `src/main/java/dev/schizo/client/gui/screens/PresetImportScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/PresetImportSnapshot.java`
- `src/main/java/dev/schizo/client/gui/screens/PresetScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/ProfilerDisplayRow.java`
- `src/main/java/dev/schizo/client/gui/screens/QuickAccessScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/RecentColorsScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/RecentSettingsScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/SavedThemesScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/ScalarHistoryScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/ScreenTooltipLayout.java`
- `src/main/java/dev/schizo/client/gui/screens/SearchFeedback.java`
- `src/main/java/dev/schizo/client/gui/screens/SnowflakeParticle.java`
- `src/main/java/dev/schizo/client/gui/screens/SoundOptionButton.java`
- `src/main/java/dev/schizo/client/gui/screens/SoundSelectionScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioButton.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioEditBox.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioEntryMotion.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioLayout.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioResizeFocus.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioReturnFocus.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioRowHover.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioScreenTheme.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioTextSelection.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioTooltipText.java`
- `src/main/java/dev/schizo/client/gui/screens/StudioViewportLayouts.java`
- `src/main/java/dev/schizo/client/gui/screens/ThemeContrastReport.java`
- `src/main/java/dev/schizo/client/gui/screens/ThemeGalleryScreen.java`
- `src/main/java/dev/schizo/client/gui/screens/ThemeSampleColors.java`
- `src/main/java/dev/schizo/client/gui/screens/ThemeStudioScreen.java`

### Changed GUI test paths (53)

- `src/test/java/dev/schizo/client/gui/AppliedPaletteHistoryTest.java`
- `src/test/java/dev/schizo/client/gui/PaletteChannelsTest.java`
- `src/test/java/dev/schizo/client/gui/PaletteHexTest.java`
- `src/test/java/dev/schizo/client/gui/PaletteHsvTest.java`
- `src/test/java/dev/schizo/client/gui/PaletteRecentColorsTest.java`
- `src/test/java/dev/schizo/client/gui/RecentVisitsTest.java`
- `src/test/java/dev/schizo/client/gui/ScissorBoundsTest.java`
- `src/test/java/dev/schizo/client/gui/SettingEditHistoryTest.java`
- `src/test/java/dev/schizo/client/gui/ThemePreviewDraftTest.java`
- `src/test/java/dev/schizo/client/gui/ThemeShareCodeTest.java`
- `src/test/java/dev/schizo/client/gui/UiMotionPolicyTest.java`
- `src/test/java/dev/schizo/client/gui/components/ButtonLabelLinesTest.java`
- `src/test/java/dev/schizo/client/gui/components/ItemListLayoutTest.java`
- `src/test/java/dev/schizo/client/gui/components/LegacyTextDraftTest.java`
- `src/test/java/dev/schizo/client/gui/components/MeasuredControlTextTest.java`
- `src/test/java/dev/schizo/client/gui/components/NumericControlInputTest.java`
- `src/test/java/dev/schizo/client/gui/components/StudioControlGeometryTest.java`
- `src/test/java/dev/schizo/client/gui/panels/PanelBoundsTest.java`
- `src/test/java/dev/schizo/client/gui/panels/PanelRegistryTest.java`
- `src/test/java/dev/schizo/client/gui/panels/QueryMatchCacheTest.java`
- `src/test/java/dev/schizo/client/gui/registry/RegistrySearchTextTest.java`
- `src/test/java/dev/schizo/client/gui/screens/ClickGuiLayoutTest.java`
- `src/test/java/dev/schizo/client/gui/screens/ClickGuiSearchTextTest.java`
- `src/test/java/dev/schizo/client/gui/screens/CommandPaletteLayoutTest.java`
- `src/test/java/dev/schizo/client/gui/screens/CommandPaletteShortcutTest.java`
- `src/test/java/dev/schizo/client/gui/screens/DashboardTextTest.java`
- `src/test/java/dev/schizo/client/gui/screens/DiscoveryEntryTest.java`
- `src/test/java/dev/schizo/client/gui/screens/ExpeditionLayoutTest.java`
- `src/test/java/dev/schizo/client/gui/screens/HudLayoutHistoryTest.java`
- `src/test/java/dev/schizo/client/gui/screens/HudLayoutPreviewTest.java`
- `src/test/java/dev/schizo/client/gui/screens/HudPositionInputTest.java`
- `src/test/java/dev/schizo/client/gui/screens/HudToolbarTest.java`
- `src/test/java/dev/schizo/client/gui/screens/InspectorEditDraftTest.java`
- `src/test/java/dev/schizo/client/gui/screens/InspectorRowsTest.java`
- `src/test/java/dev/schizo/client/gui/screens/PaletteMatcherTest.java`
- `src/test/java/dev/schizo/client/gui/screens/PerformanceViewportTest.java`
- `src/test/java/dev/schizo/client/gui/screens/PresetImportScreenPathTest.java`
- `src/test/java/dev/schizo/client/gui/screens/PresetImportSnapshotTest.java`
- `src/test/java/dev/schizo/client/gui/screens/ProfilerDisplayRowTest.java`
- `src/test/java/dev/schizo/client/gui/screens/ScreenTooltipLayoutTest.java`
- `src/test/java/dev/schizo/client/gui/screens/SearchFeedbackTest.java`
- `src/test/java/dev/schizo/client/gui/screens/SoundSelectionScreenImportTest.java`
- `src/test/java/dev/schizo/client/gui/screens/StudioEntryMotionTest.java`
- `src/test/java/dev/schizo/client/gui/screens/StudioLayoutTest.java`
- `src/test/java/dev/schizo/client/gui/screens/StudioResizeFocusTest.java`
- `src/test/java/dev/schizo/client/gui/screens/StudioReturnFocusTest.java`
- `src/test/java/dev/schizo/client/gui/screens/StudioRowHoverTest.java`
- `src/test/java/dev/schizo/client/gui/screens/StudioScreenThemeTest.java`
- `src/test/java/dev/schizo/client/gui/screens/StudioTextSelectionTest.java`
- `src/test/java/dev/schizo/client/gui/screens/StudioTooltipTextTest.java`
- `src/test/java/dev/schizo/client/gui/screens/StudioViewportLayoutsTest.java`
- `src/test/java/dev/schizo/client/gui/screens/ThemeContrastReportTest.java`
- `src/test/java/dev/schizo/client/gui/screens/ThemeSampleColorsTest.java`

## Release notes (candidate, not release sign-off)

This pass improves text-editing correctness, legacy label rendering work, resource-list refresh costs, preset import consistency, HUD preview responsiveness and transient gesture cleanup. Earlier consolidated changes add precision HUD properties and magnetic snapping, Theme Studio palette tools, accessibility, navigation and responsive workspaces. No performance benchmark claim or live acceptance claim is made. Keep main untouched and PR #301 in draft until remaining acceptance requirements are satisfied.


## Absolute finalization pass

- Verified baseline: `57221da4905d1c2ad836ac3113df1d2f73ff540f`. [Build #3541](https://github.com/sk666G/mio-client/actions/runs/46154826730) completed successfully, including rebuild security tests, Java toolchains, Gradle build, remapped artifact verification/provenance, and upload.
- Baseline artifact: [schizo-client](https://github.com/sk666G/mio-client/actions/runs/46154826730/artifacts/15267068827), ID `15267068827`. Archive digest `sha256:6a5de0f15e47e1911b446ad874e1b0db101d05f72a40e1b194114c1dba481baf`, not an individual JAR digest. Artifact metadata associates it with the verified baseline, not the new candidate.
- New source checkpoint: `cd126c8f0ab1e941b42783b16da5a56370135dfe` (17 commits, 15 files, +421/-265 since the verified baseline). [Build #3569](https://github.com/sk666G/mio-client/actions/runs/46155823732) pending at documentation time. This documentation commit creates a newer exact-head workflow; verify that head before release.
- Five regression methods added: two preset registry tests, two confirmation tests, one rejected trade edit test. New tests are not yet CI-confirmed.
- Preset manager: explicit create-new action, no overwrite on creation, reviewed-content checks before actions, category-bound list selection, two-step overwrite/delete confirmation, rename and duplicate controls using existing registry APIs, local feedback and cleanup of replaced controls. Enter commits the name draft; Create new then saves. Confirmation checks compare registry and bounded on-disk JSON; this is not a filesystem lock or atomic compare-and-swap against external processes.
- Trade configuration presentation: reject stale selected object or unavailable enchantment before opening an editor, deliberate timed deletion confirmation, clear input on panel removal, distinguish creation from reviewed replacement, validate the replacement before changing the existing registry slot. Existing matching and gameplay behavior unchanged. The registry replacement guard does not establish synchronization with arbitrary external mutation paths.
- Legacy multiline buttons: retain a minimum single-row height even when a label consists only of newline characters.

### Additional manual acceptance

1. In Preset Manager enter a new name and press Enter, then Create new. Verify that an existing exact or case-insensitive name is rejected and its file remains unchanged. Retry with another name. Confirm local feedback and undo.
2. Select a preset, change its file externally, and attempt Load, Save, Delete, Rename or Duplicate. Verify stale-content rejection. Test switching categories before a list refresh, selecting another preset during confirmation, cancelling by navigating away, and opening the preset location.
3. Verify Save changes and Delete require two explicit clicks on the same target. Verify a different action cannot consume the previous confirmation. Rename and Duplicate must not overwrite existing targets. Test failure feedback, missing files, and configuration persistence.
4. In the trade configuration UI, test creation with a duplicate name, edit an existing entry, replace the underlying configuration while its editor is open, then attempt to save. The original/current entry must not be deleted by a rejected edit. Verify missing-resource feedback, selection changes, cancellation, selector closing and timed deletion confirmation. Test only in a local or authorized environment.
5. Repeat these workflows on short and narrow logical GUI viewports. Ensure essential navigation and the status row remain accessible.

### Remaining certification limits

The reviewed preset and trade paths are not a full repository audit. No complete directory-tree reader or local Minecraft runtime was established in this session. The PR-derived inventory remains partial, comprehensive visual and performance acceptance is unperformed, and inherited integration changes remain subject to review. Do not sign off release readiness or claim measured performance gains. Comprehensive HUD locking and independent layer ordering remain excluded. Main remains untouched.


## Project Singularity infrastructure checkpoint

[Build #3571](https://github.com/sk666G/mio-client/actions/runs/46155886818) succeeded at `5a380ba17945c2876b36aef2417594880e4dfd30`; the previous preset and trade configuration changes are now CI-verified. [Matching verified baseline artifact](https://github.com/sk666G/mio-client/actions/runs/46155886818/artifacts/15267841614), ID 15267841614. See [Project Singularity audit](project-singularity-audit.md) for the newer source checkpoint's preset undo, startup, scissor, font draw, benchmark and frame timing changes, independent helper test evidence and additional acceptance steps. Exact final-head CI and live Minecraft acceptance remain outstanding.
