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
        packet.func_179749_a().toString() === 'CRIT_MAGIC' &&
        packet.func_149222_k() === 4 &&
        packet.func_149227_j() === 0.01 &&
        packet.func_149221_g() === 0.5 &&
        packet.func_149224_h() === 0.1 &&
        packet.func_149223_i() === 0.5
    ),
    MOB: new ParticleType(packet =>
        packet.func_179749_a().toString() === 'CRIT' &&
        packet.func_149222_k() === 3 &&
        packet.func_149227_j() === 0.01 &&
        packet.func_149221_g() === 0.5 &&
        packet.func_149224_h() === 0.1 &&
        packet.func_149223_i() === 0.5
    ),
    TREASURE: new ParticleType(packet =>
        packet.func_179749_a().toString() === 'DRIP_LAVA' &&
        packet.func_149222_k() === 2 &&
        packet.func_149227_j() === 0.01 &&
        packet.func_149221_g() === 0.35 &&
        packet.func_149224_h() === 0.1 &&
        packet.func_149223_i() === 0.35
    ),
    FOOTSTEP: new ParticleType(packet =>
        packet.func_179749_a().toString() === 'FOOTSTEP' &&
        packet.func_149222_k() === 1 &&
        packet.func_149227_j() === 0.0 &&
        packet.func_149221_g() === 0.05 &&
        packet.func_149224_h() === 0.0 &&
        packet.func_149223_i() === 0.05
    ),
    ENCHANT: new ParticleType(packet =>
        packet.func_179749_a().toString() === 'ENCHANTMENT_TABLE' &&
        packet.func_149222_k() === 5 &&
        packet.func_149227_j() === 0.05 &&
        packet.func_149221_g() === 0.5 &&
        packet.func_149224_h() === 0.4 &&
        packet.func_149223_i() === 0.5
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

let printCount = 0;
const printLimit = 1;

function printLimited(message) {
    if (printCount < printLimit) {
        print(message);
        printCount++;
    }
}

function burrowDetect(packet) {
    typename = packet.func_179749_a().toString();
    if (typename != "FOOTSTEP" && typename != "CRIT_MAGIC" && typename != "CRIT" && typename != "DRIP_LAVA" && typename != "ENCHANTMENT_TABLE") return;
    const particleType = getParticleType(packet);
    if (!particleType) return;
    print("particleType: " + particleType);
    packet.forEach((particle) => {
        print("pType: " + particle);
    });
    if (!particleType) return;
    const pos = particle.getPos();
    const blockPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ()).down();
    print(blockPos);    
    if (burrowshistory.contains(blockPos)) return;
    let burrow = burrows.get(blockPos) || new Burrow(blockPos, false, false, -1);
    switch (type) {
        case ParticleType.FOOTSTEP:
            burrow.hasFootstep = true;
            break;
        case ParticleType.ENCHANT:
            burrow.hasEnchant = true;
            break;
        case ParticleType.EMPTY:
            burrow.type = 0;
            break;
        case ParticleType.MOB:
            burrow.type = 1;
            break;
        case ParticleType.TREASURE:
            burrow.type = 2;
            break;
    }
    print(burrow);
    print(burrow.type);
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
    burrows.forEach((burrow) => {
        console.log(burrow);
    });
}).setName("printburrows")

// registerWhen(register("packetReceived", (packet)=> {
//     burrowDetect(packet)
// }).setFilteredClass(net.minecraft.network.play.server.S2APacketParticles), () => settings.dianaBurrowDetect && getWorld() == "Hub");
register("packetReceived", (packet) => {

  // func is getting particle type
    burrowDetect(packet)
    if(packet.func_179749_a().toString() == "FOOTSTEP") {    
        printLimited("xoffset/zoffset: " + packet.func_149221_g().toFixed(2) + " " + packet.func_149223_i().toFixed(2) + "\n" + "yOffset: "+  packet.func_149224_h() + "\n" + "speed: " + packet.func_149227_j() + "\n" + "count: " + 	packet.func_149222_k())
    }


}).setFilteredClass(S2APacketParticles);