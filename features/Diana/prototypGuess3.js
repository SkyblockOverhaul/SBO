import settings from "../../settings";
import { checkDiana } from "../../utils/checkDiana";
import { registerWhen } from "../../utils/variables";

// Globale Variablen für Zustandsspeicherung
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
let gY = 0;
let locs = [];

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
    distance2 = null;
    finalLocation = null;
    gY = 0;
    locs = [];
}

function onPlaySound(pos, name, volume, pitch, categoryName, event) {
    if (!isEnabled() || name !== "note.harp") return;

    if (lastDing === 0) {
        firstPitch = pitch;
    }

    lastDing = Date.now();

    if (pitch < lastDingPitch) {
        resetSoundState(pitch);
    }

    if (lastDingPitch === 0) {
        lastDingPitch = pitch;
        resetSoundState();
        return;
    }

    dingIndex++;
    if (dingIndex > 1) {
        dingSlope.push(pitch - lastDingPitch);
    }

    if (dingSlope.length > 20) {
        dingSlope.shift();
    }

    const slope = dingSlope.length ? dingSlope.reduce((a, b) => a + b) / dingSlope.length : 0.0;
    lastSoundPoint = new SboVec(pos.getX(), pos.getY(), pos.getZ());
    lastDingPitch = pitch;

    if (!lastParticlePoint2 || !particlePoint || !firstParticlePoint) return;

    distance2 = Math.E / slope - firstParticlePoint.distance(new SboVec(pos.getX(), pos.getY(), pos.getZ()));
    if (distance2 > 1000) {
        distance2 = null;
        guessPoint = null;
        return;
    }

    const lineDist = lastParticlePoint2.distance(particlePoint);
    distance = distance2;
    const changes = particlePoint.subtract(lastParticlePoint2).scale(1 / lineDist);

    if (lastSoundPoint) {
        guessPoint = lastSoundPoint.add(changes.scale(distance));
    }
}

function solveEquasionThing(x, y) {
    const a = (-y[0] * x[1] * x[0] - y[1] * x[1] * x[2] + y[1] * x[1] * x[0] + x[1] * x[2] * y[2] + x[0] * x[2] * y[0] - x[0] * x[2] * y[2]) /
        (x[1] * y[0] - x[1] * y[2] + x[0] * y[2] - y[0] * x[2] + y[1] * x[2] - y[1] * x[0]);
    const b = ((y[0] - y[1]) * (x[0] + a) * (x[1] + a)) / (x[1] - x[0]);
    const c = y[0] - b / (x[0] + a);
    return [a, b, c];
}

function onReceiveParticle(particle, type, event) {
    if (!isEnabled() || !particle.toString().startsWith("SparkFX")) return;

    const currLoc = new SboVec(particle.getX(), particle.getY(), particle.getZ());

    let run = false;
    if (lastSoundPoint) {
        run = Math.abs(currLoc.x - lastSoundPoint.x) < 2 &&
              Math.abs(currLoc.y - lastSoundPoint.y) < 0.5 &&
              Math.abs(currLoc.z - lastSoundPoint.z) < 2;
    }

    if (run) {
        handleParticleDetection(currLoc);
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
    const changes = particlePoint.subtract(lastParticlePoint2).scale(1 / lineDist);

    if (lastParticlePoint) {
        guessPoint = lastParticlePoint.add(changes.scale(distance));
    }
}

function isEnabled() {
    return checkDiana();
}

function handleParticleDetection(currLoc) {
    if (locs.length < 100 && (locs.length === 0 || currLoc.equals(locs[locs.length - 1]))) {
        let distMultiplier = 1.0;
        if (locs.length > 2) {
            const predictedDist = 0.06507 * locs.length + 0.259;
            const lastPos = locs[locs.length - 1];
            const actualDist = currLoc.distance(lastPos);
            distMultiplier = actualDist / predictedDist;
        }

        locs.push(currLoc);
        if (locs.length > 5 && guessPoint) {
            estimateFinalLocation(distMultiplier);
        }
    }
}

function estimateFinalLocation(distMultiplier) {
    const slopeThing = locs.map((a, i) => {
        if (i === 0) return;
        const lastLoc = locs[i - 1];
        return Math.atan((a.x - lastLoc.x) / (a.z - lastLoc.z));
    }).slice(1);  // Remove the first undefined entry

    const [a, b, c] = solveEquasionThing(
        [slopeThing.length - 5, slopeThing.length - 3, slopeThing.length - 1],
        [slopeThing[slopeThing.length - 5], slopeThing[slopeThing.length - 3], slopeThing[slopeThing.length - 1]]
    );

    const pr1 = [];
    const pr2 = [];
    const start = slopeThing.length - 1;
    const lastPos = locs[start].clone();
    const lastPos2 = locs[start].clone();
    let distCovered = 0.0;
    const ySpeed = (locs[locs.length - 1].x - locs[locs.length - 2].x) / locs[locs.length - 1].distance(locs[locs.length - 2]);

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

            distCovered = lastPos.distance(lastSoundPoint);
            if (distCovered > distance2) break;
        }
    }

    if (!pr1.length) return;

    const p1 = pr1[pr1.length - 1];
    const p2 = pr2[pr2.length - 1];
    if (guessPoint) {
        const d1 = (p1.x - guessPoint.x) ** 2 + (p1.z - guessPoint.z) ** 2;
        const d2 = (p2.x - guessPoint.x) ** 2 + (p2.z - guessPoint.x) ** 2;
        finalLocation = d1 < d2 ? p1 : p2;

        adjustFinalLocationHeight();
        if (isNaN(finalLocation.x) || isNaN(finalLocation.y) || isNaN(finalLocation.z)) {
            print("partical: Soopy finalLocation has NaN values");
        } else {
            // createDianaGuess(finalLocation.x, gY, finalLocation.z);
        }
    }
}

function adjustFinalLocationHeight() {
    gY = 255;
    while (World.getBlockAt(finalLocation.x, gY, finalLocation.z).getType().getID() !== 2 && gY > 70) {
        gY--;
    }
    finalLocation.y = gY + 3;
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

    subtract(other) {
        return new SboVec(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    scale(factor) {
        return new SboVec(this.x * factor, this.y * factor, this.z * factor);
    }

    add(other) {
        return new SboVec(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    clone() {
        return new SboVec(this.x, this.y, this.z);
    }

    equals(other) {
        return this.x === other.x && this.y === other.y && this.z === other.z;
    }
}

function resetSoundState(pitch = null) {
    lastSoundPoint = null;
    distance = null;
    firstPitch = pitch ?? firstPitch;
    guessPoint = null;
    distance2 = null;
    dingIndex = 0;
    dingSlope = [];
}

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
    if (!checkDiana()) {
        onWorldChange();
    }
}).setFps(1), () => settings.dianaBurrowGuess);
