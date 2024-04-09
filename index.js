/// <reference types="../CTAutocomplete" />
import Settings from "./settings";
import "./features/general/pickuplog";
import "./features/diana/DianaMobDetect";
import "./features/guis/BobberCounter";
import "./features/general/PartyCommands";
import "./features/general/messageHider";
import "./features/general/Waypoints";
import "./features/diana/DianaBurrowDetect";
import "./features/slayer/BlazeSlayer";
import "./features/general/CopyMessage";
import "./utils/overlays";

// in sbo addons packen
import "./features/general/QOL";
import "./features/guis/SlayerGuis";
import "./features/dungeon/recognizeRareRoom";

register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");

// Title bug fix
register("worldLoad", () => {
    Client.showTitle("", "", 0, 40, 20);
});


