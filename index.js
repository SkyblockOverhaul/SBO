/// <reference types="../CTAutocomplete" />
import Settings from "./settings";
import "./features/general/pickuplog";
import "./features/Diana/DianaMobDetect";
// import "./features/Diana/DianaWaypoints";
import "./features/guis/BobberCounter";
import "./features/general/PartyCommands";
import "./features/general/messageHider";


register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");




