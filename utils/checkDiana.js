import { getMayor, getPerks } from "./mayor";
import { getWorld } from './world';
import { playerHasSpade } from "./functions";



export function checkDiana() {
    dianaBool = (getMayor() === "Diana" && getPerks().has("Mythological Ritual"));
    return getWorld() === "Hub" && playerHasSpade() && dianaBool;
}

