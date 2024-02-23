import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";


registerWhen(register("chat", (woah) => {
    ChatLib.command("pc x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()));
}).setCriteria("&r&c&l${woah} &r&eYou dug out a &r&2Minos Inquisitor&r&e!&r"), () => getWorld() === "Hub" && settings.inquisDetect);


// register("command", () => {
//     ChatLib.command("pc x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()));
// }).setName("sbodetectinq");