import renderBeaconBeam from "../../../BeaconBeam/index";
import RenderLibV2 from "../../../RenderLibV2";
import settings from "../../settings";
import { checkDiana } from "../../utils/checkDiana";
import { isInSkyblock, isWorldLoaded, playCustomSound } from "../../utils/functions";


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
class Waypoints {
    static timedWaypoints = [];
    static waypoints = {}
    static guess = undefined;
    constructor(text, x, y, z, r, g, b, type = "normal", ttl = 0, line = false, beam = true, distance = true) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.line = line;
        this.beam = beam;
        this.distance = distance;
        this.text = text;
        this.ttl = ttl; // time to live in seconds
        this.type = type;

        // format variables
        this.formatted = false;
        this.distanceRaw = 0;
        this.distance = 0;
        this.xSign = 0;
        this.zSign = 0;
        this.warp = false;

        this.fx = 0;
        this.fz = 0;

        if (this.ttl > 0) Waypoints.timedWaypoints.push(this);
        if (!Waypoints.waypoints[type]) Waypoints.waypoints[type] = [];
        Waypoints.waypoints[type].push(this);
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
    }

    formatGuess() {
        this.warp = this.getClosestwarp();
        if (this.warp) {
            this.text = `${this.text}§7${this.warp} §b[${this.distance}]`;
        } else {
            this.text = `${this.text}§7 §b[${this.distance}]`;
        }

        let center = this.getCenter();
        this.text = `${this.text}§7${this.text} §b[${this.distance}]`;
    }

    format() {
        this.distanceRaw = this.getDistanceToPlayer();
        this.distance = Math.round(this.distanceRaw) + "m";

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
        }
        this.formatted = true;
    }
        
    render() {
        if (!this.formatted) return;
        let removeAtDistance = 10;
        let alpha = 0.5;
        // if (this.distanceRaw <= settings.removeGuessDistance && this.type == "guess" && settings.removeGuess) return;
        if (!settings.removeGuess && this.type == "guess") {
            removeAtDistance = 0;
        }

        RenderLibV2.drawInnerEspBoxV2(this.fx, this.y + 1, this.fz, 1, 1, 1, this.r, this.g, this.b, alpha/2, true);
        // print(`&6[SBO] &r&b${this.text} &7[${this.distance}] &7[${this.fx}, ${this.y}, ${this.fz}] &7[rgb: ${this.r}, ${this.g}, ${this.b}]`);

        let hexCodeString = javaColorToHex(new Color(this.r, this.g, this.b));
        if (this.text != "" && this.text != "§7") {
            Tessellator.drawString(this.text, this.fx, this.y + 1.5, this.fz, parseInt(hexCodeString, 16), true);
        }
        if (this.distanceRaw >= removeAtDistance && this.beam) {
            renderBeaconBeam(this.fx, this.y + 1, this.fz, this.r, this.g, this.b, alpha, false);
        }

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
            new Waypoints(player, x, y, z, 1, 0.84, 0);
        }
    } else {
        if (settings.patcherWaypoints) {
            if (!(player.includes(Player.getName()) && (settings.hideOwnWaypoints == 2 || settings.hideOwnWaypoints == 3))) {
                new Waypoints(player, x, y, z, 0, 0.2, 1);
            }
        }
    }
}).setCriteria("${player}&f${spacing}x: ${x}, y: ${y}, z: ${z}");


register("step", () => {
    if (!isInSkyblock() && !isWorldLoaded()) return;
    Object.keys(Waypoints.waypoints).forEach((type) => {
        Waypoints.waypoints[type].forEach((waypoint) => {
            if (waypoint.ttl > 0 && Date.now() - waypoint.time > waypoint.ttl * 1000) {
                Waypoints.waypoints[type].splice(Waypoints.waypoints[type].indexOf(waypoint), 1);
            }
            waypoint.format();
        });
    });
}).setFps(5);


register("renderWorld", () => { 
    if (!isInSkyblock() && !isWorldLoaded()) return;
    Object.keys(Waypoints.waypoints).forEach((type) => {
        Waypoints.waypoints[type].forEach((waypoint) => {
            waypoint.render();
        });
    });
})

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