import settings from "../../settings";
import { achievementsData, data, pastDianaEvents } from "../../utils/variables";

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
    constructor(id, name, description, rarity, requirement=false, timeout=1, hidden=false) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.color = rarityColorDict[rarity];
        if (requirement) this.requirement = Achievement.list.find(achievement => achievement.id == requirement);
        else this.requirement = requirement;
        this.timeout = timeout;
        this.hidden = hidden;
        this.unlocked = false;
        this.loadState();
        Achievement.list.push(this);
    }

    check() {
        if (this.requirement && !this.requirement.isUnlocked()) {
            this.requirement.check();
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
            // this.lock();
        }
    }

    lock() {
        if (achievementsData[this.id] != undefined) {
            delete achievementsData[this.id];
            achievementsData.unlocked = achievementsData.unlocked.filter(achievement => achievement != this.id);
            achievementsData.save();
            this.unlocked = false;
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
// todo: add mc sound (maybe only for over epic rarity)
// be achievement

// gute namen: Seek Help, Life Choices?, Time Well Spent?, The Endless Grind, Is This Real Life?, "Magic Find is a lie", "Magic Find is a scam", "Magic Find is a cosmectic", "Magic Find is a myth" 
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

new Achievement(31, "lf Inquisitor", "250 mobs since Inquisitor", "Common"); 
new Achievement(32, "You have legi Griffin right?", "500 mobs since Inquisitor", "Rare", 31); 
new Achievement(33, "Why do you still play?", "1000 mobs since Inquisitor", "Legendary", 32, 2); 

new Achievement(34, "lf Chimera", "15 Inquisitors since Chimera", "Common"); 
new Achievement(35, "So where is my Chimera?", "30 inquisitors since Chimera", "Epic", 34); 
new Achievement(36, "I am done", "60 Inquisitors since Chimera", "Legendary", 35, 2); 
new Achievement(37, "No more Diana", "100 Inquisitors since Chimera", "Divine", 36, 3);

new Achievement(38, "Real Diana non", "Download SBO", "Divine"); 

new Achievement(39, "Fortune seeker", "Get a Diana drop with 300 Magic Find", "Uncommon");
new Achievement(40, "Bleesed by fortune", "Get a Diana drop with 400 Magic Find", "Epic", 39);
new Achievement(41, "Greed knows no bounds", "Get a Diana drop with 500 Magic Find", "Mythic", 40, 2);
new Achievement(42, "The pinnacle of luck", "Get a Diana drop with 600 Magic Find", "Divine", 41, 3); 

// new Achivement(28, "Where Chimera?", "Get all other drops from an Inquisitor", "Legendary");

new Achievement(43, "I don't need Magic Find", "Drop a Chimera, under 100 Magic Find", "Legendary"); 
new Achievement(44, "Magic Find is overrated", "Drop a Chimera, under 200 Magic Find", "Epic");

new Achievement(45, "Inquisitor Slayer", "Max the Inquisitor Bestiary", "Epic");
new Achievement(46, "Minotaur Slayer", "Max the Minotaur Bestiary", "Legendary");
new Achievement(47, "Champion Slayer", "Max the Champion Bestiary", "Epic");
new Achievement(48, "Hunter Slayer", "Max the Hunter Bestiary", "Epic");
new Achievement(49, "Lynx Slayer", "Max the Siamese Lynx Bestiary", "Epic");
new Achievement(50, "Gaia Slayer", "Max the Gaia Bestiary", "Legendary");
new Achievement(51, "Time to get on the leaderboard", "Max all Diana Bestiaries", "Mythic", false, 1, true);

export function unlockAchievement(id) {
    if (!settings.achievementEnabler) return;
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
function unlockAchievements() {
    if (!settings.achievementEnabler) return;
    if (!unlocking) achievementsToUnlock = [...new Set(achievementsToUnlock)];
    unlocking = true;
    if (achievementsToUnlock.length > 0) {
        let achievement = achievementsToUnlock.shift();
        if (achievementsData[achievement] == undefined) {
            unlockAchievement(achievement);
            setTimeout(() => {
                unlockAchievements();
            }, 2000);
        } else {
            unlockAchievements();
        }
    } else {
        unlocking = false;
    }
}

export function trackAchievementsItem(mayorTracker, item, backtrack=false) {
    if (!settings.achievementEnabler) return;
    let totalChimera = 0;
    if (item == "Chimera") {
        if ("ChimeraLs" in mayorTracker) {
            totalChimera = mayorTracker["Chimera"] + mayorTracker["ChimeraLs"];
        } else {
            totalChimera = mayorTracker["Chimera"];
        }
    }

    if (mayorTracker["Total Burrows"] >= 5000 && item == "Total Burrows" && mayorTracker["Total Burrows"] < 10000) {
        achievementsToUnlock.push(18);
    } else if (mayorTracker["Total Burrows"] >= 10000 && item == "Total Burrows" && mayorTracker["Total Burrows"] < 15000) {
        achievementsToUnlock.push(19);
    } else if (mayorTracker["Total Burrows"] >= 15000 && item == "Total Burrows" && mayorTracker["Total Burrows"] < 20000) {
        achievementsToUnlock.push(20);
    } else if (mayorTracker["Total Burrows"] >= 20000 && item == "Total Burrows" && mayorTracker["Total Burrows"] < 25000) {
        achievementsToUnlock.push(21);
    } else if (mayorTracker["Total Burrows"] >= 25000 && item == "Total Burrows") {
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
    
    if (mayorTracker["MINOS_RELIC"] >= 1 && item == "MINOS_RELIC") {
        achievementsToUnlock.push(16);
    }

    if (mayorTracker["Daedalus Stick"] >= 1 && item == "Daedalus Stick" && mayorTracker["Daedalus Stick"] < 7) {
        achievementsToUnlock.push(14);
    } else if (mayorTracker["Daedalus Stick"] >= 7 && item == "Daedalus Stick") {
        achievementsToUnlock.push(8);
    }

    if (totalChimera >= 1 && item == "Chimera" && totalChimera < 16) {
        achievementsToUnlock.push(12);
    } else if (totalChimera >= 16 && item == "Chimera" && totalChimera < 32) {
        achievementsToUnlock.push(9);
    } else if (totalChimera >= 32 && item == "Chimera") {
        achievementsToUnlock.push(11);
    }

    if (mayorTracker["ChimeraLs"] >= 1 && item == "ChimeraLs" && mayorTracker["ChimeraLs"] < 16) {
        achievementsToUnlock.push(13);
    } else if (mayorTracker["ChimeraLs"] >= 16 && item == "ChimeraLs") {
        achievementsToUnlock.push(10);
    }
    if (!backtrack) {
        unlockAchievements();
    }
}

// export function trackAchievementsMob(mayorTracker, mob) {

// }

export function trackSinceMob() {
    if (!settings.achievementEnabler) return;
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

    if (data["champsSinceRelic"] >= 1000) {
        achievementsToUnlock.push(30);
    }
    unlockAchievements();
}

export function trackMagicFind(magicFind, chimera=false) {
    if (!settings.achievementEnabler) return;
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
    if (gaiaKills >= 100) {
        achievementsToUnlock.push(50);
    }
    if (inqKills >= 100) {
        achievementsToUnlock.push(45);
    }
    if (minoKills >= 100) {
        achievementsToUnlock.push(46);
    }
    if (champKills >= 100) {
        achievementsToUnlock.push(47);
    }
    if (hunterKills >= 100) {
        achievementsToUnlock.push(48);
    }
    if (catKills >= 100) {
        achievementsToUnlock.push(49);
    }
    if (gaiaKills >= 100 && inqKills >= 100 && minoKills >= 100 && champKills >= 100 && hunterKills >= 100 && catKills >= 100) {
        achievementsToUnlock.push(51);
    }
    unlockAchievements();
}

function getKillsFromLore(item) {
    let lore = item.getLore();
    let kills = 0;
    lore.forEach(line => {
        if (line.removeFormatting().includes("Kills: ")) {
            kills = parseInt(line.split("Kills: ")[1].removeFormatting().replace(",", ""));
        }
    });
    return kills;
}

register("guiOpened", (event) => {
    if (!settings.achievementEnabler) return;
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
    }, 400);
})

export function backTrackAchievements() {
    ChatLib.chat("&6[SBO] &eBacktracking Achievements...");
    for (let event in pastDianaEvents.events) {
        for (let key in pastDianaEvents.events[event].items) {
            trackAchievementsItem(pastDianaEvents.events[event].items, key, true);
        }
        // for (let key in event.mobs) {
        //     trackAchievementsMob(event.mobs, key);
        // }
    }
    trackSinceMob();
}

register("command", () => {
    if (!settings.achievementEnabler) return;
    backTrackAchievements();
}).setName("sbobacktrackachivements");

// achivements in txt data
function writeAchievements() {
    let achievements = [];
    Achievement.list.forEach(achievement => {
        achievements.push(achievement.getName(), ": " ,achievement.getDescription(), "\n");
    })
    FileLib.write("./config/ChatTriggers/modules/SBO/SboAchivements.txt", achievements.join(""));
}

register("command", () => {
    unlockAchievement(17);
}).setName("sbotest");
