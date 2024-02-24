/// <reference types="../CTAutocomplete" />
import Settings from "./settings";
import "./features/general/pickuplog";
import "./features/diana/DianaMobDetect";
// import "./features/Diana/DianaWaypoints";
import "./features/guis/BobberCounter";
import "./features/general/PartyCommands";
import "./features/general/messageHider";
import "./features/diana/DianaWarp";
import "./features/diana/DianaGuess";

register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");




