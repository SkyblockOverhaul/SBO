import { getMayor, getPerks } from "./mayor";
import { getWorld } from './world';
import { playerHasSpade } from "./functions";
import settings from "../settings";



export function checkDiana() {
    dianaBool = (settings.itsAlwaysDiana || ((getMayor() === "Diana" && getPerks().has("Mythological Ritual") || getMayor() === "Jerry") && playerHasSpade()) && getWorld() === "Hub");
    return dianaBool;
}

