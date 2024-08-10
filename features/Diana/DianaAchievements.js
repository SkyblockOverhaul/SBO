import { achievementsData } from "../../utils/variables";

let allAchievements = [];
rarityColorDict = {
    "Divine": "&b",
    "Mythic": "&d",
    "Legendary": "&6",
    "Epic": "&5",
    "Rare": "&9",
    "Uncommon": "&a",
    "Common": "&f"
}
class Achivement {
    constructor(name, description, rarity) {
        this.name = name;
        this.description = description;
        this.color = rarityColorDict[rarity];
        // this.sound = sound;

        allAchievements.push(this);
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
// Raritys: Divinee, Mythic, Legendary, Epic, Rare, Uncommon, Common
// todo: add mc sound (maybe only for over epic rarity)
// funny names for the achievements
// be achievement

new Achivement("b2b Chimera", "Get 2 Chimera in a row", "Mythic"); 
new Achivement("b2b Stick", "Get 2 Sticks in a row", "Legendary"); 
new Achivement("b2b Relic", "Get 2 Relics in a row", "Divine"); 
new Achivement("b2b Inquisitor", "Get 2 Inquisitors in a row", "Epic"); 

new Achivement("ls Chimera", "Lootshare a Chimera", "Mythic");
new Achivement("ls Stick", "Lootshare a Stick", "Legendary"); // 1/6250 base chance
new Achivement("1/25000", "Lootshare a Relic", "Divine"); // 1/25000 base chance

new Achivement("Can i make a ladder now?", "Get 7 Sticks in one event", "Epic");
new Achivement("Chimera V", "Get 16 Chimera in one event", "Mythic");
new Achivement("Chimera VI", "Get 32 Chimera in one event", "Divine");

new Achivement("5k Burrows", "Get 5k burrows in one event", "Common"); 
new Achivement("10k Burrows", "Get 10k burrows in one event", "Uncommon"); 
new Achivement("15k Burrows", "Get 15k burrows in one event", "Rare"); 
new Achivement("20k Burrows", "Get 20k burrows in one event", "Epic"); 
new Achivement("25k Burrows", "Get 25k burrows in one event", "Legendary"); 

new Achivement("1h Playtime", "1 hour of playtime in one event", "Common"); 
new Achivement("10h Playtime", "10 hours of playtime in one event", "Uncommon");
new Achivement("1d Playtime", "1 day of playtime in one event", "Rare");
new Achivement("2d Playtime", "2 days of playtime in one event", "Epic");
new Achivement("3d Playtime", "3 days of playtime in one event", "Legendary");

new Achivement("Where Chimera?", "Get all other drops from an inquisitor", "Legendary");



export function unlockAchievement(name) {
    allAchievements.forEach(achievement => {
        if (achievement.name == name) {
            setTimeout(() => {
                achievement.unlock();
                achievement.lock();
            }, 1000);
            return;
        }
    })
}

register("command", () => {
    unlockAchievement("b2b Chimera");
    setTimeout(() => {
        unlockAchievement("First Achievement");
    }, 1000);
}).setName("sbotest");