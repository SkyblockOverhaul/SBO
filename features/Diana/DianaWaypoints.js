import settings from "../../settings";
import { GRAY,  WHITE } from "../../utils/constants";
import { getClosest, getMayor, getPerks } from "../../utils/functions";
import { delay } from "../../utils/threads";
import { data, registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";


/**
 * Variables used to classify and display burrow estimations.
 */
let particles = [];
let distance = 0;
let heldItem = undefined;
let dig = false;
let cast = false;
let cd = false;
let theoryBurrow = [];
export function getTheory() { return theoryBurrow };
let burrows = [];
export function getBurrow() { return burrows };

/**
 * Calculates a point on a vector in regards to start/end coordinates and distance.
 *
 * @param {string} start - Starting coordinate.
 * @param {string} end - Ending coordinate.
 */
function getCoord(start, end) {
    return Math.round(start + (end - start) * distance);
}

/**
 * Tracks different particle spawns around the player to use for burrow estimator and classifier.
 *
 * @param {Object} particle - Particle entity object.
 * @param {Object} type - Particle name.
 */
let closest = undefined;
registerWhen(register("spawnParticle", (particle, type) => {
    const particlePos = particle.getPos();
    const xyz = [particlePos.getX(), particlePos.getY(), particlePos.getZ()];
    const [x, y, z] = [xyz[0], xyz[1], xyz[2]];
    switch (type.toString()) {
        case ("FIREWORKS_SPARK"): // Loads spade ability particles
            // Uses player location as last particle if first cast
            if (!particles.length)
                particles.push([Player.getX(), Player.getY(), Player.getZ()]);
            
            // Avoids stray fireworks sparks i.e. frozen scythe / crystals
            const lastParticle = particles[particles.length - 1];
            if (Math.hypot(lastParticle[0] - x, lastParticle[1] - y, lastParticle[2] - z) < 5)
                particles.push([x, y, z]);
            break;
        case ("FOOTSTEP"): // Loads burrow waypoints by footstep
            if (settings.dianaBurrow && !dig && World.getBlockAt(x, y, x).type.getName()) {
                xyz.unshift("Treasure");

                closest = getClosest(xyz, burrows);
                if (closest[1] > 3) {
                    burrows.push(xyz);
                    // Burrow Alerts
                    if (settings.dianaChat)
                        ChatLib.chat(`${WHITE}Burrow Detected at ${GRAY}x: ${x}, y: ${y}, z: ${z}!`);
                }
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
}), () => getWorld() === "Hub" && (settings.dianaWaypoint || settings.dianaBurrow) &&
getMayor() === "Diana" && getPerks().has("Mythological Ritual"));

/**
 * Uses sound pitch to determine how far away a theoretical burrow could appear.
 *
 * @param {number[]} pos - Array of x, y, z positions.
 * @param {string} name - Name of sound.
 * @param {string} vol - Volume of sound.
 * @param {string} pitch - pitch of sound.
 * @param {string} category - Sound category.
 */
registerWhen(register("soundPlay", (pos, name, vol, pitch, category) => {
    if (!cast) return;

    // Uhh so it was a little hard to figure out an equation so this may be a little brute force hardcoded :skull:
    if (pitch > 1.05)
        correct = -1
    else if (pitch > 0.92)
        correct = 1.5 - pitch;
    else if (pitch > 0.77)
        correct = 2 / Math.pow(pitch, 2);
    else
        correct = -Math.abs(0.4 / (0.7 - pitch));

    if (pitch > 0)
        distance = 4 / Math.pow(pitch, 6) + 0.2 / Math.pow(pitch, 5) - correct;
}).setCriteria("note.harp"), () => getWorld() === "Hub" && settings.dianaWaypoint &&
getMayor() === "Diana" && getPerks().has("Mythological Ritual"));

/**
 * Tracks when player uses Ancestral Spade ability as to not "spam" out the estimator.
 *
 * @param {number} x - X position of mouse click.
 * @param {number} y - Y position of mouse click.
 * @param {boolean} button - True for right click, False for left click.
 * @param {boolean} state - True for key down, False for key up
 */
registerWhen(register("clicked", (x, y, button, state) => {
    if (Player.getHeldItem() === null || !button || !state || cd) return;

    heldItem = Player.getHeldItem().getNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getString("id");
    if (heldItem.equals("ANCESTRAL_SPADE")) {
        particles = [];
        cast = true;
        cd = true;
        delay(() => { cast = false }, 2000);
        delay(() => { cd = false }, 3000);
    }
}), () => getWorld() === "Hub" && settings.dianaWaypoint &&
getMayor() === "Diana" && getPerks().has("Mythological Ritual"));

/**
 * Resets "lastCast" variable whenever player right clicks with a fishing rod in hand.
 */
let warps = [];
let start = 0;
let end = 0;
let theory = [];
const WARPS = {
    "hub": [-2.5, 70, -69.5],
    "castle": [-250, 130, 45],
    "da": [91.5, 75, 173.5],
    "museum": [-75.5, 76, 80.5],
    "crypt": [-161.5, 61, -99.5]
}
export function setWarps() {
    warps = [];
    warps.push(["player", 0, 0, 0]);

    data.warplist.forEach(warp => {
        if (warp in WARPS)
            warps.push([warp, WARPS[warp][0], WARPS[warp][1], WARPS[warp][2]])
    });
}
setWarps();

/**
 * Uses burrow estimator to determine which hub warp is closest to burrow.
 */
registerWhen(register("tick", () => {
    if (particles.length == 0 || !cast) return;

    start = particles.length > 12 ? particles[3] : particles[particles.length - 1];
    end = particles[particles.length - 1];
    warps[0] = ["player", Player.getX(), Player.getY(), Player.getZ()];
    theory = ["warp player", getCoord(start[0], end[0]), getCoord(start[1], end[1]), getCoord(start[2], end[2])];
    
    // Set theory burrow
    theory[0] = "warp " + getClosest(theory, warps)[0][0];
    theoryBurrow = [theory];
}), () => getWorld() === "Hub" && settings.dianaWaypoint &&
getMayor() === "Diana" && getPerks().has("Mythological Ritual"));

/**
 * Key press to warp player to closest burrow.
 */
const dianaKey = new KeyBind("Diana Warp", Keyboard.KEY_NONE, "SkyblockOverhaul");
dianaKey.registerKeyPress(() => {
    if (settings.dianaWarp && theory[0] != undefined && theory[0] != "warp player")
        ChatLib.command(theory[0])
})

/**
 * Deletes burrow when one is dug out.
 */
registerWhen(register("chat", () => {
    // Sets a timeout on particle loading
    dig = true;
    delay(() => dig = false, 200);

    // Delete closest burrow from list
    const closest = getClosest(["Player", Player.getX(), Player.getY(), Player.getZ()], burrows)
    if (closest != undefined);
        burrows.splice(burrows.indexOf(closest[0]), 1);
}).setCriteria("${before}urrow${after}"), () => getWorld() === "Hub" && settings.dianaWaypoint &&
getMayor() === "Diana" && getPerks().has("Mythological Ritual"));

/**
 * Removes all burrows if player leaves world or dies.
 */
register("worldUnload", () => {
    theoryBurrow = [];
    burrows = [];
});
registerWhen(register("chat", () => {
    burrows = [];
}).setCriteria(" ☠ You ${died}."), () => getWorld() == "Hub" && settings.dianaWaypoint &&
getMayor() === "Diana" && getPerks().has("Mythological Ritual"));
