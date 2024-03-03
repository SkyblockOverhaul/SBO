import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getFinalLocation } from "../diana/DianaGuess";
import { toTitleCase, isWorldLoaded, isInSkyblock } from '../../utils/functions';
import RenderLibV2 from "../../../RenderLibv2";
import renderBeaconBeam from "../../../BeaconBeam/index";
import { checkDiana } from "../../utils/checkDiana";

let patcherWaypoints = [];
export function getPatcherWaypoints() { 
    return patcherWaypoints 
};

let inqWaypoints = [];
export function getInqWaypoints() { 
    return inqWaypoints 
};

let burrowWaypoints = [];
export function getBurrowWaypoints() {
    return burrowWaypoints;
}

export function setBurrowWaypoints(burrows) {
    burrowWaypoints = burrows;
}

export function removeBurrowWaypoint(burrowshistory, burrows) {
    burrowshistory.forEach(([type, x, y, z]) => {
        for (let i = 0; i < burrowWaypoints.length; i++) {
            if (burrowWaypoints[i][1] == x && burrowWaypoints[i][2] == y && burrowWaypoints[i][3] == z) {
                burrowWaypoints.splice(i, 1);
            }
        }
        burrows = burrows.filter(([_, bx, by, bz]) => bx !== x || by !== y || bz !== z);
    });
    return burrows; 
}


function removeWaypointAfterDelay(Waypoints, seconds) {
    // remove wayspoints older than 30 seconds
    setTimeout(() => {
        Waypoints.shift();
    }, seconds*1000); // 30
} 

export function createBurrowWaypoints(burrowType, x, y, z, burrowshistory) {
    if (!burrowshistory.some(([type, xb, yb, zb]) => xb === x && yb === y && zb === z)) {
        if (burrowWaypoints.length > 0) {
            for (let i = 0; i < burrowWaypoints.length; i++) {
                if (burrowWaypoints[i][1] == x && burrowWaypoints[i][2] == y && burrowWaypoints[i][3] == z) {
                    if (burrowWaypoints[i][0] == burrowType) {
                        return;
                    }
                    else {
                        burrowWaypoints[i][0] = burrowType;
                        return;
                    }
                }
            }
            burrowWaypoints.push([burrowType, x, y, z]);
        }
        else {
            burrowWaypoints.push([burrowType, x, y, z]);
        }
    }
}

function formatWaypoints(waypoints, r, g, b, type = "Normal") {
    if (!waypoints.length) return;
    let x, y, z, distance, xSign, zSign = 0;

    waypoints.forEach((waypoint) => {
        if (type == "Burrow") {
            switch (waypoint[0]) {
                case "Start":
                    r = 0.5;
                    g = 1;
                    b = 0;
                    break;
                case "Mob":
                    r = 1;
                    g = 0.2;
                    b = 0.1;
                    break;
                case "Treasure":
                    r = 1;
                    g = 0.9;
                    b = 0;
                    break;
            }
        }
        if (waypoint[4] == undefined) {
            waypoint[4] = "";
        }

        wp = [["", 0, 0, 0], [0, 0, 0], [r, g, b]];
        x = Math.round(waypoint[1]);
        y = Math.round(waypoint[2]);
        z = Math.round(waypoint[3]);
        distance = Math.hypot(Player.getX() - x, Player.getY() - y, Player.getZ() - z);

        // Makes it so waypoint always renders
        if (distance >= 230) {
            x = Player.getX() + (x - Player.getX()) * (230 / distance);
            z = Player.getZ() + (z - Player.getZ()) * (230 / distance);
        }

        // Formats and realins everything
        distance = Math.round(distance) + "m";
        xSign = x == 0 ? 1 : Math.sign(x);
        zSign = z == 0 ? 1 : Math.sign(z);
        // waypoint message
        wp[0] = [`${waypoint[0]}§7${waypoint[4]} §b[${distance}]`, x + 0.5*xSign, y - 1, z + 0.5*zSign];

        // Aligns the beam correctly based on which quadrant it is in
        if (xSign == 1) xSign = 0;
        if (zSign == 1) zSign = 0;
        wp[1] = [x + xSign, y - 1, z + zSign];

        /* Return Matrix
           [message, x, y ,z]
           [beacon x, y, z]
           [r, g, b]
        */
        if (type == "Guess") {
            formattedGuess.push(wp);
        }
        else if (type == "Normal") {
            formatted.push(wp);
        }
        else if (type == "Burrow") {
            formattedBurrow.push(wp);
        }
    });
}


function renderWaypoint(waypoints) {
    if (!waypoints.length) return;

    waypoints.forEach((waypoint) => {
        box = waypoint[0];
        beam = waypoint[1];
        rgb = waypoint[2];

        RenderLibV2.drawEspBoxV2(box[1], box[2], box[3], 1, 1, 1, rgb[0], rgb[1], rgb[2], 1, true);
        RenderLibV2.drawInnerEspBoxV2(box[1], box[2], box[3], 1, 1, 1, rgb[0], rgb[1], rgb[2], 0.25, true);
        Tessellator.drawString(box[0], box[1], box[2] + 1.5, box[3], 0xffffff, true);
        renderBeaconBeam(beam[0], beam[1], beam[2], rgb[0], rgb[1], rgb[2], 0.5, false);
    });
}

function closestWarpString(x, y, z) {
    closestWarp = getClosestWarp(x, y, z);
    if (closestWarp == "no warp") {
        closestWarp = "";
    }
    else {
        closestWarp = ` (warp ${closestWarp})`;
    }
    return closestWarp;
}

let guessWaypointString = "";
let hubWarps = {
    castle: {x: -250, y: 130, z: 45, unlocked: true},
    da: {x: 92, y: 75, z: 174, unlocked: true},
    hub: {x: -3, y: 70, z: -70, unlocked: true},
    museum: {x: -76, y: 76, z: 81, unlocked: true},
};

const warpKey = new KeyBind("Burrow Warp", Keyboard.KEY_NONE, "SkyblockOverhaul");
let tryWarp = false;
warpKey.registerKeyPress(() => {
    if (settings.dianaBurrowWarp) {
        getClosestWarp(finalLocation.x, finalLocation.y, finalLocation.z);
        if (warpPlayer) {
            ChatLib.command("warp " + closestWarp);
            tryWarp = true;
            setTimeout(() => {
                tryWarp = false;
            }, 2000);
        }
    }
});

const inquisWarpKey = new KeyBind("Iqnuis Warp", Keyboard.KEY_NONE, "SkyblockOverhaul");
inquisWarpKey.registerKeyPress(() => {
    if (settings.inqWarpKey) {
        warps = getInqWaypoints();
        if (warps.length > 0) {
            getClosestWarp(warps[warps.length - 1][1], warps[warps.length - 1][2], warps[warps.length - 1][3]);
            if (warpPlayer) {
                ChatLib.command("warp " + closestWarp);
                tryWarp = true;
                setTimeout(() => {
                    tryWarp = false;
                }, 2000);
            }
        }
    }
});

let closestWarp = undefined;
let warpPlayer = false;
let closestDistance = Infinity;
function getClosestWarp(x, y, z){
    let closestPlayerdistance = Math.sqrt(
        (Player.getLastX() - x)**2 +
        (Player.getLastY() - y)**2 +
        (Player.getLastZ() - z)**2
    );
    closestDistance = Infinity;
    for (let warp in hubWarps) {
        if (hubWarps[warp].unlocked){
            let distance = Math.sqrt(
                (hubWarps[warp].x - x)**2 +
                (hubWarps[warp].y - y)**2 +
                (hubWarps[warp].z - z)**2
            );
            if (distance < closestDistance) {
                closestDistance = distance;
                closestWarp = warp;
            }
        }
    }
    if (Math.round(parseInt(closestPlayerdistance)) > Math.round(parseInt(closestDistance))) {
        warpPlayer = true;
    }
    else {
        warpPlayer = false;
    }

    if (warpPlayer) {
        return closestWarp;
    }
    else {
        return "no warp";
    }
}

registerWhen(register("chat", (player, spacing, x, y, z) => {
    if( isWorldLoaded()) {
        isInq = !z.includes(" ");
        const bracketIndex = player.indexOf('[') - 2;
        if (bracketIndex >= 0)
            player = player.replaceAll('&', '§').substring(bracketIndex, player.length);
        else
            player = player.replaceAll('&', '§');

        if (isInq) {
            if(settings.inqWaypoints && checkDiana()) {
                Client.showTitle(`&r&6&l<&b&l&kO&6&l> &b&lINQUISITOR! &6&l<&b&l&kO&6&l>`, player, 0, 90, 20);
                World.playSound("random.orb", 1, 1);
                z = z.replace("&r", "");
                // check if waypoint is from player
                if (!(player.includes(Player.getName()) && (settings.hideOwnWaypoints == 1 || settings.hideOwnWaypoints == 3))) {
                    inqWaypoints.push([player, x, y, z, closestWarpString(x, y, z)]);
                    removeWaypointAfterDelay(inqWaypoints, 60);
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
}).setCriteria("${player}&f${spacing}x: ${x}, y: ${y}, z: ${z}"), () => (settings.patcherWaypoints || settings.inqWaypoints) && settings.waypoints);

registerWhen(register("chat", () => {
    if (tryWarp) {
        ChatLib.chat("§6[SBO] §4Warp " + toTitleCase(closestWarp) + " is not unlocked!")
        hubWarps[closestWarp].unlocked = false;
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
}).setFps(2), () => settings.dianaBurrowGuess);;



let lastWaypoint = undefined;
let guessWaypoint = undefined;
let finalLocation = undefined;
registerWhen(register("step", () => {
    formattedGuess = [];
    finalLocation = getFinalLocation();
    if (finalLocation != null && lastWaypoint != finalLocation) {
        guessWaypoint = [`§aGuess`, finalLocation.x, finalLocation.y, finalLocation.z, guessWaypointString];
        formatWaypoints([guessWaypoint], 0, 1, 0, "Guess");
        lastWaypoint = guessWaypoint;
    }
}).setFps(20), () => settings.dianaBurrowGuess);

let formatted = [];
let formattedGuess = [];
let formattedBurrow = [];
registerWhen(register("step", () => {
    formatted = [];
    formattedBurrow = []
    formatWaypoints(patcherWaypoints, 0, 0.2, 1); // Purple Waypoint
    formatWaypoints(inqWaypoints, 1, 0.84, 0); // Gold Waypoint
    formatWaypoints(burrowWaypoints, 0, 0, 0, "Burrow" );

}).setFps(3), () => settings.waypoints || settings.dianaBurrowDetect || settings.dianaBurrowGuess);


registerWhen(register("renderWorld", () => { 
    renderWaypoint(formatted);
    if (isInSkyblock()) {
        renderWaypoint(formattedBurrow);
        renderWaypoint(formattedGuess);
    }
}), () => settings.waypoints || settings.dianaBurrowDetect || settings.dianaBurrowGuess);

