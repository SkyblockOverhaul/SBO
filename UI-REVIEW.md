# SBO menu baseline review

This draft adopts the approved General Settings template across the remaining SBO menus.
The compact `/sbo` launcher and Commands are a separate change. Both branches start at
Diana-V2 `3ed606217c857e47f26b04b76b91426f543934ba`; neither replays merged hover PR #315.

## Coverage

Each row was inspected as synthetic GuiLib pixels during the screen-by-screen review,
with normal and representative constrained widths. These are not Minecraft screenshots.

| Screen | Reviewed states / boundaries |
| --- | --- |
| Settings General | Search, clear, no results, jump context, collapsed/off/dependent groups; switches, numerical controls and descriptions |
| Diana / common pickers | Groups, dependencies, enum/multiselect, RGB/alpha, numerical controls and multiline strings |
| Medal | Its actual rows, disabled/dependent controls and wrapping |
| Party Commands | Its actual action/setting rows and descriptions |
| Customization | Colors/alpha, sliders, groups and dependencies |
| Themes | Theme/scale choices, descriptions and actions |
| QOL | Actual settings, collapsed/off groups and long descriptions |
| Debug | Actual settings, dependencies and multiline descriptions |
| Party Finder shell/list/inspector | Populated, empty, queued and constrained inspector; requirements remain legible |
| Party Finder Create/Edit/Filters | Dynamic requirements, roles, comparisons, empty values and validation surfaces |
| Party Finder Rules / Settings | General, Favorites and Look, empty lists and controls |
| Party Finder onboarding/dialogs | Tour, roles, report, confirmations and context menus |
| Events | List, summary/details, charts and deletion confirmation |
| Achievements | Search/filter, grouped progress, locked/unlocked/repeatable |
| Sounds | Picker, volume/value/Test alignment, disabled and long filenames |
| Cloud Sync | Synthetic status, compare, backups and confirmations |
| Badge editor | Places/Colors/Effects/Stat/View; synthetic preview, loading/error/eligibility and unsaved changes |
| Overlay editor | Synthetic selection/readout/help, clamping and geometry |

Settings and picker fixtures also cover the four built-in theme bases. Representative
normal/constrained cases are retained in the test harness; no real account data or
network actions are used. User accepted the resulting presentation as a practical baseline.

## Fresh upstream integration

- Retain upstream tracker overlay redesign and shared look settings (`0327e5e7`), including
  element clicks/tooltips and legacy-look notices (`a28fcec2`).
- Retain multiline textarea plus expanded editor (`ec24485a`); update the fixture from
  the former Add-line UI to exercise newlines, expanded editing and Escape/Done.
- Retain fresh stats reload (`56226803`), Profile Viewer/cache/PF integration
  (`5bc92d3b`, `69188c84`) and upstream SVG close icon.
- Retain new overlay/profile modules and badge renderer fixes. No tracker or profile
  renderer is replaced by the baseline styling.

## Verification and limitations

Run `tools/ui-review/render.sh` for optional synthetic images. Tests use real GuiLib
layout/widgets with source-derived labels and isolated state; they do not instantiate
Minecraft, persistence, live API clients or real actions. Every image/font initialization
asserts `java.awt.headless=true`; Gradle Test workers enforce it.

Native in-game text/item/SVG rendering, game routing, screen scaling, drag/scroll feel,
real audio, live API/configuration persistence and account/cloud effects remain untested.
No native client was launched for this final integration. A native follow-up is required
before treating this draft as fully validated. Build results are recorded in the PR body
and the local build logs rather than claiming native coverage here.
