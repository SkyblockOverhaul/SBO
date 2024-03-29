/// <reference types="../CTAutocomplete" />
import Settings from "./settings";
import "./features/general/pickuplog";
import "./features/diana/DianaMobDetect";
// import "./features/Diana/DianaWaypoints";
import "./features/guis/BobberCounter";
import "./features/general/PartyCommands";
import "./features/general/messageHider";
import "./features/general/Waypoints";
import "./features/diana/DianaBurrowDetect";
import "./features/slayer/BlazeSlayer";
import "./features/general/CopyMessage";

// in sbo addons packen
import "./features/general/QOL";
import "./features/guis/SlayerGuis";
import "./features/dungeon/recognizeRareRoom";
import "./features/general/alphaCheck";
import "./utils/overlays";

import { indexDict, indexDictReverse, allFigures } from "./utils/constants";



register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");

// Title bug fix
register("worldLoad", () => {
    Client.showTitle("", "", 0, 40, 20);
});

register("chat", (event) => {
    Client.showTitle("&l&9!!!!!WORM!!!!!!", "&eKILL!!!", 0, 90, 20);
}).setCriteria("&r&7&oYou hear the sound of something approaching...&r");

register("chat", (message, event) => {
    message = message.removeFormatting();
    if (!message.includes("Powder") && !message.includes("Refelctor") && !message.includes("Blue Goblin Egg") && !message.includes("Heart")) {
        cancel(event);
    }
    if (message.includes("Refelctor")) {
        Client.showTitle("&9Robotron Reflector", "&eCarrot", 0, 40, 20);
    }
    if (message.includes("Blue Goblin Egg")) {
        Client.showTitle("&3Blue Goblin Egg", "&eCarrot", 0, 40, 20);
    }
}).setCriteria("&r&aYou received ${message}");

register("chat", (player, message, event) =>{
    // cancel original message
    // send new guildbot message
    if (!player.includes(" ")) {
        cancel(event);
        player = player.removeFormatting();
        ChatLib.chat("&r&2Guild > &b[DC] &b" + player + "&r:" + message);
        // print("&r&2Guild > &b[DC] &b" + player + "&r:" + message);
    }
    else if (player.includes("replying to")) {
        cancel(event);
        let split = player.split(" ");
        let player1 = split[0];
        let player2 = split[3];
        ChatLib.chat("&r&2Guild > &b[DC] &b" + player1 + " &3replying to &b" + player2 + "&r:" + message);
        // print("&r&2Guild > &b[DC] &b" + player1 + " &3replying to &b" + player2 + "&r:" + message);
    }
}).setCriteria("&r&2Guild > &a[VIP] SlowDT &3[GM]&f: ${player}:${message}").setContains()
// geht
// &r&2Guild > &a[VIP] SlowDT &3[GM]&f: &rSuccesfully invited kenchika to the party!&r
// &r&2Guild > &b[MVP&2+&b] MasterNR &3[320]&f: &rnice&r
// testen
// &r&2Guild > &a[VIP] SlowDT &3[GM]&f: &rWiggleSnakey replying to dtAxl: WWDYM&r 

// register("command", () => {
//     // Client.showTitle(`&r&6&l<&b&l&kO&6&l> &b&lINQUISITOR! &6&l<&b&l&kO&6&l>`, "&r&b[MVP&f+&b] RolexDE", 0, 90, 20);
//     Client.showTitle(`&5&lMinos Relic!`, "", 0, 25, 35);
//     ChatLib.chat("&6[SBO] &r&6&lRARE DROP! &5Minos Relic!");
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.0);
//    }, 0);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.2);
//    }, 50);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.4);
//    }, 100);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.6);
//    }, 150);
// }).setName("sboinq");

// todo
// alle anderen figuren einbauen
// slots besser highlighten
// fossilMustBe geht nicht
// hits und miss besser auslesen

function checkIfLocationsAreValid(locations, fossilMustBeAt, fossilCantBeAt) {
    const validLocations = [];
    
    // for (let i = 0; i < fossilMustBeAt.length; i++) {
    //     print("FossilMustBeAt: " + fossilMustBeAt[i].x + " " + fossilMustBeAt[i].y);
    // }
    // for (let i = 0; i < fossilCantBeAt.length; i++) {
    //     print("FossilCantBeAt: " + fossilCantBeAt[i].x + " " + fossilCantBeAt[i].y);
    // }

    for (const location of locations) {
        let valid = true;
        for (const point of location) {
            if (fossilCantBeAt.some(p => p.x === point.x && p.y === point.y)) {
                valid = false;
                // printRemovedFigure(location);
                break;
            }
        }
        if (valid) {
            if (fossilMustBeAt.length > 0) {
                for (const point of fossilMustBeAt) {
                    if (!location.some(p => p.x === point.x && p.y === point.y)) {
                        valid = false;
                        break;
                    }
                }
            }
        }
        if (valid) {
            validLocations.push(location);
        }
    }

    return validLocations;
}

// calculate all possible locations of a figure in the map (must fit in the map and not overlap with the map borders but can overlap with other figures)
let fossilFoundAt = [];
let noFossilAt = [];
const mapSize = {'x': 9, 'y': 6 };
function calculateLocations(figure) {
    const locations = [];
    for (let x = 0; x < mapSize.x; x++) {
        for (let y = 0; y < mapSize.y; y++) {
            const location = [];
            for (const point of figure) {
                const newPoint = {'x': point.x + x, 'y': point.y + y };
                if (newPoint.x < mapSize.x && newPoint.y < mapSize.y) {
                    location.push(newPoint);
                } else {
                    location.length = 0;
                    break;
                }
            }
            if (location.length > 0) {
                locations.push(location);
            }
        }
    }
    const validLocations = checkIfLocationsAreValid(locations, fossilFoundAt, noFossilAt);
    // print original figure and all possible locations in the map (empty as "." and filled as "X")
    // console.log("Original figure:");
    for (let y = 0; y < mapSize.y; y++) {
        let row = "";
        for (let x = 0; x < mapSize.x; x++) {
            row += figure.some(p => p.x === x && p.y === y) ? "X" : ".";
        }
        // console.log(row);
    }
    // console.log("Possible locations:");
    for (const location of validLocations) {
        for (let y = 0; y < mapSize.y; y++) {
            let row = "";
            for (let x = 0; x < mapSize.x; x++) {
                row += location.some(p => p.x === x && p.y === y) ? "X" : ".";
            }
            // console.log(row);
        }
        // print(" ");
    }

    return validLocations;
}

function printRemovedFigure(figure) {
    tempString = "";
    console.log("Figure can't be at:");
    for (let y = 0; y < mapSize['y']; y++) {
        for (let x = 0; x < mapSize['x']; x++) {
            if (figure.some(coord => coord.x === x && coord.y === y)) {
                tempString += "X";
            } else {
                tempString += ".";
            }
        }
        tempString += " \n";
    }
    print("TempString: \n" + tempString);
}

// first load
// for (const figure of allFigures) {
//     calculateLocations(figure);
// }

register("chat", () => {
    print("Excavation complete")
    fossilFoundAt = [];
    fossilFoundAtIndex = [];
    noFossilAt = [];
    noFossilAtIndex = [];
    coordsAdded = [];
    calcNewCoords()
}).setCriteria("&r&cYou didn't find anything. Maybe next time!&r");

register("chat", () => {
    print("Excavation complete")
    fossilFoundAt = [];
    fossilFoundAtIndex = [];
    noFossilAt = [];
    noFossilAtIndex = [];
    coordsAdded = [];
    calcNewCoords()
}).setCriteria("&r  &r&6&lEXCAVATION COMPLETE &r");

let slotsToHighlight = []
let fossilFoundAtIndex = [];
let noFossilAtIndex = [];
function calcNewCoords() {
    let allFossilCoords = [];
    let counter = {};
    let anzahlPositions = 0;
    let tempList = [];
    slotsToHighlight = [];
    for (let figur of allFigures) {
        tempList = calculateLocations(figur, mapSize);
        for (let pos of tempList) {
            anzahlPositions++;
            for (let p of pos) {
                allFossilCoords.push(p);
                // print("Fossil at: " + p.x + " " + p.y);
                let index = indexDict[`${p.x}${p.y}`];
                if (index == undefined) {
                    print("Index undefined: " + p.x + " " + p.y);
                }
                if (!fossilFoundAtIndex.includes(index) && !noFossilAtIndex.includes(index)) {
                    if (counter.hasOwnProperty(index)) {
                        counter[index]++;
                    }
                    else {
                        counter[index] = 1;
                    }
                }
            };
        };
    };


    print("FossilMustBeAt: " + fossilFoundAt.length)
    print("FossilCantBeAt: " + noFossilAt.length)
    console.log("Figure must be at:");
    let tempString = "";
    for (let y = 0; y < mapSize['y']; y++) {
        for (let x = 0; x < mapSize['x']; x++) {
            if (fossilFoundAt.some(coord => coord.x === x && coord.y === y)) {
                tempString += "O";
            } else {
                tempString += ".";
            }
        }
        tempString += " \n";
    }
    print("TempString: \n" + tempString);
    tempString = "";
    console.log("Figure can't be at:");
    for (let y = 0; y < mapSize['y']; y++) {
        for (let x = 0; x < mapSize['x']; x++) {
            if (noFossilAt.some(coord => coord.x === x && coord.y === y)) {
                tempString += "X";
            } else {
                tempString += ".";
            }
        }
        tempString += " \n";
    }
    print("TempString: \n" + tempString);

    print("TempList: " + tempList.length)
    print("AllFossilCoords: " + allFossilCoords.length)
    print("figure: " + allFigures.length)

    
    
    // print index with most fossils
    // print complete counter
    // for (let key in counter) {
    //     print("Index: " + key + " Fossils: " + counter[key]);
    // }
    let max = 0;
    let slotToHighlight = 0;
    if (anzahlPositions > 1) {
        for (let key in counter) {
            if (counter[key] > max) {
                max = counter[key];
                slotToHighlight = parseInt(key);
            }
        }
        slotsToHighlight.push(slotToHighlight);
    }

    print("Anzahl Positionen: " + anzahlPositions);
    if (fossilFoundAt.length > 0) {
        if (anzahlPositions == 1) {
            // print all indexes of the only possible figure
            for (let key in counter) {
                if (counter[key] == 1) {
                    slotsToHighlight.push(parseInt(key));
                }
            }
        }
    }
}
calcNewCoords()

let coordsAdded = [];
register("guiMouseClick", () => {
    let slot = Client.currentGui.getSlotUnderMouse()
    if (slot == null) return;
    let index = slot.getIndex();
    if (index > 53) return;
    const container = Player.getContainer();
    if (container == null) return;
    if (container.getName() != "Fossil Excavator") return; 
    let item = container.getStackInSlot(index);
    if (item == null) return;
    if (item.getName() == "§6Dirt") {
        setTimeout(() => {
            item = container.getStackInSlot(index);
            if (item == null) {
                let xy = indexDictReverse[index];
                noFossilAt.push({'x': parseInt(xy[0]), 'y': parseInt(xy[1]) });
                noFossilAtIndex.push(index);
                print("No Fossil at: " + index);
            }
            else {
                if (item.getName() == "§6Fossil") {
                    let xy = indexDictReverse[index];
                    fossilFoundAt.push({'x': parseInt(xy[0]), 'y': parseInt(xy[1]) });
                    fossilFoundAtIndex.push(index); 
                    print("Fossil at: " + index);
                }
                else {
                    let xy = indexDictReverse[index];
                    noFossilAt.push({'x': parseInt(xy[0]), 'y': parseInt(xy[1]) });
                    noFossilAtIndex.push(index);
                    print("No Fossil at: " + index);
                }
            }
            calcNewCoords()
        }, 300);
    }
});

register("renderSlot", (slot) => {
    const container = Player.getContainer();
    if (container == null) return;
    if (container == undefined) return;
    if (container.getName() == "Fossil Excavator") {
        let item = slot.getItem();
        if (item == null) return;
        if (item.getName() == "§6Dirt" || item.getName() == "§6Fossil") {
            // if (slot.getIndex() == slotToHighlight) {
            //     let x = slot.getDisplayX() + 1.7;
            //     let y = slot.getDisplayY() - 3.6;
            //     drawOutlinedString("x", x, y, 2.5, 500)
            // }
            if (slotsToHighlight.includes(slot.getIndex())) {
                let x = slot.getDisplayX() + 1.7;
                let y = slot.getDisplayY() - 3.6;
                drawOutlinedString("x", x, y, 2.5, 500)
            }
        };
    }
});

function drawOutlinedString(text,x1,y1,scale,z) {
    let outlineString = "&0" + ChatLib.removeFormatting(text)
    let x = x1/scale
    let y = y1/scale

    Renderer.translate(0,0,z)
    Renderer.scale(scale,scale)
    Renderer.drawString(outlineString, x + 1, y)

    Renderer.translate(0,0,z)
    Renderer.scale(scale,scale)
    Renderer.drawString(outlineString, x - 1, y)

    Renderer.translate(0,0,z)
    Renderer.scale(scale,scale)
    Renderer.drawString(outlineString, x, y + 1)

    Renderer.translate(0,0,z)
    Renderer.scale(scale,scale)
    Renderer.drawString(outlineString, x, y - 1)

    Renderer.translate(0,0,z)
    Renderer.scale(scale,scale)
    Renderer.drawString(text, x, y)
}