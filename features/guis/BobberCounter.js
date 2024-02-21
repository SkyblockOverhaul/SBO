import settings from "../../settings";
import { loadGuiSettings, saveGuiSettings } from "../../utils/functions";
import { Overlay } from "../../utils/Overlay";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC} from "../../utils/constants";


guiSettings = loadGuiSettings();


bobbercounterExample =
`${GOLD}${BOLD}Bobber:
`

let BobberCounter = new Overlay("bobberCounter",["Hub"], [10, 10, 0],"moveBobberCounter",bobbercounterExample, "bobberCounter");

let bobberCount = 0;
let loadedBobber = false;
let message = true;




var movecounter = new Gui();
const EntityFishHook = Java.type("net.minecraft.entity.projectile.EntityFishHook")

function bobberOverlay() {
    if(settings.bobberCounter) {
        if(guiSettings != undefined && !loadedBobber) {
            ChatLib.chat("bobberOverlay")
            BobberCounter.setX(guiSettings["BobberLoc"]["x"]);
            BobberCounter.setY(guiSettings["BobberLoc"]["y"]);
            BobberCounter.setScale(guiSettings["BobberLoc"]["s"]);
            loadedBobber = true;
        }
        if( guiSettings["BobberLoc"]["x"] != BobberCounter.X || guiSettings["BobberLoc"]["y"] != BobberCounter.Y || guiSettings["BobberLoc"]["s"] != BobberCounter.S)
        {
            guiSettings["BobberLoc"]["x"] = BobberCounter.X;
            guiSettings["BobberLoc"]["y"] = BobberCounter.Y;
            guiSettings["BobberLoc"]["s"] = BobberCounter.S;
            saveGuiSettings(guiSettings);
        }
        BobberCounter.message = `${YELLOW}${BOLD}Bobber: ${AQUA}${BOLD}${bobberCount}`
    }
}

register('step', () => {
    bobberCount = World.getAllEntitiesOfType(EntityFishHook).filter(dist => dist.distanceTo(Player.getPlayer()) < 31).length
        bobberOverlay();
}).setFps(5)




// register('renderOverlay', () => {
//     if (settings.bobberCounter) {
//         const color = settings.bobberColor
//         Renderer.colorize(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
//         if (settings.bobberChroma) {
//             bobberCount.toString();
//             if (bobberCount > 9) {
//                 Renderer.drawString(`§zBobbers: ${bobberCount}`,  guiSettings["BobberLoc"]["x"], guiSettings["BobberLoc"]["y"], true)
//             } else {
//                 Renderer.drawString(`Bobbers: ${bobberCount}`,  guiSettings["BobberLoc"]["x"], guiSettings["BobberLoc"]["y"], true)
//             }
//         } else {
//             Renderer.drawString(`Bobbers: ${bobberCount}`,  guiSettings["BobberLoc"]["x"], guiSettings["BobberLoc"]["y"], true)
//         }
//     }
//     if (movecounter.isOpen()) {
//         const color = settings.bobberColor
//         Renderer.drawStringWithShadow(`x: ${Math.round( guiSettings["BobberLoc"]["x"])}, y: ${Math.round(guiSettings["BobberLoc"]["y"])}`, parseInt( guiSettings["BobberLoc"]["x"]) - 65, parseInt(guiSettings["BobberLoc"]["y"]) - 12)
//         Renderer.colorize(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
//         Renderer.drawString(`Bobbers:`,  guiSettings["BobberLoc"]["x"], guiSettings["BobberLoc"]["y"])
//     }
// })