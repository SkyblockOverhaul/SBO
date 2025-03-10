<h1 align="center">
  [SBO] SkyblockOverhaul: Mod for Hypixel SkyBlock
</h1>


<div align="center">
  
[![Discord](https://img.shields.io/discord/1163913835514699886?label=discord&color=9089DA&logo=discord&style=for-the-badge)](https://discord.gg/QvM6b9jsJD)
</div>

## What it does

[SBO] SkyblockOverhaul is a feature‐rich ChatTriggers module for Minecraft 1.8.9 designed primarily for the Diana event.

## Features Overview

### General
- **Bobber Counter:** Display an overlay tracking nearby bobbers.
- **Legion Counter:** Monitor nearby players to optimize legion buffs.
- **GUI Mover:** Quickly reposition in-game overlays via the “Move GUIs” menu.

### Diana

#### Diana Burrows
- **Diana Burrow Guess:** Automatically guess the burrow location using particle effects.
- **Burrow Guess Alternative:** *(WIP)* An alternate guess method based on post-burrow arrow color.
- **Diana Burrow Warp:** Warp close to the guessed burrow (keybind configurable).
- **Don’t Warp If Burrow Nearby:** Prevents warping if a burrow is already detected.
- **Diana Burrow Detect:** Actively detects Diana burrows (reset via `/sboclearburrows`).
- **Warp Block Difference:** Adjust the sensitivity for warping differences.
- **Warp Delay:** Optionally delay warps to improve guess precision.

#### Diana Tracker
- **Diana Tracker:** Keep track of your loot and mob kills during the event.
- **Mob & Loot Views:** Toggle between Total, Event, or Session views for mob kills and loot.
- **Inquis Loot Tracker:** Monitor inquisitor loot (Shelmet, Plushie, Remedies).
- **Four-Eyed Fish Toggle:** Enable if you have a Four-Eyed Fish on your griffin pet.
- **Diana Stats & Message:** View event statistics and optionally send chat messages summarizing your progress.
- **Avg Magic Find Tracker:** Calculate your average magic find during the event.
- **Bazaar Setting:** Choose your preferred bazaar price setting (Instasell or Sell Offer).
- **Reset Session:** Reset your Diana event session data with a single button.

#### Diana Waypoints & Lines
- **Inquis Line, Burrow Line & Guess Line:** Draw visual lines to inquisitors, burrows, or your guess.
- **Line Width:** Customize the thickness of drawn lines.
- **Detect Inq Cords:** Automatically create inquisitor waypoints.
- **All Waypoints Are Inqs:** In hub, treat all waypoints as inquisitor waypoints.
- **Inq Warp Key:** Enable a keybind to warp directly to an inquisitor.
- **Remove Guess & Distance for Remove:** Automatically remove guess markers when you’re within a set distance.

#### Loot Announcer & Other Diana Features
- **Rare Drop Announcer:** Announce rare drops in chat.
- **Loot Screen Announcer & Price Title:** Show on-screen notifications (with optional item price subtitles).
- **Loot Party Announcer:** Share loot events directly with your party.
- **Mythos HP:** Display the HP of mythological mobs.
- **Inquis Party Message:** Send inquisitor coordinates to your party.
- **Custom Inq Spawn Text:** Set custom messages (using placeholders like `{since}` and `{chance}`) that trigger on inquisitor spawns.
- **Highlight Inquis:** Customize a highlight color for inquisitors.
- **Add Warps:** Optionally add additional warps (e.g., Crypt, Wizard, Stonks, Dark Auction).
- **Custom Chim Message:** Personalize the chimera drop message with placeholders for Magic Find and drop counts.

### Kuudra
- **Attribute Value Overlay:** Displays an overlay with estimated values and attribute breakdowns for Kuudra chests.
- **Display Customization:** Configure the number of displayed items, line settings, key pricing, pet rarity, and more.
- **Key Discount Toggle:** Enable a discount mode if you meet the reputation requirements.

### Mining
- **Fossil Solver & Overlay:** Get hints on fossil locations and highlight all potential fossil slots.
- **Speed Boost Title:** Receive a title notification when you gain a speed boost.
- **Mineshaft Exit Waypoint:** Automatically create a waypoint at the mineshaft exit.

### Party Commands

#### General Party Commands
- **!warp / !w:** Warp to your party location.
- **!allinv / !allinvite:** Invite all party members.
- **!ptme / !transfer:** Transfer party leadership (to a specified player or yourself).
- **!promote / !demote:** Adjust party roles by promoting or demoting a player.
- **!c / !carrot:** Sends a fun "Carrot" response.
- **!time:** Displays the current local time.
- **!tps:** Shows the server’s TPS (ticks per second).

#### Diana Party Commands
*(Requires Diana Tracker and Diana Party Commands to be enabled)*
- **!chim, !chimera, !chims, !chimeras, !book, !books:** Display chimera drop counts (including LS drops).
- **!inqls, !inqsls, !inquisitorls, !inquisls, !lsinq, !lsinqs, !lsinquisitor, !lsinquis:** Show Inquisitor LS (Loot Share) count.
- **!inq, !inqs, !inquisitor, !inquis:** Display Inquisitor drop counts.
- **!burrows / !burrow:** Show total burrows detected (with burrows per hour).
- **!relic / !relics:** Display relic drop counts.
- **!chimls, !chimerals, !bookls, !lschim, !lsbook, !lootsharechim, !lschimera:** Show chimera LS (Loot Share) count with percentage.
- **!stick / !sticks:** Display Daedalus stick drop counts with percentage.
- **!feather / !feathers:** Show the count of Griffin Feathers dropped.
- **!mob / !mobs:** Display total mob count (with mobs per hour).
- **!mf / !magicfind:** Display average magic find for chimeras and sticks.
- **!since [type]:** Display event stats since a specified type (chim, stick, relic, inq, or lootshare chim).
- **!playtime:** Display playtime during the event.
- **!profit:** Show profit statistics from the event.
- **!stats [playername]:** Display detailed event statistics (playtime, profit, burrows, mobs, inquisitors, sticks, relics) for you or the specified player.

#### Additional Party Commands
- **/sbopartyblacklist:** Manage your party command blacklist.
  - **/sbopartyblacklist add <playername>:** Add a player to the blacklist.
  - **/sbopartyblacklist remove <playername>:** Remove a player from the blacklist.
  - **/sbopartyblacklist list:** List all blacklisted players.
- **/sbodropchance <magic find> <looting> (alias: /sbodc):** Calculate and display drop chances for chimera, stick, and relic based on magic find and looting values.
- **/sbopartycommands (alias: /sbopcom):** Display a list of available Diana party commands.


### Customization & Quality of Life
- **Color Customizations:** Personalize colors for burrows, waypoints, guesses, and slot highlights.
- **Custom Sounds:** Set custom sounds (with individual volume controls) for inquisitor spawns, burrow spawns, chimera drops, relic drops, and more.
- **Bridge Bot Formatter:** Automatically format bridge bot messages.
- **Clipboard & Chat Enhancements:** Copy rare drop messages, hide unwanted chat (Jacob messages, autopet, sacks, tipped players), and more.
- **Clickable Party Invites:** Invite players by simply clicking on chat messages.
- **Pickup Log Overlay:** View an overlay log of your pickups.
- **Crown Tracker:** Monitor your crown of avarice coins (with optional ghost mode and reset functionality).

### Additional Features
- **Debug Options:** Enable test features or force SkyBlock state for troubleshooting.
- **Credits & Infos:** Direct links to our Discord, GitHub, Patreon, Website, and partner projects for support and further information.

## Installation & Configuration

1. **Installation:**  
   - Download [ChatTriggers](https://www.chattriggers.com/) for Minecraft 1.8.9.  
   - Place the SBO module folder into your ChatTriggers modules directory.
   - or just do /ct import sbo. ingame

2. **Reloading:**  
   - Use `/ct load` after importing it for the first time!

## Support

For help, bug reports, or feature requests, please join our [Discord](https://discord.gg/QvM6b9jsJD).

---

This mod is actively maintained—your feedback and contributions are welcome!

---
