SBO THEMES
==========

Every .json and .css file in this folder shows up as a theme for all SBO windows:
open the SBO settings (/sbosettings) and go to Themes > Theme.
The party finder can also have its own theme: Party Finder > Settings > Look > Theme.
The list is read again every time you open the settings, no restart needed.

SBO writes this README again on every start, so changes to it are lost.
example.json is only created once, you can change or delete it. It sets everything a theme can change.
If you never changed the old example (Forest), SBO replaces it with the new one.
example.css is also only created once, it shows a CSS theme.

There are two kinds of themes:
  .json  Only colors, see MAKE YOUR OWN THEME. Easy, nothing can break.
  .css   Real CSS, see CSS THEMES. You can restyle everything: sizes, corners, fonts, borders, single windows.
And user.css: CSS that is added on top of every theme, also the built-in ones.


MAKE YOUR OWN THEME
-------------------
1. Copy example.json and give the copy a new name, e.g. my-theme.json.
2. Open it with any text editor and change "name" and the colors.
3. Open the SBO settings (or the party finder settings) and pick your theme.

A theme file looks like this:

{
  "name": "My Theme",
  "description": "Shown under the theme list when the theme is picked.",
  "base": "sbo-dark",
  "hypixelColors": false,
  "marks": false,
  "colors": {
    "accent": "#ff8800",
    "bg": "#101014"
  }
}

name           Name in the theme list (up to 32 characters). Without it the file name is used.
description    Short text about the theme (optional).
base           The built-in theme you start from. Everything you don't set in "colors" comes from it.
               One of: sbo-dark, hypixel, colorblind, high-contrast, light, midnight, purple, mythological, ocean, minecraft
hypixelColors  Party finder only: true colors values like Hypixel does (item rarities, SkyBlock level, Trophy Fisher, party types).
               Without it, the setting of the base theme is used.
marks          Party finder only: true gives requirements you don't meet a dashed border (a "x" in front where there is no border).
               Without it, the base theme decides.
colors         Only the colors you want to change. Unknown names are ignored.
               With base "minecraft" you also keep its square corners, stone buttons and Minecraft font.

Text after // up to the end of the line is a note and is ignored, like in example.json.

Colors are written as #rgb, #rgba, #rrggbb or #rrggbbaa, for example "#ff8800" or "#ff880080".
The last two digits of #rrggbbaa are the transparency: 00 is invisible, 80 is half, ff is solid.
A file with a mistake is skipped, check the Minecraft log for "[SBO] Theme".


CSS THEMES
----------
A .css file is a theme like a .json one. The comment at the very top sets the same things,
one "key: value" per line (all optional):

/*
name: My CSS Theme
description: Shown under the theme list when the theme is picked.
base: sbo-dark
hypixelColors: false
marks: false
*/

body.sbo {
    --accent: #ff8800;
}
body.sbo button {
    border-radius: 6px;
}

Everything after the comment is normal CSS. It is added after SBO's own CSS, so your rule wins over
SBO's when both are equally specific. Use a more specific selector or !important where it doesn't.
Changes to the file show up in open windows within a second, no reopening needed.
A mistake only drops that rule or line. A window then shows a popup with every mistake (file, line
and what is wrong), it comes back when you save the file with mistakes again. They are also in the log.

A .json theme can bring CSS too: "css": "my-theme.css" (a .css file in this folder).

user.css in this folder is not a theme, it is always added after the theme, whatever theme you use.
Good for small changes you want everywhere, e.g. a font or rounder buttons. It doesn't exist at first,
just create it.

Selectors you can use
  body.sbo                    Every SBO window. Put color variables here, so they beat the base theme.
  body.sbo-theme-<base>       The base theme that is active, e.g. body.sbo-theme-minecraft
  body.sbo-window-<name>      Only one window. Names: hub, settings, partyfinder, profileviewer,
                              events, achievements, sounds, cloud, badge
  button, input, select ...   The elements themselves, like on a web page

Each window also has its own classes, e.g. .hub-window, .pf-window, .cfg-sidebar or .ev-header.
Those are SBO's inner workings: they can change in an update without notice. The ones above stay.

Which classes exist?
  - In the game: open any SBO window and press Ctrl + Shift + C (inspector). The element under the
    mouse shows its classes, a left click copies its selector. Ctrl + Shift + C or Escape turns it off.
  - In the folder "reference" here: copies of all of SBO's CSS, one file per window, with their own
    README. They are not loaded, they are only there to look things up and copy rules from.
Not styled: the HUD overlays (trackers, Crown of Avarice), only the windows.

Images and fonts in url() only come from Minecraft resources (e.g. "sbo:fonts/nunito-400.ttf" or a
resource pack), not from your disk or the internet.


ALL COLORS (default values of SBO Dark)
---------------------------------------
Window
  bg                 #1e1f22    Background of the window and the player panel
  panel              #2b2d31    Header and side bar
  panel-2            #313338    Hover in the side bar, tags and requirement chips
  card               #26282c    Party cards and hover of form rows
  card-hover         #2a2c31    Party card under the mouse
  border             #3f4147    Lines between areas, card borders
  border-hover       #5a5d66    Card border under the mouse

Text
  text               #f2f3f5    Normal text
  text-strong        #ffffff    Names, headings, important text
  text-soft          #dbdee1    Labels, notes, side bar entries
  subtle             #b5bac1    Inactive tabs, chips and arrows
  muted              #949ba4    Hints and secondary text
  dim                #80848e    Placeholders and the faintest text
  title              #55ffff    "Party Finder" title, party size tag, your own value in tooltips

Accent (buttons, selected entries)
  accent             #006efa    Main color of buttons, tabs and selected things
  accent-hover       #2a88ff    Buttons under the mouse
  accent-text        #ffffff    Text on accent colored buttons
  accent-soft        #006efa4d  Selected side bar entry, active filter button
  accent-faint       #006efa2e  Player that is open in the player panel
  banner-bg          #006efa24  Info boxes
  banner-border      #006efa80  Border of info boxes
  banner-text        #bcd9ff    Text in info boxes

Requirements
  ok                 #57c96b    You meet it, your own party
  ok-soft            #57c96b2e  Background of your own party tag
  ok-border          #57c96b8c  Border of requirements you meet
  bad                #ee5c5c    You don't meet it
  bad-border         #ee5c5c99  Border of requirements you don't meet
  danger             #da373c    Close and remove buttons under the mouse
  estimated          #c9a7f0    Estimated values (marked with ~ or +)

Inputs and buttons
  input-bg           #1e1f22    Text fields, dropdowns, number fields, chips
  input-border       #4e5058    Their border
  input-border-hover #6d6f78    Their border under the mouse
  control            #3c3f45    Normal buttons
  control-border     #55595f    Border of normal buttons
  control-hover      #4a4e55    Normal buttons under the mouse
  control-active     #2f3236    Normal buttons while pressed
  disabled           #4e5058    Buttons you can't press
  disabled-text      #949ba4    Text on buttons you can't press

Popups
  menu               #2b2d31    Dropdown lists and dialogs
  popup              #111214    Tooltips, right click menus, messages bottom right
  highlight          #3f4248    Dropdown entry under the mouse
  backdrop           #0000008c  Darkens the window behind a dialog
  shadow             #00000099  Shadow of the player panel in small windows
  scrollbar-thumb    #ffffff80  Scroll bar
  scrollbar-track    #00000020  Track behind the scroll bar
  toast-success      #3ba55d    Stripe of success messages
  toast-warning      #faa61a    Stripe of warnings
  toast-error        #ed4245    Stripe of error messages
  menu-danger        #f47b7d    Red entries in right click menus
  tint               #ffffff0f  Light shine on tabs and segments under the mouse (use a dark one on light themes)
  tint-strong        #ffffff14  The same for a focused segment
  glow               #55ffff73  Glow of the title in the SBO hub
  glow-soft          #55ffff24  Soft glow behind the hub banner and its tiles

Icons
  star               #f0b232    Filled favorite star
  The other icons use text-soft (refresh), dim (empty star), muted (info) and input-border-hover (drag handle).

Hypixel colors (only used when hypixelColors is true)
  mc-black #000000      mc-dark-blue #0000aa   mc-dark-green #00aa00   mc-dark-aqua #00aaaa
  mc-dark-red #aa0000   mc-dark-purple #aa00aa mc-gold #ffaa00         mc-gray #aaaaaa
  mc-dark-gray #555555  mc-blue #5555ff        mc-green #55ff55        mc-aqua #55ffff
  mc-red #ff5555        mc-light-purple #ff55ff mc-yellow #ffff55      mc-white #ffffff
  These are the Minecraft text colors. Item rarities and the SkyBlock level use them,
  e.g. Legendary is mc-gold and Mythic is mc-light-purple.

Trophy Fisher titles (only used when hypixelColors is true)
  trophy-novice #cd7f32 (bronze)   trophy-adept #c9d1d9 (silver)   trophy-expert #ffaa00 (gold)   trophy-master #5ce1e6 (diamond)

Party types (only used when hypixelColors is true; name in the side bar and the heading)
  type-diana       gold           type-fishing  aqua           type-mining  dark aqua
  type-kuudra      red            type-bestiary   dark purple  type-rift    light purple
  type-safari      green          type-slayer   dark red       type-custom  gray
