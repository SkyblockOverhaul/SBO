import { getMayor, getPerks } from "./mayor";
import { getWorld } from './world';
import { checkItemInHotbar } from "./functions";



export function checkDiana() {
    dianaBool = (getMayor() === "Diana")
    return getWorld() === "Hub" && checkItemInHotbar("ANCESTRAL_SPADE") && dianaBool;
}

