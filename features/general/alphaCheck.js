import settings from "../../settings";
import axios from "../../../axios";
import { registerWhen } from "../../utils/variables";

let refreshBool = false;
let alphaSlots = undefined;
let alphaPlayers = undefined;
registerWhen(register("step", () => { // vielleicht verwerfen weil dicsord ping vorhanden ist
    if (!refreshBool ) {
        refreshBool = true;
        axios.get("https://api.mcsrvstat.us/3/alpha.hypixel.net").then(response => {
            // to int
            alphaSlots = parseInt(response.data.players.max);
            alphaPlayers = parseInt(response.data.players.online);
            if (alphaSlots != undefined) {
                if (alphaSlots >= 100) {
                    if (notify) {
                        ChatLib.chat("§6[SBO] §eAlpha server is maybe open! current: &b" + alphaPlayers +"/" + alphaSlots);
                        ChatLib.chat("§6[SBO] §eto toggle notifications use §b/sbonotifyalpha",);
                        Client.showTitle(`&r&6&l<&4&l!!!&6&l> &b&lAlpha Maybe Open! &6&l<&4&l!!!&6&l>`, "&ecurrent: &b" + alphaPlayers +"/" + alphaSlots, 0, 90, 20);
                    }
                }
            }
        }).catch(error => {
            print(error);
        });
        setTimeout(() => {
            refreshBool = false;
        }, 60000);
    }
}).setFps(1), () => settings.alphaCheck);

let notify = true;
register("command", () => {
    notify = !notify;
    ChatLib.chat("§6[SBO] §eAlpha server notifications: &b" + notify);
    if (!settings.alphaCheck) {
        ChatLib.chat("§6[SBO] &eAlpha check is not enabled in settings!");
    }
}).setName("sbonotifyalpha");