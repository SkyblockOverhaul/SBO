import { getMayor, getMinister, getMinisterPerk, getPerks } from "./mayor";
import { getWorld } from './world';
import { playerHasSpade } from "./functions";
import settings from "../settings";

export function checkDiana(overrideHub = false) {
    let mayorBool = ((getMayor() == "Diana" && getPerks().has("Mythological Ritual") || getMinister() == "Diana" && getMinisterPerk() == "Mythological Ritual"));
    dianaBool = (settings.itsAlwaysDiana || ((mayorBool || getMayor() == "Jerry") && playerHasSpade()) && (getWorld() == "Hub" || overrideHub));
    return dianaBool;
}