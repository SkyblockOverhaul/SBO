import settings from "../../settings";
import { effectsOverlay } from "../guis/DianaGuis";
import { data, registerWhen } from "../../utils/variables";
import { isInSkyblock } from "../../utils/functions";
import { getWorld } from "../../utils/world";


let effects = [];
register("chat", () => {
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
}).setCriteria("&a&lBUFF! &fYou splashed yourself with &r&bWisp's Ice-Flavored Water I&r&f! Press TAB or type /effects to view your active effects!&r");

register("chat", () => {
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
}).setCriteria("&r&aYou ate a &r&aRe-heated Gummy Polar Bear&r&a!&r");

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
    effectsOverlay(data.effects);
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


