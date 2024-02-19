/**
 * Colors.
 */
export const BLACK = '&0';
export const DARK_BLUE = '&1';
export const DARK_GREEN = '&2';
export const DARK_AQUA = '&3';
export const DARK_RED = '&4';
export const DARK_PURPLE = '&5';
export const GOLD = '&6';
export const GRAY = '&7';
export const DARK_GRAY = '&8';
export const BLUE = '&9';
export const GREEN = '&a';
export const AQUA = '&b';
export const RED = '&c';
export const LIGHT_PURPLE = '&d';
export const YELLOW = '&e';
export const WHITE = '&f';

/**
 * Formatting codes.
 */
export const OBFUSCATED = '&k';
export const BOLD = '&l';
export const STRIKETHROUGH = '&m';
export const UNDERLINE = '&n';
export const ITALIC = '&o';
export const RESET = '&r';

/**
 * Sounds.
 */
export const MUSIC = new Sound({source: "music.ogg"});

/**
 * VolcAddons setting constants.
 */
export const HEADER = 
`
${GOLD}${BOLD}VolcAddons ${JSON.parse(FileLib.read("VolcAddons", "metadata.json")).version}
${WHITE}Made By Volcaronitee
`;
export const LOGO = `${GRAY}[${GOLD}VolcAddons${GRAY}]`;
export const GUI_INSTRUCT = "Use +/- to change scale or press R to reset";

/**
 * Rift waypoints.
 */
export const ENIGMA_SOULS = [
    ["Tough Bark", -15, 91, 94], ["Next to Enigma", -27, 71, 90], ["Woods Flower Pot", -6, 60, 226], ["Wither Cage", -142, 68, 174],
    ["Pressurized", -137, 51, 120], ["Two Plates", -129, 72, 77], ["Fleespook", -27, 89, 136], ["Up in the Sky", -137, 133, 156],
    ["Between Branches", -108, 117, 123], ["Tel Kar", -115, 69, 61], ["Lagoon Cave", 43, 91, 56], ["Mushroom Guy", -168, 81, 12],
    ["Roy", -204, 75, 49], ["Spinning Runes", -93, 73, 36], ["Hot Dog Contest", -102, 72, -103], ["Fake Neuroscience", -34, 71, -88],
    ["2 Players Soul", -106, 78, -101], ["Cake House #1", -95, 76, -82], ["Cake House #2", -94, 70, -84], ["Dolphin Parkour", -38, 44, 130],
    ["Between Roofs", -88, 79, -102], ["Farm Flower Pot", -76, 90, -149], ["Beanstalk", -106, 249, -149], ["Buttons", -74, 65, -119],
    ["Farm Balloons", -77, 72, -176], ["Rabbit in the Hat!", -21, 72, -18], ["Back to Basics", -88, 20, 0], ["Buy BZT Today", 27, 71, -77],
    ["Plaza Fleespook", 42, 88, -91], ["Plaza Balloons", 47, 68, -59], ["Lonely Ävaeìkx", -34, 66, -25], ["Between Cooler Roofs", -23, 84, -92],
    ["Horsing Around", 40, 70, 27], ["Behind a Living Wall", 38, 63, -198], ["Flowery Message", 3, 68, -204], ["Full Circle", -161, 98, -72],
    ["Standing there", 255, 74, 160], ["Between Towers", 262, 118, 94], ["Castle Fleespook", 182, 92, 124], ["Castle Flower Pot", 266, 60, 145],
    ["Castle Balloons", 232, 94, 168], ["Fairylosopher", 256, 130, 75]
];
export const CAT_SOULS = [
    ["Piece 1", -93, 69, 112], ["Piece 2", 0, 56, 63], ["Piece 3", -139, 45, 15], ["Piece 4", -62, 88, -81], ["Piece 5", -24, 72, -198],
    ["Piece 6", 38, 65, -118], ["Piece 7", 8, 72, -15], ["Piece 8", 22, 43, -220], ["Piece 9", 152, 80, -14]
];
export const RIFT_NPCS = {
    "alabaster" : ["Alabaster", -130, 73, 167],
    "alatar" : ["Alatar", -47, 116, 71],
    "alchemist" : ["Alchemist", -50, 70, -66],
    "argofay bughunter" : ["Bughunter", -85, 108, 97],
    "bughunter" : ["Bughunter", -85, 108, 97],
    "argofay bugshopper" : ["Bugshopper", -95, 110, 95],
    "bugshopper" : ["Bugshopper", -95, 110, 95],
    "argofay serialbather" : ["Serialbather", -138, 119, 116],
    "serialbather" : ["Serialbather", -138, 119, 116],
    "argofay sonfather" : ["Sonfather", -96, 100, 156],
    "sonfather" : ["Sonfather", -96, 100, 156],
    "argofay threebrother" : [
        ["Onebrother", -99, 76, 154], ["Twobrother", -93, 76, 109], ["Threebrother", -136, 75, 113]],
    "threebrother" : [
        ["Onebrother", -99, 76, 154], ["Twobrother", -93, 76, 109], ["Threebrother", -136, 75, 113]],
    "argofay trafficker" : [
        ["Cap Trafficker", -151, 112, 115], ["Top Trafficker", -138, 97, 113], ["Leggings Trafficker", -92, 90, 107], ["Boots Trafficker", -136, 88, 114]],
    "trafficker" : [
        ["Cap Trafficker", -151, 112, 115], ["Top Trafficker", -138, 97, 113], ["Leggings Trafficker", -92, 90, 107], ["Boots Trafficker", -136, 88, 114]],
    "argofay trailblazer" : ["Argofay Trailblazer", -98, 66, 148],
    "trailblazer" : ["Argofay Trailblazer", -98, 66, 148],
    "argofay tencounter" : ["Argofay Tencounter", -93, 99, 95],
    "tencounter" : ["Argofay Tencounter", -93, 99, 95],
    "arora" : ["Arora", -129, 73, 169],
    "ashera" : ["Ashera", -124, 73, 173],
    "barry" : ["Barry", -45, 54, -143],
    "blacksmith" : ["Blacksmith", -2, 68, -142],
    "cashier" : ["Cashier", 12, 58, -78],
    "chef" : ["Chef", -103, 72, -104],
    "chester" : ["Chester", -131, 73, 173],
    "chicken" : [
        ["Chicken 1", -54, 72, -158], ["Chicken 2", -57, 72, 161]],
    "cosmo" : ["Cosmo", -67, 70, -92],
    "cow" : [
        ["Cow 1", -43, 72, -167], ["Cow 2", -38, 72, -164]],
    "cowboy nick" : ["Cowboy Nick", -13, 70, -7],
    "creed" : ["Creed", -15, 73, -112],
    "cryptosis" : ["Cryptosis", 28, 71, -77],
    "dackinoru" : ["Dackinoru", -118, 73, 173],
    "damia" : ["Damia", 31, 72, -95],
    "deer" : ["Deer", 139, 70, -12],
    "detective amog" : [
        ["Amogus 1", -50, 68, -37], ["Amogus 2", 1, 71, -13], ["Amogus 3", -188, 71, -57], ["Amogus 4", 2, 68, -140], ["Amogus 5", -50, 68, -39],
        ["Amogus 6", -29, 64, -41], ["Amogus 7", -53, 68, -49], ["Amogus 8", -51, 68, -41]],
    "disinfestor" : [
        ["Disinfestor 1", -38, 71, -103], ["Disinfestor 2", -40, 70, -88], ["Disinfestor 3", -38, 77, -94], ["Disinfestor 4", -33, 77, -89]],
    "dr emmett" : ["Dr. Emmett", 0, 0, 0],
    "dr. emmett" : ["Dr. Emmett", 0, 0, 0],
    "dr hibble" : ["Dr. Hibble", -80, 73, 10],
    "dr. hibble" : ["Dr. Hibble", -80, 73, 10],
    "dr edwin" : ["Dr. Edwin", -80, 73, 10],
    "dr. edwin" : ["Dr. Edwin", -80, 73, 10],
    "dr phear" : ["Dr. Phear", -146, 36, 28],
    "dr. phear" : ["Dr. Phear", -146, 36, 28],
    "dust" : ["Dust", -12, 71, -100],
    "elise" : [
        ["Elise (Wizard Tower)", -51, 122, 70], ["Elise (Rift Gallery)", 10, 55, 76]],
    "fafnir" : ["Fafnir", -128, 73, 173],
    "fairylosopher" : [
        ["Fairylosopher 1", 262, 106, 76], ["Fairylosopher 2", 256, 130, 75], ["Fairylosopher 3", 256, 123, 84], ["Fairylosopher 4", 256, 123, 87],
        ["Fairylosopher 5", 253, 123, 90], ["Fairylosopher 6", 250, 123, 88], ["Fairylosopher 7", 250, 123, 85]],
    "frankie" : ["Frankie", -4, 73, -111],
    "game agent" : ["Game Agent", 12, 58, -64],
    "garlacius" : ["Garlacius", -129, 79, 171],
    "grandma" : ["Grandma", -71, 65, -60],
    "gunther" : ["Gunther", -69, 71, -62],
    "harriette" : ["Harriette", -2, 74, -109],
    "hound" : ["Hound", -70, 66, 148],
    "inverted sirius" : ["Inverted Sirius", -97, 75, 191],
    "sirius" : ["Inverted Sirius", -97, 75, 191],
    "jacquelle" : ["Jacquelle", -122, 120, 137],
    "jerry" : ["Jerry", -86, 20, 5],
    "joey mcpizza" : ["Joey McPizza", -106, 72, -103],
    "joliet" : ["Joliet", -32, 65, -43],
    "kat" : ["Kat", -35, 70, -91],
    "kay" : ["Kay", -191, 68, 59],
    "kiermet" : ["Kiermet", -135, 67, 158],
    "lazarus" : ["Lazarus", -128, 79, 166],
    "maddox the slayer" : ["Maddox the Slayer", 205, 77, 45],
    "maddox" : ["Maddox the Slayer", 205, 77, 45],
    "marcia" : ["Marcia", 3, 71, -11],
    "master tactician fink" : ["Master Tactician Fink", -192, 72, -57],
    "fink" : ["Master Tactician Fink", -192, 72, -57],
    "mole" : ["Mole", -160, 97, -76],
    "motes grubber" : [
        ["Motes Grubber (Wyld Woods)", -93, 65, 157], ["Motes Grubber (West Village)", -93, 71, -73]],
    "mushroom guy" : ["Mushroom Guy", -178, 75, 13],
    "nene" : ["Nene", 24, 89, -91],
    "phaser" : ["Phaser", 60, 70, -89],
    "plumber joe" : ["Plumber Joe", -62, 70, -78],
    "porhtal" : ["Porhtal", -158, 71, 162],
    "ramero" : ["Ramero", -49, 68, -39],
    "reed" : ["Reed", -212, 72, 61],
    "ribbit" : ["Ribbit", -137, 67, 159],
    "rock" : [
        ["Rock 1", 14, 49, -187], ["Rock 2", -16, 46, -175], ["Rock 3", -25, 47, -204], ["Rock 4", 20, 45, -222], ["Rock 5", 56, 45, -200],
        ["Rock 6", 56, 45, -199], ["Rock 7", 46, 47, -181], ["Rock 8", 28, 49, -192]],
    "roger" : ["Roger", -3, 70, -44],
    "roy" : ["Roy", -205, 75, 49],
    "seraphine" : ["Seraphine", -17, 71, -101],
    "seskel" : ["Seskel", -129, 73, 164],
    "seymour" : ["Seymour", -27, 71, -45],
    "shania" : ["Shania", -48, 72, -161],
    "sheep" : [
        ["Sheep 1", -59, 72, -167], ["Sheep 2", -55, 73, -167], ["Sheep 3", -55, 73, -167]],
    "shifted" : ["Shifted", -121, 73, 175],
    "sick farmer" : ["Sick Farmer", -43, 72, -160],
    "skylark" : ["Skylark", -72, 71, -115],
    "soma" : ["Soma", -17, 74, -108],
    "sorcerer okron" : ["Sorcerer Okron", -71, 81, -111],
    "okron" : ["Sorcerer Okron", -71, 81, -111],
    "stain" : ["Stain", -7, 74, -100],
    "taylor" : ["Taylor", -29, 64, -45],
    "tel kar" : ["Tel Kar", -113, 69, 62],
    "tybalt" : ["Tybalt", -129, 79, 169],
    "unbound explorer" : ["Unbound Explorer", -29, 72, 316],
    "unhinged kloon" : ["Unhinged Kloon", -57, 79, -14],
    "minikloon" : ["Unhinged Kloon", -57, 79, -14],
    "violetta" : ["Violetta", -4, 73, -105],
    "vreike" : ["Vreike", -124, 73, 173],
    "wizard" : ["Wizard", -48, 123, 78],
    "wizardman" : ["Wizardman", -45, 90, 70],
    "yoshua" : ["Yoshua", -69, 71, -118],
}
export const RIFT_ZONES = {
    "wizard tower" : ["Wizard Tower", -48, 122, 73],
    "wyld woods" : ["Wyld Woods", -80, 67, 132],
    "enigmas crib" : ["Enigma's Crib", -40, 71, 96],
    "broken cage" : ["Broken Cage", -153, 70, 163],
    "shifted tavern" : ["Shifted Tavern", -124, 73,171 ],
    "pumpgrotto" : ["Pumpgrotto", 0, 0, 0],
    "the bastion" : ["The Bastion", -19, 92, 84],
    "otherside" : ["Otherside", 0, 0, 0],
    "black lagoon" : ["Black Lagoon", 0, 0, 0],
    "lagoon cave" : ["Lagoon Cave", 0, 0, 0],
    "lagoon hut" : ["Lagoon Hut", -195, 68, 59],
    "leeches lair" : ["Leeche's Lair", 150, 36, 28],
    "around colosseum" : ["Around Colosseum", -190, 71, -53],
    "rift gallery entrance" : ["Rift Gallery Entrance", 87, 70, 72],
    "rift gallery" : ["Rift Gallery", 14, 55, 80],
    "west village" : ["West Village", -80, 70, -40],
    "dolphin trainer" : ["Dolphin Trainer", -67, 52, -96],
    "cake house" : ["Cake House", -91, 70, -83],
    "infested house" : ["Infested House", 38, 70, -91],
    "mirrorverse" : ["Mirrorverse", -82, 64, -118],
    "dreadfarm" : ["Dreadfarm", -82, 64, -118],
    "great beanstalk" : ["Great Beanstalk", -96, 72, -149],
    "village plaza" : ["Village Plaza", -2, 70, -50],
    "taylors" : ["Taylor's", -31, 71, -40],
    "lonely terrace" : ["Lonely Terrace", -39, 68, -31],
    "murder house" : ["Murder House", -50, 68, -40],
    "book in a book" : ["Book in a Book", 2, 62, -13],
    "half-eaten cave" : ["Half-eaten Cave", -9, 69, 4],
    "your island" : ["\"Your\" Island", -92, 20, -32],
    "barter bank show" : ["Barter Bank Show", 9, 58, -71],
    "barry center" : ["Barry Center", 9, 71, -100],
    "barry hq" : ["Barry HQ", 38, 54, -142],
    "deja vu alley" : ["Deja Vu Alley", 9, 70, -96],
    "living cave" : ["Living Cave", 15, 71, -158],
    "living stillness" : ["Living Stillness", 15, 71, -158],
    "colosseum" : ["Colosseum", -190, 71, -53],
    "barrier street" : ["Barrier Street", 31, 72, -90],
    "photon pathway" : ["Photon Pathway", 56, 70, -89],
    "stillgore chateau" : ["Stillgore Chateau", 197, 76, 42],
    "oubliette" : ["Oubliette", 217, 81, 128],
    "fairylosopher tower" : ["Fairylosopher Tower", 252, 106, 78],
}
