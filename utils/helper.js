export class SboVec {
    constructor(x, y, z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    distanceTo(other) {
        const dx = other.getX() - this.x;
        const dy = other.getY() - this.y;
        const dz = other.getZ() - this.z;
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

    down(amount) {
        return new SboVec(this.x, this.y - amount, this.z);
    }

    roundLocationToBlock() {
        const x = Math.round(this.x - 0.499999);
        const y = Math.round(this.y - 0.499999);
        const z = Math.round(this.z - 0.499999);
        return new SboVec(x, y, z);
    }

    toCleanString() {
        return `${this.x.toFixed(2)}, ${this.y.toFixed(2)}, ${this.z.toFixed(2)}`;
    }

    toDoubleArray() {
        return [this.x, this.y, this.z];
    }

    static fromArray(arr) {
        return new SboVec(arr[0], arr[1], arr[2]);
    }

    length() {
        return Math.sqrt(this.x ** 2 + this.y ** 2 + this.z ** 2);
    }
}