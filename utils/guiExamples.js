import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE} from "./constants";

export let blazeLootTrackerExample = 
`${YELLOW}${BOLD}Blaze Loot Tracker
-------------------
${WHITE}${BOLD}Ice-Flavored:
${WHITE}${BOLD}Fire Aspect III:
${GREEN}${BOLD}Flawed Opal Gems:
${BLUE}${BOLD}Lavatears Runes:
${BLUE}${BOLD}Mana Disintegrator:
${BLUE}${BOLD}Kelvin Inverter:
${BLUE}${BOLD}Blaze Rod Dist:
${BLUE}${BOLD}Glowstone Dist:
${BLUE}${BOLD}Magma Cream Dist:
${BLUE}${BOLD}Nether Wart Dist:
${BLUE}${BOLD}Gabagool Dist:
${DARK_PURPLE}${BOLD}Magma Arrows:
${DARK_PURPLE}${BOLD}Archfiend Dice
${GOLD}${BOLD}Fiery Burst Rune:
${GOLD}${BOLD}Scorched Power:
${GOLD}${BOLD}Engineering Plans:
${GOLD}${BOLD}Subzero Inverter:
${GOLD}${BOLD}High Class Dice:
${LIGHT_PURPLE}${BOLD}Duplex:
${LIGHT_PURPLE}${BOLD}Scorched Books:
${GRAY}${BOLD}Blaze Killed: 
`

export let overlayExamples = {
    kuudraExampleTwo: `&r&62.49M &r&eTerror Chestplate&r
&r&b(BL 5/BR 4 - &r&6100.00K/2.49M&b)
&r&62.50M &r&eTerror Boots&r
&r&b(ER 5/DO 4 - &r&61.48M/2.50M&b)
&r&eTotal Value: &r&64.99M coins`,
    kuudraExampleOne: `&r&6600.00K &r&eCrimson Chestplate&r &r&b(BL 5/BR 4 - &r&6100.00K/600.00K&b)
&r&62.50M &r&eTerror Boots&r &r&b(ER 5/DO 4 - &r&61.48M/2.50M&b)
&r&eTotal Value: &r&63.1M coins`,
    fossilExample: `Fossil: Unknown`,
    bobbercounterExample:`${YELLOW}${BOLD}Bobber: ${AQUA}${BOLD}0`,
    effectsGuiExample:
`${YELLOW}${BOLD}Active Effects
--------------
${AQUA}${BOLD}Wisp's Water: ${WHITE}2520s`,
    mythosMobHpExample:
`&8[&7Lv750&8] &2Exalted Minos Inquisitor &a40M&f/&a40M`,
    dianaMobTrackerExample:
`${YELLOW}${BOLD}Diana Mob Tracker
${GRAY}- ${LIGHT_PURPLE}${BOLD}Minos Inquisitor: ${WHITE}
${GRAY}- ${DARK_PURPLE}${BOLD}Minos Champion: ${WHITE}
${GRAY}- ${GOLD}${BOLD}Minotaur: ${WHITE}
${GRAY}- ${GREEN}${BOLD}Gaia Construct: ${WHITE}
${GRAY}- ${GREEN}${BOLD}Siamese Lynx: ${WHITE}
${GRAY}- ${GREEN}${BOLD}Minos Hunter: ${WHITE}
${GRAY}- ${GRAY}${BOLD}Total Mobs: ${WHITE}
`,
    dianaLootTrackerExample: 
`${YELLOW}${BOLD}Diana Loot Tracker
${GRAY}- ${LIGHT_PURPLE}${BOLD}Chimera: [${AQUA}LS${GRAY}: ]
${GRAY}- ${DARK_PURPLE}${BOLD}Minos Relic: ${WHITE}
${GRAY}- ${GOLD}${BOLD}Daedalus Stick: ${WHITE}
${GRAY}- ${GOLD}${BOLD}Crown of Greed: ${WHITE}
${GRAY}- ${GOLD}${BOLD}Souvenir: ${WHITE}
${GRAY}- ${GOLD}${BOLD}Griffin Feather: ${WHITE}
${GRAY}- ${GOLD}${BOLD}Coins: ${WHITE}
${GRAY}- ${DARK_GREEN}${BOLD}Turtle Shelmet: ${WHITE}
${GRAY}- ${DARK_GREEN}${BOLD}Tiger Plushie: ${WHITE}
${GRAY}- ${DARK_GREEN}${BOLD}Antique Remedies: ${WHITE}
${GRAY}- ${BLUE}${BOLD}Ancient Claws: ${WHITE}
${GRAY}- ${BLUE}${BOLD}Enchanted Claws: ${WHITE}
${GRAY}- ${BLUE}${BOLD}Enchanted Gold: 
${GRAY}- ${BLUE}${BOLD}Enchanted Iron: 
${GRAY}- ${GRAY}${BOLD}Total Burrows: ${WHITE}
`,
legioncounterExample: `${YELLOW}${BOLD}Legion: ${AQUA}${BOLD}0`,
dianaStatsExample: `${YELLOW}${BOLD}Diana Stats Tracker
${GRAY}- ${LIGHT_PURPLE}${BOLD}Mobs since Inq:
${GRAY}- ${LIGHT_PURPLE}${BOLD}Inqs since Chimera:
${GRAY}- ${GOLD}${BOLD}Minos since Stick:
${GRAY}- ${DARK_PURPLE}${BOLD}Champs since Relic:
`,
};