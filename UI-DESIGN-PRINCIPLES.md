# Skyblock Overhaul — UI Design System

**Status:** Working design specification
**Reference:** Approved `/sbo` launcher redesign
**Purpose:** Keep SBO interfaces visually coherent without redesigning their functionality.

## 1. Scope and priorities

### Approved Settings baseline

The user approved General Settings preview v5 as the practical presentation baseline for
the remaining screens. Adapt it to each screen's work; the launcher remains a launcher.

- Prefer one flat window surface. Avoid sidebar panels and nested cards that add no grouping
  meaning. Use typography, spacing and restrained navigation indicators.
- Search and dropdowns use a slightly lighter filled surface with no visible resting outline.
  Keep readable values/placeholders, clear actions and a restrained keyboard focus indicator.
- Center row controls on the complete label/description block, including wrapped text and
  requirement explanations. Preserve descriptions rather than deleting them to simplify layout.
- Keep section controls as compact as ordinary setting switches: the reference uses a 20×11
  track and 7×7 thumb inside a stable invisible target. Keep an explicit section label for the
  control tree and a distinct collapse action.
- Use one subtle horizontal rule below a section's heading/control row, not a surrounding box.
  Attach headings to their content with a smaller gap than the separation before the next
  section. The reference has roughly 10px/20px text gaps, not large empty bands.
- Retain breathing room below dividers through row padding and actual alignment, not arbitrary
  transforms of individual controls. Keep internal row spacing consistent.
- Preserve section-gate semantics: off forces Boolean entries to read false without erasing
  saved values, while collapse only hides rows. Non-Boolean values are not universally disabled;
  do not describe this as turning every feature off or remove it during a visual change.

This approval authorizes a useful baseline, not endless aesthetic refinement or publication.
Inspect a small representative set and fix concrete alignment, wrapping and interaction issues.

This design system applies to SBO-owned screens, including the launcher, settings, Party Finder, tabbed pages, lists, forms and dialogs. The `/sbo` launcher is the **visual reference**, not a layout template. Other screens should retain their information architecture and task-specific controls.

Priorities, in order:
1. **Preserve behavior and discoverability.** Do not remove controls, routes, shortcuts, content or states simply to make a screen cleaner.
2. **Correct alignment and interaction.** Consistent hitboxes, readable layouts, and predictable hover/focus feedback come before decoration.
3. **Improve hierarchy and density.** Give important work adequate space; reduce containers, filler copy and visual noise.
4. **Unify styling.** Reuse the same typography, neutral surfaces, radii, spacing logic and interaction patterns.
5. **Avoid redesign for redesign's sake.** Don't change a working UX flow without explicit approval.

A change is not complete merely because it compiles or resembles the mockup.

## 2. Overall visual language

**Character:** Compact, restrained, game-adjacent, functional. The interface should feel like a Minecraft mod UI, not a marketing site or an enterprise dashboard.

- Use SBO's existing **neutral dark-grey palette** as the base. Do not substitute a blue/teal theme just to make the UI feel different.
- Prefer flat, subtly differentiated surfaces. Avoid gradients, glows, decorative sweeps and oversized accent regions.
- Use color for meaning: active selection, status, interactive feedback, warning, and supporter-only access.
- Avoid excessive borders, nested cards and repeated panel outlines. Each visible boundary must distinguish a genuine group or interaction.
- Keep components visually consistent across screens without forcing all content into cards.

**Do not infer exact global color tokens from screenshots.** Extract the actual base colors from the implemented approved launcher; use them as shared tokens. The one documented explicit accent is the close-icon hover coral `#ef8274`.

## 3. Shared foundations

### Typography
- Retain the mod's existing typography (the approved launcher retains its existing Inter-based styling and its established weights).
- Make hierarchy using weight, size and spacing—not gradients or novelty fonts.
- Main headings: prominent but not so large that they crowd primary actions.
- Group headings: small, muted, consistent uppercase labels where appropriate.
- Secondary text: subdued but readable at realistic in-game scaling.
- Keep useful command subtitles (for example `/sbosettings`, `/sbopf`). Remove descriptions that merely restate a button's title or advertise feature counts.
- Preserve meaningful explanation, instructions, status and acknowledgments when they help the user.

### Spacing and density
- Prefer **purposeful, compact** spacing. Avoid the failure mode of oversized elements with cramped inner content.
- Align related controls to a common grid or baseline. Keep consistent padding for instances of the same component.
- Let the content determine widths when practical; don't stretch small groups just to fill a row.
- Use whitespace to separate groups, not to make each group seem like a separate page.
- Do not reduce padding so far that text, icons or clickable targets become crowded.
- Establish a small shared spacing scale from existing launcher values; don't invent unrelated pixel values for every screen.

### Geometry and surfaces
- Use one restrained corner-radius language.
- Flat elevated surfaces can signal clickability without an outlined border.
- Reserve panel borders for real containers, such as the outer window or an input region that needs delineation.
- Avoid decorative icons-in-circles when native Minecraft item icons are sufficient.
- Use **native Minecraft item rendering** in-game; colored icon squares in HTML screenshot crops were mockup artifacts, not approved asset backgrounds.

## 4. Components and patterns

### Windows and headers
- Provide a clear page or screen title and predictable close control.
- Keep branding and version information secondary to the tasks the screen supports.
- Do not add promotional taglines, count badges or a divider by default.
- Align title, tabs, status indicators and close controls using common vertical alignment rules.
- Don't assume a launcher header's proportions are suitable for data-heavy screens.

### Navigation cards / launcher buttons
- Use consistent sizing, icon alignment, type scale and internal spacing for peer destinations.
- Keep the feature name primary; retain command shortcuts when useful.
- No permanent highlight or special size for Settings unless the interface semantics require it.
- Flat hover treatment; no moving hitbox that causes hover flicker.
- The approved `/sbo` launcher uses a three-column grid. **This is launcher-specific, not a rule for other screens.**

### Tabs and navigation bars
- Put every tab label on a shared vertical center/baseline, including the title, status indicators and close control when they share the bar.
- Keep tab widths and click targets predictable; the selected state should be clear without dominating the header.
- Use the same text treatment and active/hover logic across tabbed screens.
- Preserve the original tab order and switching behavior.

### Buttons and links
- Distinguish primary actions from secondary links through size, context and emphasis.
- Avoid using large card buttons for small external links or credits.
- Use one compact secondary-button/link style for related footer links and credited resources.
- If a resource is clickable, its hover/focus treatment should communicate that. Passive text should not misleadingly look interactive.
- Keep hover changes restrained and consistent.

### Search, settings and forms
- Prioritize labels, field states, validation and legible input values over decoration.
- Maintain familiar setting groups, categories and controls; do not hide a capability just to shorten the page.
- Use spacing and headings for grouping before adding more outer cards.
- Keep helper text only when it clarifies non-obvious behavior, constraints or consequences.
- Make active, disabled, selected and error states distinguishable without relying solely on color.

### Lists, tables and dense content
- Optimize for scanning: consistent row height, predictable columns, aligned numeric or status fields.
- Prefer subtle row separation or alternating surfaces to heavy boxes around every row.
- Avoid gratuitous padding that forces needless scrolling.
- Preserve filters, sorting, selection and status semantics.
- Within Party Finder cards, show requirement status with restrained colored or neutral text.
  Retain the outer party boundary rather than outlining every requirement again.

### Supporter status
- Keep a compact muted-gold vertical **Supporter** strip where applicable, with explicit text instead of an unexplained symbol.
- The strip must not enlarge or distort the button and must not obscure the hit target or label.
- Keep Cloud Sync and SBO Badge independent destinations.

### Close action
- Default to a small muted `×` with a transparent background.
- On hover, tint **only the icon** warm coral `#ef8274`; do not introduce a solid bright-red square.
- A close action is not inherently destructive; avoid destructive-action visual language.

### Footer and credits
- For the approved launcher: Support SBO, Community and Credits share one row with space distributed **between the groups**, not between every button.
- Credit resources use the same compact link/button style as other secondary links.
- Retain the human-written thank-you acknowledgement and author attribution.
- Other screens need not reproduce the launcher footer.

## 5. Responsive behavior

- Preserve readable text and usable hit targets on common Minecraft GUI scales and display sizes, including 1080p.
- Scale the overall layout to fit smaller viewports instead of allowing overflow or clipping.
- The approved launcher was increased by approximately **20%** in overall scale after the first in-game test; that does **not** justify increasing every component's internal padding.
- Check long labels, localization, narrower windows, tabs, and footer groups that may wrap.
- Prefer intentional wrapping or responsive rearrangement over tiny text and overlapping elements.

## 6. Interaction and accessibility

- Hover, focus and active feedback should be noticeable but not startling.
- **Visual movement must not change the hover hitbox under a stationary cursor.** Where animation is required, keep hit testing tied to a stable layout rectangle.
- Keyboard navigation and visible focus states should remain usable where supported.
- Preserve sufficient text contrast, especially for subtitles and disabled states.
- Don't rely on color alone for status, errors or restrictions.
- Avoid surprise motion, flashing, glowing or large color changes for routine actions.

## 7. Implementation guardrails for agents

When adapting an SBO screen:

1. **Inspect first.** Understand the actual screen, its current layout, interactions, shared components and game-version implementations.
2. **Use the approved `/sbo` implementation as the style reference**, not the HTML screenshot crops or this document alone.
3. **Preserve UX.** Keep navigation destinations, controls, labels needed for comprehension, permissions, error states and logic unless explicitly asked to change them.
4. **Share styling where useful.** Centralize reusable tokens and components when it reduces drift, but avoid speculative refactoring or a new UI framework.
5. **Keep layouts screen-specific.** A Party Finder list, settings form and launcher need different information structures.
6. **Preview before committing to a visual overhaul.** For major changes, create an HTML or equivalent mockup, show a screenshot, iterate and get approval before porting it into Minecraft.
7. **Verify in-game.** Compile/test all supported target versions; inspect real rendering rather than assuming automated tests prove layout quality.
8. **Keep changes reviewable.** Separate visual cleanup from unrelated feature changes and document any intentional deviations.

## 8. Visual QA checklist

- [ ] All related items share intentional vertical and horizontal alignment.
- [ ] Navigation tabs, title, status indicator and close icon line up correctly.
- [ ] Buttons have consistent padding and usable, stable hover targets.
- [ ] Hover, focus, selected, disabled and supporter states are coherent.
- [ ] No unnecessary gradient, glow, border, nested card or marketing subtitle was introduced.
- [ ] Feature names, commands, labels and real user-facing information remain intact.
- [ ] No clipping, overflow, awkward wrapping or unusually tiny text at common GUI scales.
- [ ] Native Minecraft assets render without placeholder backgrounds.
- [ ] Functionality and existing navigation still work.
- [ ] Screenshot comparisons cover default, hover, active and narrow/rescaled states where relevant.
- [ ] Builds and relevant tests pass; **manual in-game verification** has been performed before a PR is considered ready.

## 9. Decision rule for trade-offs

When two design rules conflict, prefer: **functionality and accessibility → correct alignment and hit testing → meaningful hierarchy → consistency → decorative preference**.

Consistency is not identical layout. A compact table may need denser rows than the launcher; a settings page may need explanatory text that the launcher does not. The aim is recognizable SBO visual language across purposeful interfaces.

## 10. Provenance and non-goals

This document generalizes the approved `/sbo` launcher work. It intentionally does **not** require deleting buttons, redesigning navigation flows, imposing a three-column layout everywhere, replacing fonts, or changing all colors. For the reasons behind launcher-specific decisions and rejected experiments, consult the separate **Design principles behind the `/sbo` launcher** document.
