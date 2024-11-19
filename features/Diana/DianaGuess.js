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

    if (dingIndex > 1) {
        dingSlope.push(pitch - lastDingPitch);
    }

    if (dingSlope.length > 20) {
        dingSlope.shift();
    }


    let slope = dingSlope.length ? dingSlope.reduce((a, b) => a + b) / dingSlope.length : 0.0;
    lastSoundPoint = {x: pos.getX(), y: pos.getY(), z: pos.getZ()};
    lastDingPitch = pitch;



    if (lastParticlePoint2 === null || particlePoint === null || firstParticlePoint === null) {
        return;
    }

    distance2 = Math.E / slope - Math.hypot(firstParticlePoint.getX() - pos.getX(), firstParticlePoint.getY() - pos.getY(), firstParticlePoint.getZ() - pos.getZ());

    if (distance2 > 1000) {
        distance2 = null;
        guessPoint = null;

        // workaround: returning if the distance is too big
        return;
    }

    const lineDist = lastParticlePoint2.distance(particlePoint);

    distance = distance2;
    const changesHelp = particlePoint.subtract(lastParticlePoint2);
    let changes = [changesHelp.x, changesHelp.y, changesHelp.z];
    changes = changes.map(o => o / lineDist);

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
    const type = particle.toString();
    if (!type.startsWith("SparkFX")) return;
    const currLoc = new SboVec(particle.getX(), particle.getY(), particle.getZ());

    let run = false;
    if (lastSoundPoint != null) {
        run = (Math.abs(currLoc.getX() - lastSoundPoint.x) < 2 && Math.abs(currLoc.getY() - lastSoundPoint.y) < 0.5 && Math.abs(currLoc.getZ() - lastSoundPoint.z) < 2);
    }
    if (run) {
        if (locs.length < 100 && locs.length === 0 || particle.getX() + particle.getY() + particle.getZ() !== locs[locs.length - 1][0] + locs[locs.length - 1][1] + locs[locs.length - 1][2] ) {
            let distMultiplier = 1.0;
            if (locs.length > 2) {
                const predictedDist = 0.06507 * locs.length + 0.259;
                const lastPos = locs[locs.length - 1];
                const actualDist = currLoc.distance(lastPos);
                distMultiplier = actualDist / predictedDist;
            }

            locs.push(currLoc);
            if (locs.length > 5 && guessPoint) {
                let slopeThing = locs.map((a, i) => {
                    if (i === 0) return;
                    let lastLoc = locs[i - 1];
                    let currLoc = a;
                    return Math.atan(
                      (currLoc.getX() - lastLoc.getX()) / (currLoc.getZ() - lastLoc.getZ())
                    );
                  });
                let [a, b, c] = solveEquasionThing([slopeThing.length - 5, slopeThing.length - 3, slopeThing.length - 1], [slopeThing[slopeThing.length - 5], slopeThing[slopeThing.length - 3], slopeThing[slopeThing.length - 1]]);

                const pr1 = [];
                const pr2 = [];

                const start = slopeThing.length - 1;
                const lastPos = locs[start].clone().multiply(1);
                const lastPos2 = locs[start].clone().multiply(1);

                let distCovered = 0.0;

                const ySpeed = (locs[locs.length - 1].x - locs[locs.length - 2].x) / Math.hypot(locs[locs.length - 1].x - locs[locs.length - 2].x, locs[locs.length - 1].z - locs[locs.length - 2].x);
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

                        lastSoundPoint && (distCovered = Math.hypot(lastPos.x - lastSoundPoint.x, lastPos.z - lastSoundPoint.z));

                        if (distCovered > distance2) break;
                    }
                }
                if (!pr1.length) return;
                const p1 = pr1[pr1.length - 1];
                const p2 = pr2[pr2.length - 1];
                if (guessPoint) {
                    let d1 = (p1.x - guessPoint.x) ** 2 + (p1.z - guessPoint.z) ** 2;
                    let d2 = (p2.x - guessPoint.x) ** 2 + (p2.z - guessPoint.z) ** 2;
                    if (d1 < d2) {
                        finalLocation = new SboVec(Math.floor(p1.x), 255.0, Math.floor(p1.z));
                    } else {
                        finalLocation = new SboVec(Math.floor(p2.x), 255.0, Math.floor(p2.z));
                    }
                    
                    gY = 131;
                    while (gY > 70) {
                        let block = World.getBlockAt(finalLocation.getX(), gY, finalLocation.getZ());
                        let blockType = block.getType().getID();
                        if (blockType === 2 || blockType === 3) {
                            break;
                        }
                        gY--;
                    }
                    
                    finalLocation.y = gY + 3;

                    // check if finallocation has nan values
                    if (isNaN(finalLocation.getX()) || isNaN(finalLocation.getY()) || isNaN(finalLocation.getZ())) {
                        print("partical: SBO finalLocation has nan values");
                    }
                    else {
                        hasMadeManualGuess = true;
                        
                        //createDianaGuess(finalLocation.getX(), gY, finalLocation.getZ());
                    }
                }
            }
        }

        if (!lastParticlePoint) {
            firstParticlePoint = currLoc.clone();
        }

        if (lastParticlePoint) {
            lastParticlePoint2 = lastParticlePoint;
        }
        lastParticlePoint = particlePoint;

        particlePoint = currLoc.clone();

        if (!lastParticlePoint2 || !firstParticlePoint || !distance2 || !lastSoundPoint) return;

        const lineDist = lastParticlePoint2.distance(particlePoint);

        distance = distance2;

        const changesHelp = particlePoint.subtract(lastParticlePoint2);
        let changes = [changesHelp.x, changesHelp.y, changesHelp.z];
        changes = changes.map(o => o / lineDist);

        lastParticlePoint && (guessPoint = new SboVec(
            lastParticlePoint.x + changes[0] * distance,
            lastParticlePoint.y + changes[1] * distance,
            lastParticlePoint.z + changes[2] * distance
        ));
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
        const dx = other.x - this.x;
        const dy = other.y - this.y;
        const dz = other.z - this.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
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

    equals(other) {
        return this.x === other.x && this.y === other.y && this.z === other.z;
    }

    getX() {
        return this.x;
    }

    getY() {
        return this.y;
    }

    getZ() {
        return this.z;
    }
}

function basicallyEqual(a, b) {
    return Math.abs(a.x - b.x) <= 1 && a.y == b.y && Math.abs(a.z - b.z) <= 1;
}

function tryToMakeInitialGuess() {
    if (hasMadeManualGuess || hasMadeInitialGuess) return;
    const enumerator = Object.keys(coloredArmorStands);
    enumerator.forEach(id => {
        const armorStand = World.getWorld().func_73045_a(id);
        if (armorStand === null) return;
        const CTArmorStand = new Entity(armorStand);
        const armorStandPos = CTArmorStand.getPos();
        const lastInteractedPos = getLastInteractedPos();
        if (!lastInteractedPos || !basicallyEqual(armorStandPos, lastInteractedPos)) return;

        const type = coloredArmorStands[id];
        let multiplier;
        switch(type) {
            case "FAR":
                multiplier = 320;
                break;
            case "MEDIUM":
                multiplier = 200;
                break;
            case "CLOSE":
                multiplier = 50;
                break;
        }
        const directionVec = new SboVec(-Math.sin(CTArmorStand.getYaw() * Math.PI / 180), 0, Math.cos(CTArmorStand.getYaw() * Math.PI / 180));
        finalLocation = new SboVec(lastInteractedPos.x, lastInteractedPos.y, lastInteractedPos.z).add(directionVec.multiply(multiplier));
        hasMadeInitialGuess = true;
        return;
    });
}


registerWhen(register("tick", () => {
    if (!isEnabled()) return;
    tryToMakeInitialGuess();
}), () => settings.dianaAdvancedBurrowGuess);

registerWhen(register("chat", (burrow) => {
    hasMadeManualGuess = false;
    hasMadeInitialGuess = false;
}).setCriteria("&r&eYou dug out a Griffin Burrow! &r&7${burrow}&r"), () => settings.dianaAdvancedBurrowGuess);

registerWhen(register("worldUnload", () => {
    onWorldChange()
}), () => settings.dianaBurrowGuess);
registerWhen(register("worldLoad", () => {
    onWorldChange()
}), () => settings.dianaBurrowGuess);
registerWhen(register("gameLoad", () => {
    onWorldChange()
}), () => settings.dianaBurrowGuess);

registerWhen(register("soundPlay", (pos, name, volume, pitch, categoryName, event) => {
    onPlaySound(pos, name, volume, pitch, categoryName, event)
}), () => settings.dianaBurrowGuess);

registerWhen(register("spawnParticle", (particle, type, event) => {
    onReceiveParticle(particle, type, event)
}), () => settings.dianaBurrowGuess);

registerWhen(register("step", () => {
    if (!checkDiana()) {
        onWorldChange();
    }
    else {
        GetNewY();
    }
}).setFps(1), () => settings.dianaBurrowGuess);


function GetNewY() {
    if(!finalLocation) return;
    if(World.getWorld().func_175668_a(new (Java.type('net.minecraft.util.BlockPos'))(finalLocation.x, finalLocation.y, finalLocation.z), false)) {
        gY = 131;
        while (gY > 70) {
            let block = World.getBlockAt(finalLocation.getX(), gY, finalLocation.getZ());
            let blockType = block.getType().getID();
            if (blockType === 2 || blockType === 3) {
                break;
            }
            gY--;
        }
        finalLocation.y = gY + 3;
    }
}