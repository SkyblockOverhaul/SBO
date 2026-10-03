SBO PARTY FINDER THEMES
=======================

Every .json file in this folder shows up as a theme in the party finder:
open the party finder, go to Settings > Look > Theme.
The list is read again every time you open the settings, no restart needed.

SBO writes this README again on every start, so changes to it are lost.
example.json is only created once, you can change or delete it.


MAKE YOUR OWN THEME
-------------------
1. Copy example.json and give the copy a new name, e.g. my-theme.json.
2. Open it with any text editor and change "name" and the colors.
3. Open the party finder settings and pick your theme.

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
               One of: sbo-dark, hypixel, colorblind, high-contrast, light, midnight, purple
hypixelColors  true colors values like Hypixel does (item rarities, SkyBlock level, Trophy Fisher, party types).
               Without it, the setting of the base theme is used.
marks          true gives requirements you don't meet a dashed border (a "x" in front where there is no border).
               Without it, the base theme decides.
colors         Only the colors you want to change. Unknown names are ignored.

Colors are written as #rgb, #rgba, #rrggbb or #rrggbbaa, for example "#ff8800" or "#ff880080".
The last two digits of #rrggbbaa are the transparency: 00 is invisible, 80 is half, ff is solid.
A file with a mistake is skipped, check the Minecraft log for "Party finder theme".


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
