import RenderLibV2 from "../../../RenderLibv2";
import settings from "../../settings";
import { checkDiana } from "../../utils/checkDiana";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";


let Mobs = [];
registerWhen(register("step", () => {
    if (checkDiana()) {
        World.getAllEntitiesOfType(net.minecraft.entity.item.EntityArmorStand).forEach((mob) => {
            let name = mob.getName();
            if (Mobs.filter((e) => e.getUUID() === mob.getUUID()).length === 0) {
                if (name.includes("Inquisitor")){
                    Mobs.push(mob);
                }
            }
        });
        Mobs = Mobs.filter((e) => !e.getEntity()["field_70128_L"]);
    }
}).setFps(1), () => settings.inqHighlight && getWorld() === "Hub");


export const inqHighlightRegister = register("renderWorld", () => {
    Mobs.forEach((mob) => {
        red = settings.inqColor.getRed() / 255;
        green = settings.inqColor.getGreen() / 255;
        blue = settings.inqColor.getBlue() / 255;
        alpha = settings.inqColor.getAlpha() / 255;
        RenderLibV2.drawEspBoxV2(mob.x, mob.y - 2.05, mob.z, 1, 2, 1, red, green, blue, alpha, false)   
    });
});