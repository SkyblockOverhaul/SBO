import { achievementsData, data } from "../../utils/variables";

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
class Achivement {
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
        if (this.requirement) {
            if (!this.requirement.isUnlocked()) {
                this.requirement.check();
                setTimeout(() => {
                    this.unlock();
                }, 1000 * this.timeout);
            }
        } else this.unlock();
    }

    unlock() {
        if (achievementsData[this.id] == undefined) {
            achievementsData[this.id] = true;
            achievementsData.unlocked.push(this.id);
            achievementsData.save();
            Client.showTitle(`${this.color}${this.name}`, `&aAchievement Unlocked`, 0, 40, 20);
            new TextComponent(`&6[SBO] &aAchievement Unlocked &7>> ${this.color}${this.name}`).setHover("show_text", "&a" + this.description).chat();
            this.lock();
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

new Achivement(1, "b2b Chimera", "Get 2 Chimera in a row", "Mythic"); // added
new Achivement(2, "b2b2b Chimera", "Get 3 Chimera in a row", "Divine"); // added
new Achivement(3, "b2b Stick", "Get 2 Sticks in a row", "Mythic");  // added
new Achivement(4, "b2b2b Stick", "Get 3 Sticks in a row", "Divine"); // added
new Achivement(5, "b2b Relic", "Get 2 Relics in a row", "Impossible");  // added
new Achivement(6, "b2b Inquisitor", "Get 2 Inquisitors in a row", "Epic"); // added
new Achivement(7, "b2b2b Inquisitor", "Get 3 Inquisitors in a row", "Mythic");// added


new Achivement(12, "First Chimera", "Get your first chimera", "Uncommon");// added
new Achivement(9, "Chimera V", "Get 16 chimera in one event", "Mythic", 12);// added
new Achivement(11, "Chimera VI", "Get 32 chimera in one event", "Divine", 9, 2);// added

new Achivement(13, "First lootshare Chimera", "Lootshare your first chimera", "Legendary");// added
new Achivement(10, "Tf?", "Get 16 lootshare Chimera in one event", "Mythic", 13);// added

new Achivement(14, "First Stick", "Get your first stick", "Common");// added
new Achivement(8, "Can i make a ladder now?", "Get 7 sticks in one event", "Epic", 14);// added

new Achivement(15, "ls Stick", "Lootshare a Stick", "Divine"); // 1/6250 base chance

new Achivement(16, "First Relic", "Get your first relic", "Rare"); // added
new Achivement(17, "1/25000", "Lootshare a Relic", "Impossible"); // 1/25000 base chance

new Achivement(18, "Where the grind begins", "Get 5k burrows in one event", "Common"); // added
new Achivement(19, "Touch some grass", "Get 10k burrows in one event", "Uncommon", 18); // added
new Achivement(20, "Please go outside", "Get 15k burrows in one event", "Epic", 19 , 2); // added
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

new Achivement(28, "Where Chimera?", "Get all other drops from an inquisitor", "Legendary");


export function unlockAchievement(id) {
    if (achievementsData[id] != undefined) return;
    Achivement.list.forEach(achievement => {
        if (achievement.id == id) {
            setTimeout(() => {
                achievement.check();
            }, 1000);
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
    unlockAchievement(27);
}).setName("sbotest");

export function trackAchievements(mayorTracker, item) {
    if (mayorTracker["Total Burrows"] >= 5000 && item == "Total Burrows" && mayorTracker["Total Burrows"] < 10000) {
        unlockAchievement(18);
        return;
    } else if (mayorTracker["Total Burrows"] >= 10000 && item == "Total Burrows" && mayorTracker["Total Burrows"] < 15000) {
        unlockAchievement(19);
        return;
    } else if (mayorTracker["Total Burrows"] >= 15000 && item == "Total Burrows" && mayorTracker["Total Burrows"] < 20000) {
        unlockAchievement(20);
        return;
    } else if (mayorTracker["Total Burrows"] >= 20000 && item == "Total Burrows" && mayorTracker["Total Burrows"] < 25000) {
        unlockAchievement(21);
        return;
    } else if (mayorTracker["Total Burrows"] >= 25000 && item == "Total Burrows") {
        unlockAchievement(22);  
        return;
    }

    // time saved in milliseconds
    if (mayorTracker["mayorTime"] >= 3600000 && mayorTracker["mayorTime"] < 3600000 * 10) {
        unlockAchievement(23);
        return;
    } else if (mayorTracker["mayorTime"] >= 3600000 * 10 && mayorTracker["mayorTime"] < 86400000) {
        unlockAchievement(24);
        return;
    } else if (mayorTracker["mayorTime"] >= 86400000 && mayorTracker["mayorTime"] < 86400000 * 2) {
        unlockAchievement(25);
        return;
    } else if (mayorTracker["mayorTime"] >= 86400000 * 2 && mayorTracker["mayorTime"] < 86400000 * 3) {
        unlockAchievement(26);
        return;
    } else if (mayorTracker["mayorTime"] >= 86400000 * 3) {
        unlockAchievement(27);
        return;
    }
    
    if (mayorTracker["MINOS_RELIC"] >= 1 && item == "MINOS_RELIC") {
        unlockAchievement(16);
        return;
    }

    if (mayorTracker["Daedalus Stick"] >= 1 && item == "Daedalus Stick" && mayorTracker["Daedalus Stick"] < 7) {
        unlockAchievement(14);
        return;
    } else if (mayorTracker["Daedalus Stick"] >= 7 && item == "Daedalus Stick") {
        unlockAchievement(8);
        return;
    }

    if (mayorTracker["Chimera"] >= 1 && item == "Chimera" && mayorTracker["Chimera"] < 16) {
        unlockAchievement(12);
        return;
    } else if (mayorTracker["Chimera"] >= 16 && item == "Chimera" && mayorTracker["Chimera"] < 32) {
        unlockAchievement(9);
        return;
    } else if (mayorTracker["Chimera"] >= 32 && item == "Chimera") {
        unlockAchievement(11);
        return;
    }

    if (mayorTracker["ChimeraLs"] >= 1 && item == "ChimeraLs" && mayorTracker["ChimeraLs"] < 16) {
        unlockAchievement(13);
        return;
    } else if (mayorTracker["ChimeraLs"] >= 16 && item == "ChimeraLs") {
        unlockAchievement(10);
        return;
    }
}

export function trackSinceMob() {
    if (data["mobsSinceInq"] >= 250 && data["mobsSinceInq"] < 500) {
        unlockAchievement(31);
        return;
    } else if (data["mobsSinceInq"] >= 500 && data["mobsSinceInq"] < 1000) {
        unlockAchievement(32);
        return;
    } else if (data["mobsSinceInq"] >= 1000) {
        unlockAchievement(33);
        return;
    }

    if (data["inqsSinceChim"] >= 15 && data["inqsSinceChim"] < 30) {
        unlockAchievement(34);
        return;
    } else if (data["inqsSinceChim"] >= 30 && data["inqsSinceChim"] < 60) {
        unlockAchievement(35);
        return;
    } else if (data["inqsSinceChim"] >= 60 && data["inqsSinceChim"] < 100) {
        unlockAchievement(36);
        return;
    } else if (data["inqsSinceChim"] >= 100) {
        unlockAchievement(37);
        return;
    }

    if (data["minotaursSinceStick"] >= 200) {
        unlockAchievement(29);
        return;
    }

    if (data["champsSinceRelic"] >= 1000) {
        unlockAchievement(30);
        return;
    }
}

