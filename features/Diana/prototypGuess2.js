import settings from "../../settings";
import { checkDiana } from "../../utils/checkDiana";
import { registerWhen } from "../../utils/variables";

let lastSoundEvent = null;
let particlePoints = [];
let guessPoint = null;
let finalLocation = null;

export function getFinalLocation() {
    return finalLocation;
}

function onWorldChange() {
    lastSoundEvent = null;
    particlePoints = [];
    guessPoint = null;
    finalLocation = null;
}

function onPlaySound(pos, name, volume, pitch, categoryName, event) {
    if (!isEnabled() || name !== "note.harp") return;

    const currentTime = Date.now();
    const soundPoint = { x: pos.getX(), y: pos.getY(), z: pos.getZ(), pitch };

    if (!lastSoundEvent || pitch < lastSoundEvent.pitch) {
        resetState(pitch);
    }

    lastSoundEvent = soundPoint;

    if (particlePoints.length < 2) return;

    const distance = calculateDistanceFromPitch(pitch, lastSoundEvent.pitch);
    if (distance > 1000) {
        guessPoint = null;
        return;
    }

    const vector = calculateDirectionVector(particlePoints);
    guessPoint = new SboVec(
        soundPoint.x + vector.x * distance,
        soundPoint.y + vector.y * distance,
        soundPoint.z + vector.z * distance
    );
}

function resetState(initialPitch) {
    particlePoints = [];
    guessPoint = null;
    lastSoundEvent = { pitch: initialPitch };
}

function calculateDistanceFromPitch(pitch, lastPitch) {
    const basePitch = 1.0; // Minimal pitch
    const baseDistance = 10; // Minimal distance for the base pitch
    const pitchFactor = 0.1; // Factor to adjust the distance based on pitch

    return baseDistance / Math.pow(pitch / basePitch, pitchFactor);
}

function calculateDirectionVector(points) {
    const [p1, p2] = points;
    const vector = new SboVec(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
    const length = vector.length();
    return new SboVec(vector.x / length, vector.y / length, vector.z / length);
}

function onReceiveParticle(particle, type, event) {
    if (!isEnabled()) return;
    const particleType = particle.toString();
    if (!particleType.startsWith("SparkFX")) return;

    const currLoc = new SboVec(particle.getX(), particle.getY(), particle.getZ());
    particlePoints.push(currLoc);

    if (particlePoints.length > 2) {
        particlePoints.shift(); // Keep only the last two particle coordinates
    }

    if (guessPoint) {
        updateFinalLocation();
    }
}

function updateFinalLocation() {
    if (!guessPoint) return;

    const [p1, p2] = particlePoints;
    const distance = calculateDistanceFromPitch(lastSoundEvent.pitch, firstPitch);

    const changes = calculateDirectionVector([p1, p2]);
    finalLocation = new SboVec(
        p2.x + changes.x * distance,
        p2.y + changes.y * distance,
        p2.z + changes.z * distance
    );

    adjustYCoordinate();
}

function adjustYCoordinate() {
    let y = 255;
    while (World.getBlockAt(finalLocation.getX(), y, finalLocation.getZ()).getType().getID() !== 2 && y > 70) {
        y--;
    }
    finalLocation.y = y + 3;
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

    subtract(other) {
        return new SboVec(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    clone() {
        return new SboVec(this.x, this.y, this.z);
    }
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
