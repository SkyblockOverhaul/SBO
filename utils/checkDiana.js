import { getMayor, getPerks } from "./mayor";
import { getWorld } from './world';
import { playerHasSpade } from "./functions";



export function checkDiana() {
    dianaBool = (getMayor() === "Diana")
    return getWorld() === "Hub" && playerHasSpade() && dianaBool;
}

