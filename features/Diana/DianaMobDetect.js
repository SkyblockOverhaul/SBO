import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";


registerWhen(register("chat", (woah) => {
    ChatLib.command("pc x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()) + " [Inquisitor] - SBO");
}).setCriteria("&r&c&l${woah} &r&eYou dug out a &r&2Minos Inquisitor&r&e!&r"), () => getWorld() === "Hub" && settings.inquisDetect);

register("command", () => {
    let scorboardlines = Scoreboard.getLines().map(line => line.getName().removeFormatting());
    scorboardlines.forEach(line => {
        if(line.includes("⏣")){
            line = line.replace("⏣", "");
            line = line.replace("⚽", "");
            line = line.trim(2);
            print(line);
            ChatLib.command("pc x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()));
        }
    });
}).setName("sbodetect");


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