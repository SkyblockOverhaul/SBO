import { Render3D } from "../../../tska/rendering/Render3D";
import RenderLibV2 from "../../../RenderLibV2";
import settings from "../../settings";
import { checkDiana } from "../../utils/checkDiana";
import { isInSkyblock, isWorldLoaded, playCustomSound, trace } from "../../utils/functions";

const Color = Java.type("java.awt.Color");

let hubWarps = {
    castle: {x: -250, y: 130, z: 45, unlocked: true},
    da: {x: 92, y: 75, z: 174, unlocked: true},
    hub: {x: -3, y: 70, z: -70, unlocked: true},
    museum: {x: -76, y: 76, z: 81, unlocked: true},
    wizard: {x: 42, y: 122, z: 69, unlocked: true},
    crypt: {x: -161, y: 61, z: -99, unlocked: true},
    stonks: {x: -53, y: 72, z: -53, unlocked: true},
};

// one centered waypoint class to replace the old waypoint system
/**
 * @class Waypoints
 * @description A class to create and manage waypoints in the game.
 * @param {string} text - The text to display on the waypoint.
 * @param {number} x - The x coordinate of the waypoint.
 * @param {number} y - The y coordinate of the waypoint.
 * @param {number} z - The z coordinate of the waypoint.
 * @param {number} r - The red color component of the waypoint.
 * @param {number} g - The green color component of the waypoint.
 * @param {number} b - The blue color component of the waypoint.
 * @param {number} [ttl=0] - The time to live for the waypoint in seconds.
 * @param {string} [type="normal"] - The type of the waypoint for customization.
 * @param {boolean} [line=false] - Whether to draw a line to the waypoint.
 * @param {boolean} [beam=true] - Whether to draw a beam at the waypoint.
 * @param {boolean} [distance=true] - Whether to display the distance in meters (blocks) to the waypoint.
 */
export class Waypoint {
    static waypoints = {}
    static guessWp = undefined;
    constructor(text, x, y, z, r, g, b, ttl = 0, type = "Normal", line = false, beam = true, distance = true) {
        this.x = parseFloat(x);
        this.y = parseFloat(y);
        this.z = parseFloat(z);
        this.r = r;
        this.g = g;
        this.b = b;
        this.alpha = 0.5;
        this.line = line;
        this.beam = beam;
        this.distance = distance;
        
        this.creation = Date.now();
        this.text = text;
        this.ttl = ttl; // time to live in seconds
        this.type = type;

        // format variables
        this.formatted = false;
        this.distanceRaw = 0;
        this.distanceText = "";
        this.formattedText = "";
        
        this.xSign = 0;
        this.zSign = 0;
        this.warp = false;

        this.fx = 0;
        this.fz = 0;

        if (this.type != "Guess") {
            if (!Waypoint.waypoints[type]) Waypoint.waypoints[type] = [];
            Waypoint.waypoints[type].push(this);
        } else {
            Waypoint.guessWp = this;
        }
        this.format();
    }

    getDistanceTo(waypoint) {
        return Math.hypot(
            this.x - waypoint.x,
            this.y - waypoint.y,
            this.z - waypoint.z
        );
    }

    getDistanceToPlayer() {
        return Math.hypot(
            this.x - Player.getX(),
            this.y - Player.getY(),
            this.z - Player.getZ()
        );
    }

    getCenter() {
        return {
            x: this.fx + ((this.fx + 1) - this.fx) / 2,
            y: this.y + ((this.y + 1) - this.y) / 2,
            z: this.fz + ((this.fz + 1) - this.fz) / 2
        };
    }

    getClosestwarp() {
        let closestWarp = "";
        const closestPlayerdistance = Math.sqrt(
            (Player.getLastX() - this.x)**2 +
            (Player.getLastY() - this.y)**2 +
            (Player.getLastZ() - this.z)**2
        );
        let closestDistance = Infinity;

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
                    (hubWarps[warp].x - this.x)**2 +
                    (hubWarps[warp].y - this.y)**2 +
                    (hubWarps[warp].z - this.z)**2
                );
                if (distance < closestDistance) {
                    closestDistance = distance;
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

    formatBurrow() {
        switch (this.text) {
            case "Start":
                this.r = settings.startColor.getRed()/255;
                this.g = settings.startColor.getGreen()/255;
                this.b = settings.startColor.getBlue()/255;
                break;
            case "Mob":
                this.r = settings.mobColor.getRed()/255;
                this.g = settings.mobColor.getGreen()/255;
                this.b = settings.mobColor.getBlue()/255;
                break;
            case "Treasure":
                this.r = settings.treasureColor.getRed()/255;
                this.g = settings.treasureColor.getGreen()/255;
                this.b = settings.treasureColor.getBlue()/255;
                break;
        }
        this.formattedText = `${this.text}§7 ${this.distanceText}`;

        if (this.x > 0) {
            this.xSign = 1;
        } else if (this.x < 0) {
            this.xSign = -1;
        }

        if (this.z > 0) {
            this.zSign = 1;
        } else if (this.z < 0) {
            this.zSign = -1;
        }
        this.fx = this.x + (this.xSign * 0.5);
        this.fz = this.z + (this.zSign * 0.5);
    }

    formatGuess() {
        this.warp = this.getClosestwarp();
        if (this.warp) {
            this.formattedText = `${this.text}§7${this.warp} ${this.distanceText}`;
        } else {
            this.formattedText = `${this.text}§7 ${this.distanceText}`;
        }

        let center = this.getCenter();
        this.fx = center.x;
        this.fz = center.z;
    }

    format() {
        this.distanceRaw = this.getDistanceToPlayer();
        if (this.distance) {
            this.distanceText = `§b[${Math.round(this.distanceRaw)}m]`;
        } else {
            this.distanceText = "";
        }

        if (this.distanceRaw >= 230) {
            this.fx = Player.getX() + (this.x - Player.getX()) * (230 / this.distanceRaw);
            this.fz = Player.getZ() + (this.z - Player.getZ()) * (230 / this.distanceRaw);
        } else {
            this.fx = this.x;
            this.fz = this.z;
        }

        if (this.type == "Burrow") {
            this.formatBurrow();
        } else if (this.type == "Guess") {
            this.formatGuess();
        } else {
            this.xSign = this.fx == 0 ? 1 : Math.sign(this.fx);
            this.zSign = this.fz == 0 ? 1 : Math.sign(this.fz);
            this.fx = this.fx + (this.xSign * 0.5);
            this.fz = this.fz + (this.zSign * 0.5);
            this.formattedText = `${this.text}§7 ${this.distanceText}`;
        }

        this.formatted = true;
    }

    renderLine() {
        trace(this.fx, this.y, this.fz, this.r, this.g, this.b, this.alpha, "", parseInt(settings.burrowLineWidth));
    }
        
    render() {
        if (!this.formatted) return;
        let removeAtDistance = 10;
        if (this.distanceRaw <= settings.removeGuessDistance && this.type == "Guess" && settings.removeGuess) return;
        if (!settings.removeGuess && this.type == "Guess") {
            removeAtDistance = 0;
        }

        RenderLibV2.drawInnerEspBoxV2(this.fx, this.y - 1, this.fz, 1, 1, 1, this.r, this.g, this.b, this.alpha/2, true);

        let hexCodeString = javaColorToHex(new Color(this.r, this.g, this.b));
        if (this.formattedText != "" && this.formattedText != "§7") {
            Tessellator.drawString(this.formattedText, this.fx, this.y + 0.5, this.fz, parseInt(hexCodeString, 16), true);
        }
        if (this.distanceRaw >= removeAtDistance && this.beam) {
            Render3D.renderBeaconBeam(this.fx - 0.5, this.y, this.fz - 0.5, this.r*255, this.g*255, this.b*255, this.alpha*255, true);
        }

        if (this.line) this.renderLine()
    }

    remove() {
        Waypoint.waypoints[this.type].splice(Waypoint.waypoints[this.type].indexOf(this), 1);
        if (this.type == "Guess") {
            Waypoint.guessWp = undefined;
        }
    }

    static forEachWaypoint(callback) {
        Object.keys(Waypoint.waypoints).forEach((type) => {
            Waypoint.waypoints[type].forEach((waypoint) => {
                callback(waypoint);
            });
        });
    }

    static removeAtPos(x, y, z) {
        Waypoint.forEachWaypoint(waypoint => {
            if (waypoint.x == x && waypoint.y == y && waypoint.z == z && waypoint.type != "Guess" && waypoint.type != "burrow") {
                waypoint.remove();
            }
        });
    }
}

register("chat", (player, spacing, x, y, z, event) => {
    if (!isWorldLoaded()) return;
    if (checkDiana() && settings.allWaypointsAreInqs) {
        isInq = true;
    } else {
        if (settings.inqWaypoints) {
            isInq = !z.includes(" ");
        } else {
            isInq = false;
        }
    }

    z = z.replace("&r", "").split(" ")[0];

    const bracketIndex = player.indexOf('[') - 2;
    const channel = player.substring(0, bracketIndex);

    if (channel.includes("Guild")) return;
    if (bracketIndex >= 0)
        player = player.replaceAll('&', '§').substring(bracketIndex, player.length);
    else
        player = player.replaceAll('&', '§');

    if (isInq) {
        Client.showTitle(`&r&6&l<&b&l&kO&6&l> &b&lINQUISITOR! &6&l<&b&l&kO&6&l>`, player, 0, 90, 20);
        playCustomSound(settings.inqSound, settings.inqVolume);

        if (!(player.includes(Player.getName()) && (settings.hideOwnWaypoints == 1 || settings.hideOwnWaypoints == 3))) {
            if (z.split(" ").length > 1) {
                z = z.split(" ")[0];
            }
            new Waypoint(player, x, y, z, 1, 0.84, 0, 45, "inq");
        }
    } else {
        if (settings.patcherWaypoints) {
            if (!(player.includes(Player.getName()) && (settings.hideOwnWaypoints == 2 || settings.hideOwnWaypoints == 3))) {
                new Waypoint(player, x, y, z, 0, 0.2, 1, 30);
            }
        }
    }
}).setCriteria("${player}&f${spacing}x: ${x}, y: ${y}, z: ${z}");

registerWhen(register("step", () => {
    if (!isInSkyblock() && !isWorldLoaded()) return;
    Waypoint.forEachWaypoint((waypoint) => {
        if (waypoint.ttl > 0 && Date.now() - waypoint.creation > waypoint.ttl * 1000) {
            waypoint.remove();
            return;
        }
        waypoint.format();
    });
}).setFps(5), () => settings.dianaBurrowDetect || settings.findDragonNest || settings.inqWaypoints || settings.patcherWaypoints);

registerWhen(register("step", () => {
    if (!isInSkyblock() && !isWorldLoaded()) return;
    if (Waypoint.guessWp) Waypoint.guessWp.format();
}).setFps(20), () => settings.dianaBurrowGuess);
        
registerWhen(register("renderWorld", () => { 
    if (!isInSkyblock() && !isWorldLoaded()) return;
    Waypoint.forEachWaypoint((waypoint) => {
        waypoint.render();
    });
    if (Waypoint.guessWp) Waypoint.guessWp.render();
}), () =>  settings.dianaBurrowDetect || settings.dianaBurrowGuess || settings.findDragonNest || settings.inqWaypoints || settings.patcherWaypoints);

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