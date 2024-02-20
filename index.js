/// <reference types="../CTAutocomplete" />
import Settings from "./settings";
import "./features/general/pickuplog";
import "./features/Diana/DianaMobDetect";
import "./features/Diana/DianaWaypoints";
import "./features/guis/DianaGuis";





register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");




