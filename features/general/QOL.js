import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { createNestWayoint, setNestedWaypoints } from "./Waypoints";


// register dragon wings for golden dragon nest
let found = false;
registerWhen(register("soundPlay", (pos, name, volume, pitch, categoryName, event) => {
    if (name === "mob.enderdragon.wings") {
        createNestWayoint(pos.x, pos.y, pos.z);
        if (!found) {
            Client.showTitle("", "", 0, 40, 20);
            Client.showTitle(`&r&6&l<&b&l&kO&6&l> &6&lDragon Nest Found! &6&l<&b&l&kO&6&l>`, "&eat x: " + pos.x + " y: " + pos.y + " z: " + pos.z, 0, 40, 20);
            ChatLib.chat("&6[SBO] &r&eFound &6Dragon Nest!&r&e at x: " + pos.x + " y: " + pos.y + " z: " + pos.z);
            found = true;
        }
    }
}), () => getWorld() === "Crystal Hollows" && settings.findDragonNest);

register("worldUnload", () => {
    setNestedWaypoints([]);
    found = false;
});