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
        const categories = ['General','Trackers,','Party Commands'];

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
        this.addDependency('Detect Patcher Cords','Waypoints')
        this.addDependency('Detect Inq Cords','Waypoints')
        this.addDependency('Inq Warp Key','Detect Inq Cords')
        this.addDependency('Rare Drop Announcer','Diana Loot Tracker')
        this.addDependency('Loot Screen Announcer','Diana Loot Tracker')
    }

    //-----------Diana----------------
    @SwitchProperty({
        name: "Diana Burrow Guess",
        description: "Guess the burrow location",
        category: "General",
        subcategory: "Diana"
    })
    dianaBurrowGuess = false;
    
    @SwitchProperty({
        name: "Diana Burrow Detect",
        description: "Detects Diana Burrows",
        category: "General",
        subcategory: "Diana"
    })
    dianaBurrowDetect = false;

    @SwitchProperty({
        name: "Diana Burrow Warp",
        description: "Warp to the closest burrow",
        category: "General",
        subcategory: "Diana"
    })
    dianaBurrowWarp = false;

    @SwitchProperty({
        name: "Diana Burrow Waypoints",
        description: "Waypoint for Diana Burrows",
        category: "General",
        subcategory: "Diana"
    })
    dianaBurrowWaypoints = false;

    
    @SwitchProperty({
        name: "Inquis Party Message",
        description: "Party massage for Inquis Detection (Patcher Format).",
        category: "General",
        subcategory: "Diana"
    })
    inquisDetect = false;
    


    // --- Diana Tracker ---
    @SwitchProperty({
        name: "Diana Mob Tracker",
        description: "Tracks your Diana Mob kills",
        category: "Trackers",
        subcategory: "Diana Trackers",
    })
    dianaMobTracker = false;
    @SelectorProperty({
        name: "Mob View",
        description: "Tracks your Diana Mob kills /sbomovemobcounter to move the counter",
        category: "Trackers",
        subcategory: "Diana Trackers",
        options: ["OFF", "Overall View", "Event View", "Session View"]
    })
    dianaMobTrackerView = 0;
    @SwitchProperty({
        name: "Diana Loot Tracker",
        description: "Tracks your Diana loot",
        category: "Trackers",
        subcategory: "Diana Trackers"
    })
    dianaLootTracker = false;
    @SelectorProperty({
        name: "Loot View",
        description: "Tracks your Diana loot /sbomovelootcounter to move the counter",
        category: "Trackers",
        subcategory: "Diana Trackers",
        options: ["OFF", "Overall View", "Event View", "Session View"]
    })
    dianaLootTrackerView = 0;
    // Bobber Counter
    @SwitchProperty({
        name: "Bobber Counter",
        description: "Tracks the number of bobbers near you /sbomovebobbercounter to move the counter",
        category: "Trackers",
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
        subcategory: 'Message Hider',
    })
    jacobHider = false;

    // Waypoints
    @SwitchProperty({
        name: 'Waypoints',
        description: 'Main Toggle Waypoints',
        category: 'General',
        subcategory: 'Waypoints',
    })
    waypoints = false;
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
        category: 'General',
        subcategory: 'Waypoints',
    })
    inqWaypoints = false;
    @SwitchProperty({
        name: 'Inq Warp Key',
        description: 'Enable Inq Warp Key, Set your keybind in controls.',
        category: 'General',
        subcategory: 'Waypoints',
    })
    inqWarpKey = false;
    // Loot Announcer
    @SwitchProperty({
        name: 'Rare Drop Announcer',
        description: 'Announce loot in chat',
        category: 'Trackers',
        subcategory: 'Loot Announcer',
    })
    lootAnnouncerChat = false;
    @SwitchProperty({
        name: 'Loot Screen Announcer',
        description: 'Announce Chimer/Stick/Relic on screen',
        category: 'Trackers',
        subcategory: 'Loot Announcer',
    })
    lootAnnouncerScreen = false;
}

export default new Settings();
