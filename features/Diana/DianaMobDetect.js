import settings from "../../settings";
import { data, registerWhen } from "../../utils/variables";
import { GOLD, BOLD, WHITE } from "../../utils/constants";
import {mobAnnouncement} from "../../utils/functions";
import { getWorld } from "../../utils/world";


registerWhen(register("chat", (woah) => {
    ChatLib.chat("Inquisitor Detected")
    ChatLib.command("pc x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()))
}).setCriteria("&r&c&l${woah} &r&eYou dug out a &r&2Minos Inquisitor&r&e!&r"), () => getWorld() === "Hub" && settings.inquisDetect);

// let inquis = undefined;
// registerWhen(register("command", () => {
//     entities = World.getAllEntities();
//     inquis = entities.find(entity => entity.getName().includes("Minos Inquisitor"));
//     if(inquis !== undefined && settings.inquisDetect) {
//         mobAnnouncement("pc", "Inquisitor", inquis.getX(), inquis.getY(), inquis.getZ());
//     }
// }).setName("detect"), () => getWorld() === "Hub" && settings.inquisDetect);
// let inquisitors = [];

// registerWhen(register("command", () => {
//     inquisitors = [];

//     entities = World.getAllEntities();
//     inqs = entities.filter((entity) => entity.getName().includes("Minos Inquisitor"));
//     print(inqs.length);

//     if (inqs.length > 0) {
//         Client.Companion.showTitle(`${GOLD}${BOLD}INQUISITOR ${WHITE}DETECTED!`, "", 0, 25, 5);
//         if (data.moblist.includes("inquis"))
//             inqs.forEach(inq => { inquisitors.push(inq) });
//     }
// }).setName("detectTest"), () => getWorld() === "Hub" && settings.inquisDetect);