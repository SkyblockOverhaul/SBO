import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { mythosMobHpOverlay } from "./../guis/DianaGuis";
import { checkDiana } from "../../utils/checkDiana";
import RenderLibV2 from "../../../RenderLibv2";

export function getMobsToDisplay() {
    return names;
}

registerWhen(register("chat", (woah) => {
    if (checkDiana()) {
        if(settings.inquisDetect){
            ChatLib.command("pc x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()));
        }
        if(settings.announceKill !== ""){
            setTimeout(function() 
                {ChatLib.command("pc " + settings.announceKill);
            }, 5000);
        }
    }
}).setCriteria("&r&c&l${woah} &r&eYou dug out a &r&2Minos Inquisitor&r&e!&r"), () => settings.inquisDetect || settings.announceKill);

let Mobs = [];
let inqs = [];
registerWhen(register("step", () => {
    if (checkDiana()) {
        World.getAllEntitiesOfType(net.minecraft.entity.item.EntityArmorStand).forEach((mob) => {
            let name = mob.getName();
            if (Mobs.filter((e) => e.getUUID() === mob.getUUID()).length === 0) {
                if ((name.includes("Exalted") || name.includes("Stalwart")) && !name.split(" ")[2].startsWith("0")) {
                    Mobs.push(mob);
                }
                if (name.includes("Inquisitor")) {
                    inqs.push(mob);
                }
            }
        });
        Mobs = Mobs.filter((e) => !e.getEntity()["field_70128_L"]);
    }
}).setFps(1), () => settings.mythosMobHp || settings.inqHighlight && getWorld() === "Hub");

let names = [];
registerWhen(register("step", () => {
    if (checkDiana()) {
        names = [];
        Mobs.forEach((nameTag) => {
            names.push(nameTag.getName());
        });
        mythosMobHpOverlay(names);
    }
}).setFps(6), () => settings.mythosMobHp && getWorld() === "Hub");
   
export const inqHighlightRegister = register("renderWorld", () => {
    inqs.forEach((mob) => {
        red = settings.inqColor.getRed() / 255;
        green = settings.inqColor.getGreen() / 255;
        blue = settings.inqColor.getBlue() / 255;
        alpha = settings.inqColor.getAlpha() / 255;
        RenderLibV2.drawEspBoxV2(mob.x, mob.y - 2.05, mob.z, 1, 2, 1, red, green, blue, alpha, false)   
    });
});

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