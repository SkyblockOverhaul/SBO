import settings from "../../settings";
import { getplayername, formatTime, getDianaMayorTotalProfitAndOfferType, calcPercentOne, getBurrowsPerHour, getMobsPerHour, setTimeout, formatNumber, getTotalValue, formatNumberCommas } from "../../utils/functions";
import { tpsCommand } from "../../utils/tps";
import { data, dianaTrackerMayor, dianaTrackerTotal } from "../../utils/variables";

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
    "A Carrot a day keeps your luck away",
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
    message=message.removeFormatting()
    message = message.split(" ")
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
                    setTimeout(() => {
                        ChatLib.command("p transfer " + message[1])
                    },100)
            }
            else if(settings.PartyCommands && settings.TransferCommand && Player.getName() != playername) {
                setTimeout(() => {
                    ChatLib.command("p transfer " + playername)
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
            else if (settings.PartyCommands && settings.MoteCommand && Player.getName() != playername) {
                setTimeout(() => {
                    ChatLib.command("p " + message[0].slice(1) + " " + playername)
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
            if (settings.dianaTracker && settings.dianaPartyCommands) {
                let totalChimera = dianaTrackerMayor["items"]["Chimera"] + dianaTrackerMayor["items"]["ChimeraLs"];
                setTimeout(() => {
                    ChatLib.command("pc Chimera: " + dianaTrackerMayor["items"]["Chimera"] + " (" + calcPercentOne(dianaTrackerMayor, "Chimera", "Minos Inquisitor") + "%) +" + dianaTrackerMayor["items"]["ChimeraLs"] + " LS")
                }, 200)
            }
            break
        case "!inqls":
        case "!inqsls":
        case "!inquisitorls":
        case "!inquisls":
        case "!lsinq":
        case "!lsinqs":
        case "!lsinquisitor":
        case "!lsinquis":
            if (settings.dianaTracker) {
                setTimeout(() => {
                    ChatLib.command("pc Inquisitor LS: " + dianaTrackerMayor["mobs"]["Minos Inquisitor Ls"])
                }, 200)
            }
            break
        case "!inq":
        case "!inqs":
        case "!inquisitor":
        case "!inquis":
            if (settings.dianaTracker && settings.dianaPartyCommands) {
                setTimeout(() => {
                    ChatLib.command("pc Inquisitor: " + dianaTrackerMayor["mobs"]["Minos Inquisitor"] + " (" + calcPercentOne(dianaTrackerMayor, "Minos Inquisitor") + "%)")
                }, 200)
            }
            break
        case "!burrows":
        case "!burrow":
            if (settings.dianaTracker && settings.dianaPartyCommands) {
                setTimeout(() => {
                    let burrowsPerHourText = isNaN(getBurrowsPerHour()) ? "" : " (" + getBurrowsPerHour() + "/h)";
                    ChatLib.command("pc Burrows: " + dianaTrackerMayor["items"]["Total Burrows"] + burrowsPerHourText)
                }, 200)
            }
            break
        case "!relic":
        case "!relics":
            if (settings.dianaTracker && settings.dianaPartyCommands) {
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
            if (settings.dianaTracker && settings.dianaPartyCommands) {
                percent = parseFloat((dianaTrackerMayor["items"]["ChimeraLs"] / dianaTrackerMayor["mobs"]["Minos Inquisitor Ls"] * 100).toFixed(2));
                setTimeout(() => {
                    ChatLib.command("pc Chimera LS: " + dianaTrackerMayor["items"]["ChimeraLs"] + " (" + percent + "%)")
                }, 200)
            }
            break
        case "!stick":
        case "!sticks":
            if (settings.dianaTracker && settings.dianaPartyCommands) {
                setTimeout(() => {
                    ChatLib.command("pc Sticks: " + dianaTrackerMayor["items"]["Daedalus Stick"] + " (" + calcPercentOne(dianaTrackerMayor, "Daedalus Stick", "Minotaur") + "%)")
                }, 200)
            }
            break
        case "!feather":
        case "!feathers":
            if (settings.dianaTracker && settings.dianaPartyCommands) {
                setTimeout(() => {
                    ChatLib.command("pc Feathers: " + dianaTrackerMayor["items"]["Griffin Feather"])
                }, 200)
            }
            break
        case "!coins":
        case "!coin":
            if(!settings.dianaPartyCommands) break;
            if (settings.dianaTracker) {
                setTimeout(() => {
                    ChatLib.command("pc Coins: " + formatNumber(dianaTrackerMayor["items"]["coins"]))
                }, 200)
            }
            break
        case "!mob":
        case "!mobs":
            if (settings.dianaTracker && settings.dianaPartyCommands) {
                setTimeout(() => {
                    let mobsPerHourText = isNaN(getMobsPerHour()) ? "" : " (" + getMobsPerHour() + "/h)";
                    ChatLib.command("pc Mobs: " + dianaTrackerMayor["mobs"]["TotalMobs"] + mobsPerHourText)
                }, 200)
            }
            break
        case "!mf":
        case "!magicfind":
            if (!settings.dianaPartyCommands) break
            setTimeout(() => {
                ChatLib.command(`pc Chims (${data.highestChimMagicFind}% ✯) Sticks (${data.highestStickMagicFind}% ✯)`)
            }, 200)
            break
        case "!since":
            if (settings.dianaTracker && settings.dianaPartyCommands) {
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
                    case "books":
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
                    case "inquisitors":
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
            if (settings.dianaTracker && settings.dianaPartyCommands) {
                setTimeout(() => {
                    ChatLib.command("pc Playtime: " + formatTime(dianaTrackerMayor.items.mayorTime))
                }, 200)
            }
            break
        case "!profits":
        case "!profit":
            if (settings.dianaTracker && settings.dianaPartyCommands) {
                let [profit, offerType, profitHour] = getDianaMayorTotalProfitAndOfferType();
                setTimeout(() => {
                    ChatLib.command("pc Profit: " + profit + " (" + offerType + ") " + profitHour + "/h")
                }, 200)
            }
            break
        case "!stat":
        case "!stats":
            if (settings.dianaTracker && settings.dianaPartyCommands) {
                if (args1 != undefined) {
                    let playerName = Player.getName().toLowerCase().trim();
                    args1 = args1.toLowerCase().trim();
                    switch (args1) {
                        case playerName:
                            sendPlayerStats();
                            break;
                    }
                }
            }
        case "!totalstat":
        case "!totalstats":
            if (settings.dianaTracker && settings.dianaPartyCommands) {
                if (args1 != undefined) {
                    let playerName = Player.getName().toLowerCase().trim();
                    args1 = args1.toLowerCase().trim();
                    switch (args1) {
                        case playerName:
                            sendPlayerStats(true);
                            break;
                    }
                }
            }
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
    ChatLib.chat("&7> &a!stats <playername>")
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

function getPlayerStats(total = false) {
    const tracker = total ? dianaTrackerTotal : dianaTrackerMayor;
    const playtimeType = total ? "totalTime" : "mayorTime";
    const playtime = tracker.items[playtimeType];
    const playTimeHrs = (playtime / 3600000).toFixed(2);
    const burrowsPerHour = tracker.items["Total Burrows"] / playTimeHrs;
    const mobsPerHour = tracker.mobs["TotalMobs"] / playTimeHrs;
    const totalValue = getTotalValue(tracker);
    const profit = [formatNumber(totalValue), ["Instasell", "Sell Offer"][settings.bazaarSettingDiana], formatNumber(totalValue / playTimeHrs)];
    const stats = {
        playtime: formatTime(playtime),
        profit: profit,
        burrows: formatNumber(tracker["items"]["Total Burrows"]),
        burrowsPerHour: parseFloat(burrowsPerHour.toFixed(2)),
        totalMobs: formatNumber(tracker["mobs"]["TotalMobs"]),
        mobsPerHour: parseFloat(mobsPerHour.toFixed(2)),
        inquisitors: tracker["mobs"]["Minos Inquisitor"],
        inqPercentage: calcPercentOne(tracker, "Minos Inquisitor") + "%",
        lsInqs: formatNumberCommas(tracker["mobs"]["Minos Inquisitor Ls"]),
        chimeraDrops: tracker["items"]["Chimera"],
        chimeraDropRate: calcPercentOne(tracker, "Chimera", "Minos Inquisitor") + "%",
        chimeraLSDrops: tracker["items"]["ChimeraLs"],
        chimeraLSDropRate: parseFloat((tracker["items"]["ChimeraLs"] / tracker["mobs"]["Minos Inquisitor Ls"] * 100).toFixed(2)) + "%",
        sticksDropped: tracker["items"]["Daedalus Stick"],
        stickDropRate: calcPercentOne(tracker, "Daedalus Stick", "Minotaur") + "%",
        relicsDropped: tracker["items"]["MINOS_RELIC"],
        relicDropRate: calcPercentOne(tracker, "MINOS_RELIC", "Minos Champion") + "%"
    };
    return stats;
}

function sendPlayerStats(total = false) {
    let stats = getPlayerStats(total);
    const statsArray = [
        `Playtime: ${stats.playtime}`,
        `Profit: ${stats.profit[0]} - ${stats.profit[2]}/h`,
        `Burrows: ${stats.burrows} (${stats.burrowsPerHour}/h)`,
        `Mobs: ${stats.totalMobs} (${stats.mobsPerHour}/h)`,
        `Inquisitors: ${stats.inquisitors} (${stats.inqPercentage})`,
        `LS Inqs: ${stats.lsInqs}`,
        `Chimeras: ${stats.chimeraDrops} (${stats.chimeraDropRate}) - LS: ${stats.chimeraLSDrops} (${stats.chimeraLSDropRate})`,
        `Sticks: ${stats.sticksDropped} (${stats.stickDropRate})`,
        `Relics: ${stats.relicsDropped} (${stats.relicDropRate})`
    ];
    let statsMessage = statsArray.join(" - ");
    setTimeout(() => {
        ChatLib.command("pc " + statsMessage)
    }, 200)
}

register("chat", (rank, player, playtime, profit, profitHr, burrow, burrowPerHour, mobs, mobsPerHour, inquis, 
    inqPercentage, lsInq, chimeraDrops, chimeraDropRate, chimeraLSDrops, chimeraLSDropRate, sticksDropped, 
    sticksDropRate, relicsDropped, relicsDropRate, event) => {
    let statsMessage = [
        `&9Party &8> &b${rank} ${player}&f:`,
        `&ePlaytime: &b${playtime}`,
        `&aBurrows: &b${burrow} &7(${burrowPerHour}/h)`,
        `&aMobs: &b${mobs} &7(${mobsPerHour}/h)`,
        `&dInquisitors: &b${inquis} &7(${inqPercentage}) &6LS: &b${lsInq}`,
        `&dChimeras: &b${chimeraDrops} &7(${chimeraDropRate}) &6LS: &b${chimeraLSDrops} &7(${chimeraLSDropRate})`,
        `&6Sticks: &b${sticksDropped} &7${sticksDropRate}`,
        `&5Relics: &b${relicsDropped} &7${relicsDropRate}`,
        `&6Profit: &b${profit} &7(${profitHr})`
    ].join("\n");
    ChatLib.chat(statsMessage)
    cancel(event);
}).setCriteria("&r&9Party &8> ${rank} ${player}&f: &rPlaytime: ${playtime} - Profit: ${profit} - ${profitHr} - Burrows: ${burrow} (${burrowPerHour}/h) - Mobs: ${mobs} (${mobsPerHour}/h) - Inquisitors: ${inquis} (${inqPercentage}) - LS Inqs: ${lsInq} - Chimeras: ${chimeraDrops} (${chimeraDropRate}) - LS: ${chimeraLSDrops} (${chimeraLSDropRate}) - Sticks: ${sticksDropped} ${sticksDropRate} - Relics: ${relicsDropped} ${relicsDropRate}&r");
