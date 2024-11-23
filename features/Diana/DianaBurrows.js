export const coloredArmorStands = {};
let lastInteractedPos = null;
export function getLastInteractedPos() {
    return lastInteractedPos;
}

import { registerWhen } from "../../utils/variables";
import settings from "../../settings";
import { getWorld } from "../../utils/world";
import { checkDiana } from "../../utils/checkDiana";
import { printDev } from "../../utils/functions";
import { createBurrowWaypoints, removeBurrowWaypoint, removeBurrowWaypointBySmoke, setBurrowWaypoints } from "../general/Waypoints";

class EvictingQueue {
    constructor(capacity) {
        this.capacity = capacity;
        this.queue = [];
    }

    add(item) {
        if (this.queue.length >= this.capacity) {
            this.queue.shift(); 
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

const S2APacketParticles = net.minecraft.network.play.server.S2APacketParticles
const getParticleName = packet => packet.func_179749_a();
const getParticleCount = packet => parseInt(packet.func_149222_k());
const getParticleSpeed = packet => parseFloat(packet.func_149227_j());
const getXOffset = packet => parseFloat(packet.func_149221_g());
const getYOffset = packet => parseFloat(packet.func_149224_h());
const getZOffset = packet => parseFloat(packet.func_149223_i());

class ParticleType {
    constructor(typeCheck) {
        this.check = typeCheck;
    }
}

const ParticleTypes = {
    EMPTY: new ParticleType(packet =>
        getParticleName(packet).toString() == "CRIT_MAGIC" &&
        getParticleCount(packet) == 4 &&
        getParticleSpeed(packet).toFixed(2) == 0.01 &&
        getXOffset(packet).toFixed(1) == 0.5 &&
        getYOffset(packet).toFixed(1) == 0.1 &&
        getZOffset(packet).toFixed(1) == 0.5
    ),
    MOB: new ParticleType(packet =>
        getParticleName(packet).toString() == "CRIT" &&
        getParticleCount(packet) == 3 &&
        getParticleSpeed(packet).toFixed(2) == 0.01 &&
        getXOffset(packet).toFixed(2) == 0.5 &&
        getYOffset(packet).toFixed(1) == 0.1 &&
        getZOffset(packet).toFixed(2) == 0.5
    ),
    TREASURE: new ParticleType(packet =>
        getParticleName(packet).toString() == "DRIP_LAVA" &&
        getParticleCount(packet) == 2 &&
        getParticleSpeed(packet).toFixed(2) == 0.01 &&
        getXOffset(packet).toFixed(2) == 0.35 &&
        getYOffset(packet).toFixed(1) == 0.1 &&
        getZOffset(packet).toFixed(2) == 0.35
    ),
    FOOTSTEP: new ParticleType(packet =>
        getParticleName(packet).toString() == "FOOTSTEP" &&
        getParticleCount(packet) == 1 &&
        getParticleSpeed(packet) == 0 &&
        getXOffset(packet).toFixed(2) == 0.05 &&
        getYOffset(packet) == 0 &&
        getZOffset(packet).toFixed(2) == 0.05
    ),
    ENCHANT: new ParticleType(packet =>
        getParticleName(packet).toString() == "ENCHANTMENT_TABLE" &&
        getParticleCount(packet) == 5 &&
        getParticleSpeed(packet).toFixed(2) == 0.05 &&
        getXOffset(packet).toFixed(1) == 0.5 &&
        getYOffset(packet).toFixed(1) == 0.4 &&
        getZOffset(packet).toFixed(1) == 0.5
    ),
    FAR: new ParticleType(packet =>
        getParticleName(packet).toString() == "REDSTONE" &&
        getParticleCount(packet) == 0 &&
        getParticleSpeed(packet) == 1 &&
        getXOffset(packet) == 1 &&
        getYOffset(packet).toPrecision(1) == 0 &&
        getZOffset(packet) == 0
    ),
    MEDIUM: new ParticleType(packet =>
        getParticleName(packet).toString() == "REDSTONE" &&
        getParticleCount(packet) == 0 &&
        getParticleSpeed(packet) == 1 &&
        getXOffset(packet) == 1 &&
        getYOffset(packet).toPrecision(1) == 1 &&
        getZOffset(packet) == 0
    ),
    CLOSE: new ParticleType(packet =>
        getParticleName(packet).toString() == "REDSTONE" &&
        getParticleCount(packet) == 0 &&
        getParticleSpeed(packet) == 1 &&
        getXOffset(packet) == 0 &&
        getYOffset(packet).toPrecision(1) == 0.5 &&
        getZOffset(packet) == 0
    )
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
const burrowsHistory = new EvictingQueue(2);

function burrowDetect(packet) {
    typename = packet.func_179749_a().toString();
    if (typename != "FOOTSTEP" && typename != "CRIT_MAGIC" && typename != "CRIT" && typename != "DRIP_LAVA" && typename != "ENCHANTMENT_TABLE") return;
    const particleType = getParticleType(packet);
    if (!particleType) return;
    const pos = new BlockPos(packet.func_149220_d(), packet.func_149226_e(), packet.func_149225_f()).down();
    const posstring = pos.getX() + " " + pos.getY() + " " + pos.getZ(); 
    if (burrowsHistory.contains(posstring)) return;
    
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
    let removedBurrow = removeBurrowWaypointBySmoke(x, y, z);
    const posstring = x + " " + (y - 1) + " " + z;
    delete burrows[posstring];
}

function resetBurrows() {
    setBurrowWaypoints([]);
    burrows = {};
    burrowsHistory.clear();
}

let removePos = null;
function refreshBurrows() {
    if(removePos == null) return;
    result = removeBurrowWaypoint(removePos, burrows);
    burrows = result.burrows;
    let removedBurrow = result.removedBurrow;
    if (removedBurrow != null) {
        burrowsHistory.add(removedBurrow);
    }
}

const isCloseEnough = (entity, x, y, z) => {
    const entityX = entity.getX();
    const entityY = entity.getY();
    const entityZ = entity.getZ();
    return Math.abs(entityX - x) <= 2 && Math.abs(entityY - y) <= 3 && Math.abs(entityZ - z) <= 2;
}

const ItemStack = Java.type("net.minecraft.item.ItemStack");
const MCItem = Java.type("net.minecraft.item.Item");
let arrowArmorStands = [];
registerWhen(register("tick", () => {
    arrowArmorStands = World.getAllEntitiesOfType(Java.type("net.minecraft.entity.item.EntityArmorStand")).filter(armorStand => {
        const heldItemStack = armorStand.getEntity().func_71124_b(0);
        if (heldItemStack === null) return;
        const item = new Item(heldItemStack);
        const isArrow = item.equals(new Item(new ItemStack(MCItem.func_150899_d(262), 1)));
        return isArrow;
    });
}), () => settings.dianaAdvancedBurrowGuess && getWorld() == "Hub");

registerWhen(register("packetReceived", (packet) => {
    const particleType = getParticleType(packet);
    if (particleType !== "FAR" && particleType !== "MEDIUM" && particleType !== "CLOSE") return;

    const x = packet.func_149220_d();
    const y = packet.func_149226_e();
    const z = packet.func_149225_f();
    
    arrowArmorStands.forEach(armorStand => {
        const id = armorStand.getEntity().func_145782_y()
        if (coloredArmorStands[id] !== undefined) return;

        if (!isCloseEnough(armorStand, x, y, z)) return;
        coloredArmorStands[id] = particleType;
    });
}).setFilteredClass(S2APacketParticles), () => settings.dianaAdvancedBurrowGuess && getWorld() == "Hub");

// Delete colored armor stands that no longer exist
registerWhen(register("step", () => {
    const enumerator = Object.keys(coloredArmorStands);
    enumerator.forEach(id => {
        const armorStand = World.getWorld().func_73045_a(id);
        if (armorStand === null) {
            delete coloredArmorStands[id];
            return;
        }
    });
}).setFps(1), () => settings.dianaAdvancedBurrowGuess && getWorld == "Hub");

registerWhen(register("chat", (burrow) => {
    refreshBurrows();
}).setCriteria("&r&eYou dug out a Griffin Burrow! &r&7${burrow}&r"), () => settings.dianaBurrowDetect);

registerWhen(register("chat", (burrow) => {
    refreshBurrows();
}).setCriteria("&r&eYou finished the Griffin burrow chain!${burrow}"), () => settings.dianaBurrowDetect);

registerWhen(register("chat", (died) => {
    refreshBurrows();
}).setCriteria(" ☠ You ${died}."), () => getWorld() == "Hub" && settings.dianaBurrowDetect);

registerWhen(register("worldUnload", () => {
    resetBurrows();
}), () => settings.dianaBurrowDetect);

register("command", () => {
    resetBurrows();
    ChatLib.chat("§6[SBO] §4Burrow Waypoints Cleared!§r")
}).setName("sboclearburrows").setAliases("sbocb"); 

register("chat", () => {
    resetBurrows();
    ChatLib.chat("§6[SBO] §4Burrow Waypoints Cleared!§r")
}).setCriteria("&r&6Poof! &r&eYou have cleared your griffin burrows!&r")

register("step", () => {
    for (let key in burrows) {
        if (burrows[key][0].type != undefined) {
            createBurrowWaypoints(burrows[key][0].type, burrows[key][1].x, burrows[key][1].y +1, burrows[key][1].z, [], burrows[key][2]);
        } 
    }
}).setFps(4);

registerWhen(register("packetReceived", (packet) => {
    packettype = packet.func_179749_a().toString()
    if(packettype == "SMOKE_LARGE") {
        packetSpeed = parseFloat(packet.func_149227_j()).toFixed(2)
        if(packetSpeed == 0.01) {
            pos = new BlockPos(packet.func_149220_d(), packet.func_149226_e(), packet.func_149225_f()).down();
            x = pos.getX();
            y = pos.getY();
            z = pos.getZ();
            if (!checkDiana()) return;
            removeBurrowBySmoke(x, (parseInt(y) + 1), z);
        }
    }
    burrowDetect(packet)    
}).setFilteredClass(S2APacketParticles), () => settings.dianaBurrowDetect && getWorld() == "Hub");

const C07PacketPlayerDigging = net.minecraft.network.play.client.C07PacketPlayerDigging

registerWhen(register("packetSent", (packet, event) => {
    let action = packet.func_180762_c()
    let pos = new BlockPos(packet.func_179715_a()).down()
    // print("Action: " + action + " Pos: " + pos)
    if(action == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
        let x = pos.getX();
        let y = pos.getY() +2;
        let z = pos.getZ();

        if (pos.getX() < 0) {
            x = x+ 1;
        }
        if (pos.getZ() < 0) {
            z = z + 1;
        }
        if (burrows[x + " " + (y-1) + " " + z]) {
            removePos = new BlockPos(x, y, z);
            lastInteractedPos = new BlockPos(x, y - 1, z);
        }
    }
    
}).setFilteredClass(C07PacketPlayerDigging), () => settings.dianaBurrowDetect && getWorld() == "Hub");

registerWhen(register("playerInteract", (action, pos) => {
    if(action.toString() == "RIGHT_CLICK_BLOCK") {
        let x = pos.getX();
        let y = pos.getY();
        let z = pos.getZ();

        if (pos.getX() < 0) {
            x = x+ 1;
        }
        if (pos.getZ() < 0) {
            z = z + 1;
        }
        if (burrows[x + " " + y + " " + z]) {
            removePos = new BlockPos(x, (parseInt(y) + 1), z);
            lastInteractedPos = new BlockPos(x, y, z);
        }
    }
}), () => settings.dianaBurrowDetect && getWorld() == "Hub");