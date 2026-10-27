# Offline menu review

Synthetic GuiLib presentation fixtures for the remaining SBO menus. They exercise real
layout and controls with source-derived labels and isolated state. They never launch
Minecraft, initialize saved settings or invoke live API/cloud/audio/account actions.
The Hub/Commands harness is in the independently based Hub change.

With a compatible cached JDK in `JAVA_HOME`, run from this checkout:

```sh
JAVA_TOOL_OPTIONS=-Djava.awt.headless=true ./gradlew :26.2-fabric:test --offline --no-daemon
./tools/ui-review/render.sh 26.2 settings
```

The renderer accepts `26.1.2`, `26.2`, or `26.3`, and these screen names:
`settings`, `diana`, `Medal`, `PartyCommands`, `Customization`, `Themes`, `QOL`, `Debug`,
`partyfinder`, `pf-create`, `pf-filters`, `pf-forms`, `pf-rest`, `events`, `achievements`,
`sounds`, `cloud`, `badge`, `overlay`.

Normal tests do not export image suites. The script enables the relevant suite and
writes PNGs under ignored `build/ui-review/`. It adds `-Djava.awt.headless=true`; tests
assert it before AWT/font initialization. Use only genuinely headless invocation.
Baseline source fixtures under `baseline/` preserve the pre-pass comparison styling;
`general-template.css` records the approved template iteration. They are test inputs,
not runtime resources. No generated renders, game assets or build archives are committed.

The raster adapter approximates Minecraft fonts/items, external icons, shadows and
animation. It is evidence of layout and synthetic interaction, not native screenshots
or proof of game/API/persistence behavior. See `UI-REVIEW.md` for reviewed screens and
remaining native limits. Recheck individual changed cases rather than expanding every
screen/theme/state combination without a concrete reason.
