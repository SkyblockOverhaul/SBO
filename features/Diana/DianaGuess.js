import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { coloredArmorStands, getLastInteractedPos } from "./DianaBurrows";
import { SboVec } from "../../utils/helper";

class Matrix {
    constructor(data) {
        this.data = data;
        this.rows = data.length;
        this.cols = data.length > 0 ? data[0].length : 0;
    }

    transpose() {
        let result = [];
        for (let j = 0; j < this.cols; j++) {
            let row = [];
            for (let i = 0; i < this.rows; i++) {
                row.push(this.data[i][j]);
            }
            result.push(row);
        }
        return new Matrix(result);
    }

    multiply(other) {
        if (this.cols !== other.rows) {
            throw new Error("Matrix dimensions do not match for multiplication");
        }
        let result = [];
        for (let i = 0; i < this.rows; i++) {
            let row = [];
            for (let j = 0; j < other.cols; j++) {
                let sum = 0;
                for (let k = 0; k < this.cols; k++) {
                    sum += this.data[i][k] * other.data[k][j];
                }
                row.push(sum);
            }
            result.push(row);
        }
        return new Matrix(result);
    }

    inverse() {
        if (this.rows !== this.cols) {
            throw new Error("Only square matrices can be inverted");
        }
        let n = this.rows;

        let augmented = [];
        for (let i = 0; i < n; i++) {
            augmented[i] = [];
            for (let j = 0; j < n; j++) {
                augmented[i][j] = this.data[i][j];
            }
            for (let j = 0; j < n; j++) {
                augmented[i][j + n] = (i === j) ? 1 : 0;
            }
        }
        for (let i = 0; i < n; i++) {
            let maxRow = i;

            for (let k = i + 1; k < n; k++) {
                if (Math.abs(augmented[k][i]) > Math.abs(augmented[maxRow][i])) {
                    maxRow = k;
                }
            }

            let temp = augmented[i];
            augmented[i] = augmented[maxRow];
            augmented[maxRow] = temp;

            if (Math.abs(augmented[i][i]) < 1e-12) {
                throw new Error("Matrix is singular and cannot be inverted");
            }

            let pivot = augmented[i][i];
            for (let j = 0; j < 2 * n; j++) {
                augmented[i][j] /= pivot;
            }

            for (let k = 0; k < n; k++) {
                if (k !== i) {
                    let factor = augmented[k][i];
                    for (let j = 0; j < 2 * n; j++) {
                        augmented[k][j] -= factor * augmented[i][j];
                    }
                }
            }
        }
        let inv = [];
        for (let i = 0; i < n; i++) {
            inv[i] = augmented[i].slice(n, 2 * n);
        }
        return new Matrix(inv);
    }
}

class PolynomialFitter {
    constructor(degree) {
        this.degree = degree;
        this.xPointMatrix = []; 
        this.yPoints = [];      
    }
    addPoint(x, y) {
        this.yPoints.push([y]);
        let xArray = new Array(this.degree + 1);
        for (let i = 0; i < xArray.length; i++) {
            xArray[i] = Math.pow(x, i);
        }
        this.xPointMatrix.push(xArray);
    }
    fit() {
        let xMatrix = new Matrix(this.xPointMatrix);
        let yMatrix = new Matrix(this.yPoints);
        let coeffsMatrix = xMatrix.transpose().multiply(xMatrix)
            .inverse()
            .multiply(xMatrix.transpose())
            .multiply(yMatrix);
        let coeffsRow = coeffsMatrix.transpose().data[0];
        return coeffsRow;
    }
}

let finalLocation = null;
let lastGuessTime = 0;
let hasMadeInitialGuess = false;
let hasMadeManualGuess = false;

export function getFinalLocation() {
    return finalLocation;
}

export function setFinalLocation(location) {
    finalLocation = location;
}

export function getLastGuessTime() {
    return lastGuessTime;
}

class PreciseGuessBurrow {
    constructor() {
        this.particleLocations = [];
        this.guessPoint = null;
        this.lastLavaParticle = 0;
    }

    onWorldChange() {
        this.guessPoint = null;
        this.particleLocations = [];
        hasMadeManualGuess = false;
        hasMadeInitialGuess = false;
        finalLocation = null;
    }

    onReceiveParticle(packet) {
        if (packet.func_179749_a() != 'DRIP_LAVA' || parseInt(packet.func_149222_k()) != 2 || parseFloat(packet.func_149227_j()).toFixed(1) != -0.5) return;
        const currLoc = new SboVec(packet.func_149220_d(), packet.func_149226_e(), packet.func_149225_f());
        this.lastLavaParticle = Date.now();
        if (Date.now() - lastGuessTime > 3000) return;
        
        if (this.particleLocations.length === 0) {
            this.particleLocations.push(currLoc);
            return;
        }

        const distToLast = this.particleLocations[this.particleLocations.length - 1].distanceTo(currLoc);
        if (distToLast > 3 || distToLast == 0.0) return;
        this.particleLocations.push(currLoc);

        const guessPosition = this.guessBurrowLocation();
        if (!guessPosition) return;
        finalLocation = guessPosition.down(0.5).roundLocationToBlock();
        hasMadeManualGuess = true;
    }

    guessBurrowLocation() {
        if (this.particleLocations.length < 4) return null;
        const fitters = [
            new PolynomialFitter(3),
            new PolynomialFitter(3),
            new PolynomialFitter(3)
        ];

        this.particleLocations.forEach((location, index) => {
            const x = index;
            location.toDoubleArray().forEach((val, i) => {
                fitters[i].addPoint(x, val);
            });
        });

        const coefficients = fitters.map(fitter => fitter.fit());
        const startPointDerivative = SboVec.fromArray(coefficients.map(c => c[1]));

        const pitch = this.getPitchFromDerivative(startPointDerivative);
        const controlPointDistance = Math.sqrt(24 * Math.sin(pitch - Math.PI) + 25);
        const t = (3 * controlPointDistance) / startPointDerivative.length();
        const result = coefficients.map((coeff, i) => {
            return coeff[0] + coeff[1] * t + coeff[2] * Math.pow(t, 2) + coeff[3] * Math.pow(t, 3);
        });
        return new SboVec(...result);
    }

    getPitchFromDerivative(derivative) {
        const xzLength = Math.sqrt(derivative.x ** 2 + derivative.z ** 2);
        const pitchRadians = -Math.atan2(derivative.y, xzLength);
        
        let guessPitch = pitchRadians;
        let windowMin = -Math.PI / 2;
        let windowMax = Math.PI / 2;
        
        for (let i = 0; i < 100; i++) {
            let resultPitch = Math.atan2(Math.sin(guessPitch) - 0.75, Math.cos(guessPitch));
            
            if (resultPitch == pitchRadians) {
                return guessPitch;
            }
    
            if (resultPitch < pitchRadians) {
                windowMin = guessPitch;
            } else {
                windowMax = guessPitch;
            }
            guessPitch = (windowMin + windowMax) / 2;
        }
        return guessPitch;
    }

    onUseSpade(action, event) {
        let item = Player.getHeldItem()
        if (item == null) return
        if (!item.getName().includes("Spade") || !action.toString().includes('RIGHT_CLICK')) return;
        if (Date.now() - this.lastLavaParticle < 200) {
            cancel(event);
            return;
        }
        if (Date.now() - lastGuessTime < 3000) return;
        this.particleLocations = [];
        lastGuessTime = Date.now();
    }
}
const preciseGuess = new PreciseGuessBurrow();

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


registerWhen(register("playerInteract", (action, pos, event) => {
    preciseGuess.onUseSpade(action, event);
}), () => settings.dianaBurrowGuess);

registerWhen(register("worldUnload", () => {
    preciseGuess.onWorldChange();
}), () => settings.dianaBurrowGuess);
registerWhen(register("worldLoad", () => {
    preciseGuess.onWorldChange();
}), () => settings.dianaBurrowGuess);
registerWhen(register("gameLoad", () => {
    preciseGuess.onWorldChange();
}), () => settings.dianaBurrowGuess);

registerWhen(register("packetReceived", (packet) => {
    preciseGuess.onReceiveParticle(packet);
}).setFilteredClass(net.minecraft.network.play.server.S2APacketParticles), () => settings.dianaBurrowGuess && getWorld() == "Hub");

registerWhen(register("tick", () => {
    tryToMakeInitialGuess();
}), () => settings.dianaAdvancedBurrowGuess);

registerWhen(register("chat", (burrow) => {
    hasMadeManualGuess = false;
    hasMadeInitialGuess = false;
}).setCriteria("&r&eYou dug out a Griffin Burrow! &r&7${burrow}&r"), () => settings.dianaAdvancedBurrowGuess);