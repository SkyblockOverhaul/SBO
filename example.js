import settings from "./settings";
import { registerWhen } from "./utils/variables";

registerWhen(register("RenderOverlay", DianaMobTrackerToggle), () => settings.dianaMobTracker);



function DianaMobTrackerToggle() {
    var SimpleDisplay = new Display();
    SimpleDisplay.hide();
    SimpleDisplay.setBackground("full");
    SimpleDisplay.setBackgroundColor(0x00000000);
    SimpleDisplay.setRenderLoc(150,150);
    SimpleDisplay.setLine(0, "&4Diana Mob Kills");
    SimpleDisplay.setLine(1, "Minos Inquisitor: ");
    SimpleDisplay.setLine(2, "Minos Champion: ");
    SimpleDisplay.setLine(3, "Minotaur: ");
    SimpleDisplay.setLine(4, "Gaia Construct: ");
    SimpleDisplay.setLine(5, "Siamese Lynx: ");
    SimpleDisplay.setLine(6, "Minos Hunter: ");
    if (settings.dianaMobTracker) {
        SimpleDisplay.show();
    } else {
        SimpleDisplay.hide();
    }
}


