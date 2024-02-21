import settings from "../../settings";
import { getplayername } from "../../utils/functions";

register("chat", (player, message) => {
    switch (message) {
        case "!w":
        case "!warp":
            if(settings.PartyCommands && settings.WarpCommand){
                setTimeout(function() {
                    ChatLib.command("p warp")
                },100)
            }
            break;

        case "!allinv":
        case "!allinvite":
            if(settings.PartyCommands && settings.AllinviteCommand){
                setTimeout(function() {
                    ChatLib.command("p setting allinvite")
                },100)
            }
            break;
        case "!transfer":
            if(settings.PartyCommands && settings.TransferCommand){
                setTimeout(function() {
                    ChatLib.command("p transfer " + getplayername(player))
                },100)
            }
            break;
        case "!demote":
            if (settings.PartyCommands && settings.MoteCommand) {
                setTimeout(function() {
                    ChatLib.command("p demote " + getplayername(player))
                },100)
            }
            break;

        case "!promote":
            if (settings.PartyCommands && settings.MoteCommand) {
                setTimeout(function() {
                    ChatLib.command("p promote " + getplayername(player))
                },100)
            }
            break;
    }
}).setCriteria("&r&9Party &8> ${player}&f: &r${message}&r")


register("chat", (m1, m2) => {
    switch (m1) {
        case "!transfer":
            if(settings.PartyCommands && settings.TransferCommand){
                setTimeout(function() {
                    ChatLib.command("p transfer " + m2)
                },100)
            }
            break;
        case "!promote":
            if (settings.PartyCommands && settings.MoteCommand) {
                setTimeout(function() {
                    ChatLib.command("p promote " + m2)
                },100)
            }
            break;

        case "!demote":
            if (settings.PartyCommands && settings.MoteCommand) {
            setTimeout(function() {
                ChatLib.command("p demote " + m2)
            },100)
            }
                break;
}
}).setCriteria("&r&9Party &8> ${player}&f: &r${m1} ${m2}&r")


