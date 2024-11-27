import settings from "../../settings";
import { getplayername, formatTime, getDianaMayorTotalProfitAndOfferType, calcPercentOne, getBurrowsPerHour, getMobsPerHour, setTimeout } from "../../utils/functions";
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
        ChatLib.chat("&6[SBO] &aPlease use /sbopartyblacklist <command>")
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
                setTimeout(() => {
                    ChatLib.command("p warp")
                },100)
            }
            break;

        case "!allinv":
        case "!allinvite":
            if(settings.PartyCommands && settings.AllinviteCommand) {
                setTimeout(() => {
                    ChatLib.command("p setting allinvite")
                },100)
            }
            break;
        case "!ptme":
        case "!transfer":
            if (settings.PartyCommands && settings.TransferCommand && message[1]) {
                if(settings.PartyCommands && settings.TransferCommand) {
                    setTimeout(() => {
                        ChatLib.command("p transfer " + message[1])
                    },100)
                }
            }
            else if(settings.PartyCommands && settings.TransferCommand) {
                setTimeout(() => {
                    ChatLib.command("p transfer " + getplayername(player))
                },100)
            }
            break;
        case "!demote":
        case "!promote":
            if (settings.PartyCommands && settings.MoteCommand && message[1]) {
                setTimeout(() => {
                    ChatLib.command("p " + message[0].slice(1) + " " + message[1])
                },100)
                }
            else if (settings.PartyCommands && settings.MoteCommand) {
                setTimeout(() => {
                    ChatLib.command("p " + message[0].slice(1) + " " + getplayername(player))
                },100)
            }
            break;
        case "!c":
        case "!carrot":
            if(settings.PartyCommands && settings.carrotCommand) {
                setTimeout(() => {
                    ChatLib.command("pc " + carrot[Math.floor(Math.random() * carrot.length)]);
                },100)
            }
            break;
        case "!time":
            if (settings.timeCommand) {
                setTimeout(() => {
                    ChatLib.command("pc " + new Date().toLocaleTimeString());
                }, 200)
            }
            break;
        case "!tps":
            if (settings.tpsCommand) {
                setTimeout(() => {
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
                setTimeout(() => {
                    ChatLib.command("pc Chimera: " + dianaTrackerMayor["items"]["Chimera"] + " (" + calcPercentOne(dianaTrackerMayor, "Chimera", "Minos Inquisitor") + "%) +" + dianaTrackerMayor["items"]["ChimeraLs"] + " LS")
                }, 200)
            }
            break
        case "!inq":
        case "!inqs":
        case "!inquisitor":
        case "!inquis":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                setTimeout(() => {
                    ChatLib.command("pc Inquisitor: " + dianaTrackerMayor["mobs"]["Minos Inquisitor"] + " (" + calcPercentOne(dianaTrackerMayor, "Minos Inquisitor") + "%)")
                }, 200)
            }
            break
        case "!burrows":
        case "!burrow":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                setTimeout(() => {
                    let burrowsPerHourText = isNaN(getBurrowsPerHour()) ? "" : " (" + getBurrowsPerHour() + "/h)";
                    ChatLib.command("pc Burrows: " + dianaTrackerMayor["items"]["Total Burrows"] + burrowsPerHourText)
                }, 200)
            }
            break
        case "!relic":
        case "!relics":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                setTimeout(() => {
                    ChatLib.command("pc Relics: " + dianaTrackerMayor["items"]["MINOS_RELIC"] + " (" + calcPercentOne(dianaTrackerMayor, "MINOS_RELIC", "Minos Champion") + "%)")
                }, 200)
            }
            break
        case "!chimls":
        case "!chimerals":
        case "!bookls":
        case "!lschim":
        case "!lsbook":
        case "!lootsharechim":
        case "!lschimera":
            if (!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                percent = parseFloat((dianaTrackerMayor["items"]["ChimeraLs"] / dianaTrackerMayor["mobs"]["Minos Inquisitor Ls"] * 100).toFixed(2));
                setTimeout(() => {
                    ChatLib.command("pc Chimera LS: " + dianaTrackerMayor["items"]["ChimeraLs"] + " (" + percent + "%)")
                }, 200)
            }
            break
        case "!stick":
        case "!sticks":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                setTimeout(() => {
                    ChatLib.command("pc Sticks: " + dianaTrackerMayor["items"]["Daedalus Stick"] + " (" + calcPercentOne(dianaTrackerMayor, "Daedalus Stick", "Minotaur") + "%)")
                }, 200)
            }
            break
        case "!feather":
        case "!feathers":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                setTimeout(() => {
                    ChatLib.command("pc Feathers: " + dianaTrackerMayor["items"]["Griffin Feather"])
                }, 200)
            }
            break
        case "!mob":
        case "!mobs":
            if (!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                setTimeout(() => {
                    let mobsPerHourText = isNaN(getMobsPerHour()) ? "" : " (" + getMobsPerHour() + "/h)";
                    ChatLib.command("pc Mobs: " + dianaTrackerMayor["mobs"]["TotalMobs"] + mobsPerHourText)
                }, 200)
            }
            break
        case "!mf":
        case "!magicfind":
            if (!settings.dianaPartyCommands) break;
            if (!settings.dianaAvgMagicFind) break
            setTimeout(() => {
                ChatLib.command(`pc Chims (${data.avgChimMagicFind}% ✯) Sticks (${data.avgStickMagicFind}% ✯)`)
            }, 200)
            break
        case "!since":
            if (!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                if(args1 == undefined) {
                    setTimeout(() => {
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
                        setTimeout(() => {
                            ChatLib.command("pc Inqs since chim: " + data.inqsSinceChim)
                        }, 200)
                        break
                    case "stick":
                    case "sticks":
                        setTimeout(() => {
                            ChatLib.command("pc Minos since stick: " + data.minotaursSinceStick)
                        }, 200)
                        break
                    case "relic":
                    case "relics":
                        setTimeout(() => {
                            ChatLib.command("pc Champs since relic: " + data.champsSinceRelic)
                        }, 200)
                        break
                    case "inq":
                    case "inqs":
                    case "inquisitor":
                    case "inquis":
                        setTimeout(() => {
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
                        setTimeout(() => {
                            ChatLib.command("pc Inqs since lootshare chim: " + data.inqsSinceLsChim)
                        }, 200)
                        break
                }
                
            }
            break
        case "!playtime":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                setTimeout(() => {
                    ChatLib.command("pc Playtime: " + formatTime(dianaTrackerMayor.items.mayorTime))
                }, 200)
            }
            break
        case "!profit":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                let [profit, offerType, profitHour] = getDianaMayorTotalProfitAndOfferType();
                setTimeout(() => {
                    ChatLib.command("pc Profit: " + profit + " (" + offerType + ") " + profitHour + "/h")
                }, 200)
            }
            break
    }
}).setCriteria("&r&9Party &8> ${player}&f: &r${message}&r")

register("command", (magicFindArg, lootingArg) => {
    if (!magicFindArg || !lootingArg) {
        ChatLib.chat(`&6[SBO] &4Please provide both the magic find and looting values!`);
        ChatLib.chat(`&6[SBO] &eUsage: /sbodropchance <magic find> <looting>`);
        return;
    }
    const magicFind = parseInt(magicFindArg);
    const looting = parseInt(lootingArg);
    if (magicFind < 0 || looting < 0) {
        ChatLib.chat(`&6[SBO] &4Please provide positive numbers!`);
        ChatLib.chat(`&6[SBO] &eUsage: /sbodropchance <magic find> <looting>`);
        return;
    }
    const formatChances = (chance, label) => `${formatChanceAsPercentage(chance)}${formatChanceAsFraction(chance)} ${label}`;
    const [chimChance, stickChance, relicChance] = getChance(magicFind, looting);
    const [chimChanceLs, stickChanceLs, relicChanceLs] = getChance(magicFind, looting, true);
    const chances = [
        { name: "Chimera", chance: chimChance, label: getMagicFindAndLooting(magicFind, looting) },
        { name: "Stick", chance: stickChance, label: getMagicFindAndLooting(magicFind, looting) },
        { name: "Relic", chance: relicChance, label: getMagicFindAndLooting(magicFind, looting) },
        { name: "Chimera", chance: chimChanceLs, label: `&7[MF:${magicFind}]`, ls: true },
        { name: "Stick", chance: stickChanceLs, label: `&7[MF:${magicFind}]`, ls: true },
        { name: "Relic", chance: relicChanceLs, label: `&7[MF:${magicFind}]`, ls: true },
    ];
    chances.forEach(({ name, chance, label, ls }) => {
        const lsPrefix = ls ? "&7[&bLS&7] " : "";
        ChatLib.chat(`&6[SBO] ${lsPrefix}&e${name} Chance: &b${formatChances(chance, label)}`);
    });
}).setName("sbodropchance").setAliases("sbodc");

function getChance(magicFind, looting, lootshare = false) {
    const baseChances = { chim: 0.01, stick: 0.0008, relic: 0.0002 };
    const multiplier = 1 + magicFind / 100;

    if (lootshare) {
        const factor = multiplier / 5;
        return Object.values(baseChances).map(base => base * factor);
    }

    const lootingMultiplier = 1 + looting * 0.15;
    return Object.values(baseChances).map(base => base * multiplier * lootingMultiplier);
}

register("command", () => {
    ChatLib.chat("&6[SBO] &eDiana party commands:")
    ChatLib.chat("&7> &a!chim")
    ChatLib.chat("&7> &a!chimls")
    ChatLib.chat("&7> &a!stick")
    ChatLib.chat("&7> &a!relic")
    ChatLib.chat("&7> &a!feathers")
    ChatLib.chat("&7> &a!profit")
    ChatLib.chat("&7> &a!playtime")
    ChatLib.chat("&7> &a!mobs")
    ChatLib.chat("&7> &a!burrows")
    ChatLib.chat("&7> &a!since (chim, chimls, relic, stick, inq)")
}).setName("sbopartycommands").setAliases("sbopcom")

function formatChanceAsPercentage(chance) {
    return (chance * 100).toFixed(2) + "%";
}

function formatChanceAsFraction(chance) {
    return " &7(&b1/" + Math.round(1 / chance) + "&7)";
}

function getMagicFindAndLooting(magicfind, looting) {
    return " &7[MF:" + magicfind + "] [L:" + looting + "]"
}