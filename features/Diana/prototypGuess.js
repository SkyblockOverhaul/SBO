import settings from "../../settings";
import { checkDiana } from "../../utils/checkDiana";
import { registerWhen } from "../../utils/variables";

let lastSoundPoint = null;
let particlePoints = [];
let finalLocation = null;

export function getFinalLocation() {
    return finalLocation;
}

function onWorldChange() {
    lastSoundPoint = null;
    particlePoints = [];
    finalLocation = null;
}

function onPlaySound(pos, name, volume, pitch, categoryName, event) {
    if (!isEnabled()) return;
    if (name !== "note.harp") return;

    lastSoundPoint = { x: pos.getX(), y: pos.getY(), z: pos.getZ() };

    if (particlePoints.length >= 2) {
        finalLocation = calculateIntersection(particlePoints, lastSoundPoint);
        if (finalLocation) {
            // Zusätzliche Logik zur Handhabung der finalen Position
            console.log(`Final location: ${finalLocation.x}, ${finalLocation.y}, ${finalLocation.z}`);
        }
    }
}

function onReceiveParticle(particle, type, event) {
    if (!isEnabled()) return;
    const particleType = particle.toString();
    if (!particleType.startsWith("SparkFX")) return;

    const currLoc = new SboVec(particle.getX(), particle.getY(), particle.getZ());
    particlePoints.push(currLoc);

    if (particlePoints.length > 2) {
        particlePoints.shift();  // Halte nur die letzten zwei Partikelkoordinaten
    }
}

function calculateIntersection(particles, sound) {
    if (particles.length < 2) return null;

    const p1 = particles[0];
    const p2 = particles[1];

    const v1 = new SboVec(sound.x - p1.x, sound.y - p1.y, sound.z - p1.z);
    const v2 = new SboVec(sound.x - p2.x, sound.y - p2.y, sound.z - p2.z);

    // Parameter für die Liniengleichung
    const t1 = ((p2.x - p1.x) * v1.x + (p2.y - p1.y) * v1.y + (p2.z - p1.z) * v1.z) / (v1.x * v1.x + v1.y * v1.y + v1.z * v1.z);
    const t2 = ((p1.x - p2.x) * v2.x + (p1.y - p2.y) * v2.y + (p1.z - p2.z) * v2.z) / (v2.x * v2.x + v2.y * v2.y + v2.z * v2.z);

    // Berechnung des Durchschnittspunkts
    const midPoint1 = new SboVec(p1.x + t1 * v1.x, p1.y + t1 * v1.y, p1.z + t1 * v1.z);
    const midPoint2 = new SboVec(p2.x + t2 * v2.x, p2.y + t2 * v2.y, p2.z + t2 * v2.z);

    return new SboVec(
        (midPoint1.x + midPoint2.x) / 2,
        (midPoint1.y + midPoint2.y) / 2,
        (midPoint1.z + midPoint2.z) / 2
    );
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

    clone() {
        return new SboVec(this.x, this.y, this.z);
    }
}

registerWhen(register("worldUnload", () => {
    onWorldChange();
}), () => settings.dianaBurrowGuess);
registerWhen(register("worldLoad", () => {
    onWorldChange();
}), () => settings.dianaBurrowGuess);
registerWhen(register("gameLoad", () => {
    onWorldChange();
}), () => settings.dianaBurrowGuess);

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
