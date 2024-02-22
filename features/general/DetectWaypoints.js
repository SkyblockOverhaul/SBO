import settings from "../../settings";
import { registerWhen } from "../../utils/variables";

import RenderLib from "../../../RenderLib/index.js";
import renderBeaconBeam from "../../../BeaconBeam";

let patcherWaypoints = [];
export function getPatcherWaypoints() { 
    return patcherWaypoints 
};

let inqWaypoints = [];
export function getInqWaypoints() { 
    return inqWaypoints 
};

registerWhen(register("chat", (player, spacing, x, y, z) => {
    print(z);
    isInq = !z.includes(" ");
    if (isInq) {
        z = z.split(" ")[0];
    }
    else {
        // remove from z all characters that are not numbers
        z = z.replace(/\D/g, '');
    }
    ChatLib.chat("inquis: " + isInq);
    if (isInq) {
        ChatLib.chat("inquis cords: " + x + " . " + y + " . " + z);
        inqWaypoints.push({ x: x, y: y, z: z });
        removeWaypointAfterDelay(inqWaypoints);
    }
    else {
        ChatLib.chat("patcher cords: " + x + " . " + y + " . " + z);
        patcherWaypoints.push({ x: x, y: y, z: z });
        removeWaypointAfterDelay(patcherWaypoints);
    }
}).setCriteria("${player}&f${spacing}x: ${x}, y: ${y}, z: ${z}"), () => settings.patcherCords);
//sh inq &r&9Party &8> &6ᛃ &b[MVP&f+&b] RolexDE&f: &rx: -28, y: 87, z: -208&r
// patcher waypoint &r&9Party &8> &6ᛃ &b[MVP&f+&b] RolexDE&f: &rx: -27, y: 87, z: -208 &r

function removeWaypointAfterDelay(Waypoints) {
    // remove wayspoints older than 30 seconds
    setTimeout(() => {
        ChatLib.chat("Removing old waypoint");
        Waypoints.shift();
    }, 30000); // 30
} 

let formatted = [];
register("step", () => {
    formatted = [];
    formatWaypoints(patcherWaypoints, 1, 0, 1); // Purple Waypoint
    formatWaypoints(inqWaypoints, 1, 0.84, 0); // Gold Waypoint
}).setFps(4);

register("command", () => {
    renderWaypoint(formatted);
}).setName("sboRenderWaypoints");


function formatWaypoints(waypoints, r, g, b) {
    if (!waypoints.length) return;
    let x, y, z, distance, xSign, zSign = 0;

    waypoints.forEach((waypoint) => {
        wp = [["", 0, 0, 0], [0, 0, 0], [r, g, b]];
        x = Math.round(waypoint[1]);
        y = Math.round(waypoint[2]);
        z = Math.round(waypoint[3]);
        distance = Math.hypot(Player.getX() - x, Player.getY() - y, Player.getZ() - z);

        // Makes it so waypoint always renders
        if (distance >= 100) {
            x = Player.getX() + (x - Player.getX()) * (100 / distance);
            y = Player.getY() + (y - Player.getY()) * (100 / distance);
            z = Player.getZ() + (z - Player.getZ()) * (100 / distance);
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
        formatted.push(wp);
    });
}


function renderWaypoint(waypoints) {
    if (!waypoints.length) return;

    waypoints.forEach((waypoint) => {
        box = waypoint[0];
        beam = waypoint[1];
        rgb = waypoint[2];
    
        RenderLib.drawEspBox(box[1], box[2], box[3], 1, 1, rgb[0], rgb[1], rgb[2], 1, true);
        RenderLib.drawInnerEspBox(box[1], box[2], box[3], 1, 1, rgb[0], rgb[1], rgb[2], 0.25, true);
        Tessellator.drawString(box[0], box[1], box[2] + 1.5, box[3], 0xffffff, true);
        renderBeaconBeam(beam[0], beam[1], beam[2], rgb[0], rgb[1], rgb[2], 0.5, false);
    });
}