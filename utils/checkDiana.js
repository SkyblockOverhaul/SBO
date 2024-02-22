import { getMayor, getPerks } from "./mayor";
import { getWorld } from './world';
import { checkItemInHotbar } from "./functions";



export function checkDiana() {
    dianaBool = (getMayor() === "Diana" && getPerks().has("Mythological Ritual"))
    return getWorld() === "Hub" && checkItemInHotbar("ANCESTRAL_SPADE") && dianaBool;
}

