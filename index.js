/// <reference types="../CTAutocomplete" />
import Settings from "./settings";
import "./features/general/pickuplog";
import "./features/diana/DianaMobDetect";
// import "./features/Diana/DianaWaypoints";
import "./features/guis/BobberCounter";
import "./features/general/PartyCommands";
import "./features/general/messageHider";
import "./features/general/Waypoints";
import "./features/Diana/DianaBurrowDetect";



register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");



register("command", () => {
    Client.showTitle(`&r&6&l<&b&l&kO&6&l> &b&lINQUISITOR! &6&l<&b&l&kO&6&l>`, "RolexDE", 0, 90, 20);
    World.playSound("random.orb", 1, 1);
}).setName("sboinq");
