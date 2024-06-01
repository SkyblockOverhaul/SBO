import { registerWhen } from "../../utils/variables";
import settings from "../../settings";
import { getWorld } from "../../utils/world";
import { checkDiana } from "../../utils/checkDiana";



class EvictingQueue {
    constructor(capacity) {
        this.capacity = capacity;
        this.queue = [];
    }

    add(item) {
        if (this.queue.length >= this.capacity) {
            this.queue.shift(); // Remove the oldest item
        }
        this.queue.push(item);
    }

    contains(item) {
        return this.queue.includes(item);
    }

    clear() {
        this.queue = [];
    }
}
const EnumParticleTypes = net.minecraft.util.EnumParticleTypes
const S2APacketParticles = net.minecraft.network.play.server.S2APacketParticles
class ParticleType {
    constructor(typeCheck) {
        this.check = typeCheck;
    }
}

const ParticleTypes = {
    EMPTY: new ParticleType(packet =>
        packet.func_179749_a().toString() == "CRIT_MAGIC" &&
        parseInt(packet.func_149222_k()) == 4 &&
        parseFloat(packet.func_149227_j()).toFixed(2) == 0.01 &&
        parseFloat(packet.func_149221_g()).toFixed(1) == 0.5 &&
        parseFloat(packet.func_149224_h()).toFixed(1) == 0.1 &&
        parseFloat(packet.func_149223_i()).toFixed(1) == 0.5
    ),
    MOB: new ParticleType(packet =>
        packet.func_179749_a().toString() == "CRIT" &&
        parseInt(packet.func_149222_k()) == 3 &&
        parseFloat(packet.func_149227_j()).toFixed(2) == 0.01 &&
        parseFloat(packet.func_149221_g()).toFixed(2) == 0.5 &&
        parseFloat(packet.func_149224_h()).toFixed(1) == 0.1 &&
        parseFloat(packet.func_149223_i()).toFixed(2) == 0.5
    ),
    TREASURE: new ParticleType(packet =>
        packet.func_179749_a().toString() == "DRIP_LAVA" &&
        parseInt(packet.func_149222_k()) == 2 &&
        parseFloat(packet.func_149227_j()).toFixed(2) == 0.01 &&
        parseFloat(packet.func_149221_g()).toFixed(2) == 0.35 &&
        parseFloat(packet.func_149224_h()).toFixed(1) == 0.1 &&
        parseFloat(packet.func_149223_i()).toFixed(2) == 0.35
    ),
    FOOTSTEP: new ParticleType(packet =>
        packet.func_179749_a().toString() == "FOOTSTEP" &&
        parseInt(packet.func_149222_k()) == 1 &&
        parseInt(packet.func_149227_j()) == 0 &&
        parseFloat(packet.func_149221_g().toFixed(2)) == 0.05 &&
        parseInt(packet.func_149224_h()) == 0 &&
        parseFloat(packet.func_149223_i().toFixed(2)) == 0.05
    ),
    ENCHANT: new ParticleType(packet =>
        packet.func_179749_a().toString() == "ENCHANTMENT_TABLE" &&
        parseInt(packet.func_149222_k()) == 5 &&
        parseFloat(packet.func_149227_j()).toFixed(2) == 0.05 &&
        parseFloat(packet.func_149221_g()).toFixed(1) == 0.5 &&
        parseFloat(packet.func_149224_h()).toFixed(1) == 0.4 &&
        parseFloat(packet.func_149223_i()).toFixed(1) == 0.5
    ),
};

function getParticleType(packet) {
    for (let key in ParticleTypes) {
        if (ParticleTypes[key].check(packet)) {
            return key;
        }
    }
    return null;
}


class Diggable {
    constructor(x, y, z, type) {
        x = x;
        y = y;
        z = z;
        type = type;
        blockPos = new BlockPos(x, y, z);
    }

    get waypointText() {
        switch (type) {
            case 0: return '§aStart (Particle)';
            case 1: return '§cMob (Particle)';
            case 2: return '§6Treasure (Particle)';
            default: return 'Burrow (Particle)';
        }
    }

}

class Burrow extends Diggable {
    constructor(x, y, z, hasFootstep, hasEnchant, type) {
        super(x, y, z, type);
        hasFootstep = hasFootstep;
        hasEnchant = hasEnchant;
    }

    static fromVec3(vec3, hasFootstep, hasEnchant, type) {
        return new Burrow(vec3.x, vec3.y, vec3.z, hasFootstep, hasEnchant, type);
    }
}

function burrowDetect(packet) {
    typename = packet.func_179749_a().toString();
    if (typename != "FOOTSTEP" && typename != "CRIT_MAGIC" && typename != "CRIT" && typename != "DRIP_LAVA" && typename != "ENCHANTMENT_TABLE") return;
    const particleType = getParticleType(packet);
    // print("particleType: " + particleType);
    if (!particleType) return;
    // print("particleType: " + particleType);
    const pos = new BlockPos(packet.func_149220_d(), packet.func_149226_e(), packet.func_149225_f()).down();
    if (burrowshistory.contains(pos)) return;
    let burrow = burrows.get(pos) || new Burrow(pos.x, pos.y, pos.z, false, false, -1);
    switch (particleType) {
        case ParticleType.FOOTSTEP:
            burrow.hasFootstep = true;
            break;
        case ParticleType.ENCHANT:
            burrow.hasEnchant = true;
            break;
        case ParticleType.EMPTY:
            print("Empty");
            burrow.type = 0;
            break;
        case ParticleType.MOB:
            burrow.type = 1;
            break;
        case ParticleType.TREASURE:
            burrow.type = 2;
            break;
    }
    burrows.set(blockPos, burrow);
}


function removeBurrowBySmoke(x, y, z) {
    // removeBurrowWaypointBySmoke(x, y, z);
    burrows = burrows.filter(([type, xb, yb, zb]) => xb !== x && yb !== y && zb !== z);
}

let burrows = new Map();
let burrowshistory = new EvictingQueue(5);
let lastDugBurrow = null;


function resetBurrows() {
    burrows.clear();
    burrowshistory.clear();
    lastDugBurrow = null;
}

function refreshBurrows() {
    if (lastDugBurrow !== null) {
        const burrows = burrows.get(lastDugBurrow);
        if (!burrows) return;
        burrowshistory.add(lastDugBurrow);
        burrows.delete(burrows.blockPos);
        lastDugBurrow = null;
    }
}

registerWhen(register("chat", (burrow) => {
    refreshBurrows();
}).setCriteria("&r&eYou dug out a Griffin Burrow! &r&7${burrow}&r"), () => settings.dianaBurrowDetect);

registerWhen(register("chat", (burrow) => {
    refreshBurrows();
}).setCriteria("&r&eYou finished the Griffin burrow chain!${burrow}"), () => settings.dianaBurrowDetect);

registerWhen(register("chat", () => {
    resetBurrows();
}).setCriteria(" ☠ You ${died}."), () => getWorld() == "Hub" && settings.dianaBurrowDetect);

// registerWhen(register("spawnParticle", (particle, type, event) => {
//     if (!checkDiana()) return;
//     if (type.toString() == "SMOKE_LARGE") {
//         const particlepos = particle.getPos();
//         const xyz = [particlepos.getX(), particlepos.getY(), particlepos.getZ()];
//         const [x, y , z] = [xyz[0], xyz[1], xyz[2]];
//         removeBurrowBySmoke(x, y, z);
//     }
//     burrowDetect(particle, type);

// }), () => settings.dianaBurrowDetect && getWorld() == "Hub");

registerWhen(register("worldUnload", () => {
    resetBurrows();
}), () => settings.dianaBurrowDetect);

registerWhen(register("step", () => {
    if (!checkDiana()) {
        resetBurrows();
    }
}).setFps(1), () => settings.dianaBurrowDetect);

register("command", () => {
    resetBurrows();
    ChatLib.chat("§6[SBO] §4Burrow Waypoints Cleared!§r")
}).setName("sboclearburrows").setAliases("sbocb"); 

register("command", () => {
    // test command print all burrows to console
    burrows.forEach((burrow, pos) => {
        console.log(pos); // Position der Höhle
        console.log(burrow.type); // Details zur Höhle
    });
}).setName("printburrows")

// registerWhen(register("packetReceived", (packet)=> {
//     burrowDetect(packet)
// }).setFilteredClass(net.minecraft.network.play.server.S2APacketParticles), () => settings.dianaBurrowDetect && getWorld() == "Hub");
register("packetReceived", (packet) => {

  // func is getting particle type
  if (packet.func_179749_a().toString() == "CRIT_MAGIC") {
    // print("Packet type: " + packet.func_179749_a().toString());
    // print("Packet count: " + parseInt(packet.func_149222_k()));
    // print("Packet speed: " + parseFloat(packet.func_149227_j()).toFixed(2));
    // print("Packet xOffset: " + parseFloat(packet.func_149221_g()).toFixed(1));
    // print("Packet yOffset: " + parseFloat(packet.func_149224_h()).toFixed(1));
    // print("Packet zOffset: " + parseFloat(packet.func_149223_i()).toFixed(1));
  }
  burrowDetect(packet)
}).setFilteredClass(S2APacketParticles);