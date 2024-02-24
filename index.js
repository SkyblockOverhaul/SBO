/// <reference types="../CTAutocomplete" />
import Settings from "./settings";
import "./features/general/pickuplog";
import "./features/diana/DianaMobDetect";
// import "./features/Diana/DianaWaypoints";
import "./features/guis/BobberCounter";
import "./features/general/PartyCommands";
import "./features/general/messageHider";
import "./features/Diana/DianaWarp";
import "./features/Diana/DianaBurrowDetect";



register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");




