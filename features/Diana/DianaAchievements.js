import settings from "../../settings";
import { achievementsData, data, pastDianaEvents } from "../../utils/variables";
import { calcBurrowsPerHour, checkDaxeEnchants, getSBID, isOnAlpha, setTimeout } from "../../utils/functions";

rarityColorDict = {
    "Divine": "&b",
    "Mythic": "&d",
    "Legendary": "&6",
    "Epic": "&5",
    "Rare": "&9",
    "Uncommon": "&a",
    "Common": "&f",
    "Impossible": "&4"
}

export class Achievement {
    static list = [];
    static achievementsUnlocked = 0;
    constructor(id, name, description, rarity, previousId=false, timeout=1, hidden=false) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.color = rarityColorDict[rarity];
        if (previousId) this.previousId = Achievement.list.find(achievement => achievement.id == previousId);
        else this.previousId = previousId;
        this.timeout = timeout;
        this.hidden = hidden;
        this.unlocked = false;
        this.loadState();
        Achievement.list.push(this);
    }

    check() {
        if (this.previousId && !this.previousId.isUnlocked()) {
            this.previousId.check();
            setTimeout(() => {
                this.unlock();
            }, 1000 * this.timeout);
        } else this.unlock();
    }

    unlock() {
        // print(this.id);
        if (achievementsData[this.id] == undefined) {
            achievementsData[this.id] = true;
            achievementsData.unlocked.push(this.id);
            achievementsData.save();
            Client.showTitle(`${this.color}${this.name}`, `&aAchievement Unlocked`, 0, 40, 20);
            let hiddenExtra = "";
            if (this.hidden) {
                this.description = this.description.substring(2);
                hiddenExtra = "&7[Secret Achievement] ";
            } 

            if (this.rarity == "Divine" || this.rarity == "Impossible") {
                new Sound({ source: new java.lang.String("achievementUnlockedRare.ogg") }).setVolume(settings.achievementVolume/100).play()
                new TextComponent(`&6[SBO] &aAchievement Unlocked &7>> ${this.color}&k &r ${this.color}${this.name} &k &r`).setHover("show_text", hiddenExtra + "&a" + this.description).chat();
            } else {
                new TextComponent(`&6[SBO] &aAchievement Unlocked &7>> ${this.color}${this.name}`).setHover("show_text", hiddenExtra + "&a" + this.description).chat();
                World.playSound("random.levelup", settings.achievementVolume/100, 1);
            }
            this.unlocked = true;
            Achievement.achievementsUnlocked++;
        }
    }

    lock() {
        if (achievementsData[this.id] != undefined) {
            delete achievementsData[this.id];
            achievementsData.unlocked = achievementsData.unlocked.filter(achievement => achievement != this.id);
            achievementsData.save();
            this.unlocked = false;
            Achievement.achievementsUnlocked--;
            if (this.hidden) {
                this.description = "&k" + this.description;
            }
        }
    }

    static lockById(id) {
        if (achievementsData[id] != undefined) {
            delete achievementsData[id];
            achievementsData.unlocked = achievementsData.unlocked.filter(achievement => achievement != id);
            achievementsData.save();
            Achievement.achievementsUnlocked--;
        }
    }

    loadState() {
        if (achievementsData[this.id] != undefined) {
            this.unlocked = true;
            Achievement.achievementsUnlocked++;
        }
        if (this.hidden && !this.unlocked) {
            this.description = "&k" + this.description;
        }
    }

    isUnlocked() {
        return this.unlocked;
    }

    getName() {
        return this.name;
    }   

    getDisplayName() {
        return `${this.color}${this.name}`;
    }   

    getDescription() {
        return this.description;
    }
    
    getRarity() {
        return this.rarity;
    }
}

// Raritys: Impossible, Divine, Mythic, Legendary, Epic, Rare, Uncommon, Common
// good names: Seek Help, Life Choices?, Time Well Spent?, The Endless Grind, Is This Real Life?, "Magic Find is a lie", "Magic Find is a scam", "Magic Find is a cosmetic", "Magic Find is a myth" 
new Achievement(1, "Back-to-Back Chimera", "Get 2 Chimera in a row", "Mythic");  
new Achievement(2, "b2b2b Chimera", "Get 3 Chimera in a row", "Divine"); 
new Achievement(3, "Back-to-Back Stick", "Get 2 Sticks in a row", "Divine");   
new Achievement(5, "Back-to-Back Relic", "Get 2 Relics in a row", "Impossible");   
new Achievement(6, "Inquisitor Double Trouble", "Get 2 Inquisitors in a row", "Epic");  
new Achievement(7, "b2b2b Inquisitor", "Get 3 Inquisitors in a row", "Divine");

new Achievement(12, "First Chimera", "Get your first Chimera", "Epic");
new Achievement(9, "Chimera V", "Get 16 chimera in one event", "Mythic", 12);
new Achievement(11, "Chimera VI", "Get 32 Chimera in one event", "Divine", 9, 2); 

new Achievement(13, "First lootshare Chimera", "Lootshare your first Chimera", "Legendary");
new Achievement(10, "Tf?", "Get 16 lootshare Chimera in one event", "Divine", 13);

new Achievement(14, "First Stick", "Get your first Stick", "Uncommon");
new Achievement(8, "Can i make a ladder now?", "Get 7 Sticks in one event", "Epic", 14);

new Achievement(15, "1/6250", "Lootshare a Stick (1/6250)", "Impossible", false, 1, true); 

new Achievement(16, "First Relic", "Get your first Relic", "Epic"); 
new Achievement(17, "1/25000", "Lootshare a Relic (1/25000)", "Impossible", false, 1, true); 

new Achievement(18, "Where the grind begins", "Get 5k burrows in one event", "Common"); 
new Achievement(19, "Touch some grass", "Get 10k burrows in one event", "Uncommon", 18); 
new Achievement(20, "Please go outside", "Get 15k burrows in one event", "Epic", 19 , 2); 
new Achievement(21, "Digging your own grave", "Get 20k burrows in one event", "Legendary", 20, 3); 
new Achievement(22, "Are you mentally stable?", "Get 25k burrows in one event", "Mythic", 21, 4); 

new Achievement(23, "So this is Diana?", "1 hour of playtime in one event", "Common"); 
new Achievement(24, "Is this really fun?", "10 hours of playtime in one event", "Uncommon", 23);
new Achievement(25, "No shower for me", "1 day of playtime in one event", "Rare", 24, 2);
new Achievement(26, "Are you okay?", "2 days of playtime in one event", "Epic", 25, 3); 
new Achievement(27, "Sleep is downtime!", "3 days of playtime in one event", "Legendary", 26, 4); 

new Achievement(29, "lf Stick", "200 Minotaur since Stick", "Common"); 
new Achievement(30, "lf Relic", "1000 Champions since Relic", "Uncommon"); 
new Achievement(66, "Where is my Relic?", "3000 champions since Relic", "Mythic", 30, 2);

new Achievement(31, "lf Inquisitor", "250 mobs since Inquisitor", "Common"); 
new Achievement(32, "You have legi Griffin right?", "500 mobs since Inquisitor", "Rare", 31); 
new Achievement(33, "Why do you still play?", "1000 mobs since Inquisitor", "Legendary", 32, 2); 

new Achievement(34, "lf Chimera", "15 Inquisitors since Chimera", "Common"); 
new Achievement(35, "So where is my Chimera?", "30 inquisitors since Chimera", "Epic", 34); 
new Achievement(36, "I am done", "60 Inquisitors since Chimera", "Legendary", 35, 2); 
new Achievement(37, "No more Diana", "100 Inquisitors since Chimera", "Divine", 36, 3);

new Achievement(38, "Real Diana non", "Download SBO", "Divine"); 

new Achievement(39, "Fortune seeker", "Get a Diana drop with 300 Magic Find", "Uncommon");
new Achievement(40, "Blessed by fortune", "Get a Diana drop with 400 Magic Find", "Epic", 39);
new Achievement(41, "Greed knows no bounds", "Get a Diana drop with 500 Magic Find", "Mythic", 40, 2);
new Achievement(42, "The principle of luck", "Get a Diana drop with 600 Magic Find", "Divine", 41, 3); 

new Achievement(44, "Magic Find is overrated", "Drop a Chimera, under 200 Magic Find", "Epic");
new Achievement(43, "I don't need Magic Find", "Drop a Chimera, under 100 Magic Find", "Legendary", 44); 

new Achievement(45, "Inquisitor Slayer", "Max the Inquisitor Bestiary", "Epic");
new Achievement(46, "Minotaur Slayer", "Max the Minotaur Bestiary", "Legendary");
new Achievement(47, "Champion Slayer", "Max the Champion Bestiary", "Epic");
new Achievement(48, "Hunter Slayer", "Max the Hunter Bestiary", "Epic");
new Achievement(49, "Lynx Slayer", "Max the Siamese Lynx Bestiary", "Epic");
new Achievement(50, "Gaia Slayer", "Max the Gaia Bestiary", "Legendary");
new Achievement(51, "Time to get on the leaderboard", "Max all Diana Bestiaries", "Mythic", false, 1, true);

new Achievement(52, "Daedalus Mastery: Chimera V", "Chimera V on Daedalus Axe", "Legendary");
new Achievement(53, "Daedalus Mastery: Looting V", "Looting V on Daedalus Axe", "Legendary");
new Achievement(54, "Daedalus Mastery: Divine Gift III", "Divine Gift III on Daedalus Axe", "Legendary");
new Achievement(55, "Looking Clean", "Get max Divine Gift, Chimera, Looting", "Mythic", false, 1, true);

new Achievement(56, "Now you can't complain", "Obtain Enderman Slayer 9", "Epic", false, 1, true);

new Achievement(57, "Oh look maxed Crest", "Kill 10k Diana Mobs", "Rare");
new Achievement(58, "Keep the grind going", "Kill 25k Diana Mobs", "Epic", 57);
new Achievement(59, "I am not addicted", "Kill 50k Diana Mobs", "Legendary", 58, 2);
new Achievement(60, "100k gang", "Kill 100k Diana Mobs", "Mythic", 59, 3);
new Achievement(61, "The grind never stops", "Kill 150k Diana Mobs", "Divine", 60, 4, true);

new Achievement(62, "Mom look i am on the leaderboard", "Top 100 on the kills leaderboard", "Legendary");
new Achievement(63, "So this is what addiction feels like", "Top 50 on the kills leaderboard", "Mythic", 62);
new Achievement(64, "Diana is my life", "Top 10 on the kills leaderboard", "Divine", 63, 2);

new Achievement(66, "Back-to-Back LS Chimera", "Get 2 Lootshare Chimera in a row", "Divine"); 
new Achievement(67, "b2b2b LS Chimera", "Get 3 Lootshare Chimera in a row", "Impossible", 66); 

new Achievement(68, "Dedicated Digger", "Get 300 burrows/hour (5h playtime)", "Uncommon");
new Achievement(69, "Burrow Enthusiast", "Get 360 burrows/hour (5h playtime)", "Epic", 68);
new Achievement(70, "Shovel Expert", "Get 420 burrows/hour (5h playtime)", "Legendary", 69, 2);
new Achievement(71, "Burrow Maniac", "Get 480 burrows/hour (5h playtime)", "Divine", 70, 3);
new Achievement(72, "Nice macro!", "Get 550 burrows/hour (5h playtime)", "Impossible", 71, 4, true);

// new Achievement(65, "oh baybe it's a triple", "Get 3 drops from a single Inquisitor", "Epic", false, 1, true); 
// new Achivement(28, "Where Chimera?", "Get all other drops from one Inquisitor expect Chimera", "Legendary");

export function unlockAchievement(id) {
    if (achievementsData[id] != undefined) return;
    Achievement.list.forEach(achievement => {
        if (achievement.id == id) {
            achievement.check();
            return;
        }
    })
}

export const achievementItems = [
    "Total Burrows",
    "Daedalus Stick",
    "Chimera",
    "ChimeraLs",
    "MINOS_RELIC"
]
let achievementsToUnlock = [];
let unlocking = false;  
function unlockAchievements(override=false) {
    if (!unlocking) achievementsToUnlock = [...new Set(achievementsToUnlock)];
    if (!override && unlocking) return;
    unlocking = true;
    if (achievementsToUnlock.length > 0) {
        let achievement = achievementsToUnlock.shift();
        if (achievementsData[achievement] == undefined) {
            unlockAchievement(achievement);
            setTimeout(() => {
                unlockAchievements(true);
            }, 2000);
        } else {
            unlockAchievements(true);
        }
    } else {
        unlocking = false;
    }
}

export function trackAchievementsItem(mayorTracker, item, backtrack=false) {
    if (isOnAlpha()) return;
    let totalChimera = 0;
    if ("ChimeraLs" in mayorTracker) {
        totalChimera = mayorTracker["Chimera"] + mayorTracker["ChimeraLs"];
    } else {
        totalChimera = mayorTracker["Chimera"];
    }


    if (mayorTracker["Total Burrows"] >= 5000 && mayorTracker["Total Burrows"] < 10000) {
        achievementsToUnlock.push(18);
    } else if (mayorTracker["Total Burrows"] >= 10000 && mayorTracker["Total Burrows"] < 15000) {
        achievementsToUnlock.push(19);
    } else if (mayorTracker["Total Burrows"] >= 15000 && mayorTracker["Total Burrows"] < 20000) {
        achievementsToUnlock.push(20);
    } else if (mayorTracker["Total Burrows"] >= 20000 && mayorTracker["Total Burrows"] < 25000) {
        achievementsToUnlock.push(21);
    } else if (mayorTracker["Total Burrows"] >= 25000) {
        achievementsToUnlock.push(22);
    }

    if (mayorTracker["mayorTime"] >= 3600000 && mayorTracker["mayorTime"] < 3600000 * 10) {
        achievementsToUnlock.push(23);
    } else if (mayorTracker["mayorTime"] >= 3600000 * 10 && mayorTracker["mayorTime"] < 86400000) {
        achievementsToUnlock.push(24);
    } else if (mayorTracker["mayorTime"] >= 86400000 && mayorTracker["mayorTime"] < 86400000 * 2) {
        achievementsToUnlock.push(25);
    } else if (mayorTracker["mayorTime"] >= 86400000 * 2 && mayorTracker["mayorTime"] < 86400000 * 3) {
        achievementsToUnlock.push(26);
    } else if (mayorTracker["mayorTime"] >= 86400000 * 3) {
        achievementsToUnlock.push(27);
    }
    
    if (mayorTracker["MINOS_RELIC"] >= 1) {
        achievementsToUnlock.push(16);
    }

    if (mayorTracker["Daedalus Stick"] >= 1 && mayorTracker["Daedalus Stick"] < 7) {
        achievementsToUnlock.push(14);
    } else if (mayorTracker["Daedalus Stick"] >= 7) {
        achievementsToUnlock.push(8);
    }

    if (totalChimera >= 1 && totalChimera < 16) {
        achievementsToUnlock.push(12);
    } else if (totalChimera >= 16 && totalChimera < 32) {
        achievementsToUnlock.push(9);
    } else if (totalChimera >= 32) {
        achievementsToUnlock.push(11);
    }

    if (mayorTracker["ChimeraLs"] >= 1 && mayorTracker["ChimeraLs"] < 16) {
        achievementsToUnlock.push(13);
    } else if (mayorTracker["ChimeraLs"] >= 16) {
        achievementsToUnlock.push(10);
    }

    if (item == "Total Burrows") {
        const burrowsPerHour = calcBurrowsPerHour(mayorTracker["Total Burrows"], mayorTracker["mayorTime"]);
        if (mayorTracker["mayorTime"] >= 18000000 && burrowsPerHour >= 300 && burrowsPerHour < 360) {
            achievementsToUnlock.push(68);
        } else if (mayorTracker["mayorTime"] >= 18000000 && burrowsPerHour >= 360 && burrowsPerHour < 420) {
            achievementsToUnlock.push(69);
        } else if (mayorTracker["mayorTime"] >= 18000000 && burrowsPerHour >= 420 && burrowsPerHour < 480) {
            achievementsToUnlock.push(70);
        } else if (mayorTracker["mayorTime"] >= 18000000 && burrowsPerHour >= 480 && burrowsPerHour < 550) {
            achievementsToUnlock.push(71);
        } else if (mayorTracker["mayorTime"] >= 18000000 && burrowsPerHour >= 550) {
            achievementsToUnlock.push(72);
        }
    }

    if (!backtrack) {
        unlockAchievements();
    }
}



export function trackSinceMob() {
    if (isOnAlpha()) return;
    if (data["mobsSinceInq"] >= 250 && data["mobsSinceInq"] < 500) {
        achievementsToUnlock.push(31);
    } else if (data["mobsSinceInq"] >= 500 && data["mobsSinceInq"] < 1000) {
        achievementsToUnlock.push(32);
    } else if (data["mobsSinceInq"] >= 1000) {
        achievementsToUnlock.push(33);
    }

    if (data["inqsSinceChim"] >= 15 && data["inqsSinceChim"] < 30) {
        achievementsToUnlock.push(34);
    } else if (data["inqsSinceChim"] >= 30 && data["inqsSinceChim"] < 60) {
        achievementsToUnlock.push(35);
    } else if (data["inqsSinceChim"] >= 60 && data["inqsSinceChim"] < 100) {
        achievementsToUnlock.push(36);
    } else if (data["inqsSinceChim"] >= 100) {
        achievementsToUnlock.push(37);
    }

    if (data["minotaursSinceStick"] >= 200) {
        achievementsToUnlock.push(29);
    }

    if (data["champsSinceRelic"] >= 1000 && data["champsSinceRelic"] < 3000) {
        achievementsToUnlock.push(30);
    } else if (data["champsSinceRelic"] >= 3000) {
        achievementsToUnlock.push(66);
    }
    unlockAchievements();
}

export function trackMagicFind(magicFind, chimera=false) {
    if (isOnAlpha()) return;
    if (magicFind >= 300 && magicFind < 400) {
        achievementsToUnlock.push(39);
    } else if (magicFind >= 400 && magicFind < 500) {
        achievementsToUnlock.push(40);
    } else if (magicFind >= 500 && magicFind < 600) {
        achievementsToUnlock.push(41);
    } else if (magicFind >= 600) {
        achievementsToUnlock.push(42);
    }

    if (magicFind < 100 && chimera) {
        achievementsToUnlock.push(43);
    } else if (magicFind < 200 && chimera) {
        achievementsToUnlock.push(44);
    }
    unlockAchievements();
}

function trackBeKills(gaiaKills, champKills, hunterKills, inqKills, minoKills, catKills) {
    if (isOnAlpha()) return;
    if (gaiaKills >= 3000) {
        achievementsToUnlock.push(50);
    } else if (gaiaKills != 0) Achievement.lockById(50);
    if (inqKills >= 500) {
        achievementsToUnlock.push(45);
    } else if (inqKills != 0) Achievement.lockById(45);
    if (minoKills >= 3000) {
        achievementsToUnlock.push(46);
    } else if (minoKills != 0) Achievement.lockById(46);
    if (champKills >= 1000) {
        achievementsToUnlock.push(47);
    } else if (champKills != 0) Achievement.lockById(47);
    if (hunterKills >= 1000) {
        achievementsToUnlock.push(48);
    } else if (hunterKills != 0) Achievement.lockById(48);
    if (catKills >= 3000) {
        achievementsToUnlock.push(49);
    } else if (catKills != 0) Achievement.lockById(49);
    if (gaiaKills >= 3000 && champKills >= 1000 && hunterKills >= 1000 && inqKills >= 500 && minoKills >= 3000 && catKills >= 3000) {
        achievementsToUnlock.push(51);
    } else if (gaiaKills != 0 && champKills != 0 && hunterKills != 0 && inqKills != 0 && minoKills != 0 && catKills != 0) Achievement.lockById(51);
    unlockAchievements();
}

function dAxeAchivementCheck() {
    if (isOnAlpha()) return;
    let dAxeEnchants = checkDaxeEnchants();
    if (!dAxeEnchants[0] && !dAxeEnchants[1] && !dAxeEnchants[2]) return;
    if (dAxeEnchants[0]) {
        achievementsToUnlock.push(52);
    }
    if (dAxeEnchants[1]) {
        achievementsToUnlock.push(53);
    }
    if (dAxeEnchants[2]) {
        achievementsToUnlock.push(54);
    }
    if (dAxeEnchants[0] && dAxeEnchants[1] && dAxeEnchants[2]) {
        achievementsToUnlock.push(55);
    }
    unlockAchievements();
}

function getKillsFromLore(item) {
    if (isOnAlpha()) return;
    if (item == null) return 0;
    let lore = item.getLore();
    let kills = 0;
    lore.forEach(line => {
        if (line.removeFormatting().includes("Kills: ")) {
            kills = parseInt(line.split("Kills: ")[1].removeFormatting().replace(",", ""));

        }
    });
    return kills;
}

function getSlayerLvlFromLore(item) {
    if (isOnAlpha()) return;
    if (item == null) return 0;
    let lore = item.getLore();
    let slayerLvl = 0;
    lore.forEach(line => {
        if (line.removeFormatting().includes("LVL ")) {
            slayerLvl = parseInt(line.split("LVL ")[1].removeFormatting());
        }
    });
    return slayerLvl;
}

register("guiOpened", (event) => {
    setTimeout(() => {
        const container = Player.getContainer();
        if (container == null) return;
        if (container == undefined) return;
        if (container.getName().includes("Mythological Creatur")) {
            let gaiaKills = getKillsFromLore(container.getStackInSlot(10)); 
            let champKills = getKillsFromLore(container.getStackInSlot(11));
            let hunterKills = getKillsFromLore(container.getStackInSlot(12));
            let inqKills = getKillsFromLore(container.getStackInSlot(13));
            let minoKills = getKillsFromLore(container.getStackInSlot(14));
            let catKills = getKillsFromLore(container.getStackInSlot(15));
            trackBeKills(gaiaKills, champKills, hunterKills, inqKills, minoKills, catKills);
        }
        if (container.getName() == "Slayer") {
            if (getSlayerLvlFromLore(container.getStackInSlot(13)) >= 9) {
                achievementsToUnlock.push(56);
                unlockAchievements();
            }
        }
        if (container.getName().includes("Accessory Bag")) {
            let items = container.getItems();
            items.forEach((item, index) => {
                if (item == null) return;
                if (item == undefined) return;
                if (getSBID(item) == null) return;
                if (getSBID(item).includes("BEASTMASTER_CREST")) {
                    let kills = getKillsFromLore(item);
                    if (kills >= 10000 && kills < 25000) {
                        achievementsToUnlock.push(57);
                    } else if (kills >= 25000 && kills < 50000) {
                        achievementsToUnlock.push(58);
                    } else if (kills >= 50000 && kills < 100000) {
                        achievementsToUnlock.push(59);
                    } else if (kills >= 100000) {
                        achievementsToUnlock.push(60);
                    }
                    unlockAchievements();
                }
            });
        }
    }, 400);
})

register("chat", (event) => {
    achievementsToUnlock.push(56);
    unlockAchievements();
}).setCriteria("LVL UP! ➜ Enderman Slayer LVL 9!");

export function backTrackAchievements() {
    ChatLib.chat("&6[SBO] &eBacktracking Achievements...");
    for (let event in pastDianaEvents.events) {
        for (let key in pastDianaEvents.events[event].items) {
            trackAchievementsItem(pastDianaEvents.events[event].items, key, true);
        }
    }
    trackSinceMob();
}

export function trackWithCheckPlayer(playerinfo) {
    if (playerinfo.eman9) {
        achievementsToUnlock.push(56);
    }
    if (playerinfo.mythosKills >= 10000 && playerinfo.mythosKills < 25000) {
        achievementsToUnlock.push(57);
    } else if (playerinfo.mythosKills >= 25000 && playerinfo.mythosKills < 50000) {
        achievementsToUnlock.push(58);
    } else if (playerinfo.mythosKills >= 50000 && playerinfo.mythosKills < 100000) {
        achievementsToUnlock.push(59);
    } else if (playerinfo.mythosKills >= 100000 && playerinfo.mythosKills < 150000) {
        achievementsToUnlock.push(60);
    } else if (playerinfo.mythosKills >= 150000) {
        achievementsToUnlock.push(61);
    }
    
    if (playerinfo.killLeaderboard <= 100) {
        achievementsToUnlock.push(62);
    } 
    if (playerinfo.killLeaderboard <= 50) {
        achievementsToUnlock.push(63);
    } 
    if (playerinfo.killLeaderboard <= 10) {
        achievementsToUnlock.push(64);
    }
    unlockAchievements();
}

register("command", () => {
    backTrackAchievements();
}).setName("sbobacktrackachievements");

let confirmState = false;
register("command", (args1, ...args) => {
    if (args1 != "CONFIRM") {
        ChatLib.chat("&6[SBO] &eYou are about to reset all your achievements. Type &c/sbolockachievements CONFIRM &eto confirm");
        confirmState = true;
        return;
    }
    if (confirmState) {
        Achievement.list.forEach(achievement => {
            if (achievement.isUnlocked() && achievement.id != 38) achievement.lock();
        })
        confirmState = false;
        ChatLib.chat("&6[SBO] &eAchievements locked");
        checkDaxeAchievements.register();
    }
}).setName("sbolockachievements");

let checkDaxeAchievements = register("step", () => {
    dAxeAchivementCheck();
    if(achievementsData[55] != undefined)
        checkDaxeAchievements.unregister();
}).setFps(1);
checkDaxeAchievements.register();

const achievementCheck = register("step", () => {
    if (!data.achievementFix1) {
        data.achievementFix1 = true;
        data.save();
        let buggedAchievements = [2, 7, 39, 40, 41, 42, 43, 44];
        let lockedAchievement = false;
        buggedAchievements.forEach(achievement => {
            if (achievementsData[achievement] != undefined) {
                Achievement.lockById(achievement);
                lockedAchievement = true;
            }
        });
        if (lockedAchievement) {
            ChatLib.chat("&6[SBO] &cWarning: &eSome achievements have been reset due to a bug.");
        }
    }
    achievementCheck.unregister();
}).setFps(1);