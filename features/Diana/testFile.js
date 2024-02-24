class SoopyGuessBurrow {
    constructor() {
        this.dingIndex = 0;
        this.lastDing = 0;
        this.lastDingPitch = 0;
        this.firstPitch = 0;
        this.lastParticlePoint = null;
        this.lastParticlePoint2 = null;
        this.firstParticlePoint = null;
        this.particlePoint = null;
        this.guessPoint = null;
        this.lastSoundPoint = null;
        this.locs = [];
        this.dingSlope = [];
        this.distance = null;
        this.distance2 = null;
    }

    onWorldChange(event) {
        this.lastDing = 0;
        this.lastDingPitch = 0;
        this.firstPitch = 0;
        this.lastParticlePoint = null;
        this.lastParticlePoint2 = null;
        this.lastSoundPoint = null;
        this.firstParticlePoint = null;
        this.particlePoint = null;
        this.guessPoint = null;
        this.distance = null;
        this.dingIndex = 0;
        this.dingSlope = [];
    }

    onPlaySound(event) {
        if (!this.isEnabled()) return;
        if (event.soundName !== "note.harp") return;

        const pitch = event.pitch;
        if (this.lastDing === 0) {
            this.firstPitch = pitch;
        }

        this.lastDing = Date.now();

        if (pitch < this.lastDingPitch) {
            this.firstPitch = pitch;
            this.dingIndex = 0;
            this.dingSlope = [];
            this.lastDingPitch = pitch;
            this.lastParticlePoint = null;
            this.lastParticlePoint2 = null;
            this.lastSoundPoint = null;
            this.firstParticlePoint = null;
            this.distance = null;
            this.locs = [];
        }

        if (this.lastDingPitch === 0) {
            this.lastDingPitch = pitch;
            this.distance = null;
            this.lastParticlePoint = null;
            this.lastParticlePoint2 = null;
            this.lastSoundPoint = null;
            this.firstParticlePoint = null;
            this.locs = [];
            return;
        }

        this.dingIndex++;

        if (this.dingIndex > 1) this.dingSlope.push(pitch - this.lastDingPitch);
        if (this.dingSlope.length > 20) this.dingSlope.shift();
        const slope = this.dingSlope.length ? this.dingSlope.reduce((a, b) => a + b) / this.dingSlope.length : 0.0;
        const pos = event.location;
        this.lastSoundPoint = pos;
        this.lastDingPitch = pitch;

        if (this.lastParticlePoint2 === null || this.particlePoint === null || this.firstParticlePoint === null) {
            return;
        }

        this.distance2 = (Math.E / slope) - this.firstParticlePoint.distance(pos);

        if (this.distance2 > 1000) {
            console.log("Soopy distance2 is " + this.distance2);
            this.distance2 = null;
            this.guessPoint = null;

            // workaround: returning if the distance is too big
            return;
        }

        const lineDist = this.lastParticlePoint2.distance(this.particlePoint);

        this.distance = this.distance2;
        const changesHelp = this.particlePoint.subtract(this.lastParticlePoint2);
        let changes = [changesHelp.x, changesHelp.y, changesHelp.z];
        changes = changes.map(o => o / lineDist);

        this.lastSoundPoint && (this.guessPoint = new LorenzVec(
            this.lastSoundPoint.x + changes[0] * this.distance,
            this.lastSoundPoint.y + changes[1] * this.distance,
            this.lastSoundPoint.z + changes[2] * this.distance
        ));
    }

    solveEquationThing(x, y) {
        const a = (-y.x * x.y * x.x - y.y * x.y * x.z + y.y * x.y * x.x + x.y * x.z * y.z + x.x * x.z * y.x - x.x * x.z * y.z) / (x.y * y.x - x.y * y.z + x.x * y.z - y.x * x.z + y.y * x.z - y.y * x.x);
        const b = (y.x - y.y) * (x.x + a) * (x.y + a) / (x.y - x.x);
        const c = y.x - b / (x.x + a);
        return new LorenzVec(a, b, c);
    }

    onReceiveParticle(event) {
        if (!this.isEnabled()) return;
        const type = event.type;
        if (type !== EnumParticleTypes.DRIP_LAVA) return;
        const currLoc = event.location;

        let run = false;
        this.lastSoundPoint && (run = Math.abs(currLoc.x - this.lastSoundPoint.x) < 2 && Math.abs(currLoc.y - this.lastSoundPoint.y) < 0.5 && Math.abs(currLoc.z - this.lastSoundPoint.z) < 2);
        if (run) {
            if (this.locs.length < 100 && (!this.locs.length || this.locs[this.locs.length - 1].distance(currLoc) !== 0.0)) {
                let distMultiplier = 1.0;
                if (this.locs.length > 2) {
                    const predictedDist = 0.06507 * this.locs.length + 0.259;
                    const lastPos = this.locs[this.locs.length - 1];
                    const actualDist = currLoc.distance(lastPos);
                    distMultiplier = actualDist / predictedDist;
                }
                this.locs.push(currLoc);

                if (this.locs.length > 5 && this.guessPoint) {
                    const slopeThing = this.locs.map((_, index, arr) => Math.atan((arr[index].x - arr[index + 1].x) / (arr[index].z - arr[index + 1].z)));

                    const { a, b, c } = this.solveEquationThing(
                        new LorenzVec(slopeThing.length - 5, slopeThing.length - 3, slopeThing.length - 1),
                        new LorenzVec(slopeThing[slopeThing.length - 5], slopeThing[slopeThing.length - 3], slopeThing[slopeThing.length - 1])
                    );

                    const pr1 = [];
                    const pr2 = [];

                    const start = slopeThing.length - 1;
                    const lastPos = this.locs[start].clone().multiply(1);
                    const lastPos2 = this.locs[start].clone().multiply(1);

                    let distCovered = 0.0;

                    const ySpeed = (this.locs[this.locs.length - 1].x - this.locs[this.locs.length - 2].x) / Math.hypot(this.locs[this.locs.length - 1].x - this.locs[this.locs.length - 2].x, this.locs[this.locs.length - 1].z - this.locs[this.locs.length - 2].x);

                    for (let i = start + 1; distCovered < this.distance2 && i < 10000; i++) {
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

                            this.lastSoundPoint && (distCovered = Math.hypot(lastPos.x - this.lastSoundPoint.x, lastPos.z - this.lastSoundPoint.z));

                            if (distCovered > this.distance2) break;
                        }
                    }

                    if (!pr1.length) return;

                    const p1 = pr1[pr1.length - 1];
                    const p2 = pr2[pr2.length - 1];

                    if (this.guessPoint) {
                        const d1 = Math.pow(p1.x - this.guessPoint.x, 2) * (2 + (p1.z - this.guessPoint.z));
                        const d2 = Math.pow(p2.x - this.guessPoint.x, 2) * (2 + (p2.z - this.guessPoint.z));

                        const finalLocation = d1 < d2 ? new LorenzVec(Math.floor(p1.x), 255.0, Math.floor(p1.z)) : new LorenzVec(Math.floor(p2.x), 255.0, Math.floor(p2.z));
                        new BurrowGuessEvent(finalLocation).postAndCatch();
                    }
                }
            }

            if (!this.lastParticlePoint) {
                this.firstParticlePoint = currLoc.clone();
            }

            this.lastParticlePoint2 = this.lastParticlePoint.clone();
            this.lastParticlePoint = this.particlePoint.clone();

            this.particlePoint = currLoc.clone();

            if (!this.lastParticlePoint2 || !this.firstParticlePoint || !this.distance2 || !this.lastSoundPoint) return;

            const lineDist = this.lastParticlePoint2.distance(this.particlePoint);

            this.distance = this.distance2;

            const changesHelp = this.particlePoint.subtract(this.lastParticlePoint2);
            let changes = [changesHelp.x, changesHelp.y, changesHelp.z];
            changes = changes.map(o => o / lineDist);

            this.lastParticlePoint && (this.guessPoint = new LorenzVec(
                this.lastParticlePoint.x + changes[0] * this.distance,
                this.lastParticlePoint.y + changes[1] * this.distance,
                this.lastParticlePoint.z + changes[2] * this.distance
            ));
        }
    }

    isEnabled() {
        return DianaAPI.isDoingDiana() && SkyHanniMod.feature.event.diana.burrowsSoopyGuess;
    }
}



class LorenzVec {
    constructor(x, y, z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    toBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }

    toVec3() {
        return new Vec3(this.x, this.y, this.z);
    }

    distanceIgnoreY(other) {
        return Math.sqrt(this.distanceSqIgnoreY(other));
    }

    distance(other) {
        return Math.sqrt(this.distanceSq(other));
    }

    distanceSq(x, y, z) {
        return this.distanceSq(new LorenzVec(x, y, z));
    }

    distance(x, y, z) {
        return this.distance(new LorenzVec(x, y, z));
    }

    distanceSq(other) {
        const dx = other.x - this.x;
        const dy = other.y - this.y;
        const dz = other.z - this.z;
        return dx * dx + dy * dy + dz * dz;
    }

    distanceSqIgnoreY(other) {
        const dx = other.x - this.x;
        const dz = other.z - this.z;
        return dx * dx + dz * dz;
    }

    add(x = 0.0, y = 0.0, z = 0.0) {
        return new LorenzVec(this.x + x, this.y + y, this.z + z);
    }

    add(x = 0, y = 0, z = 0) {
        return new LorenzVec(this.x + x, this.y + y, this.z + z);
    }

    toString() {
        return `LorenzVec{x=${this.x}, y=${this.y}, z=${this.z}}`;
    }

    multiply(d) {
        return new LorenzVec(this.x * d, this.y * d, this.z * d);
    }

    multiply(d) {
        return new LorenzVec(this.x * d, this.y * d, this.z * d);
    }

    add(other) {
        return new LorenzVec(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    subtract(other) {
        return new LorenzVec(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    normalize() {
        const len = this.length();
        return new LorenzVec(this.x / len, this.y / len, this.z / len);
    }

    printWithAccuracy(accuracy, splitChar = " ") {
        if (accuracy === 0) {
            const x = Math.round(this.x);
            const y = Math.round(this.y);
            const z = Math.round(this.z);
            return `${x}${splitChar}${y}${splitChar}${z}`;
        } else {
            const x = Math.round(this.x * accuracy) / accuracy;
            const y = Math.round(this.y * accuracy) / accuracy;
            const z = Math.round(this.z * accuracy) / accuracy;
            return `${x}${splitChar}${y}${splitChar}${z}`;
        }
    }

    toCleanString() {
        return `${this.x} ${this.y} ${this.z}`;
    }

    length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    isZero() {
        return this.x === 0.0 && this.y === 0.0 && this.z === 0.0;
    }

    clone() {
        return new LorenzVec(this.x, this.y, this.z);
    }

    toDoubleArray() {
        return [this.x, this.y, this.z];
    }

    equalsIgnoreY(other) {
        return this.x === other.x && this.z === other.z;
    }

    equals(other) {
        if (this === other) return true;
        if (!(other instanceof LorenzVec)) return false;
        return this.x === other.x && this.y === other.y && this.z === other.z;
    }

    hashCode() {
        let result = this.x.hashCode();
        result = 31 * result + this.y.hashCode();
        result = 31 * result + this.z.hashCode();
        return result;
    }

    round(decimals) {
        return new LorenzVec(this.x.round(decimals), this.y.round(decimals), this.z.round(decimals));
    }

    roundLocationToBlock() {
        const x = Math.round(this.x - 0.499999);
        const y = Math.round(this.y - 0.499999);
        const z = Math.round(this.z - 0.499999);
        return new LorenzVec(x, y, z);
    }

    roundLocation() {
        const x = this.x < 0 ? Math.floor(this.x) - 1 : Math.floor(this.x);
        const y = Math.floor(this.y) - 1;
        const z = this.z < 0 ? Math.floor(this.z) - 1 : Math.floor(this.z);
        return new LorenzVec(x, y, z);
    }

    slope(other, factor) {
        return this.add(other.subtract(this).scale(factor));
    }

    roundLocationToBlock() {
        return new LorenzVec(Math.round(this.x - 0.499999), Math.round(this.y - 0.499999), Math.round(this.z - 0.499999));
    }

    scale(scalar) {
        return new LorenzVec(scalar * this.x, scalar * this.y, scalar * this.z);
    }

    axisAlignedTo(other) {
        return new AxisAlignedBB(this.x, this.y, this.z, other.x, other.y, other.z);
    }

    interpolate(other, factor) {
        if (!(factor >= 0.0 && factor <= 1.0)) {
            throw new Error(`Percentage must be between 0 and 1: ${factor}`);
        }
        const x = (1 - factor) * this.x + factor * other.x;
        const y = (1 - factor) * this.y + factor * other.y;
        const z = (1 - factor) * this.z + factor * other.z;
        return new LorenzVec(x, y, z);
    }

    static getFromYawPitch(yaw, pitch) {
        yaw = (yaw + 90) * Math.PI / 180;
        pitch = (pitch + 90) * Math.PI / 180;
        const x = Math.sin(pitch) * Math.cos(yaw);
        const y = Math.sin(pitch) * Math.sin(yaw);
        const z = Math.cos(pitch);
        return new LorenzVec(x, z, y);
    }

    static decodeFromString(string) {
        const [x, y, z] = string.split(":").map(parseFloat);
        return new LorenzVec(x, y, z);
    }

    static getBlockBelowPlayer() {
        return LocationUtils.playerLocation().roundLocationToBlock().add(0, -1, 0);
    }
}
