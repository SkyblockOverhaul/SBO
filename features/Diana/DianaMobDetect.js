import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { mythosMobHpOverlay } from "./../guis/DianaGuis";
import { checkDiana } from "../../utils/checkDiana";

export function getMobsToDisplay() {
    return names;
}

registerWhen(register("chat", (woah) => {
    ChatLib.command("pc x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()));
}).setCriteria("&r&c&l${woah} &r&eYou dug out a &r&2Minos Inquisitor&r&e!&r"), () => settings.inquisDetect && checkDiana());

let Mobs = [];
registerWhen(register("step", () => {
    World.getAllEntitiesOfType(net.minecraft.entity.item.EntityArmorStand).forEach((mob) => {
        let name = mob.getName();
        if (Mobs.filter((e) => e.getUUID() === mob.getUUID()).length === 0) {
            if ((name.includes("Exalted") || name.includes("Stalwart")) && !name.split(" ")[2].startsWith("0")) {
                Mobs.push(mob);
            }
        }
    });
    Mobs = Mobs.filter((e) => !e.getEntity()["field_70128_L"]);
}).setFps(1), () => settings.mythosMobHp && checkDiana());

let names = [];
registerWhen(register("step", () => {
    names = [];
    Mobs.forEach((nameTag) => {
        names.push(nameTag.getName());
    });
    mythosMobHpOverlay(names);
}).setFps(6), () => settings.mythosMobHp && checkDiana());
        
//mob.nameTag.getName() step 10
// if (!Mobs?.map((a) => a.getUUID().toString()).includes(mob.getUUID().toString())) {
//     if ((name.includes("Exalted") || name.includes("Stalwart")) && !name.split(" ")[2].startsWith("0")) {
//         print("pushed");
//         Mobs.push(mob);
//     }
// }


// register("command", () => {
//     ChatLib.command("pc x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()));
// }).setName("sbodetectinq");