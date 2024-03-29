import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";

const rareRooms = {
    "246,-60": "Trinity",
    "354,-60": "Sanctuary",
    "282,-60": "Pillars",
    "102,-60": "Sand Dragon",
    "66,-60": "Tombstone",
    "30,-60": "Stone Window",
    "318,-60": "Lava Pool",
    "174,-60": "Lava Skull",
    "210,-60": "Mini Rail Track",
    "138,-60": "Hanging Vines"
}
let scoreBoardId = undefined;
let rareRoomFound = false;
registerWhen(register("step", () => {
    scoreBoardLines = Scoreboard.getLines();
    scoreBoardLastLine = scoreBoardLines[scoreBoardLines.length - 1].toString().split(" ");
    scoreBoardId = scoreBoardLastLine[scoreBoardLastLine.length - 1].toString();
    let isSolo = scoreBoardLines[2].toString().includes("Solo");
    if (rareRooms.hasOwnProperty(scoreBoardId)) {
        if (rareRooms[scoreBoardId]) {
            foundRoom(rareRooms[scoreBoardId], isSolo);
            rareRoomFound = true;
        }
    }
    else {
        if (scoreBoardId.split(",")[1] == -60 && scoreBoardId.split(",")[0] != -60) {
            foundRoom("Unknown", isSolo);
            rareRoomFound = true;
        }
    }
}).setFps(1), () => getWorld() == "Catacombs" && settings.recognizeRareRoom);

function foundRoom(room, isSolo) {
    if (!rareRoomFound) {
        if (room != "Unknown") {
            ChatLib.chat("§6[SBO] §eYou are in a rare room: " + rareRooms[scoreBoardId]);
            if (settings.notifyPartyRareRoom && !isSolo) {
                ChatLib.command("pc [SBO] Found Rare Room: " + rareRooms[scoreBoardId]);
            }
            if (settings.announceRareRoomScreen) {
                Client.showTitle(`&r&6&l<&b&l&kO&6&l> &e&l${rareRooms[scoreBoardId]}! &6&l<&b&l&kO&6&l>`, "", 0, 20, 20);
            }
        }
        else {
            ChatLib.chat("§6[SBO] §eYou are in a new rare room");
            if (settings.notifyPartyRareRoom && !isSolo) {
                ChatLib.command("pc [SBO] Found new Rare Room");
            }
            if (settings.announceRareRoomScreen) {
                Client.showTitle(`&r&6&l<&b&l&kO&6&l> &e&lNew Rare Room! &6&l<&b&l&kO&6&l>`, "", 0, 20, 20);
            }
        }
    }
}

register("worldLoad", () => {
    rareRoomFound = false;
});
