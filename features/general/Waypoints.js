import renderBeaconBeam from "../../../BeaconBeam/index";
import RenderLibV2 from "../../../RenderLibV2";
import settings from "../../settings";
import { Keybind } from "../../../KeybindFix"
import { checkDiana } from "../../utils/checkDiana";
import { isInSkyblock, isWorldLoaded, playCustomSound, setTimeout, toTitleCase, trace } from '../../utils/functions';
import { registerWhen } from "../../utils/variables";
import { getFinalLocation, getLastGuessTime, setFinalLocation } from "../Diana/DianaGuess";
import { Color } from '../../../Vigilance';
import { inqHighlightRegister } from "../Diana/DianaMobDetect";

let patcherWaypoints = [];
export function getPatcherWaypoints() { 
    return patcherWaypoints 
};

let inqWaypoints = [];

let burrowWaypoints = [];
export function getBurrowWaypoints() {
    return burrowWaypoints;
}

export function setBurrowWaypoints(burrows) {
    burrowWaypoints = burrows;
}

let worldWaypoints = [];
export function createWorldWaypoint(name, x, y, z, r, g, b, line = false, beam = true, distance = true) {
    // check if x y z are already in worldWaypoints
    if (worldWaypoints.some(([_, wx, wy, wz]) => wx === x && wy === y && wz === z)) return;
    worldWaypoints.push([name, x, y, z, "", r, g, b, line, beam, distance]);
}

export function removeWorldWaypoint(x, y, z) {
    worldWaypoints = worldWaypoints.filter(([_, wx, wy, wz]) => wx !== x || wy !== y || wz !== z);
}
    
register("worldUnload", () => {
    worldWaypoints = [];
})


export function removeBurrowWaypoint(pos, burrows) {
    let x = pos.getX();
    let y = pos.getY();
    let z = pos.getZ();
    let removedBurrowstring = null;

    for (let i = 0; i < burrowWaypoints.length; i++) {
        if (burrowWaypoints[i][1] == x && burrowWaypoints[i][2] == y && burrowWaypoints[i][3] == z) {
            removedBurrowstring = x + " " + (y - 1) + " " + z; 
            burrowWaypoints.splice(i, 1);
        }
    }
    // burrows = burrows.filter(([_, bx, by, bz]) => bx !== closetburrow[1] || by !== closetburrow[2] || bz !== closetburrow[3] );
    const posstring = `${x} ${y-1} ${z}`;
    delete burrows[posstring];
    return {burrows: burrows, removedBurrow: removedBurrowstring};
}

export function removeInqWaypoint(x, y, z) {
    for (let i = 0; i < inqWaypoints.length; i++) {
        if (inqWaypoints[i][1] == x && inqWaypoints[i][2] == y && inqWaypoints[i][3] == z) {
            inqWaypoints.splice(i, 1);
        }
    }
}

export function removeBurrowWaypointBySmoke(x, y, z) {
    let removedBurrowstring = null;
    for (let i = 0; i < burrowWaypoints.length; i++) {
        if (burrowWaypoints[i][1] == x && burrowWaypoints[i][2] == y && burrowWaypoints[i][3] == z) {
            removedBurrowstring = x + " " + (y - 1) + " " + z;
            burrowWaypoints.splice(i, 1);
        }
    }
    return removedBurrowstring;
}

function removeWaypointAfterDelay(Waypoints, seconds) {
    setTimeout(() => {
        Waypoints.shift();
    }, seconds*1000);
} 

function numberToBurrowType(number) {
    switch (number) {
        case 0:
            return "Start";
        case 1:
            return "Mob";
        case 2:
            return "Treasure";
    }
}

export function createBurrowWaypoints(burrowType, x, y, z, burrowshistory, xyzcheck) {
    if (!burrowshistory.some(([type, xb, yb, zb]) => xb === x && yb === y && zb === z)) {
        if (burrowWaypoints.length > 0) {
            if (burrowWaypoints.some(([type, xb, yb, zb]) => xb === x && yb === y && zb === z)) return; 
            burrowWaypoints.push([numberToBurrowType(burrowType), x, y, z, "", xyzcheck]);
            playCustomSound(settings.burrowSound, settings.burrowVolume);   
        }
        else {
            playCustomSound(settings.burrowSound, settings.burrowVolume);
            burrowWaypoints.push([numberToBurrowType(burrowType), x, y, z, "", xyzcheck]);
        }
    }
}

class AxisAlignedBB {
    constructor(minX, minY, minZ, maxX, maxY, maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.x = minX + (maxX - minX) / 2;
        this.y = minY + (maxY - minY) / 2;
        this.z = minZ + (maxZ - minZ) / 2;
    }
}
let guessRemovedAt = {x: 10000, y: 10000, z: 10000};
function formatWaypoints(waypoints, r, g, b, type = "Normal") {
    if (!waypoints.length) return;
    let x, y, z, distanceRaw, xSign, zSign = 0;

    
    waypoints.forEach((waypoint) => {
        let beam = true;
        let distancBool = true
        if (type == "Burrow") {
            switch (waypoint[0]) {
                case "Start":
                    waypoint[0] = "Start";
                    r = settings.startColor.getRed()/255;
                    g = settings.startColor.getGreen()/255;
                    b = settings.startColor.getBlue()/255;
                    break;
                case "Mob":
                    r = settings.mobColor.getRed()/255;
                    g = settings.mobColor.getGreen()/255;
                    b = settings.mobColor.getBlue()/255;
                    break;
                case "Treasure":
                    r = settings.treasureColor.getRed()/255;
                    g = settings.treasureColor.getGreen()/255;
                    b = settings.treasureColor.getBlue()/255;
                    break;
            }
        }
        else if (type == "world") {
            r = waypoint[5]/255;
            g = waypoint[6]/255;
            b = waypoint[7]/255;
            beam = waypoint[9];
            distancBool = waypoint[10];
        }

        if (waypoint[4] == undefined) {
            waypoint[4] = "";
        }

        wp = [["", 0, 0, 0], [0, 0, 0], [r, g, b]];
        x = Math.round(waypoint[1]);
        y = Math.round(waypoint[2]);
        z = Math.round(waypoint[3]);

        distanceRaw = Math.hypot(Player.getX() - x, Player.getY() - y, Player.getZ() - z);

        // Makes it so waypoint always renders
        if (distanceRaw >= 230) {
            x = Player.getX() + (x - Player.getX()) * (230 / distanceRaw);
            z = Player.getZ() + (z - Player.getZ()) * (230 / distanceRaw);
        }

        // Formats and realins everything
        distance = Math.round(distanceRaw) + "m";
        if (type == "Burrow") {
            if (waypoint[5][0] > 0) {
                xSign = 1;
            }
            else if (waypoint[5][0] < 0) {
                xSign = -1;
            }
            if (waypoint[5][2] > 0) {
                zSign = 1;
            }
            else if (waypoint[5][2] < 0) {
                zSign = -1;
            }
        }
        else {
            xSign = x == 0 ? 1 : Math.sign(x);
            zSign = z == 0 ? 1 : Math.sign(z);
        }
        
        if (type != "Guess") {
            if (distancBool)
                wp[0] = [`${waypoint[0]}§7${waypoint[4]} §b[${distance}]`, x + 0.5*xSign, y - 1, z + 0.5*zSign, distanceRaw, beam, distancBool];
            else
                wp[0] = [`${waypoint[0]}§7${waypoint[4]}`, x + 0.5*xSign, y - 1, z + 0.5*zSign, distanceRaw, beam, distancBool];
            if (xSign == 1) xSign = 0;
            if (zSign == 1) zSign = 0;
            wp[1] = [x + xSign, y - 1, z + zSign];
        }
        else {
            aabb = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
            wp[0] = [`${waypoint[0]}§7${waypoint[4]} §b[${distance}]`, aabb.x, aabb.y - 0.5, aabb.z, distanceRaw, beam, distancBool];
            wp[1] = [aabb.x - 0.5, aabb.y - 1, aabb.z - 0.5];
        }

        if (type == "Guess") {
            formattedGuess.push(wp);
        }
        else if (type == "Normal" || type == "world") {
            formatted.push(wp);
        }
        else if (type == "Burrow") {
            formattedBurrow.push(wp);
            if (formattedGuess.length == 0) return;
            if (formattedGuess[0][0][1] == wp[0][1] && formattedGuess[0][0][2] == wp[0][2] && formattedGuess[0][0][3] == wp[0][3]) {
                if (!settings.removeGuessWhenBurrow) return;
                if (finalLocation.distanceTo(Player) >= 80) return; 
                guessRemovedAt = {x: finalLocation.x, y: finalLocation.y, z: finalLocation.z};
                setFinalLocation(null);
                guessWaypoint = undefined;
            }
        }
    });
}

function closestWarpString(x, y, z) {
    let warpString = "";
    closestWarp = getClosestWarp(x, y, z);
    if (!closestWarp) {
        warpString = "";
    }
    else {
        warpString = ` (warp ${closestWarp})`;
    }
    return warpString;
}

let guessWaypointString = "";
let hubWarps = {
    castle: {x: -250, y: 130, z: 45, unlocked: true},
    da: {x: 92, y: 75, z: 174, unlocked: true},
    hub: {x: -3, y: 70, z: -70, unlocked: true},
    museum: {x: -76, y: 76, z: 81, unlocked: true},
};

let closestWarpGuess = undefined;
let closestWarpInq = undefined;
let warpedTo = ""
let tryWarp = false;
const warpKey = new Keybind("Burrow Warp", Keyboard.KEY_NONE, "SkyblockOverhaul");
warpKey.registerKeyPress(() => {
    if (settings.dianaBurrowWarp && finalLocation != null) { 
        if (settings.warpDelay && Date.now() - getLastGuessTime() < settings.warpDelayTime) return;
        closestWarpGuess = getClosestWarp(finalLocation.x, finalLocation.y, finalLocation.z, "guess");
        if (closestWarpGuess && !tryWarp) {
            ChatLib.command("warp " + closestWarpGuess);
            warpedTo = closestWarpGuess;
            tryWarp = true;
            setTimeout(() => {
                tryWarp = false;
            }, 2000);
        }
    }
});

const inquisWarpKey = new Keybind("Iqnuis Warp", Keyboard.KEY_NONE, "SkyblockOverhaul");
inquisWarpKey.registerKeyPress(() => {
    if (settings.inqWarpKey) {
        if (inqWaypoints.length > 0) {
            closestWarpInq = getClosestWarp(inqWaypoints[inqWaypoints.length - 1][1], inqWaypoints[inqWaypoints.length - 1][2], inqWaypoints[inqWaypoints.length - 1][3], "inq");
            if (closestWarpInq && !tryWarp) {
                ChatLib.command("warp " + closestWarpInq);
                warpedTo = closestWarpInq;
                tryWarp = true;
                setTimeout(() => {
                    tryWarp = false;
                }, 2000);
            }
        }
    }
});



let closestDistance = Infinity;
function getClosestWarp(x, y, z, type) {
    let closestWarp = "";
    const closestPlayerdistance = Math.sqrt(
        (Player.getLastX() - x)**2 +
        (Player.getLastY() - y)**2 +
        (Player.getLastZ() - z)**2
    );
    closestDistance = Infinity;

    switch (settings.dianaAddWarps) {
        case 0:
            delete hubWarps.wizard;
            delete hubWarps.crypt;
            break;
        case 1:
            hubWarps.wizard = {x: 42, y: 122, z: 69, unlocked: true}
            delete hubWarps.crypt;
            break;
        case 2:
            hubWarps.crypt = {x: -161, y: 61, z: -99, unlocked: true}
            delete hubWarps.wizard;
            break;
        case 3:
            hubWarps.wizard = {x: 42, y: 122, z: 69, unlocked: true}
            hubWarps.crypt = {x: -161, y: 61, z: -99, unlocked: true}
            break;
    }

    if (settings.stonksWarp) {
        hubWarps.stonks = {x: -53, y: 72, z: -53, unlocked: true}
    } else {
        delete hubWarps.stonks;
    }

    if (settings.darkAuctionWarp) {
        if (hubWarps.da == undefined) {
            hubWarps.da = {x: 92, y: 75, z: 174, unlocked: true}
        }
    } else {
        delete hubWarps.da;
    }

    for (let warp in hubWarps) {
        if (hubWarps[warp].unlocked) {
            let distance = Math.sqrt(
                (hubWarps[warp].x - x)**2 +
                (hubWarps[warp].y - y)**2 +
                (hubWarps[warp].z - z)**2
            );
            if (distance < closestDistance) {
                closestDistance = distance;
                if (type == "guess") {
                    closestWarpGuess = warp;
                } else if (type == "inq") {
                    closestWarpInq = warp;
                }
                closestWarp = warp;
            }
        }
    }
    settings.warpDiff = settings.warpDiff.replace(/\D/g, '');
    let warpDiff = parseInt(settings.warpDiff);

    const warpConditions = {
        condition1: Math.round(parseInt(closestPlayerdistance)) > Math.round(parseInt(closestDistance) + warpDiff),
        condition2: (Math.round(parseInt(closestPlayerdistance)) > Math.round(parseInt(closestDistance) + warpDiff) && (Math.round(getClosestBurrow(formattedBurrow)[1]) > 60 || inqWaypoints.length > 0))
    };
    
    if (settings.dontWarpIfBurrowNearby ? warpConditions.condition2 : warpConditions.condition1) {
        return closestWarp;
    }
    else {
        return false;
    }
}
// check if player got loot share //
register("chat" , (player) => {
    let playerX = Player.getX();
    let playerY = Player.getY();
    let playerZ = Player.getZ();
    function isWithin20BlockRadius(x, y, z) {
        let dx = x - playerX;
        let dy = y - playerY;
        let dz = z - playerZ;
        let distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance <= 20;
    }
    inqWaypoints = inqWaypoints.filter((waypoint) => {
        return !waypoint[0].includes(player.removeFormatting()) && !isWithin20BlockRadius(waypoint[1], waypoint[2], waypoint[3]);
    });
}).setCriteria("&r&e&lLOOT SHARE &r&r&r&fYou received loot for assisting &r${player}&r&f!&r");

// check waypoint
let highlighInquis = false;
register("step", () => {
    if (highlighInquis && (settings.inqHighlight || settings.inqCircle)) { 
        inqHighlightRegister.register(); 
    }
    else { 
        inqHighlightRegister.unregister(); 
    }
    if (isWorldLoaded()) {
        // remvoe each waypoint thats older than 45 seconds
        inqWaypoints = inqWaypoints.filter(([_, _, _, _, _, time]) => Date.now() - time < 45000);
        // patcherWaypoints = patcherWaypoints.filter(([_, _, _, _, time]) => Date.now() - time < 30000);
    }
}).setFps(1);

registerWhen(register("chat", (player, spacing, x, y, z, event) => {
    if (isWorldLoaded()) {
        if (checkDiana() && settings.allWaypointsAreInqs) {
            isInq = true;
        }
        else {
            isInq = !z.includes(" ");
        }
        const bracketIndex = player.indexOf('[') - 2;
        const channel = player.substring(0, bracketIndex);
        // channel.includes("Guild") || channel.includes("Party") || channel.includes("Co-op")
        if (channel.includes("Guild")) return;
        if (bracketIndex >= 0)
            player = player.replaceAll('&', '§').substring(bracketIndex, player.length);
        else
            player = player.replaceAll('&', '§');

        if (isInq) {
            if(settings.inqHighlight || settings.inqCircle && checkDiana()) {
                highlighInquis = true;
                setTimeout(() => {
                    highlighInquis = false;
                }, 80000); // 80 seconds so it only unregisters after inq is 100% dead cause it despawns after 75 secs
            }
            if(settings.inqWaypoints && checkDiana()) {
                Client.showTitle(`&r&6&l<&b&l&kO&6&l> &b&lINQUISITOR! &6&l<&b&l&kO&6&l>`, player, 0, 90, 20);
                playCustomSound(settings.inqSound, settings.inqVolume);
                z = z.replace("&r", "");
                // check if waypoint is from player
                
                if (!(player.includes(Player.getName()) && (settings.hideOwnWaypoints == 1 || settings.hideOwnWaypoints == 3))) {
                    if (z.split(" ").length > 1) {
                        z = z.split(" ")[0];
                    }
                    // print(player + " " + x + " " + y + " " + z);
                    inqWaypoints.push([player, x, y, z, closestWarpString(x, y, z), Date.now()]);
                }
            }
            else{
                z = z.replace("&r", "");
                if (!(player.includes(Player.getName()) && (settings.hideOwnWaypoints == 2 || settings.hideOwnWaypoints == 3))) {
                    patcherWaypoints.push([player, x, y, z, ""]);
                    removeWaypointAfterDelay(patcherWaypoints, 30);
                }
            }
        }
        else {
            if(settings.patcherWaypoints) {
                z = z.split(" ")[0];
                if (!(player.includes(Player.getName()) && (settings.hideOwnWaypoints == 2 || settings.hideOwnWaypoints == 3))) {
                    patcherWaypoints.push([player, x, y, z, ""]);
                    removeWaypointAfterDelay(patcherWaypoints, 30);
                }
            }
        }
    }
}).setCriteria("${player}&f${spacing}x: ${x}, y: ${y}, z: ${z}"), () => settings.patcherWaypoints || settings.inqWaypoints || settings.inqHighlight || settings.inqCircle);

registerWhen(register("chat", () => {
    if (tryWarp) {
        ChatLib.chat("§6[SBO] §4Warp " + toTitleCase(warpedTo) + " is not unlocked!")
        hubWarps[warpedTo].unlocked = false;
    }
}).setCriteria("&r&cYou haven't unlocked this fast travel destination!&r"), () => settings.inqWarpKey);
// wenn scroll ulocked dann diese message &r&eYou may now Fast Travel to &r&aSkyBlock Hub &r&7- &r&bCrypts&r&e!&r

registerWhen(register("step", () => { 
    if (isWorldLoaded() && settings.dianaBurrowWarp) {
        if (finalLocation != null) {
            guessWaypointString = closestWarpString(finalLocation.x, finalLocation.y, finalLocation.z);
        }
        // same for inquis waypoints
        if (settings.inqWaypoints) {
            inqWaypoints.forEach((waypoint) => {
                waypoint[4] = closestWarpString(waypoint[1], waypoint[2], waypoint[3]);
            });
        }
    }
    else {
        guessWaypointString = "";   
        inqWaypoints.forEach((waypoint) => {
            waypoint[4] = "";
        });
    }
}).setFps(3), () => settings.dianaBurrowGuess);;



let lastWaypoint = undefined;
let guessWaypoint = undefined;
let finalLocation = undefined;
registerWhen(register("step", () => {
    formattedGuess = [];
    finalLocation = getFinalLocation();
    if (finalLocation != null && lastWaypoint != finalLocation) {
        if (finalLocation.x == guessRemovedAt.x && finalLocation.y == guessRemovedAt.y && finalLocation.z == guessRemovedAt.z && finalLocation.distanceTo(Player) < 80) {
            setFinalLocation(null);
            return;
        }
        guessWaypoint = [`Guess`, finalLocation.x, finalLocation.y, finalLocation.z, guessWaypointString];
        formatWaypoints([guessWaypoint], settings.guessColor.getRed()/255, settings.guessColor.getGreen()/255, settings.guessColor.getBlue()/255, "Guess");
        lastWaypoint = guessWaypoint;
    }
}).setFps(20), () => settings.dianaBurrowGuess);

let formatted = [];
let formattedGuess = [];
let formattedBurrow = [];
registerWhen(register("step", () => {
    formatted = [];
    formattedBurrow = []
    formatWaypoints(patcherWaypoints, 0, 0.2, 1); 
    formatWaypoints(inqWaypoints, 1, 0.84, 0); 
    formatWaypoints(burrowWaypoints, 0, 0, 0, "Burrow");
    formatWaypoints(worldWaypoints, 0, 0, 0, "world");
    
}).setFps(5), () => settings.dianaBurrowDetect || settings.findDragonNest || settings.inqWaypoints || settings.patcherWaypoints);

registerWhen(register("renderWorld", () => { 
    if (isInSkyblock()) {
        renderWaypoint(formatted);
        renderWaypoint(formattedBurrow);
        renderWaypoint(formattedGuess);
        renderBurrowLines();
        renderWaypointLines();
    }
}), () =>  settings.dianaBurrowDetect || settings.dianaBurrowGuess || settings.findDragonNest || settings.inqWaypoints || settings.patcherWaypoints);

// let guessLineRemoved = false;
function renderBurrowLines() {
    if(burrowWaypoints.length > 0 && settings.burrowLine && inqWaypoints.length == 0) {
        let [closestBurrow, burrowDistance] = getClosestBurrow(formattedBurrow);
        if (burrowDistance < 60) {
            trace(closestBurrow[1], closestBurrow[2] + 1, closestBurrow[3], closestBurrow[4], closestBurrow[5], closestBurrow[6], 0.7, "", parseInt(settings.burrowLineWidth));
        }
    }
    if (inqWaypoints.length > 0 && settings.inqLine) {
        trace(inqWaypoints[inqWaypoints.length - 1][1], parseInt(inqWaypoints[inqWaypoints.length - 1][2]), inqWaypoints[inqWaypoints.length - 1][3], 1, 0.84, 0, 0.7, "calc", parseInt(settings.burrowLineWidth));
    }
    if (guessWaypoint != null && settings.guessLine && inqWaypoints.length == 0) {
        if(getFinalLocation() === null) return;
        x = guessWaypoint[1];
        y = guessWaypoint[2];
        z = guessWaypoint[3];
        aabb = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
        aabb.y = aabb.y + 0.5;
        let [closestBurrow, burrowDistance] = getClosestBurrow(formattedBurrow);
        if (burrowDistance > 60 && guessDistance(aabb.x, aabb.y, aabb.z) > parseInt(settings.removeGuessDistance)) {
            trace(aabb.x, aabb.y, aabb.z, settings.guessColor.getRed()/255, settings.guessColor.getGreen()/255, settings.guessColor.getBlue()/255, 0.7, "", parseInt(settings.burrowLineWidth));
        }
    }
}

function renderWaypointLines() {
    if (worldWaypoints.length > 0) {
        worldWaypoints.forEach((waypoint) => {
            if (waypoint[8]) {
                trace(waypoint[1], waypoint[2], waypoint[3], waypoint[5]/255, waypoint[6]/255, waypoint[7]/255, 0.7, "calc", 2);
            }
        }
    )}
}


function guessDistance(x,y,z) {
    return Math.sqrt(
        (Player.getX() - x)**2 +
        (Player.getY() - y)**2 +
        (Player.getZ() - z)**2
    );
}

function getClosestBurrow(formattedBurrow) {
    let closestDistance = Infinity;
    let closestBurrow = null;
    formattedBurrow.forEach((waypoint) => {
        const distance = Math.sqrt(
            (Player.getX() - waypoint[0][1])**2 +
            (Player.getY() - waypoint[0][2])**2 +
            (Player.getZ() - waypoint[0][3])**2
        );
        if (distance < closestDistance) {
            closestDistance = distance;
            closestBurrow = ["type", waypoint[0][1], waypoint[0][2], waypoint[0][3], waypoint[2][0], waypoint[2][1], waypoint[2][2]];
        }
    });
    return [closestBurrow, closestDistance];
}

function renderWaypoint(waypoints) {
    if (!waypoints.length) return;

    waypoints.forEach((waypoint) => {
        box = waypoint[0];
        beam = waypoint[1];
        rgb = waypoint[2];
        let removeAtDistance = 10;
        let alpha = 0.5;
        if (box[4] <= settings.removeGuessDistance && box[0].includes("Guess") && settings.removeGuess) return;
        if (!settings.removeGuess && box[0].includes("Guess")) {
            removeAtDistance = 0;
        }
        // RenderLibV2.drawEspBoxV2(box[1], box[2], box[3], 1, 1, 1, rgb[0], rgb[1], rgb[2], 1, true);
        RenderLibV2.drawInnerEspBoxV2(box[1], box[2], box[3], 1, 1, 1, rgb[0], rgb[1], rgb[2], alpha/2, true);
        let hexCodeString = javaColorToHex(new Color(rgb[0], rgb[1], rgb[2]));
        if (box[0] != "" && box[0] != "§7") {
            Tessellator.drawString(box[0], box[1], box[2] + 1.5, box[3], parseInt(hexCodeString, 16), true);
        }

        if (box[4] >= removeAtDistance && box[5]) {
            renderBeaconBeam(beam[0], beam[1]+1, beam[2], rgb[0], rgb[1], rgb[2], alpha, false);
        }
    });
}

function javaColorToHex(javaColor) {
    // Extract RGB components
    let red = javaColor.getRed();
    let green = javaColor.getGreen();
    let blue = javaColor.getBlue();

    // Convert RGB to hexadecimal
    let hex = "0x" + componentToHex(red) + componentToHex(green) + componentToHex(blue);

    return hex;
}

// Helper function to convert a single color component to hexadecimal
function componentToHex(c) {
    let hex = c.toString(16);
    return hex.length == 1 ? "0" + hex : hex;
}