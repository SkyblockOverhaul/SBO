import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getFinalLocation } from "../diana/DianaGuess";
import { toTitleCase } from '../../utils/functions';
import RenderLibV2 from "../../../RenderLibv2";
import renderBeaconBeam from "../../../BeaconBeam/index";

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


function removeWaypointAfterDelay(Waypoints, seconds) {
    // remove wayspoints older than 30 seconds
    setTimeout(() => {
        Waypoints.shift();
    }, seconds*1000); // 30
} 

export function creatBurrowWaypoints(burrowType, x, y, z) {
    burrowWaypoints.push([burrowType, x, y, z]);
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
        wp[0] = [`${waypoint[0]} §b[${distance}]`, x + 0.5*xSign, y - 1, z + 0.5*zSign];

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
        else if (type == "Normal")
        {
            formatted.push(wp);
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
        Tessellator.drawString(box[0], box[1], box[2] + 1.5, box[3], 0xffffff, false);
        renderBeaconBeam(beam[0], beam[1], beam[2], rgb[0], rgb[1], rgb[2], 0.5, false);
    });
}

function closestWarpString(x, y, z) {
    closestWarp = getClosestWarp(x, y, z);
    if (closestWarp == "no warp") {
        closestWarp = "";
    }
    else {
        closestWarp = `(warp ${closestWarp})`;
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

registerWhen(register("chat", (trash, player, spacing, x, y, z) => {
    isInq = !z.includes(" ");
    if (isInq) {
        if(settings.inqWaypoints) {
            Client.Companion.showTitle(`&6INQUISITOR &dFound!`, "", 0, 90, 35);
            World.playSound("random.orb", 1, 1);
            z = z.replace("&r", "");
            player = player + "'s " + "Inquisitor " + closestWarpString(x, y, z);
            inqWaypoints.push([player, x, y, z]);
            removeWaypointAfterDelay(inqWaypoints, 60);
        }
        else{
            z = z.replace("&r", "");
            patcherWaypoints.push([player, x, y, z]);
            removeWaypointAfterDelay(patcherWaypoints, 30);
        }
    }
    else {
        if(settings.patcherWaypoints) {
            z = z.split(" ")[0];
            patcherWaypoints.push([player, x, y, z]);
            removeWaypointAfterDelay(patcherWaypoints, 30);
        }
    }
}).setCriteria("&r&9Party ${trash} ${player}&f${spacing}x: ${x}, y: ${y}, z: ${z}"), () => (settings.patcherWaypoints || settings.inqWaypoints) && settings.waypoints);

registerWhen(register("chat", () => {
    if (tryWarp) {
        ChatLib.chat("§6[SBO] §4" + toTitleCase(closestWarp) + " is not unlocked!")
        hubWarps[closestWarp].unlocked = false;
    }
}).setCriteria("&r&cYou haven't unlocked this fast travel destination!&r"), () => settings.inqWarpKey);
// wenn scroll ulocked dann diese message &r&eYou may now Fast Travel to &r&aSkyBlock Hub &r&7- &r&bCrypts&r&e!&r

registerWhen(register("step", () => { 
    if (finalLocation != null) {
        guessWaypointString = closestWarpString(finalLocation.x, finalLocation.y, finalLocation.z);
    }
}).setFps(2), () => settings.dianaBurrowGuess);;


let formattedGuess = [];
let lastWaypoint = undefined;
let guessWaypoint = undefined;
let finalLocation = undefined;
registerWhen(register("step", () => {
    formattedGuess = [];
    finalLocation = getFinalLocation();
    if (finalLocation != null && lastWaypoint != finalLocation) {
        guessWaypoint = [`Guess ${guessWaypointString}`, finalLocation.x, finalLocation.y, finalLocation.z];
        formatWaypoints([guessWaypoint], 0, 1, 0, "Guess");
        lastWaypoint = guessWaypoint;
    }
}).setFps(20), () => settings.dianaBurrowGuess);

let formatted = [];
registerWhen(register("step", () => {
    formatted = [];
    formatWaypoints(patcherWaypoints, 0, 0.2, 1); // Purple Waypoint
    formatWaypoints(inqWaypoints, 1, 0.84, 0); // Gold Waypoint
    formatWaypoints(burrowWaypoints, 0, 0, 0, "Burrow" ); // Red Waypoint

}).setFps(3), () => settings.waypoints);


registerWhen(register("renderWorld", () => {
    renderWaypoint(formattedGuess);
    renderWaypoint(formatted);
    renderWaypoint(burrowWaypoints);
}), () => settings.waypoints || settings.dianaBurrowDetect || settings.dianaBurrowGuess);

