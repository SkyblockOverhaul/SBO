import {
    @ButtonProperty,
    @CheckboxProperty,
    Color,
    @ColorProperty,
    @PercentSliderProperty,
    @SelectorProperty,
    @SwitchProperty,
    @TextProperty,
    @Vigilant,
} from 'Vigilance';

// The only parameter that is required is the first, which should be the Module name.
// The other 2 parameters are optional.
// The 2nd parameter is the title of the settings window, seen in the top left above the
// category list.
// The 3rd parameter is an object that determines the sorting order of the categories.

@Vigilant('SBO', 'SkyblockOverhaul', {
    getCategoryComparator: () => (a, b) => {
        // By default, categories, subcategories, and properties are sorted alphabetically.
        // You can override this behavior by returning a negative number if a should be sorted before b,
        // or a positive number if b should be sorted before a.

        // In this case, we can put Not general! to be above general.
        const categories = ['General','Diana','Slayer','Party Commands','Quality of Life','Credits/Infos'];

        return categories.indexOf(a.name) - categories.indexOf(b.name);
    },
    // getSubcategoryComparator: () => (a, b) => {
    //     // These function examples will sort the subcategories by the order in the array, so eeeeeee
    //     // will be above Category.
    //     const subcategories = ['Burrows', 'Tracker', 'Waypoints', 'Loot Announcer', 'Bobber Counter', 'Other', 'Party Commands'];

    //     return subcategories.indexOf(a.name) - subcategories.indexOf(b.name);
    // },
    // getPropertyComparator: () => (a, b) => {
    //     // And this will put the properties in the order we want them to appear.
    //     const names = ["Do action!!!", "password", "text", "Color Picker"];

    //     return names.indexOf(a.attributesExt.name) - names.indexOf(b.attributesExt.name);
    // }
})
class Settings {
    constructor() {
        this.initialize(this);
        // this.addDependency("Checkbox", "Do action!!!")
        this.addDependency("Mob View", "Diana Mob Tracker");
        this.addDependency("Loot View", "Diana Loot Tracker");
        this.addDependency('Warp Party','Party Commands')
        this.addDependency('Allinvite','Party Commands')
        this.addDependency('Party Transfer','Party Commands')
        this.addDependency('Promote/Demote','Party Commands')
        this.addDependency('Ask Carrot','Party Commands')
        this.addDependency('Inq Warp Key','Detect Inq Cords')
    }

    //-----------Diana----------------
    @SwitchProperty({
        name: "Diana Burrow Guess",
        description: "Guess the burrow location",
        category: "Diana",
        subcategory: "Diana Burrows"
    })
    dianaBurrowGuess = false;

    @SwitchProperty({
        name: "Diana Burrow Warp",
        description: "Warp to the closest burrow",
        category: "Diana",
        subcategory: "Diana Burrows"
    })
    dianaBurrowWarp = false;
    
    @SwitchProperty({
        name: "Diana Burrow Detect",
        description: "Detects diana burrows | to reset waypoints /sboclearburrows",
        category: "Diana",
        subcategory: "Diana Burrows"
    })
    dianaBurrowDetect = false;

    @SwitchProperty({
        name: "Inquis Party Message",
        description: "Party massage for inquisitor detection (patcher format).",
        category: "Diana",
        subcategory: "Other",
    })
    inquisDetect = false;
    


    // --- Diana Tracker ---
    @SwitchProperty({
        name: "Diana Mob Tracker",
        description: "Tracks your diana mob kills",
        category: "Diana",
        subcategory: "Diana Tracker",
    })
    dianaMobTracker = false;
    @SelectorProperty({
        name: "Mob View",
        description: "Tracks your diana mob kills /sbomovemobcounter to move the counter",
        category: "Diana",
        subcategory: "Diana Tracker",
        options: ["OFF", "Overall View", "Event View", "Session View"]
    })
    dianaMobTrackerView = 0;
    @SwitchProperty({
        name: "Diana Loot Tracker",
        description: "Tracks your diana loot",
        category: "Diana",
        subcategory: "Diana Tracker",
    })
    dianaLootTracker = false;
    @SelectorProperty({
        name: "Loot View",
        description: "Tracks your diana loot /sbomovelootcounter to move the counter",
        category: "Diana",
        subcategory: "Diana Tracker",
        options: ["OFF", "Overall View", "Event View", "Session View"]
    })
    dianaLootTrackerView = 0;
    // Bobber Counter
    @SwitchProperty({
        name: "Bobber Counter",
        description: "Tracks the number of bobbers near you /sbomovebobbercounter to move the counter",
        category: "General",
    })
    bobberCounter = false;
    //Party Commands
    @SwitchProperty({
        name: 'Party Commands',
        description: 'Enable party commands',
        category: 'Party Commands',
        subcategory: 'Party Commands',
    })
    PartyCommands = false;

    @SwitchProperty({
        name: 'Warp Party',
        description: '!w, !warp',
        category: 'Party Commands',
        subcategory: 'Party Commands',
    })
    WarpCommand = false;

    @SwitchProperty({
        name: 'Allinvite',
        description: '!allinv, !allinvite',
        category: 'Party Commands',
        subcategory: 'Party Commands',
    })
    AllinviteCommand = false;

    @SwitchProperty({
        name: 'Party Transfer',
        description: '!transfer [Player] (if no player is defined it transfers the party to the command writer)',
        category: 'Party Commands',
        subcategory: 'Party Commands',
    })
    TransferCommand = false;

    @SwitchProperty({
        name: 'Promote/Demote',
        description: '!promote/demote [Player] (if no player is defined it pro/demotes the command writer)',
        category: 'Party Commands',
        subcategory: 'Party Commands',
    })
    MoteCommand = false;

    @SwitchProperty({
        name: 'Ask Carrot',
        description: 'Enable !carrot Command',
        category: 'Party Commands',
        subcategory: 'Party Commands',
    })
    carrotCommand = false;

    // messageHider
    @SwitchProperty({
        name: 'Jacob Message Hider',
        description: 'Hide messages from jacob NPC in the chat',
        category: 'Quality of Life',
    })
    jacobHider = false;

    // Waypoints
    @SwitchProperty({
        name: 'Detect Patcher Cords',
        description: 'Create patcher waypoints',
        category: 'General',
        subcategory: 'Waypoints',
    })
    patcherWaypoints = false;
    @SwitchProperty({
        name: 'Detect Inq Cords',
        description: 'Create inquisitor waypoints',
        category: 'Diana',
        subcategory: 'Diana Waypoints',
    })
    inqWaypoints = false;
    @SwitchProperty({
        name: 'Inq Warp Key',
        description: 'Enable inquisitor warp key, set your keybind in controls.',
        category: 'Diana',
        subcategory: 'Diana Waypoints',
    })
    inqWarpKey = false;
    // Loot Announcer
    @SwitchProperty({
        name: 'Rare Drop Announcer',
        description: 'Announce loot in chat',
        category: 'Diana',
        subcategory: 'Loot Announcer',
    })
    lootAnnouncerChat = false;
    @SwitchProperty({
        name: 'Loot Screen Announcer',
        description: 'Announce chimera/stick/relic on screen',
        category: 'Diana',
        subcategory: 'Loot Announcer',
    })
    lootAnnouncerScreen = false;

    @SwitchProperty({
        name: 'Mythos HP',
        description: 'Displays HP of mythological mobs near you',
        category: 'Diana',
        subcategory: "Other",
    })
    mythosMobHp = false;




    // noch in settings einflegen
    @SwitchProperty({
        name: 'Effects For Blaze',
        description: 'Displays effects for blaze slayer',
        category: 'Slayer',
        subcategory: 'Blaze',
    })
    effectsGui = false;

    @TextProperty({
        name: "Parrot Level",
        description: "Enter parrot level for effect duration (0 = off/no parrot)",
        category: "Slayer",
        subcategory: 'Blaze',
    })
    parrotLevel = "0";

    @SelectorProperty({
        name: "hide Own Waypoints",
        description: "Hide your own patcher/inquisitor waypoints",
        category: "General",
        subcategory: "Waypoints",
        options: ["OFF", "Inq Waypoints", "Patcher Waypoints", "Both Waypoints"]
    })
    hideOwnWaypoints = 0;
    @SwitchProperty({
        name: 'Copy Rare Drop',
        description: 'Copy rare drop message to clipboard',
        category: 'Quality of Life',
    })
    copyRareDrop = false;

    @ButtonProperty({
        name: "Reset Session Tracker",
        description: "Resets the session tracker for mobs and items (/sboresetsession)",
        placeholder: "Reset Session",
        category: "Diana",
        subcategory: "Diana Tracker",
    })
    resetTrackerSession() {
       ChatLib.command("sboresetsession", true);
    }

    @ButtonProperty({
        name: "Discord",
        description: "Open Tickets for help/bug reports",
        placeholder: "Click me",
        category: "Credits/Infos",
        subcategory: "Infos",
    })
    openDiscord() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://discord.gg/QvM6b9jsJD"));
    }
    @ButtonProperty({
        name: "Github",
        description: "View our progress on github",
        placeholder: "Click me",
        category: "Credits/Infos",
        subcategory: "Infos",
    })
    openGithub() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://github.com/SkyblockOverhaul/SBO"));
    }
    @ButtonProperty({
        name: "Patreon",
        description: "Support our development ☕",
        placeholder: "Click me",
        category: "Credits/Infos",
        subcategory: "Infos",
    })
    openPatreon() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.patreon.com/Skyblock_Overhaul"));
    }
    @ButtonProperty({
        name: "Website",
        description: "Explore our website for tracking Magic Find upgrades and Attribute upgrades",
        placeholder: "Click me",
        category: "Credits/Infos",
        subcategory: "Infos",
    })
    openWebsite() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://skyblockoverhaul.com/"));
    }
    @ButtonProperty({
        name: "SoopyV2",
        description: "(Diana guess, Mob HP)",
        placeholder: "Click me",
        category: "Credits/Infos",
        subcategory: "Credits",
    })
    openSoopyV2() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.chattriggers.com/modules/v/SoopyV2"));
    }
    @ButtonProperty({
        name: "VolcAddons",
        description: "(Burrow detect, Render waypoints and some utils)",
        placeholder: "Click me",
        category: "Credits/Infos",
        subcategory: "Credits",
    })
    openVolcAddons() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.chattriggers.com/modules/v/VolcAddons"));
    }
}

SboData = JSON.parse(FileLib.read("./config/ChatTriggers/modules/SBO/SboData.json"));
if(SboData.hasOwnProperty("version") == false) {
    SboData["version"] = "0.0.0";
}
let newVersion = "0.1.3"; // change this to the new version for config.toml reset
if (SboData.version != newVersion) {
    FileLib.deleteDirectory("./config/ChatTriggers/modules/SBO/config.toml");
    SboData.version = newVersion;
    FileLib.write("./config/ChatTriggers/modules/SBO/SboData.json", JSON.stringify(SboData, null, 4));
}
export default new Settings();

//  java.awt.Desktop.getDesktop().browse(new java.net.URI("url"));