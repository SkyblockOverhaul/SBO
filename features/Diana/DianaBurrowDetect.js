import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { creatBurrowWaypoints, removeBurrowWaypoint, setBurrowWaypoints } from "../general/Waypoints";
import { getWorld } from "../../utils/world";

let burrows = [];
let burrowshistory = [];
function burrowDetect(particle, type) {
    let typename = type.toString();
    if (typename == "FOOTSTEP" || typename == "CRIT_MAGIC" || typename == "CRIT") {
        const particlepos = particle.getPos();
        const xyz = [particlepos.getX(), particlepos.getY(), particlepos.getZ()];
        const [x, y , z] = [xyz[0], xyz[1], xyz[2]];
        
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
function refreshBurrows() {
    let closetburrow = getClosestBurrowToPlayer();
    // wenn closest burow vorhanden in history dann nicht machen
    if (!burrowshistory.some(([type, x, y, z]) => x === closetburrow[1] && y === closetburrow[2] && z === closetburrow[3])) {
        burrowshistory.push(closetburrow);
    }
    if (burrowshistory.length > 7) {
        // remove oldest burrow
        burrowshistory.shift();
    }
    burrows = removeBurrowWaypoint(burrowshistory, burrows);
}
registerWhen(register("spawnParticle", (particle, type, event) => {
    burrowDetect(particle, type);
}), () => settings.dianaBurrowDetect);

registerWhen(register("step", () => {
    burrows.forEach(([type, x, y, z]) => {
        creatBurrowWaypoints(type, x, y, z, burrowshistory);
    });
}).setFps(4), () => settings.dianaBurrowWaypoints);

registerWhen(register("chat", (burrow) => {
    refreshBurrows();
}).setCriteria("&r&eYou dug out a Griffin Burrow! &r&7${burrow}&r"), () => settings.dianaBurrowDetect);

registerWhen(register("chat", (burrow) => {
    refreshBurrows();
}).setCriteria("&r&eYou finished the Griffin burrow chain!${burrow}"), () => settings.dianaBurrowDetect);

register("command", () => {
    setBurrowWaypoints([]);
    burrows = [];
    burrowshistory = [];
}).setName("sboclearburrows"); 

registerWhen(register("chat", () => {
    setBurrowWaypoints([]);
    burrows = [];
    burrowshistory = [];
}).setCriteria(" ☠ You ${died}."), () => getWorld() == "Hub" && settings.dianaBurrowDetect);

