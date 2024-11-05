import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { checkDiana } from "../../utils/checkDiana";
import { getWorld } from "../../utils/world";
import { SboOverlay, OverlayTextLine, OverlayButton, hoverText } from "../../utils/overlays";

register("actionBar", (before, gain, type, amount, next, after) => {
    if (type !== "Combat" || combo.expired) return;
    timer.setComboType(combo.type);
    if (!combo.expired) timer.start();
}).setCriteria("${before}+${gain} ${type} (${amount}/${next})${after}");

register("chat", (type, trash) => {
    combo.type = parseInt(type);
    if (!combo.type) return;
    combo.expired = false;
    timer.setComboType(combo.type);
    timer.start();
    ChatLib.chat(combo.type);
    ChatLib.chat(combo.expired);
}).setCriteria("${type} Kill Combo ${trash}");

register("chat", (amount) => {
    combo.expired = true;
    timer.stop();
    ChatLib.chat(combo.expired);
}).setCriteria("&r&cYour Kill Combo has expired! You reached a ${amount} Kill Combo!&r");

class Combo {
    constructor() {
        this.type = 0;
        this.expired = false;
    }
}

class Timer {
    constructor(time, autostart = false, isCountdown = true) {
        this.defaultTime = time * 20;
        this.time = this.defaultTime;
        this.currenttime = isCountdown ? this.time : 0;
        this.ticks = isCountdown ? this.time : 0;
        this.isCountdown = isCountdown;
        this.tickEvent = null;
        this.active = false;

        if (autostart) this.start();

        register("worldUnload", () => {
            this.stop();
        });
    }

    setComboType(comboType) {
        switch(comboType) {
            case 5:
                this.time = 7.5 * 20;
                break;
            case 10:
                this.time = 5.9 * 20;
                break;
            case 15:
                this.time = 4.5 * 20;
                break;
            case 20:
                this.time = 3.5 * 20;
                break;
            case 25:
                this.time = 2.9 * 20;
                break;
            case 30:
                this.time = 1.5 * 20;
                break;
            default:
                this.active = false;
                return;
        }
        this.currenttime = this.time;
        this.ticks = this.time;
        this.active = true;
    }

    start() {
        this.stop();
        this.ticks = this.time;
        this.registerTicks();
        this.active = true;
    }

    registerTicks() {
        this.tickEvent = register("tick", () => {
            if (!this.active) return;

            this.isCountdown ? this.ticks-- : this.ticks++;

            if ((this.isCountdown && this.ticks <= 0) || (!this.isCountdown && this.ticks >= this.time)) {
                this.stop();
                console.log("Timer gestoppt");
            } else {
                console.log(`Timer: ${(this.ticks / 20).toFixed(3)} Sekunden`);
            }
        });
    }

    stop() {
        if (this.tickEvent) {
            this.tickEvent.unregister();
            this.tickEvent = null;
        }
        this.active = false;
        console.log("Timer gestoppt");
    }
}

let combo = new Combo();
let timer = new Timer(0, false, true);
