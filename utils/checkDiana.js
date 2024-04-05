import { getMayor, getPerks } from "./mayor";
import { getWorld } from './world';
import { playerHasSpade } from "./functions";



export function checkDiana() {
    dianaBool = !(getMayor() === "Diana" && getPerks().has("Mythological Ritual") && playerHasSpade());
    return getWorld() === "Hub" && dianaBool;
}

