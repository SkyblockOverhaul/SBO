import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { createBurrowWaypoints, removeBurrowWaypoint, setBurrowWaypoints, removeBurrowWaypointBySmoke } from "../general/Waypoints";
import { getWorld } from "../../utils/world";
import { checkDiana } from "../../utils/checkDiana";

let burrows = [];
let burrowshistory = [];
function burrowDetect(particle, type) {
    let typename = type.toString();
    if (typename == "FOOTSTEP" || typename == "CRIT_MAGIC" || typename == "CRIT") {
        const particlepos = particle.getPos();
        let xyzcheck = [particle.getX(), particle.getY(), particle.getZ()];
        let xyz = [particlepos.getX(), particlepos.getY(), particlepos.getZ(), xyzcheck];
        if (Math.abs(particle.getY() % 1) > 0.1) return;
        if (Math.abs(particle.getX() % 1) < 0.1) return;
        if (Math.abs(particle.getX() % 1) > 0.9) return;
        if (Math.abs(particle.getZ() % 1) < 0.1) return;
        if (Math.abs(particle.getZ() % 1) > 0.9) return;


        switch (typename) {
            case ("FOOTSTEP"): // Loads burrow waypoints by footstep
                xyz.unshift("Treasure");

                closest = getClosest(xyz, burrows);
                if (closest[1] > 3) {
                    burrows.push(xyz);
                }
            
                break;
            // Determine burrow type
            case ("CRIT_MAGIC"):
                xyz.unshift("Start");
                closest = getClosest(xyz, burrows);
                if (closest[1] < 3)
                    closest[0][0] = "Start";
                break;
            case ("CRIT"):
                xyz.unshift("Mob");
                closest = getClosest(xyz, burrows);
                if (closest[1] < 3)
                    closest[0][0] = "Mob";
                break;
        }
    }
}

function resetBurrows() {
    setBurrowWaypoints([]);
    burrows = [];
    burrowshistory = [];
}

function getClosest(origin, positions) {
    let closestPosition = positions.length > 0 ? positions[0] : [0, 0, 0];
    let closestDistance = 999;
    let distance = 999;

    positions.forEach(position => {
        distance = Math.hypot(origin[1] - position[1], origin[2] - position[2], origin[3] - position[3]);
        if (distance < closestDistance) {
            closestDistance = distance;
            closestPosition = position;
        }
    });

    return [closestPosition, closestDistance];
};

function getClosestBurrowToPlayer() {
    let closestDistance = Infinity;
    let closestBurrow = null;
    

    burrows.forEach(([type, x, y, z]) => {
        const distance = Math.sqrt(
            (Player.getX() - x)**2 +
            (Player.getY() - y)**2 +
            (Player.getZ() - z)**2
        );
        if (distance < closestDistance) {
            closestDistance = distance;
            closestBurrow = [type, x, y, z];
        }
    });
    return closestBurrow;
}

registerWhen(register("spawnParticle", (particle, type, event) => {
    if (!checkDiana()) return;
    if (type.toString() == "SMOKE_LARGE") {
        const particlepos = particle.getPos();
        const xyz = [particlepos.getX(), particlepos.getY(), particlepos.getZ()];
        const [x, y , z] = [xyz[0], xyz[1], xyz[2]];
        removeBurrowBySmoke(x, y, z);
    }
    burrowDetect(particle, type);

}), () => settings.dianaBurrowDetect && getWorld() == "Hub");

registerWhen(register("step", () => {
    burrows.forEach(([type, x, y, z, xyzcheck]) => {
        createBurrowWaypoints(type, x, y, z, burrowshistory, xyzcheck);
    });
}).setFps(4), () => settings.dianaBurrowDetect);

registerWhen(register("chat", (burrow) => {
    refreshBurrows();
}).setCriteria("&r&eYou dug out a Griffin Burrow! &r&7${burrow}&r"), () => settings.dianaBurrowDetect);

registerWhen(register("chat", (burrow) => {
    refreshBurrows();
}).setCriteria("&r&eYou finished the Griffin burrow chain!${burrow}"), () => settings.dianaBurrowDetect);

register("command", () => {
    resetBurrows();
    ChatLib.chat("§6[SBO] §4Burrow Waypoints Cleared!§r")
}).setName("sboclearburrows"); 

registerWhen(register("chat", () => {
    resetBurrows();
}).setCriteria(" ☠ You ${died}."), () => getWorld() == "Hub" && settings.dianaBurrowDetect);

registerWhen(register("worldUnload", () => {
    resetBurrows();
}), () => settings.dianaBurrowDetect);

registerWhen(register("step", () => {
    if (!checkDiana()) {
        resetBurrows();
    }
}).setFps(1), () => settings.dianaBurrowDetect);
