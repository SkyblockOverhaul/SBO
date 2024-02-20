import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import {  state } from "../../utils/functions";

registerWhen(register("entityDeath", () => {
    state.entityDeathOccurred = true;
    setTimeout(() => {
        state.entityDeathOccurred = false;
    }, 1000);
}), () => getWorld() === "Hub" && settings.dianaMobTracker);









