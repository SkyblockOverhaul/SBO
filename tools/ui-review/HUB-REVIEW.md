# Hub / Commands review

The approved launcher keeps nine destinations in a compact three-column grid, the existing
footer links/credits and the supporter strip. Commands uses aligned text, compact close
targets, visible keyboard focus, contained Tab navigation and focus restoration.

This branch is based on current `Diana-V2`, including merged hover PR #315 and the newer
profile-viewer credits. It does not replay that hover commit or restyle other menus.
The Credits group wraps its links so the two new upstream credits fit inside the window;
the three footer groups still share one row.

Run `sh tools/ui-review/render-hub.sh 26.2` for offline synthetic GuiLib before/after renders
in `build/ui-review/hub-commands/26.2-fabric/`. All JVMs and font/image initialization are
explicitly headless. The fixture reads current labels/commands and never opens a game,
browser, account or network action. Native items/fonts/shaders and in-game routing remain
unverified. No generated PNGs or jars are committed.
