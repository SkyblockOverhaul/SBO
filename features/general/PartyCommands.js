import settings from "../../settings";
import { getplayername, formatTime, getDianaMayorTotalProfitAndOfferType, calcPercentOne, getBurrowsPerHour, getMobsPerHour, sboSetTimeout } from "../../utils/functions";
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

register("command", (args1, args2, ...args) => {
    if(args1 == undefined) {
        ChatLib.chat("&6[SBO] &aPlease use /sbopartycommands <command>")
        ChatLib.chat("&6[SBO] &eadd <playername>")
        ChatLib.chat("&6[SBO] &eremove <playername>")
        ChatLib.chat("&6[SBO] &elist")
        return;
    }
    if(args2 != undefined) {
        args2 = args2.toLowerCase().trim();
    }
    switch (args1.toLowerCase()) {
        case "add":
            if(args2 == undefined) {
                ChatLib.chat("&6[SBO] &aPlease provide a playername!")
                return;
            }
            if(data.partyBlacklist.includes(args2)) {
                ChatLib.chat("&6[SBO] &e" + args2 + " is already in the list!")
                return;
            }
            data.partyBlacklist.push(args2)
            ChatLib.chat("&6[SBO] &eAdded " + args2 + " to the blacklist!")
            break;
        case "remove":
            if(args2 == undefined) {
                ChatLib.chat("&6[SBO] &aPlease provide a playername!")
                return;
            }
            if(!data.partyBlacklist.includes(args2)) {
                ChatLib.chat("&6[SBO] &e" + args2 + " is not in the list!")
                return;
            }
            data.partyBlacklist = data.partyBlacklist.filter((name) => name != args2)
            ChatLib.chat("&6[SBO] &eRemoved " + args2 + " from the blacklist!")
            break;
        case "list":
            ChatLib.chat("&6[SBO] &eBlacklisted players:")
            data.partyBlacklist.forEach((name) => {
                ChatLib.chat("&7> &e" + name)
            })
            break;
    }
}).setName("sbopartyblacklist")

register("chat", (player, message) => {
    message = message.split(" ");
    let playername = getplayername(player)
    if (data.partyBlacklist.includes(playername.toLowerCase())) return;
    let args1 = message[1] ? message[1].toLowerCase() : undefined;
    switch (message[0].toLowerCase()) {
        case "!w":
        case "!warp":
            if(settings.PartyCommands && settings.WarpCommand) {
                sboSetTimeout(() => {
                    ChatLib.command("p warp")
                },100)
            }
            break;

        case "!allinv":
        case "!allinvite":
            if(settings.PartyCommands && settings.AllinviteCommand) {
                sboSetTimeout(() => {
                    ChatLib.command("p setting allinvite")
                },100)
            }
            break;
        case "!ptme":
        case "!transfer":
            if (settings.PartyCommands && settings.TransferCommand && message[1]) {
                if(settings.PartyCommands && settings.TransferCommand) {
                    sboSetTimeout(() => {
                        ChatLib.command("p transfer " + message[1])
                    },100)
                }
            }
            else if(settings.PartyCommands && settings.TransferCommand) {
                sboSetTimeout(() => {
                    ChatLib.command("p transfer " + getplayername(player))
                },100)
            }
            break;
        case "!demote":
        case "!promote":
            if (settings.PartyCommands && settings.MoteCommand && message[1]) {
                sboSetTimeout(() => {
                    ChatLib.command("p " + message[0].slice(1) + " " + message[1])
                },100)
                }
            else if (settings.PartyCommands && settings.MoteCommand) {
                sboSetTimeout(() => {
                    ChatLib.command("p " + message[0].slice(1) + " " + getplayername(player))
                },100)
            }
            break;
        case "!c":
        case "!carrot":
            if(settings.PartyCommands && settings.carrotCommand) {
                sboSetTimeout(() => {
                    ChatLib.command("pc " + carrot[Math.floor(Math.random() * carrot.length)]);
                },100)
            }
            break;
        case "!time":
            if (settings.timeCommand) {
                sboSetTimeout(() => {
                    ChatLib.command("pc " + new Date().toLocaleTimeString());
                }, 200)
            }
            break;
        case "!tps":
            if (settings.tpsCommand) {
                sboSetTimeout(() => {
                    tpsCommand(player)
                }, 200)
            }
            break
        case "!chim":
        case "!chimera":
        case "!chims":
        case "!chimeras":
        case "!book":
        case "!books":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                let totalChimera = dianaTrackerMayor["items"]["Chimera"] + dianaTrackerMayor["items"]["ChimeraLs"];
                sboSetTimeout(() => {
                    ChatLib.command("pc Chimera: " + dianaTrackerMayor["items"]["Chimera"] + " (" + calcPercentOne(dianaTrackerMayor, "Chimera", "Minos Inquisitor") + "%) +" + dianaTrackerMayor["items"]["ChimeraLs"] + " LS")
                }, 200)
            }
            break
        case "!inq":
        case "!inqs":
        case "!inquisitor":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                sboSetTimeout(() => {
                    ChatLib.command("pc Inquisitor: " + dianaTrackerMayor["mobs"]["Minos Inquisitor"] + " (" + calcPercentOne(dianaTrackerMayor, "Minos Inquisitor") + "%)")
                }, 200)
            }
            break
        case "!burrows":
        case "!burrow":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                sboSetTimeout(() => {
                    let burrowsPerHourText = isNaN(getBurrowsPerHour()) ? "" : " (" + getBurrowsPerHour() + "/h)";
                    ChatLib.command("pc Burrows: " + dianaTrackerMayor["items"]["Total Burrows"] + burrowsPerHourText)
                }, 200)
            }
            break
        case "!relic":
        case "!relics":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                sboSetTimeout(() => {
                    ChatLib.command("pc Relics: " + dianaTrackerMayor["items"]["MINOS_RELIC"] + " (" + calcPercentOne(dianaTrackerMayor, "MINOS_RELIC", "Minos Champion") + "%)")
                }, 200)
            }
            break
        case "!stick":
        case "!sticks":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                sboSetTimeout(() => {
                    ChatLib.command("pc Sticks: " + dianaTrackerMayor["items"]["Daedalus Stick"] + " (" + calcPercentOne(dianaTrackerMayor, "Daedalus Stick", "Minotaur") + "%)")
                }, 200)
            }
            break
        case "!feather":
        case "!feathers":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                sboSetTimeout(() => {
                    ChatLib.command("pc Feathers: " + dianaTrackerMayor["items"]["Griffin Feather"])
                }, 200)
            }
            break
        case "!mob":
        case "!mobs":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                sboSetTimeout(() => {
                    let mobsPerHourText = isNaN(getMobsPerHour()) ? "" : " (" + getMobsPerHour() + "/h)";
                    ChatLib.command("pc Mobs: " + dianaTrackerMayor["mobs"]["TotalMobs"] + mobsPerHourText)
                }, 200)
            }
            break
        case "!since":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                if(args1 == undefined) {
                    sboSetTimeout(() => {
                        ChatLib.command("pc Mobs since inq: " + data.mobsSinceInq)
                    }, 200)
                    return;
                }
                switch(args1){
                    case "chimera":
                    case "chim":
                    case "chims":
                    case "chimeras":
                    case "book":
                        sboSetTimeout(() => {
                            ChatLib.command("pc Inqs since chim: " + data.inqsSinceChim)
                        }, 200)
                        break
                    case "stick":
                    case "sticks":
                        sboSetTimeout(() => {
                            ChatLib.command("pc Minos since stick: " + data.minotaursSinceStick)
                        }, 200)
                        break
                    case "relic":
                    case "relics":
                        sboSetTimeout(() => {
                            ChatLib.command("pc Champs since relic: " + data.champsSinceRelic)
                        }, 200)
                        break
                    case "inq":
                    case "inqs":
                    case "inquisitor":
                        sboSetTimeout(() => {
                            ChatLib.command("pc Mobs since inq: " + data.mobsSinceInq)
                        }, 200)
                        break
                    case "lschim":
                    case "chimls":
                    case "lschimera":
                    case "chimerals":
                    case "lsbook":
                    case "bookls":
                    case "lootsharechim":
                        sboSetTimeout(() => {
                            ChatLib.command("pc Inqs since lootshare chim: " + data.inqsSinceLsChim)
                        }, 200)
                        break
                }
                
            }
            break
        case "!playtime":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                sboSetTimeout(() => {
                    ChatLib.command("pc Playtime: " + formatTime(dianaTrackerMayor.items.mayorTime))
                }, 200)
            }
            break
        case "!profit":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                let [profit, offerType, profitHour] = getDianaMayorTotalProfitAndOfferType();
                sboSetTimeout(() => {
                    ChatLib.command("pc Profit: " + profit + " (" + offerType + ") " + profitHour + "/h")
                }, 200)
            }
            break
    }
}).setCriteria("&r&9Party &8> ${player}&f: &r${message}&r")

register("command", (args1, args2, ...args) => {
    if(args1 == undefined) {
        ChatLib.chat("&6[SBO] &4Please provide the magic find value and looting value!")
        ChatLib.chat("&6[SBO] &eUsage: /sbodropchance <magic find> <looting>")
        return;
    }
    if(args2 == undefined) {
        ChatLib.chat("&6[SBO] &4Please provide the looting value!")
        ChatLib.chat("&6[SBO] &eUsage: /sbodropchance <magic find> <looting>")
        return;
    }
    if(parseInt(args1) < 0 || parseInt(args2) < 0) {
        ChatLib.chat("&6[SBO] &4Please provide a positive number!")
        ChatLib.chat("&6[SBO] &eUsage: /sbodropchance <magic find> <looting>")
        return;
    }
    let magicfind = parseInt(args1);
    let looting = parseInt(args2);
    let [chimChance, stickChance, relicChance] = getChance(magicfind, looting);
    let [chimChanceLs, stickChanceLs, relicChanceLs] = getChance(magicfind, looting, true);
    ChatLib.chat("&6[SBO] &eChimera Chance: &b" + formatChanceAsPercentage(chimChance) + formatChanceAsFraction(chimChance) + getMagicFindAndLooting(magicfind, looting))
    ChatLib.chat("&6[SBO] &eStick Chance: &b" + formatChanceAsPercentage(stickChance) + formatChanceAsFraction(stickChance) + getMagicFindAndLooting(magicfind, looting))
    ChatLib.chat("&6[SBO] &eRelic Chance: &b" + formatChanceAsPercentage(relicChance) + formatChanceAsFraction(relicChance) + getMagicFindAndLooting(magicfind, looting))
    ChatLib.chat("&6[SBO] &7[&bLS&7] &eChimera Chance: &b" + formatChanceAsPercentage(chimChanceLs) + formatChanceAsFraction(chimChanceLs) + " &7[MF:" + magicfind + "]")
    ChatLib.chat("&6[SBO] &7[&bLS&7] &eStick Chance: &b" + formatChanceAsPercentage(stickChanceLs) + formatChanceAsFraction(stickChanceLs) + " &7[MF:" + magicfind + "]")
    ChatLib.chat("&6[SBO] &7[&bLS&7] &eRelic Chance: &b" + formatChanceAsPercentage(relicChanceLs) + formatChanceAsFraction(relicChanceLs) + " &7[MF:" + magicfind + "]")
}).setName("sbodropchance").setAliases("sbodc")

register("command", () => {
    ChatLib.chat("&6[SBO] &eDiana party commands:")
    ChatLib.chat("&7> &a!chim")
    ChatLib.chat("&7> &a!stick")
    ChatLib.chat("&7> &a!relic")
    ChatLib.chat("&7> &a!feathers")
    ChatLib.chat("&7> &a!profit")
    ChatLib.chat("&7> &a!playtime")
    ChatLib.chat("&7> &a!mobs")
    ChatLib.chat("&7> &a!burrows")
    ChatLib.chat("&7> &a!since (chim, chimls, relic, stick, inq)")
}).setName("sbopartycommands").setAliases("sbopcom")

function getChance(magicfind, looting, lootshare = false) { 

    const chimBaseChance = 0.01;
    const stickBaseChance = 0.0008;
    const relicBaseChance = 0.0002;

    if(lootshare) {
        let chimChanceLs = (chimBaseChance * (1 + magicfind / 100)) / 5;
        let stickChanceLs = (stickBaseChance * (1 + magicfind / 100)) / 5;
        let relicChanceLs = (relicBaseChance * (1 + magicfind / 100)) / 5;
        return [chimChanceLs, stickChanceLs, relicChanceLs];
    }
    else{
        let chimChance = chimBaseChance * (1 + magicfind / 100) * (1 + looting * 0.15);
        let stickChance = stickBaseChance * (1 + magicfind / 100) * (1 + looting * 0.15);
        let relicChance = relicBaseChance * (1 + magicfind / 100) * (1 + looting * 0.15);
        return [chimChance, stickChance, relicChance];
    }
}

function formatChanceAsPercentage(chance) {
    return (chance * 100).toFixed(2) + "%";
}

function formatChanceAsFraction(chance) {
    return " &7(&b1/" + Math.round(1 / chance) + "&7)";
}

function getMagicFindAndLooting(magicfind, looting) {
    return " &7[MF:" + magicfind + "] [L:" + looting + "]"
}