import { drawRect } from "./../../utils/functions";
import { indexDict, indexDictReverse, allFigures } from "./../../utils/constants";
import { registerWhen } from "./../../utils/variables";
import { newOverlay } from "../../utils/overlays";
import { UIWrappedText } from "../../../Elementa";
import settings from "../../settings";
import { getWorld } from "../../utils/world";

let fossilOverlayObj = newOverlay("fossilSolver", settings.fossilSolver, "fossilExample", "post", "fossilLoc");
let fossilOverlay = fossilOverlayObj.overlay
let fossilPossibleNames = new UIWrappedText("Possible Fossils: ");

fossilPossibleNames.setY((10).pixels());

let fossilProcent = 0;
function checkIfLocationsAreValid(locations, fossilMustBeAt, fossilCantBeAt) {
    const validLocations = [];
    for (const location of locations) {
        let valid = true;
        for (const point of location.coords) {
            if (fossilCantBeAt.some(p => p.x === point.x && p.y === point.y)) {
                valid = false;
                break;
            }
        }
        if (valid) {
            if (fossilProcent != 0) {
                // cut off the decimal places after 1 digit behind the comma
                if (Math.floor((100/location.coords.length)*10) != fossilProcent && Math.floor((100/location.coords.length)*10) != fossilProcent - 1 && Math.floor((100/location.coords.length)*10) != fossilProcent + 1) {
                    // print("locations length: " + location.coords.length);
                    // print("Fossil procent to match: " + Math.floor((100/location.coords.length)*10));
                    // print("Fossil real procent: " + fossilProcent);
                    valid = false;
                }
            }
        }
        if (valid) {
            if (fossilMustBeAt.length > 0) {
                for (const point of fossilMustBeAt) {
                    if (!location.coords.some(p => p.x === point.x && p.y === point.y)) {
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
    let tempList = [];
    const locations = [];
    for (let x = 0; x < mapSize.x; x++) {
        for (let y = 0; y < mapSize.y; y++) {
            const location = [];
            let locObj = {'name': figure.name, 'coords': []};
            for (const point of figure.coords) {
                const newPoint = {'x': point.x + x, 'y': point.y + y };
                if (newPoint.x < mapSize.x && newPoint.y < mapSize.y) {
                    locObj.coords.push(newPoint);
                    location.push(newPoint);
                } else {
                    locObj = {};
                    location.length = 0;
                    break;
                }
            }
            if (location.length > 0) {
                locations.push(locObj);
            }
        }
    }
    

    // kann nicht ausgeklammert werden weil es sonst nicht funktioniert (wegen den globalen variablen)
    // print original figure and all possible locations in the map (empty as "." and filled as "X")
    // console.log("Original figure:");
    // for (let y = 0; y < mapSize.y; y++) {
    //     let row = "";
    //     for (let x = 0; x < mapSize.x; x++) {
    //         row += figure.coords.some(p => p.x === x && p.y === y) ? "X" : ".";
    //     }
    //     // console.log(row);
    // }
    // console.log("Possible locations:");
    for (const location of tempList) {
        for (let y = 0; y < mapSize.y; y++) {
            let row = "";
            for (let x = 0; x < mapSize.x; x++) {
                row += location.coords.some(p => p.x === x && p.y === y) ? "X" : ".";
            }
            // if (locations.length == 6) {
            //     console.log(row);
            // }
        }
        // if (locations.length == 6) {
        //     print(" ");
        // }
    }

    return locations;
}

// test code
// function printRemovedFigure(figure) {
//     tempString = "";
//     console.log("Figure can't be at:");
//     for (let y = 0; y < mapSize['y']; y++) {
//         for (let x = 0; x < mapSize['x']; x++) {
//             if (figure.some(coord => coord.x === x && coord.y === y)) {
//                 tempString += "X";
//             } else {
//                 tempString += ".";
//             }
//         }
//         tempString += " \n";
//     }
//     print("TempString: \n" + tempString);
// }

// first load
// for (const figure of allFigures) {
//     calculateLocations(figure);
// }

register("chat", () => {
    // print("Excavation complete")
    fossilFoundAt = [];
    fossilFoundAtIndex = [];
    noFossilAt = [];
    noFossilAtIndex = [];
    coordsAdded = [];
    fossilProcent = 0;
    check1 = true;
    calcNewCoords()
}).setCriteria("&r&cYou didn't find anything. Maybe next time!&r");

register("chat", () => {
    // print("Excavation complete")
    fossilFoundAt = [];
    fossilFoundAtIndex = [];
    noFossilAt = [];
    noFossilAtIndex = [];
    coordsAdded = [];
    fossilProcent = 0;
    check1 = true;
    calcNewCoords()
}).setCriteria("&r  &r&6&lEXCAVATION COMPLETE &r");

let allPossibleLocations = [];
// create all possible locations for all figures
for (let figur of allFigures) {
    allPossibleLocations.push(calculateLocations(figur, mapSize));
}

let slotsToHighlight = []
let fossilFoundAtIndex = [];
let noFossilAtIndex = [];
let anzahlPositions = 0;
function calcNewCoords() {
    // let allFossilCoords = [];
    let tempList = [];
    let counter = {};
    let validLocations = [];
    anzahlPositions = 0;
    slotsToHighlight = [];
    let possibleFossils = [];
    // print("allPossibleLocations: " + allPossibleLocations.length)
    for (let pos of allPossibleLocations) {
        tempList = checkIfLocationsAreValid(pos, fossilFoundAt, noFossilAt);
        for (let pos of tempList) {
            // check if figure name is already in possibleFossils
            if (!possibleFossils.includes(pos.name)) {
                possibleFossils.push(pos.name);
            }
            validLocations.push(pos);
            anzahlPositions++;
            for (let p of pos.coords) {
                // allFossilCoords.push(p);
                // print("Fossil at: " + p.x + " " + p.y);
                let index = indexDict[`${p.x}${p.y}`];
                if (!fossilFoundAtIndex.includes(index) && !noFossilAtIndex.includes(index)) {
                    if (counter.hasOwnProperty(index)) {
                        counter[index]++;
                    }
                    else {
                        counter[index] = 1;
                    }
                }
            };
        }
    };



    // print("FossilMustBeAt: " + fossilFoundAt.length)
    // print("FossilCantBeAt: " + noFossilAt.length)
    // console.log("Figure must be at:");
    // let tempString = "";
    // for (let y = 0; y < mapSize['y']; y++) {
    //     for (let x = 0; x < mapSize['x']; x++) {
    //         if (fossilFoundAt.some(coord => coord.x === x && coord.y === y)) {
    //             tempString += "O";
    //         } else {
    //             tempString += ".";
    //         }
    //     }
    //     tempString += " \n";
    // }
    // // print("TempString: \n" + tempString);
    // tempString = "";
    // console.log("Figure can't be at:");
    // for (let y = 0; y < mapSize['y']; y++) {
    //     for (let x = 0; x < mapSize['x']; x++) {
    //         if (noFossilAt.some(coord => coord.x === x && coord.y === y)) {
    //             tempString += "X";
    //         } else {
    //             tempString += ".";
    //         }
    //     }
    //     tempString += " \n";
    // }
    // print("TempString: \n" + tempString);

    // print index with most fossils
    // print complete counter
    // for (let key in counter) {
    //     print("Index: " + key + " Fossils: " + counter[key]);
    // }
    let max = 0;
    let slotToHighlight = -1;
    if (anzahlPositions >= 1) {
        for (let key in counter) {
            if (counter[key] > 0) {
                if (counter[key] > max) {
                    max = counter[key];
                    slotToHighlight = parseInt(key);
                }
            }
        }
        // all slots with the most fossils
        if (settings.highlightAllSlots) {
            for (let key in counter) {
                if (counter[key] == max) {
                    slotsToHighlight.push(parseInt(key));
                }
            }
        }
        else {
            // only highlight the first slot with the most fossils
            if (slotToHighlight != -1) {
                slotsToHighlight.push(slotToHighlight);
            }
        }
    }

    if (possibleFossils.length > 1) {
        let tempStringNames = "Possible Fossils: \n";
            possibleFossils.forEach((fossil) => {
                tempStringNames += fossil + " \n";
            });
        fossilPossibleNames.setText(tempStringNames);
    }
    else if (possibleFossils.length == 1) {
        fossilPossibleNames.setText("Fossil: " + possibleFossils[0]);
    }
    else {
        fossilPossibleNames.setText("Fossil: No Fossil");
    }
    // print each possible figure

    // print("Anzahl Positionen: " + anzahlPositions);
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

// guiClick new
let isInExcavatorGui = false;
let check1 = true;
registerWhen(register("step", () => {
    if (middleBool) return;
    let check2 = false;
    const container = Player.getContainer();
    if (container == null) return;
    if (container.getName() != "Fossil Excavator") return; 
    if (!fossilOverlay.children.includes(fossilPossibleNames)) {
        fossilOverlay.addChild(fossilPossibleNames);
    }
    isInExcavatorGui = false;
    const items = container.getItems();
    for (let i = 0; i < items.length; i++) {
        if (items[i] == null) continue;
        if (items[i].getName() == "§6Dirt") {
            isInExcavatorGui = true;
            break;
        }
    }
    if (!isInExcavatorGui) return;
    items.forEach((item, index) => {
        if (index > 53) return;
        if (item == null) {
            if (!noFossilAtIndex.includes(index)) {
                let xy = indexDictReverse[index];
                noFossilAt.push({'x': parseInt(xy[0]), 'y': parseInt(xy[1]) });
                noFossilAtIndex.push(index);
                // print("No Fossil at: " + index);
                check2 = true;
            }
        }
        else {
            if (item.getName() == "§6Fossil") {
                if (!fossilFoundAtIndex.includes(index)) {
                    if (fossilProcent == 0) {
                        // print("Fossil procents: " + item.getLore()[6]);
                        let procentString = item.getLore()[6];
                        fossilProcent = parseInt(procentString.substring(procentString.indexOf("§c") + 2, procentString.indexOf("%")).replace(".", ""));
                        if (fossilProcent == 10) {
                            fossilProcent *= 10;
                        }
                        // print("Fossil procents: " + fossilProcent);
                    }
                    if (noFossilAtIndex.includes(index)) {
                        let indexToRemove = noFossilAtIndex.indexOf(index);
                        noFossilAt.splice(indexToRemove, 1);
                        noFossilAtIndex.splice(indexToRemove, 1);
                    }
                    let xy = indexDictReverse[index];
                    fossilFoundAt.push({'x': parseInt(xy[0]), 'y': parseInt(xy[1]) });
                    fossilFoundAtIndex.push(index); 
                    // print("Fossil at: " + index);
                    check2 = true;
                }
            }
            else {
                if (item.getName() != "§6Dirt") {
                    if (!noFossilAtIndex.includes(index)) {
                        let xy = indexDictReverse[index];
                        noFossilAt.push({'x': parseInt(xy[0]), 'y': parseInt(xy[1]) });
                        noFossilAtIndex.push(index);
                        // print("No Fossil at: " + index);
                        check2 = true;
                    }
                }
            }
        }
    });
    noFossilAtIndex.forEach((index) => {
        let itemAtIndex = items[index];
        if (itemAtIndex == null) return;
        if (itemAtIndex.getName() == "§6Fossil" || itemAtIndex.getName() == "§6Dirt") {
            let indexToRemove = noFossilAtIndex.indexOf(index);
            noFossilAt.splice(indexToRemove, 1);
            noFossilAtIndex.splice(indexToRemove, 1);
            check2 = true;
        }
    });
    fossilFoundAtIndex.forEach((index) => {
        let itemAtIndex = items[index];
        if (itemAtIndex == null) return;
        if (itemAtIndex.getName() != "§6Fossil") {
            let indexToRemove = fossilFoundAtIndex.indexOf(index);
            fossilFoundAt.splice(indexToRemove, 1);
            fossilFoundAtIndex.splice(indexToRemove, 1);
            check2 = true;
        }
    });
    if (check2) {
        if (check1) {
            check1 = false;
            setTimeout(() => {
                check1 = true;
                calcNewCoords()
            }, 200);
        }
    }
}).setFps(10), () => settings.fossilSolver && getWorld() == "Dwarven Mines");


register("guiClosed", () => {
    fossilOverlay.clearChildren();
})

registerWhen(register("renderSlot", (slot) => {
    const container = Player.getContainer();
    if (container == null) return;
    if (container == undefined) return;
    if (container.getName() == "Fossil Excavator") {
        let item = slot.getItem();
        if (item == null) return;
        if (item.getName() == "§6Dirt" || item.getName() == "§6Fossil") {

            if (slotsToHighlight.includes(slot.getIndex())) {
                if (fossilFoundAtIndex.includes(slot.getIndex())) return;

                let x = slot.getDisplayX() 
                let y = slot.getDisplayY() 
                drawRect(x, y, 2.5, 200);
            }
        };
    }
}), () => settings.fossilSolver && getWorld() == "Dwarven Mines");

let middleBool = false;
register("guiMouseClick", () => {
    if (middleBool) return;
    middleBool = true;
    setTimeout(() => {
        middleBool = false;
    }, 50);
})
