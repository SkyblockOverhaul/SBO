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



export const attributeShorts = {
    'mana_pool': 'MP',
    'mana_regeneration': 'MR',
    'dominance': 'DO',
    'veteran': 'VE',
    'vitality': 'VI',
    'magic_find': 'MF',
    'breeze': 'BR',
    'life_regeneration': 'LR',
    'lifeline': 'LL',
    'speed': 'SP',
    'fortitude': 'FO',
    'arachno_resistance': 'AR',
    'blazing_resistance': 'BL',
    'ender_resistance': 'ER',
    'undead_resistance': 'UR',
    'experience': 'EP',
}

export const bazaarIds = [
    "MANDRAA", 
    "KUUDRA_MANDIBLE", 
    "ENCHANTMENT_STRONG_MANA_1",
    "ENCHANTMENT_STRONG_MANA_2",
    "ENCHANTMENT_STRONG_MANA_3",
    "ENCHANTMENT_STRONG_MANA_4",
    "ENCHANTMENT_STRONG_MANA_5",
    "ENCHANTMENT_FEROCIOUS_MANA_1",
    "ENCHANTMENT_FEROCIOUS_MANA_2",
    "ENCHANTMENT_FEROCIOUS_MANA_3",
    "ENCHANTMENT_FEROCIOUS_MANA_4",
    "ENCHANTMENT_FEROCIOUS_MANA_5",
    "ENCHANTMENT_HARDENED_MANA_1",  
    "ENCHANTMENT_HARDENED_MANA_2",  
    "ENCHANTMENT_HARDENED_MANA_3",
    "ENCHANTMENT_HARDENED_MANA_4",
    "ENCHANTMENT_HARDENED_MANA_5",
    "ENCHANTMENT_MANA_VAMPIRE_1",
    "ENCHANTMENT_MANA_VAMPIRE_2",
    "ENCHANTMENT_MANA_VAMPIRE_3",
    "ENCHANTMENT_MANA_VAMPIRE_4",
    "ENCHANTMENT_MANA_VAMPIRE_5",
    "ESSENCE_CRIMSON",
    "ENCHANTMENT_ULTIMATE_INFERNO_1",
    "ENCHANTMENT_ULTIMATE_FATAL_TEMPO_1",
    "ENCHANTED_MYCELIUM",
    "ENCHANTED_RED_SAND",
]

export const ahIds = ["ENRAGER", "RUNIC_STAFF", "HOLLOW_WAND", "WHEEL_OF_FATE", "BURNING_KUUDRA_CORE", "TENTACLE_DYE"]

export const allowedItemIds = [
    "CRIMSON_HELMET",
    "CRIMSON_CHESTPLATE",
    "CRIMSON_LEGGINGS",
    "CRIMSON_BOOTS",
    "AURORA_HELMET",
    "AURORA_CHESTPLATE",
    "AURORA_LEGGINGS",
    "AURORA_BOOTS",
    "HOLLOW_HELMET",
    "HOLLOW_CHESTPLATE",
    "HOLLOW_LEGGINGS",
    "HOLLOW_BOOTS",
    "TERROR_HELMET",
    "TERROR_CHESTPLATE",
    "TERROR_LEGGINGS",
    "TERROR_BOOTS",
    "FERVOR_HELMET",
    "FERVOR_CHESTPLATE",
    "FERVOR_LEGGINGS",
    "FERVOR_BOOTS",
    "MOLTEN_NECKLACE",
    "MOLTEN_BELT",
    "MOLTEN_BRACELET",
    "MOLTEN_CLOAK",
];


export const indexDict = {
    "00": 0,
    "10": 1,
    "20": 2,
    "30": 3,
    "40": 4,
    "50": 5,
    "60": 6,
    "70": 7,
    "80": 8,
    "01": 9,
    "11": 10,
    "21": 11,
    "31": 12,
    "41": 13,
    "51": 14,
    "61": 15,
    "71": 16,
    "81": 17,
    "02": 18,
    "12": 19,
    "22": 20,
    "32": 21,
    "42": 22,
    "52": 23,
    "62": 24,
    "72": 25,
    "82": 26,
    "03": 27,
    "13": 28,
    "23": 29,
    "33": 30,
    "43": 31,
    "53": 32,
    "63": 33,
    "73": 34,
    "83": 35,
    "04": 36,   
    "14": 37,
    "24": 38,
    "34": 39,
    "44": 40,
    "54": 41,
    "64": 42,
    "74": 43,
    "84": 44,
    "05": 45,
    "15": 46,
    "25": 47,
    "35": 48,
    "45": 49,
    "55": 50,
    "65": 51,
    "75": 52,
    "85": 53
};


export const indexDictReverse = {
    0: "00",
    1: "10",
    2: "20",
    3: "30",
    4: "40",
    5: "50",
    6: "60",
    7: "70",
    8: "80",
    9: "01",
    10: "11",
    11: "21",
    12: "31",
    13: "41",
    14: "51",
    15: "61",
    16: "71",
    17: "81",
    18: "02",
    19: "12",
    20: "22",
    21: "32",
    22: "42",
    23: "52",
    24: "62",
    25: "72",
    26: "82",
    27: "03",
    28: "13",
    29: "23",
    30: "33",
    31: "43",
    32: "53",
    33: "63",
    34: "73",
    35: "83",
    36: "04",
    37: "14",
    38: "24",
    39: "34",
    40: "44",
    41: "54",
    42: "64",
    43: "74",
    44: "84",
    45: "05",
    46: "15",
    47: "25",
    48: "35",
    49: "45",
    50: "55",
    51: "65",
    52: "75",
    53: "85"
};

// all figures
const clubbed1 = {'name': "Clubbed",'coords': [{'x': 6, 'y': 0}, {'x': 7, 'y': 0}, {'x': 1, 'y': 1}, {'x': 6, 'y': 1}, {'x': 7, 'y': 1}, {'x': 0, 'y': 2}, {'x': 5, 'y': 2}, {'x': 1, 'y': 3}, {'x': 2, 'y': 3}, {'x': 3, 'y': 3}, {'x': 4, 'y': 3}]}
const clubbed2 = {'name': "Clubbed",'coords': [{'x': 1, 'y': 0}, {'x': 2, 'y': 0}, {'x': 3, 'y': 0}, {'x': 4, 'y': 0}, {'x': 0, 'y': 1}, {'x': 5, 'y': 1}, {'x': 1, 'y': 2}, {'x': 6, 'y': 2}, {'x': 7, 'y': 2}, {'x': 6, 'y': 3}, {'x': 7, 'y': 3}]}
const clubbed3 = {'name': "Clubbed",'coords': [{'x': 1, 'y': 3}, {'x': 0, 'y': 3}, {'x': 6, 'y': 2}, {'x': 1, 'y': 2}, {'x': 0, 'y': 2}, {'x': 7, 'y': 1}, {'x': 2, 'y': 1}, {'x': 6, 'y': 0}, {'x': 5, 'y': 0}, {'x': 4, 'y': 0}, {'x': 3, 'y': 0}]}
const clubbed4 = {'name': "Clubbed",'coords': [{'x': 6, 'y': 3}, {'x': 5, 'y': 3}, {'x': 4, 'y': 3}, {'x': 3, 'y': 3}, {'x': 7, 'y': 2}, {'x': 2, 'y': 2}, {'x': 6, 'y': 1}, {'x': 1, 'y': 1}, {'x': 0, 'y': 1}, {'x': 1, 'y': 0}, {'x': 0, 'y': 0}]}

const webbed1 = {'name': "Webbed",'coords': [{'x': 0, 'y': 1}, {'x': 1, 'y': 2}, {'x': 2, 'y': 3}, {'x': 3, 'y': 3}, {'x': 3, 'y': 2}, {'x': 3, 'y': 1}, {'x': 3, 'y': 0}, {'x': 4, 'y': 3}, {'x': 5, 'y': 2}, {'x': 6, 'y': 1}]}
const webbed2 = {'name': "Webbed",'coords': [{'x': 6, 'y': 2}, {'x': 5, 'y': 1}, {'x': 4, 'y': 0}, {'x': 3, 'y': 0}, {'x': 3, 'y': 1}, {'x': 3, 'y': 2}, {'x': 3, 'y': 3}, {'x': 2, 'y': 0}, {'x': 1, 'y': 1}, {'x': 0, 'y': 2}]}

const spine1 = {'name': "Spine",'coords': [{'x': 0, 'y': 0}, {'x': 0, 'y': 1}, {'x': 1, 'y': 1}, {'x': 0, 'y': 2}, {'x': 1, 'y': 2}, {'x': 2, 'y': 2}, {'x': 0, 'y': 3}, {'x': 1, 'y': 3}, {'x': 2, 'y': 3}, {'x': 0, 'y': 4}, {'x': 1, 'y': 4}, {'x': 0, 'y': 5}]}
const spine2 = {'name': "Spine",'coords': [{'x': 0, 'y': 0}, {'x': 1, 'y': 0}, {'x': 2, 'y': 0}, {'x': 3, 'y': 0}, {'x': 4, 'y': 0}, {'x': 5, 'y': 0}, {'x': 1, 'y': 1}, {'x': 2, 'y': 1}, {'x': 3, 'y': 1}, {'x': 4, 'y': 1}, {'x': 2, 'y': 2}, {'x': 3, 'y': 2}]}
const spine3 = {'name': "Spine",'coords': [{'x': 2, 'y': 5}, {'x': 2, 'y': 4}, {'x': 1, 'y': 4}, {'x': 2, 'y': 3}, {'x': 1, 'y': 3}, {'x': 0, 'y': 3}, {'x': 2, 'y': 2}, {'x': 1, 'y': 2}, {'x': 0, 'y': 2}, {'x': 2, 'y': 1}, {'x': 1, 'y': 1}, {'x': 2, 'y': 0}]}
const spine4 = {'name': "Spine",'coords': [{'x': 5, 'y': 2}, {'x': 4, 'y': 2}, {'x': 3, 'y': 2}, {'x': 2, 'y': 2}, {'x': 1, 'y': 2}, {'x': 0, 'y': 2}, {'x': 4, 'y': 1}, {'x': 3, 'y': 1}, {'x': 2, 'y': 1}, {'x': 1, 'y': 1}, {'x': 3, 'y': 0}, {'x': 2, 'y': 0}]}

const ugly1 = {'name': "Ugly",'coords': [{"x": 1, "y": 0}, {"x": 0, "y": 1}, {"x": 1, "y": 1}, {"x": 2, "y": 1}, {"x": 0, "y": 2}, {"x": 1, "y": 2}, {"x": 2, "y": 2}, {"x": 3, "y": 2}, {"x": 0, "y": 3}, {"x": 1, "y": 3}, {"x": 2, "y": 3}, {"x": 3, "y": 3}, {"x": 0, "y": 4}, {"x": 1, "y": 4}, {"x": 2, "y": 4}, {"x": 1, "y": 5}]}
const ugly2 = {'name': "Ugly",'coords': [{'x': 1, 'y': 0}, {'x': 2, 'y': 0}, {'x': 3, 'y': 0}, {'x': 4, 'y': 0}, {'x': 0, 'y': 1}, {'x': 1, 'y': 1}, {'x': 2, 'y': 1}, {'x': 3, 'y': 1}, {'x': 4, 'y': 1}, {'x': 5, 'y': 1}, {'x': 1, 'y': 2}, {'x': 2, 'y': 2}, {'x': 3, 'y': 2}, {'x': 4, 'y': 2}, {'x': 2, 'y': 3}, {'x': 3, 'y': 3}]}
const ugly3 = {'name': "Ugly",'coords': [{'x': 2, 'y': 5}, {'x': 3, 'y': 4}, {'x': 2, 'y': 4}, {'x': 1, 'y': 4}, {'x': 3, 'y': 3}, {'x': 2, 'y': 3}, {'x': 1, 'y': 3}, {'x': 0, 'y': 3}, {'x': 3, 'y': 2}, {'x': 2, 'y': 2}, {'x': 1, 'y': 2}, {'x': 0, 'y': 2}, {'x': 3, 'y': 1}, {'x': 2, 'y': 1}, {'x': 1, 'y': 1}, {'x': 2, 'y': 0}]}
const ugly4 = {'name': "Ugly",'coords': [{'x': 4, 'y': 3}, {'x': 3, 'y': 3}, {'x': 2, 'y': 3}, {'x': 1, 'y': 3}, {'x': 5, 'y': 2}, {'x': 4, 'y': 2}, {'x': 3, 'y': 2}, {'x': 2, 'y': 2}, {'x': 1, 'y': 2}, {'x': 0, 'y': 2}, {'x': 4, 'y': 1}, {'x': 3, 'y': 1}, {'x': 2, 'y': 1}, {'x': 1, 'y': 1}, {'x': 3, 'y': 0}, {'x': 2, 'y': 0}]}

const tusk1 = {'name': "Tusk",'coords': [{'x': 2, 'y': 0}, {'x': 1, 'y': 1}, {'x': 3, 'y': 1}, {'x': 0, 'y': 2}, {'x': 1, 'y': 3}, {'x': 2, 'y': 4}, {'x': 3, 'y': 4}, {'x': 4, 'y': 4}]}
const tusk2 = {'name': "Tusk",'coords': [{'x': 2, 'y': 0}, {'x': 1, 'y': 1}, {'x': 3, 'y': 1}, {'x': 0, 'y': 2}, {'x': 4, 'y': 2}, {'x': 0, 'y': 3}, {'x': 3, 'y': 3}, {'x': 0, 'y': 4}]}
const tusk3 = {'name': "Tusk",'coords': [{'x': 2, 'y': 4}, {'x': 3, 'y': 3}, {'x': 1, 'y': 3}, {'x': 4, 'y': 2}, {'x': 3, 'y': 1}, {'x': 2, 'y': 0}, {'x': 1, 'y': 0}, {'x': 0, 'y': 0}]}
const tusk4 = {'name': "Tusk",'coords': [{'x': 2, 'y': 4}, {'x': 3, 'y': 3}, {'x': 1, 'y': 3}, {'x': 4, 'y': 2}, {'x': 0, 'y': 2}, {'x': 4, 'y': 1}, {'x': 1, 'y': 1}, {'x': 4, 'y': 0}]}

const tuskMirrored1 = {'name': "Tusk",'coords': [{'x': 2, 'y': 0}, {'x': 3, 'y': 0}, {'x': 4, 'y': 0}, {'x': 1, 'y': 1}, {'x': 0, 'y': 2}, {'x': 1, 'y': 3}, {'x': 3, 'y': 3}, {'x': 2, 'y': 4}]}
const tuskMirrored2 = {'name': "Tusk",'coords': [{'x': 0, 'y': 0}, {'x': 0, 'y': 1}, {'x': 3, 'y': 1}, {'x': 0, 'y': 2}, {'x': 4, 'y': 2}, {'x': 1, 'y': 3}, {'x': 3, 'y': 3}, {'x': 2, 'y': 4}]}
const tuskMirrored3 = {'name': "Tusk",'coords': [{'x': 2, 'y': 4}, {'x': 1, 'y': 4}, {'x': 0, 'y': 4}, {'x': 3, 'y': 3}, {'x': 4, 'y': 2}, {'x': 3, 'y': 1}, {'x': 1, 'y': 1}, {'x': 2, 'y': 0}]}
const tuskMirrored4 = {'name': "Tusk",'coords': [{'x': 4, 'y': 4}, {'x': 4, 'y': 3}, {'x': 1, 'y': 3}, {'x': 4, 'y': 2}, {'x': 0, 'y': 2}, {'x': 3, 'y': 1}, {'x': 1, 'y': 1}, {'x': 2, 'y': 0}]}

const helix1 = {'name': "Helix",'coords': [{'x': 0, 'y': 0}, {'x': 1, 'y': 0}, {'x': 2, 'y': 0}, {'x': 3, 'y': 0}, {'x': 4, 'y': 0}, {'x': 0, 'y': 1}, {'x': 4, 'y': 1}, {'x': 0, 'y': 2}, {'x': 2, 'y': 2}, {'x': 4, 'y': 2}, {'x': 0, 'y': 3}, {'x': 2, 'y': 3}, {'x': 3, 'y': 3}, {'x': 4, 'y': 3}]}
const helix2 = {'name': "Helix",'coords': [{'x': 0, 'y': 0}, {'x': 1, 'y': 0}, {'x': 2, 'y': 0}, {'x': 3, 'y': 0}, {'x': 3, 'y': 1}, {'x': 0, 'y': 2}, {'x': 1, 'y': 2}, {'x': 3, 'y': 2}, {'x': 0, 'y': 3}, {'x': 3, 'y': 3}, {'x': 0, 'y': 4}, {'x': 1, 'y': 4}, {'x': 2, 'y': 4}, {'x': 3, 'y': 4}]}
const helix3 = {'name': "Helix",'coords': [{'x': 4, 'y': 3}, {'x': 3, 'y': 3}, {'x': 2, 'y': 3}, {'x': 1, 'y': 3}, {'x': 0, 'y': 3}, {'x': 4, 'y': 2}, {'x': 0, 'y': 2}, {'x': 4, 'y': 1}, {'x': 2, 'y': 1}, {'x': 0, 'y': 1}, {'x': 4, 'y': 0}, {'x': 2, 'y': 0}, {'x': 1, 'y': 0}, {'x': 0, 'y': 0}]}
const helix4 = {'name': "Helix",'coords': [{'x': 3, 'y': 4}, {'x': 2, 'y': 4}, {'x': 1, 'y': 4}, {'x': 0, 'y': 4}, {'x': 0, 'y': 3}, {'x': 3, 'y': 2}, {'x': 2, 'y': 2}, {'x': 0, 'y': 2}, {'x': 3, 'y': 1}, {'x': 0, 'y': 1}, {'x': 3, 'y': 0}, {'x': 2, 'y': 0}, {'x': 1, 'y': 0}, {'x': 0, 'y': 0}]}

const helixMirrored1 = {'name': "Helix",'coords': [{'x': 0, 'y': 0}, {'x': 1, 'y': 0}, {'x': 2, 'y': 0}, {'x': 3, 'y': 0}, {'x': 0, 'y': 1}, {'x': 3, 'y': 1}, {'x': 0, 'y': 2}, {'x': 1, 'y': 2}, {'x': 3, 'y': 2}, {'x': 3, 'y': 3}, {'x': 0, 'y': 4}, {'x': 1, 'y': 4}, {'x': 2, 'y': 4}, {'x': 3, 'y': 4}]}
const helixMirrored2 = {'name': "Helix",'coords': [{'x': 0, 'y': 0}, {'x': 2, 'y': 0}, {'x': 3, 'y': 0}, {'x': 4, 'y': 0} ,{'x': 0, 'y': 1}, {'x': 2, 'y': 1}, {'x': 4, 'y': 1}, {'x': 0, 'y': 2}, {'x': 4, 'y': 2}, {'x': 0, 'y': 3}, {'x': 1, 'y': 3}, {'x': 2, 'y': 3}, {'x': 3, 'y': 3}, {'x': 4, 'y': 3}]}
const helixMirrored3 = {'name': "Helix",'coords': [{'x': 3, 'y': 4}, {'x': 2, 'y': 4}, {'x': 1, 'y': 4}, {'x': 0, 'y': 4}, {'x': 3, 'y': 3}, {'x': 0, 'y': 3}, {'x': 3, 'y': 2}, {'x': 2, 'y': 2}, {'x': 0, 'y': 2}, {'x': 0, 'y': 1}, {'x': 3, 'y': 0}, {'x': 2, 'y': 0}, {'x': 1, 'y': 0}, {'x': 0, 'y': 0}]}
const helixMirrored4 = {'name': "Helix",'coords': [{'x': 4, 'y': 3}, {'x': 2, 'y': 3}, {'x': 1, 'y': 3}, {'x': 0, 'y': 3}, {'x': 4, 'y': 2}, {'x': 2, 'y': 2}, {'x': 0, 'y': 2}, {'x': 4, 'y': 1}, {'x': 0, 'y': 1}, {'x': 4, 'y': 0}, {'x': 3, 'y': 0}, {'x': 2, 'y': 0}, {'x': 1, 'y': 0}, {'x': 0, 'y': 0}]}

const footprint1 = {'name': "Footprint",'coords': [{"x": 0, "y": 0}, {"x": 2, "y": 0}, {"x": 4, "y": 0}, {"x": 0, "y": 1}, {"x": 2, "y": 1}, {"x": 4, "y": 1}, {"x": 1, "y": 2}, {"x": 2, "y": 2}, {"x": 3, "y": 2}, {"x": 1, "y": 3}, {"x": 2, "y": 3}, {"x": 3, "y": 3}, {"x": 2, "y": 4}]}
const footprint2 = {'name': "Footprint",'coords': [{'x': 3, 'y': 0}, {'x': 4, 'y': 0}, {'x': 1, 'y': 1}, {'x': 2, 'y': 1}, {'x': 0, 'y': 2}, {'x': 1, 'y': 2}, {'x': 2, 'y': 2}, {'x': 3, 'y': 2}, {'x': 4, 'y': 2}, {'x': 1, 'y': 3}, {'x': 2, 'y': 3}, {'x': 3, 'y': 4}, {'x': 4, 'y': 4}]}
const footprint3 = {'name': "Footprint",'coords': [{'x': 4, 'y': 4}, {'x': 2, 'y': 4}, {'x': 0, 'y': 4}, {'x': 4, 'y': 3}, {'x': 2, 'y': 3}, {'x': 0, 'y': 3}, {'x': 3, 'y': 2}, {'x': 2, 'y': 2}, {'x': 1, 'y': 2}, {'x': 3, 'y': 1}, {'x': 2, 'y': 1}, {'x': 1, 'y': 1}, {'x': 2, 'y': 0}]}
const footprint4 = {'name': "Footprint",'coords': [{'x': 1, 'y': 4}, {'x': 0, 'y': 4}, {'x': 3, 'y': 3}, {'x': 2, 'y': 3}, {'x': 4, 'y': 2}, {'x': 3, 'y': 2}, {'x': 2, 'y': 2}, {'x': 1, 'y': 2}, {'x': 0, 'y': 2}, {'x': 3, 'y': 1}, {'x': 2, 'y': 1}, {'x': 1, 'y': 0}, {'x': 0, 'y': 0}]}

const claw1 = {'name': "Claw",'coords': [{"x": 1, "y": 0}, {"x": 0, "y": 1}, {"x": 1, "y": 1}, {"x": 2, "y": 1}, {"x": 3, "y": 1}, {"x": 1, "y": 2}, {"x": 2, "y": 2}, {"x": 4, "y": 2}, {"x": 1, "y": 3}, {"x": 3, "y": 3}, {"x": 5, "y": 3}, {"x": 2, "y": 4}, {"x": 4, "y": 4}]}
const claw2 = {'name': "Claw",'coords': [{'x': 4, 'y': 0}, {'x': 2, 'y': 1}, {'x': 3, 'y': 1}, {'x': 4, 'y': 1}, {'x': 5, 'y': 1}, {'x': 1, 'y': 2}, {'x': 3, 'y': 2}, {'x': 4, 'y': 2}, {'x': 0, 'y': 3}, {'x': 2, 'y': 3}, {'x': 4, 'y': 3}, {'x': 1, 'y': 4}, {'x': 3, 'y': 4}]}
const claw3 = {'name': "Claw",'coords': [{'x': 1, 'y': 0}, {'x': 3, 'y': 0}, {'x': 0, 'y': 1}, {'x': 2, 'y': 1}, {'x': 4, 'y': 1}, {'x': 1, 'y': 2}, {'x': 3, 'y': 2}, {'x': 4, 'y': 2}, {'x': 2, 'y': 3}, {'x': 3, 'y': 3}, {'x': 4, 'y': 3}, {'x': 5, 'y': 3}, {'x': 4, 'y': 4}]}
const claw4 = {'name': "Claw",'coords': [{'x': 1, 'y': 4}, {'x': 3, 'y': 3}, {'x': 2, 'y': 3}, {'x': 1, 'y': 3}, {'x': 0, 'y': 3}, {'x': 4, 'y': 2}, {'x': 2, 'y': 2}, {'x': 1, 'y': 2}, {'x': 5, 'y': 1}, {'x': 3, 'y': 1}, {'x': 1, 'y': 1}, {'x': 4, 'y': 0}, {'x': 2, 'y': 0}]}

// const clawMirrored1 = {'name': "clawMirrored",'coords': [{'x': 2, 'y': 0}, {'x': 4, 'y': 0}, {'x': 1, 'y': 1}, {'x': 3, 'y': 1}, {'x': 5, 'y': 1}, {'x': 1, 'y': 2}, {'x': 2, 'y': 2}, {'x': 4, 'y': 2}, {'x': 0, 'y': 3}, {'x': 1, 'y': 3}, {'x': 2, 'y': 3}, {'x': 3, 'y': 3}, {'x': 1, 'y': 4}]}
// const clawMirrored2 = {'name': "clawMirrored",'coords': [{'x': 1, 'y': 0}, {'x': 0, 'y': 1}, {'x': 1, 'y': 1}, {'x': 2, 'y': 1}, {'x': 3, 'y': 1}, {'x': 1, 'y': 2}, {'x': 2, 'y': 2}, {'x': 4, 'y': 2}, {'x': 1, 'y': 3}, {'x': 3, 'y': 3}, {'x': 2, 'y': 4}, {'x': 4, 'y': 4}, {'x': 5, 'y': 3}]}
// const clawMirrored3 = {'name': "clawMirrored",'coords': [{'x': 3, 'y': 4}, {'x': 1, 'y': 4}, {'x': 4, 'y': 3}, {'x': 2, 'y': 3}, {'x': 0, 'y': 3}, {'x': 4, 'y': 2}, {'x': 3, 'y': 2}, {'x': 1, 'y': 2}, {'x': 5, 'y': 1}, {'x': 4, 'y': 1}, {'x': 3, 'y': 1}, {'x': 2, 'y': 1}, {'x': 4, 'y': 0}]}
// const clawMirrored4 = {'name': "clawMirrored",'coords': [{'x': 4, 'y': 4}, {'x': 5, 'y': 3}, {'x': 4, 'y': 3}, {'x': 3, 'y': 3}, {'x': 2, 'y': 3}, {'x': 4, 'y': 2}, {'x': 3, 'y': 2}, {'x': 1, 'y': 2}, {'x': 4, 'y': 1}, {'x': 2, 'y': 1}, {'x': 3, 'y': 0}, {'x': 1, 'y': 0}, {'x': 0, 'y': 1}]}

// const bugged = {'name': "bugged",'coords': [{'x': 3, 'y': 0}, {'x': 4, 'y': 0}, {'x': 5, 'y': 0}, {'x': 1, 'y': 1}, {'x': 0, 'y': 2}, {'x': 1, 'y': 3}, {'x': 3, 'y': 3}, {'x': 1, 'y': 4}]}
// mssing figures
// claw1mirrowed, claw2mirrowed, claw3mirrowed, claw4mirrowed, tusk1mirrowed, tusk2mirrowed, tusk3mirrowed, tusk4mirrowed 


// all figures in a list
export const allFigures = [
    clubbed1, clubbed2, clubbed3, clubbed4, 
    webbed1, webbed2, 
    spine1, spine2, spine3, spine4, 
    ugly1, ugly2, ugly3, ugly4,
    tusk1, tusk2, tusk3, tusk4, 
    helix1, helix2, helix3, helix4, 
    footprint1, footprint2, footprint3, footprint4, 
    claw1, claw2, claw3, claw4, 
    helixMirrored1, helixMirrored2, helixMirrored3, helixMirrored4, 
    tuskMirrored1, tuskMirrored2, tuskMirrored3, tuskMirrored4,
    // clawMirrored1, clawMirrored2, clawMirrored3, clawMirrored4,
];