import { registerWhen } from "../../utils/variables";
import settings from "../../settings";
import { getWorld } from "../../utils/world";
import { checkDiana } from "../../utils/checkDiana";
import { createBurrowWaypoints, removeBurrowWaypoint, removeBurrowWaypointBySmoke, setBurrowWaypoints } from "../general/Waypoints";



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

let burrows = {};
let burrowshistory = new EvictingQueue(5);
let lastDugBurrow = null;

function burrowDetect(packet) {
    typename = packet.func_179749_a().toString();
    if (typename != "FOOTSTEP" && typename != "CRIT_MAGIC" && typename != "CRIT" && typename != "DRIP_LAVA" && typename != "ENCHANTMENT_TABLE") return;
    const particleType = getParticleType(packet);
    // print("particleType: " + particleType);
    if (!particleType) return;
    // print("particleType: " + particleType);
    const pos = new BlockPos(packet.func_149220_d(), packet.func_149226_e(), packet.func_149225_f()).down();
    const posstring = pos.getX() + " " + pos.getY() + " " + pos.getZ(); 
    if (burrowshistory.contains(pos)) return;
    
    if (!burrows[posstring]) {
        burrows[posstring] = [new Burrow(pos.x, pos.y, pos.z, null), { x : pos.x, y : pos.y, z : pos.z }, [packet.func_149220_d(), packet.func_149226_e(), packet.func_149225_f()]];
    }

    switch (particleType) {
        case "FOOTSTEP":
            burrows[posstring][0].hasFootstep = true;
            break;
        case "ENCHANT":
            burrows[posstring][0].hasEnchant = true;
            break;
        case "EMPTY":
            burrows[posstring][0].type = 0;
            break;
        case "MOB":
            burrows[posstring][0].type = 1;
            break;
        case "TREASURE":
            burrows[posstring][0].type = 2;
            break;
    }
}


function removeBurrowBySmoke(x, y, z) {
    removeBurrowWaypointBySmoke(x, y, z);
    // print("x" + x + " y: " + (y-1) + " z: " + z)
    const posstring = x + " " + (y - 1) + " " + z;
    // remove burrow from burrows
    delete burrows[posstring];
}


function resetBurrows() {
    burrows = {};
    burrowshistory.clear();
    lastDugBurrow = null;
}


function getClosestBurrowToPlayer() {
    let closestDistance = Infinity;
    let closestBurrow = null;

    for (let key in burrows) {
        const burrow = burrows[key][1];
        const distance = Math.sqrt(
            (Player.getX() - burrow.x)**2 +
            (Player.getY() - burrow.y)**2 +
            (Player.getZ() - burrow.z)**2
        );
        if (distance < closestDistance) {
            closestDistance = distance;
            closestBurrow = burrow;
        }
    }
    return closestBurrow;
}



function refreshBurrows(pos) {
    if(pos == null) return;
    burrows = removeBurrowWaypoint(pos, burrows);
}

function resetBurrows() {
    setBurrowWaypoints([]);
    burrows = {};
    // burrowshistory = [];
}

registerWhen(register("chat", (burrow) => {
    pos = getDigPos()
    refreshBurrows(pos);
}).setCriteria("&r&eYou dug out a Griffin Burrow! &r&7${burrow}&r"), () => settings.dianaBurrowDetect);

registerWhen(register("chat", (burrow) => {
    pos = getDigPos()
    refreshBurrows(pos);
}).setCriteria("&r&eYou finished the Griffin burrow chain!${burrow}"), () => settings.dianaBurrowDetect);

registerWhen(register("chat", (died) => {
    resetBurrows();
}).setCriteria(" ☠ You ${died}."), () => getWorld() == "Hub" && settings.dianaBurrowDetect);

registerWhen(register("spawnParticle", (particle, type, event) => {
    if (!checkDiana()) return;
    if (type.toString() == "SMOKE_LARGE") {
        const particlepos = particle.getPos();
        const xyz = [particlepos.getX(), particlepos.getY(), particlepos.getZ()];
        const [x, y , z] = [xyz[0], xyz[1], xyz[2]];
        // print("x: " + x + " y: " + y + " z: " + z);
        removeBurrowBySmoke(x, y, z);
    }
}), () => settings.dianaBurrowDetect && getWorld() == "Hub");

registerWhen(register("worldUnload", () => {
    resetBurrows();
}), () => settings.dianaBurrowDetect);

registerWhen(register("step", () => {
    if (!checkDiana()) {
        // resetBurrows();
    }
}).setFps(1), () => settings.dianaBurrowDetect);

register("command", () => {
    resetBurrows();
    ChatLib.chat("§6[SBO] §4Burrow Waypoints Cleared!§r")
}).setName("sboclearburrows").setAliases("sbocb"); 

register("step", () => {
    // test command print all burrows to console
    // print("Burrows: ");
    for (let key in burrows) {
        // print each burrow with cords and type
        // print("x: " + burrows[key][1].x + " y: " + burrows[key][1].y + " z: " + burrows[key][1].z + " type: " + burrows[key][0].type);
        createBurrowWaypoints(burrows[key][0].type, burrows[key][1].x, burrows[key][1].y +1, burrows[key][1].z, [], burrows[key][2]);
    }
}).setFps(4);

// registerWhen(register("packetReceived", (packet)=> {
//     burrowDetect(packet)
// }).setFilteredClass(net.minecraft.network.play.server.S2APacketParticles), () => settings.dianaBurrowDetect && getWorld() == "Hub");
register("packetReceived", (packet) => {
    // print("Packet type: " + packet.func_179749_a().toString());
    // print("Packet count: " + parseInt(packet.func_149222_k()));
    // print("Packet speed: " + parseFloat(packet.func_149227_j()).toFixed(2));
    // print("Packet xOffset: " + parseFloat(packet.func_149221_g()).toFixed(1));
    // print("Packet yOffset: " + parseFloat(packet.func_149224_h()).toFixed(1));
    // print("Packet zOffset: " + parseFloat(packet.func_149223_i()).toFixed(1));

    burrowDetect(packet)
}).setFilteredClass(S2APacketParticles);
const C07PacketPlayerDigging = net.minecraft.network.play.client.C07PacketPlayerDigging
let digPos = null;
let digAction = null;
var regex = /x=(\d+), y=(\d+), z=(\d+)/;
register("packetSent", (packet, event) => {
    let action = packet.func_180762_c()
    let pos = packet.func_179715_a()
    // print("Action: " + action + " Pos: " + pos)
    if(action == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
        var match = pos.toString().match(regex);
        // var x = parseInt(match[1]);
        // var y = parseInt(match[2]);
        // var z = parseInt(match[3]);
        digPos = match
        digAction = action
    }
    
}).setFilteredClass(C07PacketPlayerDigging);

function getDigPos(){
    return digPos
}