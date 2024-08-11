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
export class Achivement {
    static list = [];
    constructor(id, name, description, rarity, requirement=false, timeout=1) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.color = rarityColorDict[rarity];
        if (requirement) this.requirement = Achivement.list.find(achievement => achievement.id == requirement);
        else this.requirement = requirement;
        this.timeout = timeout;
        // this.sound = sound;

        Achivement.list.push(this);
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
            if (this.rarity == "Divine" || this.rarity == "Impossible") {
                new TextComponent(`&6[SBO] &aAchievement Unlocked &7>> &b&k &r ${this.color}${this.name} &k &r`).setHover("show_text", "&a" + this.description).chat();
            } else {
                new TextComponent(`&6[SBO] &aAchievement Unlocked &7>> ${this.color}${this.name}`).setHover("show_text", "&a" + this.description).chat();
            }
            // this.lock();
        }
    }

    lock() {
        if (achievementsData[this.id] != undefined) {
            delete achievementsData[this.id];
            achievementsData.unlocked = achievementsData.unlocked.filter(achievement => achievement != this.id);
            achievementsData.save();
        }
    }

    isUnlocked() {
        return achievementsData[this.id] != undefined;
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
}
// Raritys: Impossible, Divine, Mythic, Legendary, Epic, Rare, Uncommon, Common
// todo: add mc sound (maybe only for over epic rarity)
// be achievement

// gute namen: Seek Help, Life Choices?, Time Well Spent?, The Endless Grind, Is This Real Life?
new Achivement(1, "Back-to-Back Chimera", "Get 2 Chimera in a row", "Mythic"); // added 
new Achivement(2, "b2b2b Chimera", "Get 3 Chimera in a row", "Divine"); // added
new Achivement(3, "Back-to-Back Stick", "Get 2 Sticks in a row", "Divine");  // added 
new Achivement(5, "Back-to-Back Relic", "Get 2 Relics in a row", "Impossible");  // added 
new Achivement(6, "Inquisitor Double Trouble", "Get 2 Inquisitors in a row", "Epic"); // added 
new Achivement(7, "b2b2b Inquisitor", "Get 3 Inquisitors in a row", "Divine");// added


new Achivement(12, "First Chimera", "Get your first chimera", "Epic");// added
new Achivement(9, "Chimera V", "Get 16 chimera in one event", "Mythic", 12);// added
new Achivement(11, "Chimera VI", "Get 32 chimera in one event", "Divine", 9, 2);// added - missing

new Achivement(13, "First lootshare Chimera", "Lootshare your first chimera", "Legendary");// added
new Achivement(10, "Tf?", "Get 16 lootshare Chimera in one event", "Mythic", 13);// added

new Achivement(14, "First Stick", "Get your first stick", "Common");// added
new Achivement(8, "Can i make a ladder now?", "Get 7 sticks in one event", "Epic", 14);// added

new Achivement(15, "ls Stick", "Lootshare a Stick (1/6250 base chance)", "Divine"); // added

new Achivement(16, "First Relic", "Get your first relic", "Rare"); // added
new Achivement(17, "1/25000", "Lootshare a Relic ( 1/25000 base chance)", "Impossible"); // added

new Achivement(18, "Where the grind begins", "Get 5k burrows in one event", "Common"); // added
new Achivement(19, "Touch some grass", "Get 10k burrows in one event", "Uncommon", 18); // added
new Achivement(20, "Please go outside", "Get 15k burrows in one event", "Epic", 19 , 2); // added - missing
new Achivement(21, "Digging your own grave", "Get 20k burrows in one event", "Legendary", 20, 3); // added
new Achivement(22, "Are you mentally stable?", "Get 25k burrows in one event", "Mythic", 21, 4); // added

new Achivement(23, "So this is Diana", "1 hour of playtime in one event", "Common"); // added
new Achivement(24, "Is this really fun?", "10 hours of playtime in one event", "Uncommon", 23);// added
new Achivement(25, "No shower for me", "1 day of playtime in one event", "Rare", 24, 2);// added
new Achivement(26, "Are you okay?", "2 days of playtime in one event", "Epic", 25, 3); // added
new Achivement(27, "Sleep is downtime!", "3 days of playtime in one event", "Legendary", 26, 4); // added

new Achivement(29, "lf Stick", "200 minotaur since stick", "Common"); // added
new Achivement(30, "lf Relic", "1000 champions since relic", "Uncommon"); // added

new Achivement(31, "lf inquisitor", "250 mobs since inquisitor", "Common"); // added
new Achivement(32, "you have legi griffin right?", "500 mobs since inquisitor", "Rare", 31); // added
new Achivement(33, "why do you still play?", "1000 mobs since inquisitor", "Legendary", 32, 2); // added

new Achivement(34, "lf Chimera", "15 inquisitors since chimera", "Common"); // added
new Achivement(35, "So where is my Chimera?", "30 inquisitors since chimera", "Epic", 34); // added
new Achivement(36, "I am done", "60 inquisitors since chimera", "Legendary", 35, 2); // added 
new Achivement(37, "No more Diana", "100 inquisitors since chimera", "Divine", 36, 3);// added

new Achivement(38, "Real Diana Non", "Download SBO", "Divine"); // added

new Achivement(39, "Fortune seeker", "Get a Diana drop with 300 Magic Find", "Uncommon");
new Achivement(40, "Bleesed by fortune", "Get a Diana drop with 400 Magic Find", "Epic", 39);
new Achivement(41, "Greed knows no bounds", "Get a Diana drop with 500 Magic Find", "Mythic", 40, 2);
new Achivement(42, "The pinnacle of luck", "Get a Diana drop with 600 Magic Find", "Divine", 41, 3); 

new Achivement(28, "Where Chimera?", "Get all other drops from an inquisitor", "Legendary");

new Achivement(43, "", "Drop a Chimera with 100 Magic Find or below", "Legendary"); 
new Achivement(44, "", "Drop a Chimera with 200 Magic Find or below", "Epic");
// possible name: "Don't need Magic Find" or "Magic Find is overrated" or "Magic Find is a lie" or "Magic Find is a scam" or "Magic Find is a cosmectic" or "Magic Find is a myth" 


export function unlockAchievement(id) {
    if (achievementsData[id] != undefined) return;
    Achivement.list.forEach(achievement => {
        if (achievement.id == id) {
            achievement.check();
            return;
        }
    })
}
// achivements in txt data
function writeAchievements() {
    let achievements = [];
    Achivement.list.forEach(achievement => {
        achievements.push(achievement.getName(), ": " ,achievement.getDescription(), "\n");
    })
    FileLib.write("./config/ChatTriggers/modules/SBO/SboAchivements.txt", achievements.join(""));
}

register("command", () => {
    unlockAchievement(42);
}).setName("sbotest");


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
    if (!unlocking) achievementsToUnlock = [...new Set(achievementsToUnlock)];
    unlocking = true;
    if (achievementsToUnlock.length > 0) {
        let achievement = achievementsToUnlock.shift();
        unlockAchievement(achievement);
        setTimeout(() => {
            unlockAchievements();
        }, 2000);
    } else {
        unlocking = false;
    }
}

export function trackAchievementsItem(mayorTracker, item, backtrack=false) {
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
    backTrackAchievements();
}).setName("sbobacktrackachivements");
