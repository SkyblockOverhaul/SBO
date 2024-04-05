import { getMayor, getPerks } from "./mayor";
import { getWorld } from './world';
import { playerHasSpade } from "./functions";
import settings from "../settings";



export function checkDiana() {
    if (settings.alwaysDiana) return true;
    dianaBool = ((getMayor() === "Diana" && getPerks().has("Mythological Ritual") || getMayor() === "Jerry") && playerHasSpade());
    return getWorld() === "Hub" && dianaBool;
}

