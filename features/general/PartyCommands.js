import settings from "../../settings";
import { getplayername } from "../../utils/functions";
import { tpsCommand } from "../../utils/tps";
import { data, dianaTrackerMayor } from "../../utils/variables";

const carrot = [
    "As I see it, Carrot",
    "It is Carrot",
    "It is decidedly Carrot",
    "Most likely Carrot",
    "Outlook Carrot",
    "Signs point to Carrot",
    "Without a Carrot",
    "Yes - Carrot",
    "Carrot - definitely",
    "You may rely on Carrot",
    "Ask Carrot later",
    "Carrot predict now",
    "Concentrate and ask Carrot ",
    "Don't count on it - Carrot 2024",
    "My reply is Carrot",
    "My sources say Carrot",
    "Outlook not so Carrot",
    "Very Carrot",
    "Carrot",
];


register("chat", (player, message) => {
    message = message.split(" ");
    switch (message[0].toLowerCase()) {
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
        case "!ptme":
        case "!transfer":
            if (settings.PartyCommands && settings.TransferCommand && message[1]){
                if(settings.PartyCommands && settings.TransferCommand){
                    setTimeout(function() {
                        ChatLib.command("p transfer " + message[1])
                    },100)
                }
            }
            else if(settings.PartyCommands && settings.TransferCommand){
                setTimeout(function() {
                    ChatLib.command("p transfer " + getplayername(player))
                },100)
            }
            break;
        case "!demote":
        case "!promote":
            if (settings.PartyCommands && settings.MoteCommand && message[1]) {
                setTimeout(function() {
                    ChatLib.command("p " + message[0].slice(1) + " " + message[1])
                },100)
                }
            else if (settings.PartyCommands && settings.MoteCommand) {
                setTimeout(function() {
                    ChatLib.command("p " + message[0].slice(1) + " " + getplayername(player))
                },100)
            }
            break;
        case "!c":
        case "!carrot":
            if(settings.carrotCommand){
                setTimeout(function() {
                    ChatLib.command("pc " + carrot[Math.floor(Math.random() * carrot.length)]);
                },100)
            }
            break;
        case "!time":
            if (settings.timeCommand) {
                setTimeout(function() {
                    ChatLib.command("pc " + new Date().toLocaleTimeString());
                }, 100)
            }
            break;
        case "!tps":
            if (settings.tpsCommand) {
                setTimeout(function() {
                    tpsCommand(player)
                }, 50)
            }
            break
        case "!chim":
        case "!chimera":
        case "!chims":
        case "!chimeras":
        case "!book":
        case "!books":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker && settings.chimeraCount) {
                setTimeout(function() {
                    ChatLib.command("pc Chimera: " + dianaTrackerMayor["items"]["Chimera"])
                }, 50)
            }
            break
        case "!inq":
        case "!inqs":
        case "!inquisitor":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker && settings.inquisitorCount) {
                setTimeout(function() {
                    ChatLib.command("pc Inquisitor: " + dianaTrackerMayor["mobs"]["Minos Inquisitor"])
                }, 50)
            }
            break
        case "!burrows":
        case "!burrow":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker && settings.burrowCount) {
                setTimeout(function() {
                    ChatLib.command("pc Burrows: " + dianaTrackerMayor["items"]["Total Burrows"])
                }, 50)
            }
            break
        case "!relic":
        case "!relics":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker && settings.relicCount) {
                setTimeout(function() {
                    ChatLib.command("pc Relics: " + dianaTrackerMayor["items"]["MINOS_RELIC"])
                }, 50)
            }
            break
        case "!stick":
        case "!sticks":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker && settings.stickCount) {
                setTimeout(function() {
                    ChatLib.command("pc Sticks: " + dianaTrackerMayor["items"]["Daedalus Stick"])
                }, 50)
            }
            break
        case "!mob":
        case "!mobs":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker && settings.mobCount) {
                setTimeout(function() {
                    ChatLib.command("pc Mobs: " + dianaTrackerMayor["mobs"]["TotalMobs"])
                }, 50)
            }
            break
        case "!since":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker && settings.sinceCount) {
                setTimeout(function() {
                    ChatLib.command("pc Mobs since inq: " + data.mobsSinceInq)
                }, 50)
            }
            break


    }
}).setCriteria("&r&9Party &8> ${player}&f: &r${message}&r")
