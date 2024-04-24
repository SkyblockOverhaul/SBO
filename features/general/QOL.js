import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { createWorldWaypoint } from "./Waypoints";

// register dragon wings for golden dragon nest
let found = false;
registerWhen(register("soundPlay", (pos, name, volume, pitch, categoryName, event) => {
    if (name === "mob.enderdragon.wings") {
        if (!found) {
            // createNestWayoint(pos.x, pos.y, pos.z);
            createWorldWaypoint("§6Dragon's Lair", pos.x, pos.y, pos.z, 1*255, 0.84*255, 0)
            Client.showTitle(`&r&6&l<&b&l&kO&6&l> &6&lDragon's Lair Found! &6&l<&b&l&kO&6&l>`, "&eat x: " + pos.x + " y: " + pos.y + " z: " + pos.z, 0, 40, 20);
            ChatLib.chat("&6[SBO] &r&eFound &6Dragon's Lair!&r&e at x: " + pos.x + " y: " + pos.y + " z: " + pos.z);
            found = true;
        }
    }
}), () => getWorld() === "Crystal Hollows" && settings.findDragonNest);

registerWhen(register("chat", (player) => {
    if (player.includes(Player.getName())) {
        checkIfInMineshaft()    
    }
}).setCriteria("${player} &r&7entered the mineshaft&r&7!&r"), () => settings.exitWaypoint);

let timeout = 0;
function checkIfInMineshaft() {
    if (getWorld() === "Mineshaft") {
        timeout = 0;
        setTimeout(function() {
            createWorldWaypoint("§eExit", Math.round(Player.getLastX()), Math.round(Player.getLastY()), Math.round(Player.getLastZ()), 3, 252, 244);
        }, 100);
    }
    else {
        timeout++;
        if (timeout < 15) {
            setTimeout(checkIfInMineshaft, 500);
        }
    }
}

registerWhen(register("chat", (player, dings) => {
    if (player.includes(Player.getName())) {
        checkIfInMineshaft();
    }
}).setCriteria("${player} &r&eentered &r&aGlacite Mineshafts${dings}"), () => settings.exitWaypoint);

registerWhen(register("chat", () => {
    Client.showTitle("&l&9MINESHEFT!", "", 0, 90, 20);
}).setCriteria("&r&5&lWOW! &r&aYou found a &r&bGlacite Mineshaft &r&aportal!&r"), () => settings.mineshaftAnnouncer);
