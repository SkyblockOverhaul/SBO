import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { checkDiana } from "../../utils/checkDiana";
import { getWorld } from "../../utils/world";
import { SboOverlay, OverlayTextLine, OverlayButton, hoverText } from "../../utils/overlays";
import Countdown from "../../utils/Countdown";

class Combo {
    constructor() {
        this.type = 0;
        this.expired = false;
    }
    
    setType(type) {
        this.type = type;
        this.expired = false;
    }

    expire() {
        this.expired = true;
    }
}

let combo = new Combo();
let countdownTimer = null;

function startCountdownTimer(comboType) {
    let time;
    switch (comboType) {
        case 5: time = 7.5; break;
        case 10: time = 5.9; break;
        case 15: time = 4.5; break;
        case 20: time = 3.5; break;
        case 25: time = 2.9; break;
        case 30: time = 1.5; break;
        default: combo.expired = true; return;
    }

    if (countdownTimer) {
        countdownTimer.stop();
    }

    countdownTimer = new Countdown(
        time,
        (remainingTime) => console.log(`Timer: ${remainingTime} seconds`),
        () => {
            console.log("Countdown beendet");
            combo.expire();
        }
    );
    
    countdownTimer.start();
}

register("actionBar", (before, gain, type, amount, next, after) => {
    if (type !== "Combat" || combo.expired) return;
    startCountdownTimer(combo.type);
}).setCriteria("${before}+${gain} ${type} (${amount}/${next})${after}");

register("chat", (type, trash) => {
    combo.setType(parseInt(type));
    startCountdownTimer(combo.type);
    ChatLib.chat(`Combo Type: ${combo.type}, Expired: ${combo.expired}`);
}).setCriteria("${type} Kill Combo ${trash}");

register("chat", (amount) => {
    combo.expire();
    if (countdownTimer) countdownTimer.stop();
    ChatLib.chat(`Combo Expired: ${combo.expired}`);
}).setCriteria("&r&cYour Kill Combo has expired! You reached a ${amount} Kill Combo!&r");
