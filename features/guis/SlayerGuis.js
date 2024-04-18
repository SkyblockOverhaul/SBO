// import settings from "../../settings";
// import { registerWhen } from "../../utils/variables";
// import { getWorld } from "../../utils/world";
// import { loadGuiSettings, saveGuiSettings } from "../../utils/functions";
// import { Overlay } from "../../utils/overlay";
// import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE} from "../../utils/constants";
// import { blazeLootTrackerExample } from "../../utils/guiExamples";
// import {  } from "../slayer/SlayerDropDetect";

// let guiSettings = loadGuiSettings();

// let BlazeLootTracker = new Overlay("blazeLootTrackerView",["Crimson Isle"], [10, 10, 0],"sbomoveBlazeCounter",blazeLootTrackerExample,"blazeLootTracker");

// let slayerSettingLoaded = false;
// let blazeTrackerType = undefined;

// function blazeLootTrackerOverlay(lootTracker) {
//     if (!slayerSettingLoaded) {
//         if(guiSettings != undefined) {
//             BlazeLootTracker.setX(guiSettings["BlazeLoc"]["x"]);
//             BlazeLootTracker.setY(guiSettings["BlazeLoc"]["y"]);
//             BlazeLootTracker.setScale(guiSettings["BlazeLoc"]["s"]);
//             slayerSettingLoaded = true;
//         }
//     }
//     if (guiSettings["BlazeLoc"]["x"] != BlazeLootTracker.X || guiSettings["BlazeLoc"]["y"] != BlazeLootTracker.Y || guiSettings["BlazeLoc"]["s"] != BlazeLootTracker.S) {
//         guiSettings["BlazeLoc"]["x"] = BlazeLootTracker.X;
//         guiSettings["BlazeLoc"]["y"] = BlazeLootTracker.Y;
//         guiSettings["BlazeLoc"]["s"] = BlazeLootTracker.S;
//         saveGuiSettings(guiSettings);
//     }
//     if (settings.blazeLootTrackerView == 2) {
//         // lootTracker = lootTracker[getDateMayorElected().getFullYear()] 
//     }
//     if (settings.blazeLootTrackerView > 0) {
//         switch (settings.blazeLootTrackerView) {
//             case 1:
//                 blazeTrackerType = "Total";
//                 break;
//             case 2:
//                 blazeTrackerType = "Event";
//                 break;
//             case 3:
//                 blazeTrackerType = "Session";
//                 break;
//         };
//         BlazeLootTracker.message = `${YELLOW}${BOLD}Blaze Loot Tracker ${GRAY}(${YELLOW}${BOLD}${blazeTrackerType}${GRAY})
// --------------------------
// `
//     }
// }

// registerWhen(register("step", () => {
//     blazeLootTrackerOverlay();
// }).setFps(1), () => settings.blazeLootTracker);



// // ${WHITE}${BOLD}Ice-Flavored: ${AQUA}${BOLD}${lootTracker["Ice-Flavored"]}
// // ${WHITE}${BOLD}Fire Aspect III: ${AQUA}${BOLD}${lootTracker["Fire Aspect III"]}
// // ${GREEN}${BOLD}Flawed Opal Gems: ${AQUA}${BOLD}${lootTracker["Flawed Opal Gems"]}
// // ${BLUE}${BOLD}Lavatears Runes: ${AQUA}${BOLD}${lootTracker["Lavatears Runes"]}
// // ${BLUE}${BOLD}Mana Disintegrator: ${AQUA}${BOLD}${lootTracker["Mana Disintegrator"]}
// // ${BLUE}${BOLD}Kelvin Inverter: ${AQUA}${BOLD}${lootTracker["Kelvin Inverter"]}
// // ${BLUE}${BOLD}Blaze Rod Dist: ${AQUA}${BOLD}${lootTracker["Blaze Rod Dist"]}
// // ${BLUE}${BOLD}Glowstone Dist: ${AQUA}${BOLD}${lootTracker["Glowstone Dist"]}
// // ${BLUE}${BOLD}Magma Cream Dist: ${AQUA}${BOLD}${lootTracker["Magma Cream Dist"]}
// // ${BLUE}${BOLD}Nether Wart Dist: ${AQUA}${BOLD}${lootTracker["Nether Wart Dist"]}
// // ${BLUE}${BOLD}Gabagool Dist: ${AQUA}${BOLD}${lootTracker["Gabagool Dist"]}
// // ${DARK_PURPLE}${BOLD}Magma Arrows: ${AQUA}${BOLD}${lootTracker["Magma Arrows"]}
// // ${DARK_PURPLE}${BOLD}Archfiend Dice: ${AQUA}${BOLD}${lootTracker["Archfiend Dice"]}
// // ${GOLD}${BOLD}Fiery Burst Rune: ${AQUA}${BOLD}${lootTracker["Fiery Burst Rune"]}
// // ${GOLD}${BOLD}Scorched Power: ${AQUA}${BOLD}${lootTracker["Scorched Power"]}
// // ${GOLD}${BOLD}Engineering Plans: ${AQUA}${BOLD}${lootTracker["Engineering Plans"]}
// // ${GOLD}${BOLD}Subzero Inverter: ${AQUA}${BOLD}${lootTracker["Subzero Inverter"]}
// // ${GOLD}${BOLD}High Class Dice: ${AQUA}${BOLD}${lootTracker["High Class Dice"]}
// // ${LIGHT_PURPLE}${BOLD}Duplex: ${AQUA}${BOLD}${lootTracker["Duplex"]}
// // ${LIGHT_PURPLE}${BOLD}Scorched Books: ${AQUA}${BOLD}${lootTracker["Scorched Books"]}
// // ${GRAY}${BOLD}Blaze Killed: ${AQUA}${BOLD}${lootTracker["Blaze Killed"]}