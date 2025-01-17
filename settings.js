import { Color } from "../Vigilance";
import {
    @ButtonProperty,
    @CheckboxProperty,
    Color,
    @ColorProperty,
    @PercentSliderProperty,
    @SelectorProperty,
    @SwitchProperty,
    @TextProperty,
    @SliderProperty,
    @Vigilant,
} from 'Vigilance';
// import { data, resetVersion } from './utils/variables';
import FileUtilities from "../FileUtilities/main";


let customSounds = ["none"];

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
        const categories = ['General','Diana','Slayer','Kuudra','Mining', 'Partyfinder','Party Commands','Customization','Quality of Life','Debug','Credits/Infos'];

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
        this.addDependency("Mob View", "Diana Tracker");
        this.addDependency("Loot View", "Diana Tracker");
        this.addDependency('Warp Party','Party Commands');
        this.addDependency('Allinvite','Party Commands');
        this.addDependency('Party Transfer','Party Commands');
        this.addDependency('Promote/Demote','Party Commands');
        this.addDependency('Ask Carrot','Party Commands');
        this.addDependency('Inq Warp Key','Detect Inq Cords');
        // this.addDependency('Blaze View','Blaze Tracker');
        this.addDependency('Max Displayed Items','Attribute Value Overlay')
        this.addDependency('Select Displayed Lines','Attribute Value Overlay');
        this.addDependency('Bazaar Setting Kuudra','Attribute Value Overlay');
        this.addDependency('Key Price','Attribute Value Overlay');
        this.addDependency('Kuudra Pet Rarity','Attribute Value Overlay');
        this.addDependency('Kuudra Pet Level','Attribute Value Overlay');
        this.addDependency('Attribute Shards For Chest Profit','Attribute Value Overlay');
        this.addDependency('Distance For Remove','Remove Guess');
        this.addDependency('Highlight All Possible Fossils','Fossil Solver');
        this.addDependency('Bridge Bot Name','Formatted Bridge Bot');
        this.addDependency('Four-Eyed Fish','Diana Tracker');
        this.addDependency('Show Price Title','Loot Screen Announcer');
        this.addDependency('Inquis Lootshare Cylinder','Inquis Lootshare Circle');
        this.addDependency('Diana Burrow Warp','Diana Burrow Guess');
        this.addDependency('Dont warp if a burrow is nearby','Diana Burrow Warp');
        this.addDependency('Carnival Zombie Line','Carnival Zombie Helper');
        this.addDependency('Custom Chim Message Text','Chim Message');
        this.addDependency('Reset Custom Chim Message','Chim Message');
        this.addDependency('Chim Message Test','Chim Message');
        this.addDependency('Golden Fish Notification','Golden Fish Timer');
        this.addDependency('Flare Expire Soon Alert','Flare Tracker');
        this.addDependency('Hide Own Flare When Not In Range','Flare Tracker');
        this.addDependency('Burrow Guess Alternative','Diana Burrow Guess');

    } 
    //-----------Diana Burrows----------------
    @SwitchProperty({
        name: "Diana Burrow Guess",
        description: "Guess the burrow location",
        category: "Diana",
        subcategory: "Diana Burrows"
    })
    dianaBurrowGuess = true;
    @SwitchProperty({
        name: "Burrow Guess Alternative",
        description: "[WIP] Makes a guess based on the arrow that spawns when you finished a burrow and the color it shows",
        category: "Diana",
        subcategory: "Diana Burrows"
    })
    dianaAdvancedBurrowGuess = false;
    @SwitchProperty({
        name: "Diana Burrow Warp",
        description: "Warp close to the guess. set your keybind in controls",
        category: "Diana",
        subcategory: "Diana Burrows"
    })
    dianaBurrowWarp = true;
    @SwitchProperty({
        name: "Dont warp if a burrow is nearby",
        description: "Doesnt warp you if a burrow is nearby",
        category: "Diana",
        subcategory: "Diana Burrows"
    })
    dontWarpIfBurrowNearby = false;
    @SwitchProperty({
        name: "Diana Burrow Detect",
        description: "Detects Diana burrows | to reset waypoints /sboclearburrows",
        category: "Diana",
        subcategory: "Diana Burrows"
    })
    dianaBurrowDetect = true;
    @TextProperty({
        name: "Warp Block Difference",
        description: "Increase it to set the difference when player warps (inq/burrow warp)",
        category: "Diana",
        subcategory: "Diana Burrows"
    })
    warpDiff = "10";

    // --- Diana Tracker ---
    @SwitchProperty({
        name: "Diana Tracker",
        description: "Tracks your Diana loot and mob kills (you need to have Settings -> Personal -> Chat -> Sacks Notifications enabled for Gold and Iron to work)",
        category: "Diana",
        subcategory: "Diana Tracker",
    })
    dianaTracker = true;
    @SelectorProperty({
        name: "Mob View",
        description: "Shows your Diana mob kills /sboguis to move the counter",
        category: "Diana",
        subcategory: "Diana Tracker",
        options: ["OFF", "Total", "Event", "Session"]
    })
    dianaMobTrackerView = 0;
    @SelectorProperty({
        name: "Loot View",
        description: "Shows your Diana loot /sboguis to move the counter",
        category: "Diana",
        subcategory: "Diana Tracker",
        options: ["OFF", "Total", "Event", "Session"]
    })
    dianaLootTrackerView = 0;
    // @SwitchProperty({
    //     name: "Inquis Tracker",
    //     description: "Tracks your Inquisitor Loot",
    //     category: "Diana",
    //     subcategory: "Diana Tracker",
    //     options: ["OFF", "Total", "Event", "Session"]
    // })
    // inquisTracker = 0;
    @SwitchProperty({
        name: "Four-Eyed Fish",
        description: "Set if you have a Four-Eyed Fish on your griffin pet",
        category: "Diana",
        subcategory: "Diana Tracker",
    })
    fourEyedFish = false;
    @SwitchProperty({
        name: "Diana Stats",
        description: "Shows stats like Mobs since Inquisitor, Inquisitors since Chimera",
        category: "Diana",
        subcategory: "Diana Tracker",
    })
    dianaStatsTracker = false;
    @SwitchProperty({
        name: "Stats Message",
        description: "Sends the chat Message with stat: [SBO] Took 120 Mobs to get a Inquis!",
        category: "Diana",
        subcategory: "Diana Tracker",
    })
    sendSinceMessage = false;
    @SwitchProperty({
        name: "Avg Magic Find Tracker",
        description: "Shows your avg magic find for sticks and chimeras",
        category: "Diana",
        subcategory: "Diana Tracker",
    })
    dianaAvgMagicFind = false;
    @ButtonProperty({
        name: "Reset Session Tracker",
        description: "Resets the session tracker For mobs and items (/sboresetsession)",
        placeholder: "Reset Session",
        category: "Diana",
        subcategory: "Diana Tracker",
    })
    resetTrackerSession() {
       ChatLib.command("sboresetsession", true);
    }
    @SelectorProperty({
        name: "Bazaar Setting Diana",
        description: "Bazaar setting to set the price for loot",
        category: "Diana",
        subcategory: "Diana Tracker",
        options: ['Instasell','Sell Offer'],
    })
    bazaarSettingDiana = 1;

    // --- Diana Waypoints ---
    @SwitchProperty({
        name: 'Detect Inq Cords',
        description: 'Create inquisitor waypoints',
        category: 'Diana',
        subcategory: 'Diana Waypoints',
    })
    inqWaypoints = true;
    @SwitchProperty({
        name: 'All Waypoints Are Inqs',
        description: 'all the waypoints are inquisitor waypoints in hub during Diana',
        category: 'Diana',
        subcategory: 'Diana Waypoints',
    })
    allWaypointsAreInqs = false;
    @SwitchProperty({
        name: 'Inq Warp Key',
        description: 'Enable inquisitor warp key, set your keybind in controls.',
        category: 'Diana',
        subcategory: 'Diana Waypoints',
    })
    inqWarpKey = true;
    @SwitchProperty({
        name: 'Remove Guess',
        description: 'Removes guess when getting close to it',
        category: 'Diana',
        subcategory: 'Diana Waypoints',
    })
    removeGuess = false;
    @SliderProperty({
        name: 'Distance For Remove',
        description: 'Input distance for guess removal',
        category: 'Diana',
        subcategory: 'Diana Waypoints',
        min: 1,
        max: 30
    })
    removeGuessDistance = 10;
    // --- Diana lines ---
    @SwitchProperty({
        name: 'Inquis Line',
        description: 'Draws lines for Inquisitor, Disable View Bobbing in controls if its buggy',
        category: 'Diana',
        subcategory: 'Diana Waypoint Lines',
    })
    inqLine = false;
    @SwitchProperty({
        name: 'Burrow Line',
        description: 'Draws lines for burrows, Disable View Bobbing in controls if its buggy',
        category: 'Diana',
        subcategory: 'Diana Waypoint Lines',
    })
    burrowLine = false;
    @SwitchProperty({
        name: 'Guess Line',
        description: 'Draws line for guess, Disable View Bobbing in controls if its buggy',
        category: 'Diana',
        subcategory: 'Diana Waypoint Lines',
    })
    guessLine = false;
    @SliderProperty({
        name: 'Line Width',
        description: 'Set the width of the lines',
        category: 'Diana',
        subcategory: 'Diana Waypoint Lines',
        min: 1,
        max: 10
    })
    burrowLineWidth = 1;
    // --- Diana Other ---
    @SwitchProperty({
        name: 'Mythos HP',
        description: 'Displays HP of mythological mobs near you. /sboguis to move it',
        category: 'Diana',
        subcategory: "Other",
    })
    mythosMobHp = false;
    @SwitchProperty({
        name: "Share Inquisitor",
        description: "Sends cords for inquisitor in party message (patcher format).",
        category: "Diana",
        subcategory: "Other",
    })
    inquisDetect = true;
    @TextProperty({
        name: "Send Text On Inq Spawn",
        description: "Sends a text on inq spawn 5 seconds after spawn, use {since} for mobs since inq, {chance} for inq chance",
        category: "Diana",
        subcategory: "InqMessage",
    })
    announceKilltext = "";
    @ButtonProperty({
        name: "Send Test Inq Message",
        description: "Sends a test inq message in chat",
        category: "Diana",
        subcategory: "InqMessage",
    })
    testInqMessage() {
        ChatLib.command("sboinqmsgtest", true);
    }
    @SwitchProperty({
        name: "Highlight Inquis",
        description: "Highlights inquisitor.",
        category: "Diana",
        subcategory: "Other",
    })
    inqHighlight = false;
    @ColorProperty({
        name: "Inquis/Lootshare Color",
        description: "Pick a color for inquisitor highlighting and lootshare circle",
        category: "Diana",
        subcategory: "Other"
    })
    inqColor = new Color(0,0.9,1,0.6);
    @SwitchProperty({
        name: "Inquis Lootshare Circle",
        description: "Draws a circle around inquisitor which shows the lootshare range",
        category: "Diana",
        subcategory: "Other",
    })
    inqCircle = false;
    @SwitchProperty({
        name: "Inquis Lootshare Cylinder",
        description: "Draws a Cylinder instead of a circle around inquisitor which shows the lootshare range",  
        category: "Diana",
        subcategory: "Other",
    })
    inqCylinder = false;


    @SelectorProperty({
        name: "Add Warps",
        description: "Adds warp points for burrow warp",
        category: "Diana",
        subcategory: "Other",
        options: ["None", "Wizard", "Crypt", "Both"]
    })
    dianaAddWarps = 0;
    @SwitchProperty({
        name: "Add Stonks Warp",
        description: "Adds a warp point /warp stonks",
        category: "Diana",
        subcategory: "Other",
    })
    stonksWarp = false;
    @SwitchProperty({
        name: "Add Dark Auction Warp",
        description: "Adds a warp point /warp da",
        category: "Diana",
        subcategory: "Other",
    })
    darkAuctionWarp = true;
    @SwitchProperty({
        name: "Chim Message",
        description: "Enables custom chim message",
        category: "Diana",
        subcategory: "Other",
    })
    chimMessageBool = false;
    @TextProperty({
        name: "Custom Chim Message Text",
        description: 'use: {mf} for MagicFind, {amount} for drop Amount this event.',
        category: "Diana",
        subcategory: "Other",
    })
    customChimMessage = "&6[SBO] &6&lRARE DROP! &d&lChimera! &b{mf} &b#{amount}";
    @ButtonProperty({
        name: "Chim Message Test",
        description: "Sends a test chim message in chat",
        category: "Diana",
        subcategory: "Other",
    })
    chimMessageTest() {
        ChatLib.command("sbochimtest", true);
    }
    @ButtonProperty({
        name: "Reset Custom Chim Message",
        description: "Resets the custom chim message to default, reopen settings to see the change",
        category: "Diana",
        subcategory: "Other",
    })
    resetCustomChimMessage() {
        this.customChimMessage = "&6[SBO] &6&lRARE DROP! &d&lChimera! &b{mf} &b#{amount}}";
    }

    // Loot Announcer
    @SwitchProperty({
        name: 'Rare Drop Announcer',
        description: 'Announce loot in chat',
        category: 'Diana',
        subcategory: 'Loot Announcer',
    })
    lootAnnouncerChat = true;
    @SwitchProperty({
        name: 'Loot Screen Announcer',
        description: 'Announce chimera/stick/relic on screen',
        category: 'Diana',
        subcategory: 'Loot Announcer',
    })
    lootAnnouncerScreen = false;
    @SwitchProperty({
        name: 'Show Price Title',
        description: 'Announce chimera/stick/relic Price as a subtiltle on screen',
        category: 'Diana',
        subcategory: 'Loot Announcer',
    })
    lootAnnouncerPrice = true;
    @SwitchProperty({
        name: 'Loot Party Announcer',
        description: 'Announce chimera/stick/relic to party',
        category: 'Diana',
        subcategory: 'Loot Announcer',
    })
    lootAnnouncerParty = false;

    //Party Commands
    @SwitchProperty({
        name: 'Party Commands',
        description: 'Enable party commands',
        category: 'Party Commands',
        subcategory: 'Party Commands',
    })
    PartyCommands = true;
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
    @SwitchProperty({
        name: 'Time Check',
        description: 'Sends your time in party chat (!time)',
        category: 'Party Commands',
        subcategory: 'Party Commands',
    })
    timeCommand = false;
    @SwitchProperty({
        name: 'Check Tps',
        description: 'Sends the server tps in party chat (!tps)',
        category: 'Party Commands',
        subcategory: 'Party Commands',
    })
    tpsCommand = false;
    //--Diana Party Commands--
    @SwitchProperty({
        name: 'Diana Party Commands',
        description: 'Enable Diana party commands (!chim, !inq, !relic, !stick, !since, !burrow, !mob) (note: you need to have Diana tracker enabled)',
        category: 'Party Commands',
        subcategory: 'Party Commands',
    })
    dianaPartyCommands = true;

    // Slayer
    @SwitchProperty({
        name: 'Effects For Blaze',
        description: 'Displays effects for blaze slayer. /sboguis to move the overlay',
        category: 'Quality of Life',
        subcategory: 'Blaze',
    })
    effectsGui = false;
    @TextProperty({
        name: "Parrot Level",
        description: "Enter parrot level for effect duration (0 = off/no parrot)",
        category: "Quality of Life",
        subcategory: 'Blaze',
    })
    parrotLevel = "0";
    // @SwitchProperty({
    //     name: 'Slayer Drop Detect',
    //     description: 'Detect slayer drops',
    //     category: 'Slayer',
    //     subcategory: 'Slayer Drop Detect',
    // })
    // slayerDropDetect = false;
    // @SwitchProperty({
    //     name: "Blaze Tracker",
    //     description: "Tracks your Blaze loot",
    //     category: "Slayer",
    //     subcategory: "Blaze Tracker",
    // })
    // blazeLootTracker = false;
    // @SelectorProperty({
    //     name: "Blaze View",
    //     description: "/sbomoveblazecounter to move the counter",
    //     category: "Slayer",
    //     subcategory: "Blaze Tracker",
    //     options: ["OFF", "Overall View", "Event View", "Session View"]
    // })
    // blazeLootTrackerView = 0;
    // Quality of Life
    @SwitchProperty({
        name: 'Formatted Bridge Bot',
        description: 'Format bridge bot messages (that are like this "Guild > bridgeBot: player: message")',
        category: 'Quality of Life',
    })
    formatBridgeBot = false;
    @TextProperty({
        name: 'Bridge Bot Name',
        description: 'Set the name of the bridge bot',
        category: 'Quality of Life',
    })
    bridgeBotName = "";
    @SwitchProperty({
        name: 'Copy Rare Drop',
        description: 'Copy rare drop Message to clipboard',
        category: 'Quality of Life',
    })
    copyRareDrop = false;
    @SwitchProperty({
        name: 'Find Dragon Lair',
        description: "Find Dragon's Lair in crystal hollows (requires hostile mob sounds enabled)",
        category: 'Quality of Life',
    })
    findDragonNest = false;
    @SwitchProperty({
        name: 'Jacob Message Hider',
        description: 'Hide Messages from jacob NPC in the chat',
        category: 'Quality of Life',
    })
    jacobHider = false;
    @SwitchProperty({
        name: 'Hide Radio Weak Message',
        description: 'Hides the radio weak message',
        category: 'Quality of Life',
    })
    hideRadioWeak = false;
    @SwitchProperty({
        name: 'Clean Diana Chat',
        description: 'Hides all spammy Diana messages',
        category: 'Quality of Life',
    })
    cleanDianaChat = false;
    @SwitchProperty({
        name: 'Hide AutoPet Messages',
        description: 'Hides all autopet messages',
        category: 'Quality of Life',
    })
    hideAutoPetMSG = false;
    @SwitchProperty({
        name: 'Hide Sacks Message',
        description: 'Hides all sacks messages',
        category: 'Quality of Life',
    })
    hideSackMessage = false;
    @SwitchProperty({
        name: 'Clickable Messages To Invite a Player',
        description: 'Click on a message to invite a player to your party when he sends you a "!inv" per msg',
        category: 'Quality of Life',
    })
    clickableInvite = false;
    @SwitchProperty({
        name: 'Pickup Log Overlay',
        description: 'Displays your pickup log in an overlay like sba, /sboguis to move the overlay (WIP)',
        category: 'Quality of Life',
    })
    pickuplogOverlay = false;
    @SwitchProperty({
        name: 'Hide Tipped Players',
        description: 'Hides the players you tipped in the chat',
        category: 'Quality of Life',
    })
    hideTippedPlayers = false;
    @SwitchProperty({
        name: 'Carnival Redstone Lamp Helper',
        description: 'Highlights the redstone lamps and draws a line to it',
        category: 'Quality of Life',
    })
    carnivalLamp = false;
    @SwitchProperty({
        name: 'Carnival Zombie Helper',
        description: 'Highlights the best zombie to shoot',
        category: 'Quality of Life',
    })
    carnivalZombie = false; 
    @SwitchProperty({
        name: 'Carnival Zombie Line',
        description: 'Draws a line to the best zombie to shoot',
        category: 'Quality of Life',
    })
    CarnivalZombieLine = false;
    @SwitchProperty({
        name: 'Golden Fish Timer',
        description: 'Shows a overlay with the timer until the next golden fish can spawn',
        category: 'Quality of Life',
    })
    goldenFishTimer = false;
    @SwitchProperty({
        name: 'Golden Fish Notification',
        description: 'Notifies you when you have not thrown your Lava Rod in over 2 minutes and 30 seconds',
        category: 'Quality of Life',
    })
    goldenFishNotification = false;
    @SwitchProperty({
        name: 'Copy Chat Message',
        description: '[WIP] Copy chat message to clipboard (Like SBE)',
        category: 'Quality of Life',
    })
    copyChatMessage = false;
    @SwitchProperty({
        name: 'Flare Tracker',
        description: 'Tracks the flare you placed (works with all flares)',
        category: 'Quality of Life',
    })
    flareTimer = false;
    @SwitchProperty({
        name: 'Flare Expire Soon Alert',
        description: 'Alerts you when your flare expires soon with a title',
        category: 'Quality of Life',
    })
    flareExpireAlert = false;
    @SwitchProperty({
        name: 'Hide Own Flare When Not In Range',
        description: 'Hides your own flare when your not in range of it',
        category: 'Quality of Life',
    })
    notInRangeSetting = false;
    // General
    @ButtonProperty({
        name: "Move GUIs",
        description: "Opens Gui Move Menu you can use /sboguis too",
        placeholder: "Move GUIs",
        category: "General",
    })
    sbomoveguis() {
       ChatLib.command("sboguis", true);
    }
    //guis
    @SwitchProperty({
        name: "Bobber Overlay",
        description: "Tracks the number of bobbers near you /sboguis to move the counter",
        category: "General",
        subcategory: "GUIs",
    })
    bobberOverlay = false;
    @SwitchProperty({
        name: "Legion Overlay",
        description: "Tracks the players near you for legion buff /sboguis to move the counter",
        category: "General",
        subcategory: "GUIs",
    })
    legionOverlay = false;
    // General Waypoints
    @SwitchProperty({
        name: 'Detect Patcher Cords',
        description: 'Create patcher waypoints',
        category: 'General',
        subcategory: 'Waypoints',
    })
    patcherWaypoints = false;
    @SelectorProperty({
        name: "Hide Own Waypoints",
        description: "Hide your own patcher/inquisitor waypoints",
        category: "General",
        subcategory: "Waypoints",
        options: ["OFF", "Inq Waypoints", "Patcher Waypoints", "Both Waypoints"]
    })
    hideOwnWaypoints = 0;

    // Kuudra
    // ---ProfitHud---
    @SwitchProperty({
        name: 'Attribute Value Overlay',
        description: 'Displays value of attributes in chests and profit for kuudra chest. /sboguis to move the overlay',
        category: 'Kuudra',
    })
    attributeValueOverlay = false;
    @SliderProperty({
        name: 'Max Displayed Items',
        description: 'Max amount of items displayed in the overlay',
        category: 'Kuudra',
        min: 1,
        max: 20
    })
    maxDisplayedItems = 15;
    @SelectorProperty({
        name: 'Select Displayed Lines',
        description: 'Select if attributes are displayed in one or two lines',
        category: 'Kuudra',
        options: ['two lines','one line'],
    })
    lineSetting = 0;
    @SelectorProperty({
        name: 'Bazaar Setting Kuudra',
        description: 'Bazaar setting for instasell/sell offer',
        category: 'Kuudra',
        options: ['Instasell','Sell Offer'],
    })
    bazaarSetting = 0;
    @SelectorProperty({
        name: 'Key Price',
        description: 'Use instabuy/buy offer for materials',
        category: 'Kuudra',
        options: ['Instabuy','Buy Offer'],
    })
    keySetting = 0;
    @SwitchProperty({
        name: 'Kuudra Key Discount',
        description: 'Enable this if you have 12k reputation for the 20% discount on keys',
        category: 'Kuudra',
    })
    keyDiscount = false;
    @SelectorProperty({
        name: 'Kuudra Pet Rarity',
        description: 'Set the rarity of your pet for essence perk',
        category: 'Kuudra',
        options: ['Legendary','Epic','Rare','Uncommon','Common','None'],
    })
    kuudraPet = 0;
    @TextProperty({
        name: 'Kuudra Pet Level',
        description: 'Set the level of your pet for essence perk',
        category: 'Kuudra',
    })
    kuudraPetLevel = "100";
    @SwitchProperty({
        name: 'Attribute Shards For Chest Profit',
        description: 'Use attribute shards for chest profit calculation',
        category: 'Kuudra',
    })
    attributeShards = false;

    // Mining
    @SwitchProperty({
        name: "Fossil Solver",
        description: "Enables the fossil solver",
        category: "Mining",
    })
    fossilSolver = false;
    @SwitchProperty({
        name: "Fossil Overlay",
        description: "Tells you the fossil you excavate /sboguis to move the overlay",
        category: "Mining",
    })
    fossilOverlay = false;
    @SwitchProperty({
        name: "Highlight All Possible Fossils",
        description: "Highlights all potential fossil locations with equal probability (if this is off only one location will be highlighted)",
        category: "Mining",
    })
    highlightAllSlots = false;
    @SwitchProperty({
        name: "Create Exit Waypoint",
        description: "Creates a waypoint at the exit of the mineshaft",
        category: "Mining",
    })
    exitWaypoint = false;
    @SwitchProperty({
        name: "Speed Boost Title",
        description: "Shows a title when you get a speed boost",
        category: "Mining",
    })
    mineSpeedBoost = false;

    // Color Settings
    @ColorProperty({
        name: "Start Burrow Color",
        description: "Pick a color for start burrows",
        category: "Customization",
        subcategory: "Colors"
    })
    startColor = new Color(0.333,1,0.333);
    @ColorProperty({
        name: "Mob Burrow Color",
        description: "Pick a color for mob burrows",
        category: "Customization",
        subcategory: "Colors"
    })
    mobColor = new Color(1,0.333,0.333);
    @ColorProperty({
        name: "Treasure Burrow Color",
        description: "Pick a color for treasure burrows",
        category: "Customization",
        subcategory: "Colors"
    })
    treasureColor = new Color(1,0.667,0);
    @ColorProperty({
        name: "Guess Color",
        description: "Pick a color for your guess",
        category: "Customization",
        subcategory: "Colors"
    })
    guessColor = new Color(1,1,1);
    @ColorProperty({
        name: "Slot Highlighting Color",
        description: "Pick a color for slot highlighting",
        category: "Customization",
        subcategory: "Colors"
    })
    slotColor = Color.RED;

    //Crown Tracker
    @SwitchProperty({
        name: "Crown Tracker",
        description: "Tracks your crown of avarice coins",
        category: "Quality of Life",
        subcategory: "Crown Tracker"
    })
    crownTracker = false;
    @SwitchProperty({
        name: "Crown Ghost Mode",
        description: "Displays Session stats for ghosts: Ghosts/Tier (how many ghosts for next tier (not perfetly accurate))",
        category: "Quality of Life",
        subcategory: "Crown Tracker"
    })
    crownGhostMode = false;
    @ButtonProperty({
        name: "Reset Crown Tracker",
        description: "Resets the crown tracker",
        placeholder: "Reset Crown Tracker",
        category: "Quality of Life",
        subcategory: "Crown Tracker"
    })
    resetCrownTracker() {
        ChatLib.command("sboresetcrowntracker", true);
    }



    // sound settings
    @ButtonProperty({
        name: "Open Sound Folder",
        description: 'Custom sounds go in here (sound must be a .ogg) (do "/ct load" after adding sounds else they wont work)',
        category: "Customization",
        subcategory: "Sound Config"
    })
    openSoundFolder() {
        java.awt.Desktop.getDesktop().open(new java.io.File(Config.modulesFolder + "/SBO/assets"));
    }
    @TextProperty({
        name: "Inquisitor Spawn Sound",
        description: "Set the sound for inquisitor spawn (enter filename)",
        category: "Customization",
        subcategory: "Sound Settings"
    })
    inqSound = "expOrb";
    @SliderProperty({
        name: "Inquisitor Spawn Volume",
        description: "Set the volume for inquisitor spawn sound",
        category: "Customization",
        subcategory: "Sound Settings",
        min: 0,
        max: 100
    })
    inqVolume = 100;

    @TextProperty({
        name: "Burrow Spawn Sound",
        description: "Set the sound for burrow spawn (enter filename)",
        category: "Customization",
        subcategory: "Sound Settings"
    })
    burrowSound = "";
    @SliderProperty({
        name: "Burrow Spawn Volume",
        description: "Set the volume for burrow spawn sound",
        category: "Customization",
        subcategory: "Sound Settings",
        min: 0,
        max: 100
    })
    burrowVolume = 50;

    @TextProperty({
        name: "Chimera Drop Sound",
        description: "Set the sound for chimera drop (enter filename)",
        category: "Customization",
        subcategory: "Sound Settings"
    })
    chimSound = "";
    @SliderProperty({
        name: "Chimera Drop Volume",
        description: "Set the volume for chimera drop sound",
        category: "Customization",
        subcategory: "Sound Settings",
        min: 0,
        max: 100
    })
    chimVolume = 50;

    @TextProperty({
        name: "Relic Drop Sound",
        description: "Set the sound for relic drop (enter filename)",
        category: "Customization",
        subcategory: "Sound Settings"
    })
    relicSound = "";
    @SliderProperty({
        name: "Relic Drop Volume",
        description: "Set the volume for relic drop sound",
        category: "Customization",
        subcategory: "Sound Settings",
        min: 0,
        max: 100
    })
    relicVolume = 50;

    @TextProperty({
        name: "Daedalus Stick Drop Sound",
        description: "Set the sound for stick drop (enter filename)",
        category: "Customization",
        subcategory: "Sound Settings"
    })
    stickSound = "";
    @SliderProperty({
        name: "Daedalus Stick Drop Volume",
        description: "Set the volume for stick drop sound",
        category: "Customization",
        subcategory: "Sound Settings",
        min: 0,
        max: 100
    })
    stickVolume = 50;

    @TextProperty({
        name: "Shelmet, Plushie and Remedies Drop Sound",
        description: "Set the sound for Shelmet, Plushie and Remedis (enter filename)",
        category: "Customization",
        subcategory: "Sound Settings"
    })
    sprSound = "mfsound";
    @SliderProperty({
        name: "Shelmet, Plushie and Remedies Drop Volume",
        description: "Set the volume for Shelmet, Plushie and Remedis sound",
        category: "Customization",
        subcategory: "Sound Settings",
        min: 0,
        max: 100
    })
    sprVolume = 50;
    @SliderProperty({
        name: "Achievement Sound Volume",
        description: "Set the volume for the achievement sound",
        category: "Customization",
        subcategory: "Sound Settings",
        min: 0,
        max: 100
    })
    achievementVolume = 50;

    // Partyfinder
    @SwitchProperty({
        name: 'Auto Invite For Partyfinder',
        description: 'Auto invites players that send you a join request with partyfinder',
        category: 'Partyfinder',
    })
    autoInvite = false;

    // Debug
    @SelectorProperty({
        name: "Test Property Sound",
        description: "Select a custom sound for a specific item",
        category: "Customization",
        subcategory: "Sound Settings",
        options: customSounds
    })
    customSound = 0;
    @SliderProperty({
        name: "Test Property Sound Volume",
        description: "Set the volume for the custom sound",
        category: "Customization",
        subcategory: "Sound Settings",
        min: 0,
        max: 100
    })
    customVolume = 50;
    @ButtonProperty({
        name: "Play Test Sound",
        description: "Plays the selected sound to test it",
        placeholder: "Play Sound",
        category: "Customization",
        subcategory: "Sound Settings"
    })
    playTestSound() {
        ChatLib.command("playsbotestsound", true);
    }
    @SwitchProperty({
        name: 'Test Features',
        description: 'Enable test features',
        category: 'Debug',
    })
    testFeatures = false;
    @SwitchProperty({
        name: 'Always Diana Mayor',
        description: 'Its always Diana, no need to check for mayor, perks or spade',
        category: 'Debug',
    })
    itsAlwaysDiana = false;
    @SwitchProperty({
        name: 'Always in Skyblock',
        description: 'you are always in skyblock, just for trolls and debug',
        category: 'Debug',
    })
    alwaysInSkyblock = false;

    // credits/infos
    @ButtonProperty({
        name: "Discord",
        description: "Open Tickets for help/bug reports",
        placeholder: "Click Me",
        category: "Credits/Infos",
        subcategory: "Infos",
    })
    openDiscord() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://discord.gg/QvM6b9jsJD"));
    }
    @ButtonProperty({
        name: "Github",
        description: "View our progress on github",
        placeholder: "Click Me",
        category: "Credits/Infos",
        subcategory: "Infos",
    })
    openGithub() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://github.com/SkyblockOverhaul/SBO"));
    }
    @ButtonProperty({
        name: "Patreon",
        description: "Support our development ☕",
        placeholder: "Click Me",
        category: "Credits/Infos",
        subcategory: "Infos",
    })
    openPatreon() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.patreon.com/Skyblock_Overhaul"));
    }
    @ButtonProperty({
        name: "Website",
        description: "Explore our website for tracking Magic Find upgrades and Attribute upgrades",
        placeholder: "Click Me",
        category: "Credits/Infos",
        subcategory: "Infos",
    })
    openWebsite() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://skyblockoverhaul.com/"));
    }
    @ButtonProperty({
        name: "SoopyV2",
        description: "(Diana guess, Mob HP)",
        placeholder: "Click Me",
        category: "Credits/Infos",
        subcategory: "Credits",
    })
    openSoopyV2() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.chattriggers.com/modules/v/SoopyV2"));
    }
    @ButtonProperty({
        name: "VolcAddons",
        description: "(Render waypoints and some utils)",
        placeholder: "Click Me",
        category: "Credits/Infos",
        subcategory: "Credits",
    })
    openVolcAddons() {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.chattriggers.com/modules/v/VolcAddons"));
    }
}
// let SboData = {
//     "effects": [],
//     "version": "0.1.3"
// };
// if (FileLib.exists("./config/ChatTriggers/modules/SBO/SboData.json")) {
//     SboData = JSON.parse(FileLib.read("./config/ChatTriggers/modules/SBO/SboData.json"));
// }
// if(!SboData.hasOwnProperty("version")) {
//     SboData["version"] = "0.0.0";
// }
// let newResetVersion = resetVersion; 
// if (data.resetVersion != newResetVersion) {
//     FileLib.deleteDirectory("./config/ChatTriggers/modules/SBO/config.toml");
//     data.resetVersion = newResetVersion;
//     data.save();
//     // FileLib.write("./config/ChatTriggers/modules/SBO/SboData.json", JSON.stringify(SboData, null, 4));
// }

FileUtilities.listFiles(Config.modulesFolder.replace("modules", "images") + "/").forEach(file => {
    if (file.endsWith(".ogg")) {
        
        let filename = file.split("\\").pop();
        customSounds.push(filename.replace(".ogg", ""));
    }
});
export function getcustomSounds() {
    return customSounds;
}

export default new Settings();