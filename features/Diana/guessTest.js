import settings from "../../settings";
import { checkDiana } from "../../utils/checkDiana";
import { registerWhen } from "../../utils/variables";
import { coloredArmorStands, getLastInteractedPos } from "./DianaBurrows";

let lastDing = 0;
let lastDingPitch = 0;
let firstPitch = 0;
let lastParticlePoint = null;
let lastParticlePoint2 = null;
let lastSoundPoint = null;
let firstParticlePoint = null;
let particlePoint = null;
let guessPoint = null;
let distance = null;
let dingIndex = 0;
let dingSlope = [];
let distance2 = null;
let finalLocation = null;
let hasMadeInitialGuess = false;
let hasMadeManualGuess = false;
let distMultiplierHistory = [];
let locs = [];
let gY = 0;

export function getFinalLocation() {
    return finalLocation;
}

function onWorldChange() {
    lastDing = 0;
    lastDingPitch = 0;
    firstPitch = 0;
    lastParticlePoint = null;
    lastParticlePoint2 = null;
    lastSoundPoint = null;
    firstParticlePoint = null;
    particlePoint = null;
    guessPoint = null;
    distance = null;
    dingIndex = 0;
    dingSlope = [];
    finalLocation = null;
    gY = 0;
    hasMadeManualGuess = false;
    hasMadeInitialGuess = false;
    distMultiplierHistory = [];
    locs = [];
}

function onPlaySound(pos, name, volume, pitch, categoryName, event) {
    if (!isEnabled()) return;
    if (name !== "note.harp") return;

    if (lastDing === 0) {
        firstPitch = pitch;
    }

    lastDing = Date.now();

    if (pitch < lastDingPitch) {
        firstPitch = pitch;
        dingIndex = 0;
        dingSlope = [];
        lastDingPitch = pitch;
        lastParticlePoint = null;
        lastParticlePoint2 = null;
        lastSoundPoint = null;
        firstParticlePoint = null;
        distance = null;
        locs = [];
    }

    if (lastDingPitch === 0) {
        lastDingPitch = pitch;
        distance = null;
        lastParticlePoint = null;
        lastParticlePoint2 = null;
        lastSoundPoint = null;
        firstParticlePoint = null;
        locs = [];
        return;
    }

    dingIndex++;
    if (dingIndex > 1) dingSlope.push(pitch - lastDingPitch);
    if (dingSlope.length > 20) dingSlope.shift();

    let slope = dingSlope.length ? dingSlope.reduce((a, b) => a + b) / dingSlope.length : 0.0;
    lastSoundPoint = { x: pos.getX(), y: pos.getY(), z: pos.getZ() };
    lastDingPitch = pitch;

    if (!lastParticlePoint2 || !particlePoint || !firstParticlePoint) return;

    // Improved distance calculation
    distance2 = (1.85 / Math.abs(slope)) * Math.hypot(
        firstParticlePoint.getX() - pos.getX(),
        firstParticlePoint.getZ() - pos.getZ()
    );
    distance2 = Math.min(Math.max(distance2, 15), 320);

    if (isNaN(distance2) || !isFinite(distance2)) {
        console.debug("Invalid distance2 detected, resetting calculation");
        distance2 = null;
        guessPoint = null;
        return;
    }

    const lineDist = lastParticlePoint2.distance(particlePoint);
    distance = distance2;
    const changesHelp = particlePoint.subtract(lastParticlePoint2);
    let changes = [changesHelp.x, changesHelp.y, changesHelp.z];
    changes = changes.map(o => o / lineDist);

    if (changes.some(coord => Math.abs(coord) > 10)) {
        console.debug("Abnormal direction vector detected");
        return;
    }

    lastSoundPoint && (guessPoint = new SboVec(
        lastSoundPoint.x + changes[0] * distance,
        lastSoundPoint.y + changes[1] * distance,
        lastSoundPoint.z + changes[2] * distance
    ));
}

function solveEquasionThing(x, y) {
    let a =
        (-y[0] * x[1] * x[0] -
            y[1] * x[1] * x[2] +
            y[1] * x[1] * x[0] +
            x[1] * x[2] * y[2] +
            x[0] * x[2] * y[0] -
            x[0] * x[2] * y[2]) /
        (x[1] * y[0] -
            x[1] * y[2] +
            x[0] * y[2] -
            y[0] * x[2] +
            y[1] * x[2] -
            y[1] * x[0]);
    let b = ((y[0] - y[1]) * (x[0] + a) * (x[1] + a)) / (x[1] - x[0]);
    let c = y[0] - b / (x[0] + a);
    return [a, b, c];
}

function onReceiveParticle(particle, type, event) {
    if (!isEnabled()) return;
    const particleType = particle.toString();
    if (!particleType.startsWith("SparkFX")) return;
    const currLoc = new SboVec(particle.getX(), particle.getY(), particle.getZ());

    let run = false;
    if (lastSoundPoint != null) {
        run = (Math.abs(currLoc.getX() - lastSoundPoint.x) < 2 &&
            Math.abs(currLoc.getY() - lastSoundPoint.y) < 0.5 &&
            Math.abs(currLoc.getZ() - lastSoundPoint.z) < 2);
    }

    if (run) {
        if ((locs.length < 100 && locs.length === 0) ||
            (particle.getX() + particle.getY() + particle.getZ() !== locs[locs.length - 1][0] + locs[locs.length - 1][1] + locs[locs.length - 1][2])) {

            let distMultiplier = 1.0;
            if (locs.length > 4) {
                const recentDists = [];
                for (let i = 1; i < 4; i++) {
                    recentDists.push(locs[i].distance(locs[i - 1]));
                }
                const predictedDist = recentDists.reduce((a, b) => a + b) / recentDists.length;
                const actualDist = currLoc.distance(locs[locs.length - 1]);
                distMultiplier = actualDist / predictedDist;
                distMultiplier = 0.3 * distMultiplier + 0.7 * (distMultiplierHistory[distMultiplierHistory.length - 1] || 1);
                distMultiplierHistory.push(distMultiplier);
                if (distMultiplierHistory.length > 5) distMultiplierHistory.shift();
            }

            locs.push(currLoc);
            if (locs.length > 5 && guessPoint) {
                let slopeThing = locs.map((a, i) => {
                    if (i === 0) return;
                    let lastLoc = locs[i - 1];
                    let dx = a.getX() - lastLoc.getX();
                    let dz = a.getZ() - lastLoc.getZ();
                    return Math.atan2(dx, dz + 0.0001);
                }).filter(x => x !== undefined);

                if (slopeThing.length < 5) return;
                let [a, b, c] = solveEquasionThing(
                    [slopeThing.length - 5, slopeThing.length - 3, slopeThing.length - 1],
                    [slopeThing[slopeThing.length - 5], slopeThing[slopeThing.length - 3], slopeThing[slopeThing.length - 1]]
                );

                const pr1 = [];
                const pr2 = [];
                const start = slopeThing.length - 1;
                const lastPos = locs[start].clone();
                const lastPos2 = locs[start].clone();

                let distCovered = 0.0;
                const ySpeed = (locs[locs.length - 1].x - locs[locs.length - 2].x) /
                    Math.hypot(locs[locs.length - 1].x - locs[locs.length - 2].x, locs[locs.length - 1].z - locs[locs.length - 2].x);

                for (let i = start + 1; distCovered < distance2 && i < 10000; i++) {
                    const y = b / (i + a) + c;
                    const dist = distMultiplier * (0.06507 * i + 0.259);

                    const xOff = dist * Math.sin(y);
                    const zOff = dist * Math.cos(y);
                    const density = 5;

                    for (let o = 0; o <= density; o++) {
                        lastPos.x += xOff / density;
                        lastPos.z += zOff / density;
                        lastPos.y += ySpeed * dist / density;
                        lastPos2.y += ySpeed * dist / density;
                        lastPos2.x -= xOff / density;
                        lastPos2.z -= zOff / density;

                        pr1.push(lastPos.clone());
                        pr2.push(lastPos2.clone());

                        if (lastSoundPoint) {
                            distCovered = Math.hypot(lastPos.x - lastSoundPoint.x, lastPos.z - lastSoundPoint.z);
                        }

                        if (distCovered > distance2) break;
                    }
                }

                if (!pr1.length) return;
                const p1 = pr1[pr1.length - 1];
                const p2 = pr2[pr2.length - 1];

                if (guessPoint) {
                    let d1 = (p1.x - guessPoint.x) ** 2 + (p1.z - guessPoint.z) ** 2;
                    let d2 = (p2.x - guessPoint.x) ** 2 + (p2.z - guessPoint.z) ** 2;
                    finalLocation = new SboVec(
                        Math.floor(d1 < d2 ? p1.x : p2.x),
                        255.0,
                        Math.floor(d1 < d2 ? p1.z : p2.z)
                    );

                    GetNewY();
                    if (isNaN(finalLocation.getX()) || isNaN(finalLocation.getY()) || isNaN(finalLocation.getZ())) {
                        console.log("SBO: Invalid finalLocation detected");
                    } else {
                        hasMadeManualGuess = true;
                    }
                }
            }
        }

        if (!lastParticlePoint) firstParticlePoint = currLoc.clone();
        if (lastParticlePoint) lastParticlePoint2 = lastParticlePoint;
        lastParticlePoint = particlePoint;
        particlePoint = currLoc.clone();
    }
}

function isEnabled() {
    return checkDiana();
}

class SboVec {
    constructor(x, y, z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    distance(other) {
        return Math.hypot(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    add(other) {
        return new SboVec(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    subtract(other) {
        return new SboVec(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    multiply(d) {
        return new SboVec(this.x * d, this.y * d, this.z * d);
    }

    clone() {
        return new SboVec(this.x, this.y, this.z);
    }

    getX() { return this.x; }
    getY() { return this.y; }
    getZ() { return this.z; }
}

function basicallyEqual(a, b) {
    return Math.abs(a.x - b.x) <= 1 && a.y == b.y && Math.abs(a.z - b.z) <= 1;
}

function tryToMakeInitialGuess() {
    if (hasMadeManualGuess || hasMadeInitialGuess) return;
    Object.keys(coloredArmorStands).forEach(id => {
        const armorStand = World.getWorld().func_73045_a(id);
        if (!armorStand) return;
        const CTArmorStand = new Entity(armorStand);
        const armorStandPos = CTArmorStand.getPos();
        const lastInteractedPos = getLastInteractedPos();
        if (!lastInteractedPos || !basicallyEqual(armorStandPos, lastInteractedPos)) return;

        const type = coloredArmorStands[id];
        let multiplier = { "FAR": 320, "MEDIUM": 200, "CLOSE": 50 }[type];
        const directionVec = new SboVec(
            -Math.sin(CTArmorStand.getYaw() * Math.PI / 180),
            0,
            Math.cos(CTArmorStand.getYaw() * Math.PI / 180)
        );
        finalLocation = new SboVec(lastInteractedPos.x, lastInteractedPos.y, lastInteractedPos.z)
            .add(directionVec.multiply(multiplier));
        hasMadeInitialGuess = true;
    });
}

function GetNewY() {
    if (!finalLocation) return;
    
    const scanHeight = 131;
    const blockPos = new BlockPos(finalLocation.x, scanHeight, finalLocation.z);
    
    for (let y = scanHeight; y > 70; y--) {
        blockPos.y = y;
        const block = World.getBlockAt(blockPos);
        const belowBlock = World.getBlockAt(blockPos.x, y - 1, blockPos.z);
        
        if (block.getType().getID() === 0 && 
            (belowBlock.getType().getID() === 2 || belowBlock.getType().getID() === 3)) {
            finalLocation.y = y + 1;
            return;
        }
    }
    
    let gY = 131;
    while (gY > 70) {
        let block = World.getBlockAt(finalLocation.x, gY, finalLocation.z);
        if (block.getType().getID() === 2 || block.getType().getID() === 3) {
            finalLocation.y = gY + 3;
            return;
        }
        gY--;
    }
}

// Registrierungen
registerWhen(register("tick", () => {
    if (!isEnabled()) return;
    tryToMakeInitialGuess();
}), () => settings.dianaAdvancedBurrowGuess);

registerWhen(register("chat", (burrow) => {
    hasMadeManualGuess = false;
    hasMadeInitialGuess = false;
}).setCriteria("&r&eYou dug out a Griffin Burrow! &r&7${burrow}&r"), () => settings.dianaAdvancedBurrowGuess);

registerWhen(register("worldUnload", onWorldChange), () => settings.dianaBurrowGuess);
registerWhen(register("worldLoad", onWorldChange), () => settings.dianaBurrowGuess);
registerWhen(register("gameLoad", onWorldChange), () => settings.dianaBurrowGuess);

registerWhen(register("soundPlay", (pos, name, volume, pitch, categoryName, event) => {
    onPlaySound(pos, name, volume, pitch, categoryName, event);
}), () => settings.dianaBurrowGuess);

registerWhen(register("spawnParticle", (particle, type, event) => {
    onReceiveParticle(particle, type, event);
}), () => settings.dianaBurrowGuess);

registerWhen(register("step", () => {
    if (!checkDiana()) onWorldChange();
    else GetNewY();
}).setFps(1), () => settings.dianaBurrowGuess);