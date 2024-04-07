import { getMayor, getPerks } from "./mayor";
import { getWorld } from './world';
import { playerHasSpade } from "./functions";
import settings from "../settings";



export function checkDiana() {
    dianaBool = (settings.alwaysDiana && ((getMayor() === "Diana" && getPerks().has("Mythological Ritual") || getMayor() === "Jerry") && playerHasSpade()));
    return getWorld() === "Hub" && dianaBool;
}

