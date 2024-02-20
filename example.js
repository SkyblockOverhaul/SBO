import settings from "./settings";
import { registerWhen } from "./utils/variables";
import { Overlay } from "./utils/overlay";
import { getWorld } from "./utils/world";

dianaMobTrackerExample = 
`&4Diana Mob Kills
Minos Inquisitor: 
Minos Champion:
Minotaur:
Gaia Construct:
Siamese Lynx:
Minos Hunter:
`

const DianaMobTracker = new Overlay("dianaMobTracker",["Hub"], [150,150,1],"moveMobCoounter",dianaMobTrackerExample);
registerWhen(register("entityDeath", () => {
    DianaMobTracker.message = settings.dianaMobTracker === 1 ?
`&4Diana Mob Kills
Minos Inquisitor: 40
Minos Champion: 20
Minotaur: 30
Gaia Construct: 10
Siamese Lynx: 20 
Minos Hunter: 30
`: dianaMobTrackerExample;
}), () => getWorld() === "Hub" && settings.dianaMobTracker !== 0);


// registerWhen(register("RenderOverlay", DianaMobTrackerToggle), () => settings.dianaMobTracker);



// function DianaMobTrackerToggle() {
//     var SimpleDisplay = new Display();
//     SimpleDisplay.hide();
//     SimpleDisplay.setBackground("full");
//     SimpleDisplay.setBackgroundColor(0x00000000);
//     SimpleDisplay.setRenderLoc(150,150);
//     SimpleDisplay.setLine(0, "&4Diana Mob Kills");
//     SimpleDisplay.setLine(1, "Minos Inquisitor: ");
//     SimpleDisplay.setLine(2, "Minos Champion: ");
//     SimpleDisplay.setLine(3, "Minotaur: ");
//     SimpleDisplay.setLine(4, "Gaia Construct: ");
//     SimpleDisplay.setLine(5, "Siamese Lynx: ");
//     SimpleDisplay.setLine(6, "Minos Hunter: ");
//     if (settings.dianaMobTracker) {
//         SimpleDisplay.show();
//     } else {
//         SimpleDisplay.hide();
//     }
// }


