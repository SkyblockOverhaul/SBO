import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { mythosMobHpOverlay } from "./../guis/DianaGuis";
import { checkDiana } from "../../utils/checkDiana";
import RenderLibV2 from "../../../RenderLibV2";
import { data, dianaTrackerMayor } from "../../utils/variables";
import { calcPercentOne } from "../../utils/functions";
export function getMobsToDisplay() {
    return names;
}

function checkSendInqMsg() {
    let text = settings.announceKilltext;
    if (text != "") {
        if (text.includes("{since}")) {
            let since = data.mobsSinceInq
            text = text.replace(/{since}/g, since);
        }
        if (text.includes("{chance}")) {
            let chance = calcPercentOne(dianaTrackerMayor, "Minos Inquisitor")
            text = text.replace(/{chance}/g, chance);
        }
        return [true, text];
    } else {
        return [false, ""];
    }
}

registerWhen(register("chat", (woah, skytils) => {
    if (checkDiana()) {
        if(settings.inquisDetect) {
            ChatLib.command("pc x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()));
        }
        if(settings.announceKilltext !== "") {
            setTimeout(function() {
                let [send, text] = checkSendInqMsg();
                if (send) {
                    ChatLib.command("pc " + text);
                }
            }, 5000);
        }
    }
}).setCriteria("&r&c&l${woah} &r&eYou dug out a &r&2Minos Inquisitor&r&e!${skytils}"), () => settings.inquisDetect || settings.announceKilltext);

register("command", () => {
    let [send, text] = checkSendInqMsg();
    if (send) {
        ChatLib.chat(text);
    }
}).setName("sboinqmsgtest");


let Mobs = [];
let inqs = [];
registerWhen(register("step", () => {
    if (checkDiana()) {
        World.getAllEntitiesOfType(net.minecraft.entity.item.EntityArmorStand).forEach((mob) => {
            let name = mob.getName();   
            if (name != "Armor Stand") {
                if (Mobs.filter((e) => e.getUUID() === mob.getUUID()).length === 0) {
                    if ((name.includes("Exalted") || name.includes("Stalwart")) && !name.split(" ")[2].startsWith("0")) { //  || name.includes("Graveyard Zombie")
                        Mobs.push(mob);
                    }
                    
                }
            }
        });
        Mobs = Mobs.filter((e) => !e.getEntity()["field_70128_L"]);
        inqs = Mobs.filter((mob) => mob.getName().includes("Inquisitor")); // || mob.getName().includes("Graveyard Zombie")
    }
}).setFps(1), () => settings.mythosMobHp || settings.inqHighlight || settings.inqCircle && getWorld() === "Hub");

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
        let red = settings.inqColor.getRed() / 255;
        let green = settings.inqColor.getGreen() / 255;
        let blue = settings.inqColor.getBlue() / 255;
        let alpha = settings.inqColor.getAlpha() / 255;
        if (settings.inqHighlight) {
            RenderLibV2.drawEspBoxV2(mob.x, mob.y - 2.05, mob.z, 1, 2, 1, red, green, blue, alpha, false)   
        }
        if (settings.inqCircle) {
            let hight = 0.6;
            let y = mob.y - 2.05;
            if (settings.inqCylinder) {
                hight = 50;
                y = 50;
            }
            RenderLibV2.drawCyl(mob.x, y, mob.z, 30, 30, hight, 120, 1, 0, 90, 90, red, green, blue, alpha, false, false)
        }
    });
});
inqHighlightRegister.unregister();

// register("tick", () => {
//     // check if player is inside circle
//     // player x = Player.getX()
//     // player z = Player.getZ()
//     let inCircle = false;
//     inqs.forEach((mob) => {
//         let distance = Math.sqrt((mob.x - Player.getX()) ** 2 + (mob.z - Player.getZ()) ** 2);
//         if (distance < 30) {
//             inCircle = true;
//         }
//     });
//     ChatLib.chat("circle: " + inCircle);
// });

register("worldUnload", () => {
    Mobs = [];
    inqs = [];
    names = [];
});