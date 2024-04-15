import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { state, loadGuiSettings, saveGuiSettings, playerHasSpade } from "../../utils/functions";
import { Overlay } from "../../utils/overlay";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE} from "../../utils/constants";
import { getDateMayorElected } from "../../utils/mayor";
import { dianaLootTrackerExample, dianaMobTrackerExample} from "../../utils/guiExamples";
import { UIBlock, UIWrappedText, ChildBasedRangeConstraint } from "../../../Elementa";
import { setOverlay, getGuiOpen } from "../../utils/overlays";

registerWhen(register("entityDeath", (entity) => {
    let dist = entity.distanceTo(Player.getPlayer());
    if (dist < 10 ) {
        state.entityDeathOccurred = true;
        setTimeout(() => {
            state.entityDeathOccurred = false;
        }, 2000);
    }
}), () => getWorld() === "Hub" && settings.dianaLootTracker);



// ${GRAY}${BOLD}Enchanted Gold: 
// ${GRAY}${BOLD}Enchanted Iron: 
// ${GRAY}${BOLD}Enchanted Ancient Claw: 
// ${GRAY}${BOLD}Ancient Claw: 

let guiSettings = loadGuiSettings();
const Color = Java.type("java.awt.Color");

let DianaMobTracker = new Overlay("dianaMobTrackerView",["Hub"], [10, 10, 0],"sbomoveMobCounter",dianaMobTrackerExample,"dianaMobTracker");
let DianaLootTracker = new Overlay("dianaLootTrackerView",["Hub"], [10, 10, 0],"sbomoveLootCounter",dianaLootTrackerExample,"dianaLootTracker");



let mobSettingsLoad = false;
/**
 * 
 * @param {string} setting 
 */
export function mobOverlay(mobTracker, setting, percentDict) {
    if (!mobSettingsLoad) {
        if(guiSettings != undefined) {
            DianaMobTracker.setX(guiSettings["MobLoc"]["x"]);
            DianaMobTracker.setY(guiSettings["MobLoc"]["y"]);
            DianaMobTracker.setScale(guiSettings["MobLoc"]["s"]);
            mobSettingsLoad = true;
        }
    }
    if (guiSettings["MobLoc"]["x"] != DianaMobTracker.X || guiSettings["MobLoc"]["y"] != DianaMobTracker.Y || guiSettings["MobLoc"]["s"] != DianaMobTracker.S) {
        guiSettings["MobLoc"]["x"] = DianaMobTracker.X;
        guiSettings["MobLoc"]["y"] = DianaMobTracker.Y;
        guiSettings["MobLoc"]["s"] = DianaMobTracker.S;
        saveGuiSettings(guiSettings);
    }
    if (setting == 2) {
        mobTracker = mobTracker[getDateMayorElected().getFullYear()] 
    }
    if (setting > 0) {
        switch (setting) {
            case 1:
                mobTrackerType = "Total";
                break;
            case 2:
                mobTrackerType = "Event";
                break;
            case 3:
                mobTrackerType = "Session";
                break;
        };
    DianaMobTracker.message =
    `${YELLOW}${BOLD}Diana Mob Tracker ${GRAY}(${YELLOW}${BOLD}${mobTrackerType}${GRAY})
------------------
${LIGHT_PURPLE}${BOLD}Minos Inquisitor: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Inquisitor"]} ${GRAY}(${AQUA}${percentDict["Minos Inquisitor"]}%${GRAY})
${DARK_PURPLE}${BOLD}Minos Champion: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Champion"]} ${GRAY}(${AQUA}${percentDict["Minos Champion"]}%${GRAY})
${GOLD}${BOLD}Minotaur: ${AQUA}${BOLD}${mobTracker["mobs"]["Minotaur"]} ${GRAY}(${AQUA}${percentDict["Minotaur"]}%${GRAY})
${GREEN}${BOLD}Gaia Construct: ${AQUA}${BOLD}${mobTracker["mobs"]["Gaia Construct"]} ${GRAY}(${AQUA}${percentDict["Gaia Construct"]}%${GRAY})
${GREEN}${BOLD}Siamese Lynx: ${AQUA}${BOLD}${mobTracker["mobs"]["Siamese Lynxes"]} ${GRAY}(${AQUA}${percentDict["Siamese Lynxes"]}%${GRAY})
${GREEN}${BOLD}Minos Hunter: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Hunter"]} ${GRAY}(${AQUA}${percentDict["Minos Hunter"]}%${GRAY})
${GRAY}${BOLD}Total Mobs: ${AQUA}${BOLD}${mobTracker["mobs"]["TotalMobs"]}
`
    }
}

let lootSettingsLoad = false;
let mobTrackerType = undefined;
let lootTrackerType = undefined;
/**
 * 
 * @param {string} setting 
 */
export function itemOverlay(lootTracker, lootViewSetting, percentDict){
    if (!lootSettingsLoad) {
        if(guiSettings != undefined) {
            DianaLootTracker.setX(guiSettings["LootLoc"]["x"]);
            DianaLootTracker.setY(guiSettings["LootLoc"]["y"]);
            DianaLootTracker.setScale(guiSettings["LootLoc"]["s"]);
            lootSettingsLoad = true;
        }
    }
    if (guiSettings["LootLoc"]["x"] != DianaLootTracker.X || guiSettings["LootLoc"]["y"] != DianaLootTracker.Y || guiSettings["LootLoc"]["s"] != DianaLootTracker.S) {
        guiSettings["LootLoc"]["x"] = DianaLootTracker.X;
        guiSettings["LootLoc"]["y"] = DianaLootTracker.Y;
        guiSettings["LootLoc"]["s"] = DianaLootTracker.S;
        saveGuiSettings(guiSettings);
    }
    if (lootViewSetting == 2) {
        lootTracker = lootTracker[getDateMayorElected().getFullYear()] 
    }
    if (lootViewSetting > 0) {
        DianaLootTracker.message = getLootMessage(lootTracker, lootViewSetting, settings.dianaMobTracker, percentDict);
    }
}

function getLootMessage(lootTracker, lootViewSetting, mobSetting, percentDict) {
    switch (lootViewSetting) {
        case 1:
            lootTrackerType = "Total";
            break;
        case 2:
            lootTrackerType = "Event";
            break;
        case 3:
            lootTrackerType = "Session";
            break;
    };
    let lootMessage = `${YELLOW}${BOLD}Diana Loot Tracker ${GRAY}(${YELLOW}${BOLD}${lootTrackerType}${GRAY})
-------------------
`;
    if (mobSetting) {
        lootMessage += `${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]} ${GRAY}(${AQUA}${percentDict["Chimera"]}%${GRAY})
${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]} ${GRAY}(${AQUA}${percentDict["Minos Relic"]}%${GRAY})
${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]} ${GRAY}(${AQUA}${percentDict["Daedalus Stick"]}%${GRAY})
`
    }
    else {
        lootMessage += `${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]}
${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]}
${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]}
`
    }
    lootMessage += `${GOLD}${BOLD}Crown of Greed: ${AQUA}${BOLD}${lootTracker["items"]["Crown of Greed"]}
${GOLD}${BOLD}Souvenir: ${AQUA}${BOLD}${lootTracker["items"]["Washed-up Souvenir"]}
${DARK_GREEN}${BOLD}Turtle Shelmet: ${AQUA}${BOLD}${lootTracker["items"]["DWARF_TURTLE_SHELMET"]}
${DARK_GREEN}${BOLD}Tiger Plushie: ${AQUA}${BOLD}${lootTracker["items"]["CROCHET_TIGER_PLUSHIE"]}
${DARK_GREEN}${BOLD}Antique Remedies: ${AQUA}${BOLD}${lootTracker["items"]["ANTIQUE_REMEDIES"]}
${GOLD}${BOLD}Griffin Feather: ${AQUA}${BOLD}${lootTracker["items"]["Griffin Feather"]}
`
    if (lootTracker["items"]["coins"] > 1000000) {
        lootMessage += `${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${(lootTracker["items"]["coins"]/1000000).toFixed(2)}M
`
    }
    else if (lootTracker["items"]["coins"] > 1000) {
        lootMessage += `${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${Math.round(lootTracker["items"]["coins"]/1000)}K
`
    }
    else {
        lootMessage += `${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${lootTracker["items"]["coins"]}
`
    }
    lootMessage += `${BLUE}${BOLD}Ancient Claws: ${AQUA}${BOLD}${lootTracker["items"]["ANCIENT_CLAW"]}
${BLUE}${BOLD}Enchanted Ancient Claws: ${AQUA}${BOLD}${lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]}
${GRAY}${BOLD}Total Burrows: ${AQUA}${BOLD}${lootTracker["items"]["Total Burrows"]}
`
    return lootMessage;
}

// ${GRAY}${BOLD}Enchanted Gold: ${GRAY}${lootTracker["items"]["ENCHANTED_GOLD"]}
// ${GRAY}${BOLD}Enchanted Iron: ${GRAY}${lootTracker["items"]["ENCHANTED_IRON"]}
// ${GRAY}${BOLD}Enchanted Ancient Claw: ${GRAY}${lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]}
// ${GRAY}${BOLD}Ancient Claw: ${GRAY}${lootTracker["items"]["ANCIENT_CLAW"]}



// let MythosMobHp = new Overlay("mythosMobHp",["Hub"], [10, 10, 1],"sbomoveMythosHp",mythosMobHpExample,"mythosMobHp");

let loadedMythosHp = false;
let mythosHpOverlay = new UIBlock(new Color(0.2, 0.2, 0.2, 0));
let mythosHpSelected = false;
setOverlay(mythosHpOverlay, mythosHpSelected, "mythosMobHpOverlay");
const mythoHpOffset = {x: 0, y: 0};

mythosHpOverlay.setWidth(new ChildBasedRangeConstraint());
mythosHpOverlay.setHeight(new ChildBasedRangeConstraint());
mythosHpOverlay.onMouseClick((comp, event) => {
    mythosHpSelected = true;
    mythoHpOffset.x = event.absoluteX;
    mythoHpOffset.y = event.absoluteY;
});

mythosHpOverlay.onMouseRelease(() => {
    mythosHpSelected = false;
});

mythosHpOverlay.onMouseDrag((comp, mx, my) => {
    if(!mythosHpSelected) return;
    guiSettings = loadGuiSettings();
    const absoluteX = mx + comp.getLeft()
    const absoluteY = my + comp.getTop()
    const dx = absoluteX - mythoHpOffset.x;
    const dy = absoluteY - mythoHpOffset.y;
    mythoHpOffset.x = absoluteX;
    mythoHpOffset.y = absoluteY;
    const newX = mythosHpOverlay.getLeft() + dx;
    const newY = mythosHpOverlay.getTop() + dy;
    mythosHpOverlay.setX(newX.pixels());
    mythosHpOverlay.setY(newY.pixels());
    guiSettings["MythosHpLoc"]["x"] = newX;
    guiSettings["MythosHpLoc"]["y"] = newY;
    saveGuiSettings(guiSettings);
});

let mythosMobHpText = new UIWrappedText("");
// mythosHpOverlay.addChild(mythosMobHpText);

function loadMythosHpOverlay() {
    if(guiSettings != undefined && !loadedMythosHp) {
        mythosHpOverlay.setX((guiSettings["MythosHpLoc"]["x"]).pixels());
        mythosHpOverlay.setY((guiSettings["MythosHpLoc"]["y"]).pixels());
        loadedMythosHp = true;
    }
}
loadMythosHpOverlay();

export function mythosMobHpOverlay(mobNamesWithHp) {
    if(getGuiOpen()) return
    if(!mythosHpOverlay.children.includes(mythosMobHpText)) {
        mythosHpOverlay.clearChildren();
        mythosHpOverlay.addChild(mythosMobHpText);
    }
    let message = "";
    if (mobNamesWithHp.length > 0) {
        message = "";
        mobNamesWithHp.forEach((mob) => {
            message += `${mob}\n`;
        });
    }
    else {
        message = "";
    }
    mythosMobHpText.setText(message);
}

register("step", () => {
    if (playerHasSpade()) {
        DianaMobTracker.setRenderGuiBool(true);
        DianaLootTracker.setRenderGuiBool(true);
    }
    else if (!playerHasSpade()) {
        DianaMobTracker.setRenderGuiBool(false);
        DianaLootTracker.setRenderGuiBool(false);
    }
}).setFps(1);