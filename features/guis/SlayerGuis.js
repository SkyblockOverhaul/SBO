import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { loadGuiSettings, saveGuiSettings } from "../../utils/functions";
import { Overlay } from "../../utils/overlay";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE} from "../../utils/constants";
import { getDropArray } from "../slayer/SlayerDropDetect";


let blazeLootTrackerExample = 
`${YELLOW}${BOLD}Blaze Loot Tracker
------------------
${WHITE}${BOLD}Ice-Flavored Waters:
${WHITE}${BOLD}Fire Aspect III:
${GREEN}${BOLD}Flawed Opal Gems:
${BLUE}${BOLD}Lavatears Runes:
${BLUE}${BOLD}Mana Disintegrator:
${BLUE}${BOLD}Kelvin Inverter:
${BLUE}${BOLD}Blaze Rod Dist:
${BLUE}${BOLD}Glowstone Dist:
${BLUE}${BOLD}Magma Cream Dist:
${BLUE}${BOLD}Nether Wart Dist:
${BLUE}${BOLD}Gabagool Dist:
${DARK_PURPLE}${BOLD}Magma Arrows:
${DARK_PURPLE}${BOLD}Archfiend Dice
${GOLD}${BOLD}Fiery Burst Rune:
${GOLD}${BOLD}Scorched Power:
${GOLD}${BOLD}Engineering Plans:
${GOLD}${BOLD}Subzero Inverter:
${GOLD}${BOLD}High Class Dice:
${LIGHT_PURPLE}${BOLD}Duplex:
${GRAY}${BOLD}Blaze Killed: 
`
guiSettings = loadGuiSettings();

let BlazeLootTracker = new Overlay("blazeLootTrackerView",["Crimson Isle"], [10, 10, 0],"sbomoveblazecounter",blazeLootTrackerExample,"blazeLootTracker");

let slayerSettingLoaded = false;
let blazeTrackerType = undefined;

function blazeLootTrackerOverlay(lootTracker) {
    if (!slayerSettingLoaded) {
        if(guiSettings != undefined) {
            BlazeLootTracker.setX(guiSettings["BlazeLoc"]["x"]);
            BlazeLootTracker.setY(guiSettings["BlazeLoc"]["y"]);
            BlazeLootTracker.setScale(guiSettings["BlazeLoc"]["s"]);
            mobSettingsLoad = true;
        }
    }
    if (guiSettings["BlazeLoc"]["x"] != BlazeLootTracker.X || guiSettings["BlazeLoc"]["y"] != BlazeLootTracker.Y || guiSettings["BlazeLoc"]["s"] != BlazeLootTracker.S) {
        guiSettings["BlazeLoc"]["x"] = BlazeLootTracker.X;
        guiSettings["BlazeLoc"]["y"] = BlazeLootTracker.Y;
        guiSettings["BlazeLoc"]["s"] = BlazeLootTracker.S;
        saveGuiSettings(guiSettings);
    }
    if (settings.blazeLootTrackerView == 2) {
        lootTracker = lootTracker[getDateMayorElected().getFullYear()] 
    }
    if(settings.blazeLootTrackerView > 0){
        switch (settings.blazeLootTrackerView) {
            case 1:
                blazeTrackerType = "Total";
                break;
            case 2:
                blazeTrackerType = "Event";
                break;
            case 3:
                blazeTrackerType = "Session";
                break;
        };
    }
}

registerWhen(register("step", () => {
    blazeLootTrackerOverlay();
}
).setFps(5), () => settings.blazeLootTracker);
