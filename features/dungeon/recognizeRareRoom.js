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
    "138,-60": "Hanging Vines",
    "570, -312": "Carpets" // m6 rare room
}
let rareRoomFound = false;
registerWhen(register("step", () => {
    let scoreBoardLines = Scoreboard.getLines();
    if (scoreBoardLines != undefined) {
        if (scoreBoardLines[scoreBoardLines.length - 1] != undefined) {
            let scoreBoardLastLine = scoreBoardLines[scoreBoardLines.length - 1].toString().split(" ");
            let scoreBoardId = scoreBoardLastLine[scoreBoardLastLine.length - 1].toString();
            let isSolo = scoreBoardLines[2].toString().includes("Solo");
            if (rareRooms.hasOwnProperty(scoreBoardId)) {
                if (rareRooms[scoreBoardId]) {
                    foundRoom(rareRooms[scoreBoardId], isSolo, scoreBoardId);
                    rareRoomFound = true;wa
                }
            }
            else {
                if (scoreBoardId.split(",")[1] == -60 && scoreBoardId.split(",")[0] != -60) {
                    foundRoom("Unknown", isSolo, scoreBoardId);
                    rareRoomFound = true;
                }
            }
        }
    }
}).setFps(1), () => getWorld() == "Catacombs" && settings.recognizeRareRoom);

function foundRoom(room, isSolo, scoreBoardId) {
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



// org.mozilla.javascript.EcmaError: TypeError: Cannot call method "toString" of undefined (file:/C:/Users/felix/AppData/Roaming/.minecraft/config/ChatTriggers/modules/SBO/features/dungeon/recognizeRareRoom.js#22)
// 	at org.mozilla.javascript.ScriptRuntime.constructError(ScriptRuntime.java:4642)
// 	at org.mozilla.javascript.ScriptRuntime.constructError(ScriptRuntime.java:4622)
// 	at org.mozilla.javascript.ScriptRuntime.typeError(ScriptRuntime.java:4651)
// 	at org.mozilla.javascript.ScriptRuntime.typeError2(ScriptRuntime.java:4666)
// 	at org.mozilla.javascript.ScriptRuntime.undefCallError(ScriptRuntime.java:4684)
// 	at org.mozilla.javascript.ScriptRuntime.getPropFunctionAndThis(ScriptRuntime.java:2864)
// 	at org.mozilla.javascript.optimizer.OptRuntime.callProp0(OptRuntime.java:90)
// 	at SBO_features_dungeon_recognizeRareRoom_js_1216._c_anonymous_1(SBO/features/dungeon/recognizeRareRoom.js:22)
// 	at SBO_features_dungeon_recognizeRareRoom_js_1216.call(SBO/features/dungeon/recognizeRareRoom.js)
// 	at org.mozilla.javascript.ContextFactory.doTopCall(ContextFactory.java:342)
// 	at org.mozilla.javascript.ScriptRuntime.doTopCall(ScriptRuntime.java:3951)
// 	at SBO_features_dungeon_recognizeRareRoom_js_1216.call(SBO/features/dungeon/recognizeRareRoom.js)
// 	at org.mozilla.javascript.ArrowFunction.call(ArrowFunction.java:40)
// 	at com.chattriggers.ctjs.engine.langs.js.JSLoader.trigger(JSLoader.kt:298)
