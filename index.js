/// <reference types="../CTAutocomplete" />
import Settings from "./settings";
import "./features/general/pickuplog";
import "./features/Diana/DianaTracker";




register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");




