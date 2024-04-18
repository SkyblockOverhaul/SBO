import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getGuiOpen, newOverlay } from "../../utils/overlays";
import { UIWrappedText } from "../../../Elementa";
import { YELLOW, BOLD, AQUA } from "../../utils/constants";

let legionOverlayObj = newOverlay("legionOverlay", "legionCounter", "legioncounterExample", "render", "LegionLoc");
let legionOverlay = legionOverlayObj.overlay;

let legionText = new UIWrappedText(`${YELLOW}${BOLD}Legion: ${AQUA}${BOLD}0`);
legionText.setHeight((10).pixels())
legionOverlay.addChild(legionText);

function getLegionCount() {
    let legionDistance = 30;
    const players = World
        .getAllPlayers()
        .filter(player =>
            (player.getUUID().version() === 4 || player.getUUID().version() === 1) && // Players and Watchdog have version 4, nicked players have version 1, this is done to exclude NPCs
            player.ping === 1 && // -1 is watchdog and ghost players, also there is a ghost player with high ping value when joining a world
            player.name != Player.getName() && // Exclude current player because they do not count for legion
            player.distanceTo(Player.getPlayer()) <= legionDistance
        )
        .map(player => player.name)
        .filter((x, i, a) => a.indexOf(x) == i); // Distinct, sometimes the players are duplicated in the list

    playersCount = players.length;
    return playersCount;
}

function refreshLegionOverlay() {
    if(getGuiOpen()) return;
    if(!legionOverlay.children.includes(legionText)) {
        legionOverlay.clearChildren();
        legionOverlay.addChild(legionText);
    }
    legionText.setText(`${YELLOW}${BOLD}Legion: ${AQUA}${BOLD}${getLegionCount()}`);
}

registerWhen(register("step", () => {
    refreshLegionOverlay();
}).setFps(1), () => settings.legionCounter);