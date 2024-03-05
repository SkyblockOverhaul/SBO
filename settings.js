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
        const categories = ['General','Diana','Slayer','Party Commands'];

        return categories.indexOf(a.name) - categories.indexOf(b.name);
    },
    // getSubcategoryComparator: () => (a, b) => {
    //     // These function examples will sort the subcategories by the order in the array, so eeeeeee
    //     // will be above Category.
    //     const subcategories = ["eeeeee", "Category"];

    //     return subcategories.indexOf(a.getValue()[0].attributesExt.subcategory) -
    //         subcategories.indexOf(b.getValue()[0].attributesExt.subcategory);
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
        this.addDependency('Rare Drop Announcer','Diana Loot Tracker')
        this.addDependency('Loot Screen Announcer','Diana Loot Tracker')
    }

    //-----------Diana----------------
    @SwitchProperty({
        name: "Diana Burrow Guess",
        description: "Guess the burrow location",
        category: "Diana",
        subcategory: "Burrows"
    })
    dianaBurrowGuess = false;
    
    @SwitchProperty({
        name: "Diana Burrow Detect",
        description: "Detects Diana Burrows | to reset waypoints /sboclearburrows",
        category: "Diana",
        subcategory: "Burrows"
    })
    dianaBurrowDetect = false;

    @SwitchProperty({
        name: "Diana Burrow Warp",
        description: "Warp to the closest burrow",
        category: "Diana",
        subcategory: "Burrows"
    })
    dianaBurrowWarp = false;

    @SwitchProperty({
        name: "Diana Burrow Waypoints",
        description: "Waypoint for Diana Burrows",
        category: "Diana",
        subcategory: "Burrows"
    })
    dianaBurrowWaypoints = false;

    
    @SwitchProperty({
        name: "Inquis Party Message",
        description: "Party massage for Inquis Detection (Patcher Format).",
        category: "Diana",
        subcategory: "Other",
    })
    inquisDetect = false;
    


    // --- Diana Tracker ---
    @SwitchProperty({
        name: "Diana Mob Tracker",
        description: "Tracks your Diana Mob kills",
        category: "Diana",
        subcategory: "Tracker",
    })
    dianaMobTracker = false;
    @SelectorProperty({
        name: "Mob View",
        description: "Tracks your Diana Mob kills /sbomovemobcounter to move the counter",
        category: "Diana",
        subcategory: "Tracker",
        options: ["OFF", "Overall View", "Event View", "Session View"]
    })
    dianaMobTrackerView = 0;
    @SwitchProperty({
        name: "Diana Loot Tracker",
        description: "Tracks your Diana loot",
        category: "Diana",
        subcategory: "Tracker",
    })
    dianaLootTracker = false;
    @SelectorProperty({
        name: "Loot View",
        description: "Tracks your Diana loot /sbomovelootcounter to move the counter",
        category: "Diana",
        subcategory: "Tracker",
        options: ["OFF", "Overall View", "Event View", "Session View"]
    })
    dianaLootTrackerView = 0;
    // Bobber Counter
    @SwitchProperty({
        name: "Bobber Counter",
        description: "Tracks the number of bobbers near you /sbomovebobbercounter to move the counter",
        category: "General",
        subcategory: "Bobber Counter"
    })
    bobberCounter = false;
    //Party Commands
    @SwitchProperty({
        name: 'Party Commands',
        description: 'Enable Party Commands',
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
        description: 'Hide messages from Jacob NPC in the chat',
        category: 'General',
    })
    jacobHider = false;

    // Waypoints
    @SwitchProperty({
        name: 'Detect Patcher Cords',
        description: 'Create Patcher Waypoints',
        category: 'General',
        subcategory: 'Waypoints',
    })
    patcherWaypoints = false;
    @SwitchProperty({
        name: 'Detect Inq Cords',
        description: 'Create Inq Waypoints',
        category: 'Diana',
        subcategory: 'Waypoints',
    })
    inqWaypoints = false;
    @SwitchProperty({
        name: 'Inq Warp Key',
        description: 'Enable Inq Warp Key, Set your keybind in controls.',
        category: 'Diana',
        subcategory: 'Waypoints',
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
        description: 'Announce Chimera/Stick/Relic on screen',
        category: 'Diana',
        subcategory: 'Loot Announcer',
    })
    lootAnnouncerScreen = false;

    @SwitchProperty({
        name: 'Mythos HP',
        description: 'Displays Hp of Mythos Mobs near you',
        category: 'Diana',
        subcategory: "Other",
    })
    mythosMobHp = false;




    // noch in settings einflegen
    @SwitchProperty({
        name: 'Effects For Blaze',
        description: 'Displays Effects for Blaze Slayer',
        category: 'Slayer',
        subcategory: 'Blaze',
    })
    effectsGui = false;

    @TextProperty({
        name: "Parrot Level",
        description: "Enter Parrot Level for Effect duration (0 = off/no Parrot)",
        category: "Slayer",
        subcategory: 'Blaze',
    })
    parrotLevel = "0";

    @SelectorProperty({
        name: "hide Own Waypoints",
        description: "Hide your own Patcher/inq Waypoints",
        category: "General",
        subcategory: "Waypoints",
        options: ["OFF", "Inq Waypoints", "Patcher Waypoints", "Both Waypoints"]
    })
    hideOwnWaypoints = 0;
    @SwitchProperty({
        name: 'Copy Rare Drop',
        description: 'Copy Rare Drop Message to clipboard',
        category: 'General',
    })
    copyRareDrop = false;

    @ButtonProperty({
        name: "Reset Session Tracker",
        description: "Resets the Session Tracker for Mobs and Items (/sboresetsession)",
        category: "Diana",
        subcategory: "Tracker",
    })
    resetTrackerSession() {
       ChatLib.command("sboresetsession", true);
    }
}

export default new Settings();

//  java.awt.Desktop.getDesktop().browse(new java.net.URI("url"));