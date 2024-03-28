/// <reference types="../CTAutocomplete" />
import Settings from "./settings";
import "./features/general/pickuplog";
import "./features/diana/DianaMobDetect";
// import "./features/Diana/DianaWaypoints";
import "./features/guis/BobberCounter";
import "./features/general/PartyCommands";
import "./features/general/messageHider";
import "./features/general/Waypoints";
import "./features/diana/DianaBurrowDetect";
import "./features/slayer/BlazeSlayer";
import "./features/general/CopyMessage";

// in sbo addons packen
import "./features/general/QOL";
import "./features/guis/SlayerGuis";
import "./features/dungeon/recognizeRareRoom";
import "./features/general/alphaCheck";
import "./utils/overlays";



register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");

// Title bug fix
register("worldLoad", () => {
    Client.showTitle("", "", 0, 40, 20);
});

register("chat", (event) => {
    Client.showTitle("&l&9!!!!!WORM!!!!!!", "&eKILL!!!", 0, 90, 20);
}).setCriteria("&r&7&oYou hear the sound of something approaching...&r");

register("chat", (message, event) => {
    message = message.removeFormatting();
    if (!message.includes("Powder") && !message.includes("Refelctor") && !message.includes("Blue Goblin Egg") && !message.includes("Heart")) {
        cancel(event);
    }
    if (message.includes("Refelctor")) {
        Client.showTitle("&9Robotron Reflector", "&eCarrot", 0, 40, 20);
    }
    if (message.includes("Blue Goblin Egg")) {
        Client.showTitle("&3Blue Goblin Egg", "&eCarrot", 0, 40, 20);
    }
}).setCriteria("&r&aYou received ${message}");

register("chat", (player, message, event) =>{
    // cancel original message
    // send new guildbot message
    if (!player.includes(" ")) {
        cancel(event);
        player = player.removeFormatting();
        ChatLib.chat("&r&2Guild > &b[DC] &b" + player + "&r:" + message);
        // print("&r&2Guild > &b[DC] &b" + player + "&r:" + message);
    }
    else if (player.includes("replying to")) {
        cancel(event);
        let split = player.split(" ");
        let player1 = split[0];
        let player2 = split[3];
        ChatLib.chat("&r&2Guild > &b[DC] &b" + player1 + " &3replying to &b" + player2 + "&r:" + message);
        // print("&r&2Guild > &b[DC] &b" + player1 + " &3replying to &b" + player2 + "&r:" + message);
    }
}).setCriteria("&r&2Guild > &a[VIP] SlowDT &3[GM]&f: ${player}:${message}").setContains()
// geht
// &r&2Guild > &a[VIP] SlowDT &3[GM]&f: &rSuccesfully invited kenchika to the party!&r
// &r&2Guild > &b[MVP&2+&b] MasterNR &3[320]&f: &rnice&r
// testen
// &r&2Guild > &a[VIP] SlowDT &3[GM]&f: &rWiggleSnakey replying to dtAxl: WWDYM&r 

// register("command", () => {
//     // Client.showTitle(`&r&6&l<&b&l&kO&6&l> &b&lINQUISITOR! &6&l<&b&l&kO&6&l>`, "&r&b[MVP&f+&b] RolexDE", 0, 90, 20);
//     Client.showTitle(`&5&lMinos Relic!`, "", 0, 25, 35);
//     ChatLib.chat("&6[SBO] &r&6&lRARE DROP! &5Minos Relic!");
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.0);
//     }, 0);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.2);
//     }, 50);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.4);
//     }, 100);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.6);
//     }, 150);
// }).setName("sboinq");

function rotateFigure(figure) {
    let minX = Math.min(...figure.map(p => p.x));
    let minY = Math.min(...figure.map(p => p.y));
    return figure.map(p => ({x: p.y - minY, y: minX - p.x}));
}

function calculatePositions(figure, mapSize) {
    let positions = [];
    let rotations = [figure, rotateFigure(figure)];

    for (let rotation of rotations) {
        let figureWidth = Math.max(...rotation.map(p => p.x)) - Math.min(...rotation.map(p => p.x));
        let figureHeight = Math.max(...rotation.map(p => p.y)) - Math.min(...rotation.map(p => p.y));

        for (let x = 0; x <= mapSize.x - figureWidth; x++) {
            for (let y = 0; y <= mapSize.y - figureHeight; y++) {
                let newPosition = rotation.map(p => ({x: p.x + x, y: p.y + y}));
                positions.push(newPosition);
            }
        }
    }

    return positions;
}

let figure = [{x:0,y:1},{x:1,y:2},{x:2,y:3},{x:3,y:3},{x:3,y:2},{x:3,y:1},{x:3,y:0},{x:4,y:3},{x:5,y:2},{x:6,y:1}];
let mapSize = {x: 8, y: 5};


positionen = calculatePositions(figure, mapSize);
print("Anzahl Positionen: " + positionen.length);
let renderSlots = [];

indexDict = {
    "00": 0,
    "10": 1,
    "20": 2,
    "30": 3,
    "40": 4,
    "50": 5,
    "60": 6,
    "70": 7,
    "80": 8,
    "01": 9,
    "11": 10,
    "21": 11,
    "31": 12,
    "41": 13,
    "51": 14,
    "61": 15,
    "71": 16,
    "81": 17,
    "02": 18,
    "12": 19,
    "22": 20,
    "32": 21,
    "42": 22,
    "52": 23,
    "62": 24,
    "72": 25,
    "82": 26,
    "03": 27,
    "13": 28,
    "23": 29,
    "33": 30,
    "43": 31,
    "53": 32,
    "63": 33,
    "73": 34,
    "83": 35,
    "04": 36,   
    "14": 37,
    "24": 38,
    "34": 39,
    "44": 40,
    "54": 41,
    "64": 42,
    "74": 43,
    "84": 44,
    "05": 45,
    "15": 46,
    "25": 47,
    "35": 48,
    "45": 49,
    "55": 50,
    "65": 51,
    "75": 52,
    "85": 53
};

// for (let position of positionen) {
    renderSlots = [];
    for (let p of figure) {
        let index = indexDict[`${p.x}${p.y}`];
        const x = index % 9;
        const y = Math.floor(index / 9);
        const renderX = Renderer.screen.getWidth() / 2 + ((x - 4) * 18);
        const renderY = (Renderer.screen.getHeight() + 10) / 2 + ((y - Player.getOpenedInventory().getSize() / 18) * 18);
        renderSlots.push({x: renderX, y: renderY});
        print(p.x + " " + p.y);
    }
// }

register("postGuiRender", (gui) => {
    for (let slot of renderSlots) {
        Renderer.translate(0, 0, 260);
        Renderer.drawRect(Renderer.color(0, 255, 127, 150), slot.x-9, slot.y-9, 17, 17);
    }
});