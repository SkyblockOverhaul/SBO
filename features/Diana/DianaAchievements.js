import { achievementsData } from "../../utils/variables";

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
    constructor(id, name, description, rarity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.color = rarityColorDict[rarity];
        // this.sound = sound;

        Achivement.list.push(this);
    }

    unlock() {
        if (achievementsData[this.name] == undefined) {
            achievementsData[this.name] = true;
            achievementsData.unlocked.push(this.name);
            achievementsData.save();
            Client.showTitle(`${this.color}${this.name}`, `&aAchievement Unlocked`, 0, 40, 20);
            new TextComponent(`&6[SBO] &aAchievement Unlocked &7>> ${this.color}${this.name}`).setHover("show_text", "&a" + this.description).chat();
            // World.playSound("ui.toast.challenge_complete",10,1);
        }
    }

    lock() {
        if (achievementsData[this.name] != undefined) {
            delete achievementsData[this.name];
            achievementsData.unlocked = achievementsData.unlocked.filter(achievement => achievement != this.name);
            achievementsData.save();
        }
    }

    isUnlocked() {
        return achievementsData[this.name] != undefined;
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

new Achivement(1, "b2b Chimera", "Get 2 Chimera in a row", "Mythic"); 
new Achivement(2, "b2b2b Chimera", "Get 3 Chimera in a row", "Divine");
new Achivement(3, "b2b Stick", "Get 2 Sticks in a row", "Mythic"); 
new Achivement(4, "b2b2b Stick", "Get 3 Sticks in a row", "Divine");
new Achivement(5, "b2b Relic", "Get 2 Relics in a row", "Impossible"); 
new Achivement(6, "b2b Inquisitor", "Get 2 Inquisitors in a row", "Epic"); 
new Achivement(7, "b2b2b Inquisitor", "Get 3 Inquisitors in a row", "Mythic");

new Achivement(8, "Can i make a ladder now?", "Get 7 sticks in one event", "Epic");
new Achivement(9, "Chimera V", "Get 16 chimera in one event", "Mythic");
new Achivement(10, "Tf?", "Get 16 lootshare Chimera in one event", "Mythic");
new Achivement(11, "Chimera VI", "Get 32 chimera in one event", "Divine");

new Achivement(12, "First Chimera", "Get your first chimera", "Uncommon");
new Achivement(13, "First lootshare Chimera", "Lootshare your first chimera", "Legendary");

new Achivement(14, "First Stick", "Get your first stick", "Common");
new Achivement(15, "ls Stick", "Lootshare a Stick", "Divine"); // 1/6250 base chance

new Achivement(16, "First Relic", "Get your first relic", "Rare");
new Achivement(17, "1/25000", "Lootshare a Relic", "Impossible"); // 1/25000 base chance

new Achivement(18, "Where the grind begins", "Get 5k burrows in one event", "Common"); 
new Achivement(19, "Touch some grass", "Get 10k burrows in one event", "Uncommon"); 
new Achivement(20, "Please go outside", "Get 15k burrows in one event", "Epic"); 
new Achivement(21, "Digging your own grave", "Get 20k burrows in one event", "Legendary"); 
new Achivement(22, "Are you mentally stable?", "Get 25k burrows in one event", "Mythic"); 

new Achivement(23, "So this is Diana", "1 hour of playtime in one event", "Common"); 
new Achivement(24, "Is this really fun?", "10 hours of playtime in one event", "Uncommon");
new Achivement(25, "No shower for me", "1 day of playtime in one event", "Rare");
new Achivement(26, "Are you okay?", "2 days of playtime in one event", "Epic"); 
new Achivement(27, "Sleep is downtime!", "3 days of playtime in one event", "Legendary"); 

new Achivement(29, "lf Stick", "200 minotaur since stick", "Common");
new Achivement(30, "lf Relic", "1000 champions since relic", "Uncommon");

new Achivement(31, "lf inquisitor", "250 mobs since inquisitor", "Common");
new Achivement(32, "you have legi griffin right?", "500 mobs since inquisitor", "Rare");
new Achivement(33, "why do you still play?", "1000 mobs since inquisitor", "Legendary");

new Achivement(34, "lf Chimera", "15 inquisitors since chimera", "Common");
new Achivement(35, "So where is my Chimera?", "30 inquisitors since chimera", "Epic");
new Achivement(36, "I am done", "60 inquisitors since chimera", "Legendary");
new Achivement(37, "No more Diana", "100 inquisitors since chimera", "Divine");

new Achivement(38, "Real Diana Non", "Download SBO", "Divine");


new Achivement(28, "Where Chimera?", "Get all other drops from an inquisitor", "Legendary");


export function unlockAchievement(id) {
    Achivement.list.forEach(achievement => {
        if (achievement.id == id) {
            setTimeout(() => {
                achievement.unlock();
                achievement.lock();
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
    // unlockAchievement(5);
    setTimeout(() => {
    }, 1000);
}).setName("sbotest");