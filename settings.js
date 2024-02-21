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
        const categories = ['General','Trackers'];

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
    }

    //-----------Diana----------------
    @SwitchProperty({
        name: "Diana Waypoint",
        description: "Estimates Diana burrows (particles => ON, /togglemusic => OFF)",
        category: "General",
        subcategory: "Diana",
    })
    dianaWaypoint = false;

    @SwitchProperty({
        name: "Diana Warp",
        description: "change button in controls to warp to closest location to guess.",
        category: "General",
        subcategory: "Diana"
    })
    dianaWarp = false;
    
    // @SwitchProperty({
    //     name: "Inquis Detection",
    //     description: "Chat massage for Inquis detection.",
    //     category: "General",
    //     subcategory: "Diana"
    // })
    // inquisDetect = false;

    // --- Griffin Burrow ---
    @SwitchProperty({
        name: "Burrow Detection",
        description: "Identifies and generates waypoints for the burrow particles surrounding you.",
        category: "General",
        subcategory: "Griffin Burrow"
    })
    dianaBurrow = false;
    // @SwitchProperty({
    //     name: "Burrow Chat Alert",
    //     description: "Chat massage for burrow detection.",
    //     category: "General",
    //     subcategory: "Griffin Burrow"
    // })
    // dianaChat = false;

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
        description: "Tracks your Diana Mob kills /movemobcounter to move the counter",
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
        description: "Tracks your Diana loot /movelootcounter to move the counter",
        category: "Trackers",
        subcategory: "Diana Trackers",
        options: ["OFF", "Overall View", "Event View", "Session View"]
    })
    dianaLootTrackerView = 0;

    //Bobber Counter
    @SwitchProperty({
        name: 'Bobber Counter',
        description: 'Estimates the number of bobbers cast around you',
        category: 'General',
        subcategory: 'Bobber Counter'
    })
    bobberCounter = false;

    @ButtonProperty({
        name: 'Move Bobber Counter HUD',
        description: 'Click the button to move the Bobber Counter HUD',
        category: 'General',
        subcategory: 'Bobber Counter'
    })
    openBobberGUI() {
        ChatLib.command('movebobbercounter', true);
    }

    @ColorProperty({
        name: 'Bobber Counter Color',
        description: 'Pick a Color for the Bobber Counter!',
        category: 'General',
        subcategory: 'Bobber Counter'
    })
    bobberColor = Color.ORANGE;

    @SwitchProperty({
        name: 'Chroma Counter',
        description: 'Counter changes to chroma when the selected or more bobbers are cast',
        category: 'General',
        subcategory: 'Bobber Counter'
    })
    bobberChroma = false;
    // @TextProperty({
    //     name: 'text',
    //     description: 'Example of text input that does not wrap the text',
    //     category: 'Tracker',
    //     subcategory: 'Category',
    //     placeholder: 'Empty... :(',
    //     triggerActionOnInitialization: false,
    // })
    // textInput = '';

    // @TextProperty({
    //     name: 'password',
    //     description: 'Example of text input that uses protected',
    //     category: 'General',
    //     subcategory: 'Category',
    //     protected: true,
    // })
    // password = '';

    // @ColorProperty({
    //     name: 'Color Picker',
    //     description: 'Pick a color! (hopefully...)',
    //     category: 'General',
    //     subcategory: 'Category',
    // })
    // myColor = Color.BLUE;

    // @SelectorProperty({
    //     name: 'Selector',
    //     description: 'Select an option',
    //     category: 'General',
    //     subcategory: 'eeeeee',
    //     options: ['opt1', 'opty2', 'third option'],
    // })
    // myOptions = 0; // Stores index of option

    // @PercentSliderProperty({
    //     name: 'Percent Slider',
    //     description: 'Select a percent',
    //     category: 'General',
    //     subcategory: 'eeeeee',
    // })
    // percentSlider = 0.0;


}

export default new Settings();
