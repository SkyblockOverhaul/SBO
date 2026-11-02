SBO CSS REFERENCE
=================

The .css files in this folder are copies of the CSS that SBO itself uses. They are only here so you can
look up which classes exist and what SBO sets on them.

  - They are NOT loaded. Changing them does nothing in the game.
  - SBO writes them again so they match your SBO version, so changes here are lost anyway.
  - To change something: copy the rule into user.css or your own .css theme in the folder above
    (config/sbo/themes), change it there, done. The README one folder up explains themes and user.css.
  - A mistake in your CSS shows a popup in the window with the line and what is wrong.


WHICH FILE IS WHICH WINDOW
--------------------------
  themes.css         The built-in themes (body.sbo-theme-...). Shows which colors each theme sets.
  hub.css            The SBO hub (/sbo)                                   body.sbo-window-hub
  settings.css       The SBO settings (/sbosettings)                      body.sbo-window-settings
  partyfinder.css    The party finder                                     body.sbo-window-partyfinder
  profileviewer.css  The profile viewer (also uses partyfinder.css)       body.sbo-window-profileviewer
  events.css         Events                                               body.sbo-window-events
  achievements.css   Achievements                                         body.sbo-window-achievements
  sounds.css         Sounds                                               body.sbo-window-sounds
  cloud.css          Cloud sync                                           body.sbo-window-cloud
  badge.css          Badges                                               body.sbo-window-badge
  elements.css       Default look of the plain elements in every window: button, input, select,
                     checkbox, switch, slider, scroll bars, tooltips, menus, dialogs, toasts ...
                     The classes starting with "guilib-" come from here.

The HUD overlays (trackers, Crown of Avarice) can't be styled, only the windows.


HOW TO FIND THE CLASS OF SOMETHING
----------------------------------
Easiest: open the window in the game and press Ctrl + Shift + C. That turns on the inspector:
  - the element under the mouse gets a frame and a box shows its tag, classes and the path to it
  - left click copies its selector to the clipboard, paste it into your CSS
  - press Ctrl + Shift + C or Escape again to turn it off

Or search the files here: each window's classes start with a short name, e.g.
  hub-   hub.css          cfg-   settings.css     pf-    partyfinder.css   pv-   profileviewer.css
  ev-    events.css       ach-   achievements.css snd-   sounds.css        cs-   cloud.css
  bd-    badge.css


WHAT STAYS THE SAME IN UPDATES
------------------------------
These are kept stable, rules on them keep working:
  body.sbo, body.sbo-window-<name>, body.sbo-theme-<base>, the color variables (--accent, --bg, ...)
  and the plain elements (button, input, ...).

All other classes are SBO's inner workings and can change in any update. If your CSS stops working
after an update, compare it with the fresh copies here.


A SHORT EXAMPLE
---------------
In hub.css you find:

  .hub-close:hover { color: var(--text-strong); background-color: var(--danger); }

To make the close button of the hub green under the mouse, put this into user.css:

  body.sbo-window-hub .hub-close:hover { background-color: #3ba55d; }
