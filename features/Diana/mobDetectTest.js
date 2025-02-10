import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { mythosMobHpOverlay } from "../guis/DianaGuis";
import { checkDiana } from "../../utils/checkDiana";
import RenderLibV2 from "../../../RenderLibV2";
import { data } from "../../utils/variables";
import { checkSendInqMsg } from "../../utils/functions";
export function getMobsToDisplay() {
    return names;
}

register("command", () => {
    let [send, text] = checkSendInqMsg(data.mobsSinceInq);
    if (send) {
        ChatLib.chat(text);
    }
}).setName("sboinqmsgtest");

let TrackedInqs = {};
let TrackedMobs = {};
let names = [];

function cleanName(rawName) {
    return rawName.removeFormatting().replace(/[^a-zA-Z\s]/g, "").trim();
}

function trackEntity(uuid, mob, dictionary, entityTypeCheck, entityClasses) {
    if (entityTypeCheck(cleanName(mob.getName()))) {
        if (!dictionary[uuid]) {
            dictionary[uuid] = {
                entity: mob,
                x: mob.getX(),
                y: mob.getY(),
                z: mob.getZ(),
                trackedEntity: null,
                lastSeen: Date.now(),
            };
        } else {
            let data = dictionary[uuid];
            data.x = mob.getX();
            data.y = mob.getY();
            data.z = mob.getZ();
            data.lastSeen = Date.now();

            if (!data.trackedEntity) {
                for (let entityClass of entityClasses) {
                    let foundEntity = World.getAllEntitiesOfType(entityClass)
                        .find(entity => entity.distanceTo(mob) < 3);

                    if (foundEntity) {
                        data.trackedEntity = foundEntity;
                        break;
                    }
                }
            }

            if (data.trackedEntity) {
                data.x = data.trackedEntity.getX();
                data.y = data.trackedEntity.getY();
                data.z = data.trackedEntity.getZ();
            }
        }
    }
}


function updateTrackedEntities(trackedEntities, seenUUIDs) {
    for (let uuid in trackedEntities) {
        let mob = trackedEntities[uuid];
        if (!seenUUIDs.has(uuid)) {
            if (mob.trackedEntity) {
                if (mob.trackedEntity.getEntity()["field_70128_L"]) {
                    delete trackedEntities[uuid];
                    continue;
                }
                mob.x = mob.trackedEntity.getX();
                mob.y = mob.trackedEntity.getY();
                mob.z = mob.trackedEntity.getZ();
                mob.lastSeen = Date.now();
            } else if (Date.now() - mob.lastSeen > 5000) {
                delete trackedEntities[uuid];
            }
        }
    }
}

const dianaEntityList = [
    net.minecraft.entity.monster.EntityZombie,
    net.minecraft.entity.passive.EntityIronGolem,
    net.minecraft.entity.passive.EntityOcelot
];

registerWhen(register("step", () => {
    if (checkDiana()) {
        let seenUUIDs = new Set();
        
        World.getAllEntitiesOfType(net.minecraft.entity.item.EntityArmorStand).forEach((armorStand) => {
            let uuid = armorStand.getUUID();
            seenUUIDs.add(uuid);
            
            trackEntity(uuid, armorStand, TrackedInqs, (name) => name.includes("Golden"), [net.minecraft.entity.monster.EntityZombie]);  
            trackEntity(uuid, armorStand, TrackedMobs, (name) => (name.includes("Exalted") || 
            name.includes("Stalwart")) && !name.split(" ")[2].startsWith("0"), dianaEntityList);
        });
        
        updateTrackedEntities(TrackedInqs, seenUUIDs);
        updateTrackedEntities(TrackedMobs, seenUUIDs);
    }
}).setFps(6), () => settings.mythosMobHp || settings.inqHighlight || settings.inqCircle && getWorld() === "Hub");

export const inqHighlightRegister = register("renderWorld", () => {
    Object.values(TrackedInqs).forEach((mob) => {
        let yOffset = mob.trackedEntity ? 0 : -2.05;
        let red = settings.inqColor.getRed() / 255;
        let green = settings.inqColor.getGreen() / 255;
        let blue = settings.inqColor.getBlue() / 255;
        let alpha = settings.inqColor.getAlpha() / 255;
        
        if (settings.inqHighlight) {
            RenderLibV2.drawEspBoxV2(mob.x, mob.y + yOffset, mob.z, 1, 2, 1, red, green, blue, alpha, false);
        }
        if (settings.inqCircle) {
            let height = 0.6;
            let y = mob.y + yOffset;
            if (settings.inqCylinder) {
                height = 50;
                y = 50;
            }
            RenderLibV2.drawCyl(mob.x, y, mob.z, 30, 30, height, 120, 1, 0, 90, 90, red, green, blue, alpha, false, false);
        }
    });
});
// inqHighlightRegister.unregister();

registerWhen(register("step", () => {
    if (checkDiana()) {
        names = [];
        Object.values(TrackedMobs).forEach((mob) => {
            names.push(mob.entity.getName());
        });
        mythosMobHpOverlay(names);
    }
}).setFps(6), () => settings.mythosMobHp && getWorld() === "Hub");

register("worldUnload", () => {
    TrackedInqs = {};
    TrackedMobs = {};
    names = [];
});
