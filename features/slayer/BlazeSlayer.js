import settings from "../../settings";
import { data, registerWhen } from "../../utils/variables";
import { isInSkyblock } from "../../utils/functions";
import { loadGuiSettings, saveGuiSettings } from "../../utils/functions";
import { UIBlock, UIWrappedText, ChildBasedRangeConstraint } from "../../../Elementa";
import { YELLOW, BOLD, WHITE, AQUA,} from "../../utils/constants";



let guiSettings = loadGuiSettings();
let loadedEffects = false;
const Color = Java.type("java.awt.Color");
export let effectsOverlaySelected = false;
export let effectsOverlay = new UIBlock(new Color(0.2, 0.2, 0.2, 0));
const dragOffset = {x: 0, y: 0};

effectsOverlay.setWidth(new ChildBasedRangeConstraint());
effectsOverlay.setHeight(new ChildBasedRangeConstraint());
effectsOverlay.onMouseClick((comp, event) => {
    effectsOverlaySelected = true;
    dragOffset.x = event.absoluteX;
    dragOffset.y = event.absoluteY;
});

effectsOverlay.onMouseRelease(() => {
    effectsOverlaySelected = false;
});

effectsOverlay.onMouseDrag((comp, mx, my) => {
    if(!effectsOverlaySelected) return;
    guiSettings = loadGuiSettings();
    const absoluteX = mx + comp.getLeft()
    const absoluteY = my + comp.getTop()
    const dx = absoluteX - dragOffset.x;
    const dy = absoluteY - dragOffset.y;
    dragOffset.x = absoluteX;
    dragOffset.y = absoluteY;
    const newX = effectsOverlay.getLeft() + dx;
    const newY = effectsOverlay.getTop() + dy;
    effectsOverlay.setX(newX.pixels());
    effectsOverlay.setY(newY.pixels());
    guiSettings["EffectsLoc"]["x"] = newX;
    guiSettings["EffectsLoc"]["y"] = newY;
    saveGuiSettings(guiSettings);
});

function loadEffectsOverlay() {
    if(guiSettings != undefined && !loadedEffects) {
        effectsOverlay.setX((guiSettings["EffectsLoc"]["x"]).pixels());
        effectsOverlay.setY((guiSettings["EffectsLoc"]["y"]).pixels());
        loadedEffects = true;
    }
}
loadEffectsOverlay();

let effectsText = new UIWrappedText("Active Effects");
function refreshEffectOverlay(effects) {
    let pixelIncrementOne = 15;
    let height = 10;
    if(!effectsOverlay.children.includes(effectsText)) {
        effectsOverlay.clearChildren();
        effectsOverlay.addChild(effectsText);
    }
    let message = "";
    if (effects.length > 0) {
        message = `${YELLOW}${BOLD}Active Effects
--------------
`;
        // add to message each effect and duration and if duration is over 60s convert to minutes and if over 3600s convert to hours
        effects.forEach((effect) => {
            height += pixelIncrementOne;
            let duration = effect.duration;
            let durationMessage = "";
            if (duration > 3600) {
                durationMessage = `${Math.floor(duration/3600)}h `;
                duration = duration % 3600;
            }
            if (duration > 60) {
                durationMessage += `${Math.floor(duration/60)}m `;
                duration = duration % 60;
            }
            if (duration > 0) {
                durationMessage += `${Math.floor(duration)}s`;
            }
            message += `${AQUA}${BOLD}${effect.name}: ${WHITE}${durationMessage}\n`;
        });
        effectsText.setHeight((height).pixels());
        print(message);
        effectsText.setText(message);
    }
    else {
        effectsText.setHeight((height).pixels());
        effectsText.setText(` `);
    }
}


let effects = [];
registerWhen(register("chat", () => {
    // ChatLib.chat("Buff received!");
    let baseDuration = 1800;
    if (effects.some(e => e.name === "Wisp's Water")) {
        let wisp = effects.find(e => e.name === "Wisp's Water");
        wisp.duration = calcDuration(baseDuration);
        wisp.timeStamp = Date.now();
        return;
    }
    else {
        effects.push({
            name: "Wisp's Water",
            duration: calcDuration(baseDuration),
            timeStamp: Date.now(),
            loggedOff: false,
        });
    }
}).setCriteria("&a&lBUFF! &fYou splashed yourself with &r&bWisp's Ice-Flavored Water I&r&f! Press TAB or type /effects to view your active effects!&r"), () => settings.effectsGui);;

registerWhen(register("chat", () => {
    let baseDuration = 3600;
    if (effects.some(e => e.name === "Gummy Bear")) {
        let gummy = effects.find(e => e.name === "Gummy Bear");
        gummy.duration = baseDuration;
        gummy.timeStamp = Date.now();
        return;
    }
    else {
        effects.push({
            name: "Gummy Bear",
            duration: baseDuration,
            timeStamp: Date.now(),
            loggedOff: false
        });
    }
}).setCriteria("&r&aYou ate a &r&aRe-heated Gummy Polar Bear&r&a!&r"), () => settings.effectsGui);;

let first = true;
registerWhen(register("step", () => {
    if (first) {
        first = false;
        effects = data.effects;
    }
    checkLogOff();
    updateEffectTime();
    data.effects = effects;
    // remove all effects with duration <= 0
    effects = effects.filter(e => e.duration > 0);
    refreshEffectOverlay(data.effects);
}).setFps(1), () => settings.effectsGui);

let loggedOff = true;
function checkLogOff() {
    if ((Server.getName() == "" || !isInSkyblock()) && !loggedOff) {
        // print("Logged off!");
        loggedOff = true;
    }
    else if ((Server.getName() != "" || isInSkyblock()) && loggedOff) {
        // print("Logged on!");
        data.effects.forEach(e => {
            e.timeStamp = Date.now();
        });
        loggedOff = false;
    }
}

function updateEffectTime() {
    if (!loggedOff) {
        effects.forEach(e => {
            e.duration = e.duration - (Date.now() - e.timeStamp)/1000;
            e.timeStamp = Date.now();
        });
    }
}

function calcDuration(baseDuration) {
    // remove all characters that are not numbers
    settings.parrotLevel = settings.parrotLevel.replace(/\D/g, '');
    let parrotLevel = parseInt(settings.parrotLevel);
    if (parrotLevel > 100) {
        parrotLevel = 100;
    }
    else if (parrotLevel < 0) {
        parrotLevel = 0;
    }
    return baseDuration*(1 + settings.parrotLevel*0.004)
}

register("worldUnload", () => {
    loggedOff = true;
})


