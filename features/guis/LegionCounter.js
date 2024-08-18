import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { OverlayTextLine, SboOverlay } from "../../utils/overlays";
import { YELLOW, BOLD, AQUA } from "../../utils/constants";

let legionOverlay = new SboOverlay("legionOverlay", "legionOverlay", "render", "LegionLoc");
let legionOverlayText = new OverlayTextLine("");

// credits FeeshNotifier
function getLegionCount() { // not working perfectly
    let legionDistance = 30;
    const players = World
        .getAllPlayers()
        .filter(player =>
            (player.getUUID().version() === 4 || player.getUUID().version() === 1) && // Players and Watchdog have version 4, nicked players have version 1, this is done to exclude NPCs
            player.ping === 1 && // -1 is watchdog and ghost players, also there is a ghost player with high ping value when joining a world
            player.name != Player.getName() && // Exclude current player because they do not count for legion
            disctanceToPlayer(player) <= legionDistance
        )
        .map(player => player.name)
        .filter((x, i, a) => a.indexOf(x) == i); // Distinct, sometimes the players are duplicated in the list
    
    playersCount = players.length;
    return playersCount;
}

function refreshLegionOverlay() {
    legionOverlay.setLines([legionOverlayText.setText(`${YELLOW}${BOLD}Legion: ${AQUA}${BOLD}${getLegionCount()}`)]);
}

registerWhen(register("step", () => {
    refreshLegionOverlay();
}).setFps(1), () => settings.legionOverlay);

function disctanceToPlayer(player) {
    return distance = Math.sqrt((player.x - Player.getX()) ** 2 + (player.z - Player.getZ()) ** 2);
}