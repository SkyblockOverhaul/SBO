import {
    SiblingConstraint,
    FillConstraint,
    CenterConstraint,
    SubtractiveConstraint,
    AdditiveConstraint,
    animate,
    Animations,
    ChildBasedMaxSizeConstraint,
    ChildBasedSizeConstraint,
    ConstantColorConstraint,
    ScissorEffect,
    UIBlock,
    UIContainer,
    UIMultilineTextInput,
    UIText,
    WindowScreen,
    UIRoundedRectangle,
    Window
} from "../../Elementa";
import settings from "./../settings";
// import { getTestUI, getTestGUISelected } from "../features/teste";
import { getkuudraValueOverlay, getkuudraValueOverlaySelected} from "../features/kuudra";
const gui = new Gui();
const renderWindow = new Window()
const postWindow = new Window()
this.gui.registerClicked((x,y,b) => this.renderWindow.mouseClick(x,y,b));
this.gui.registerMouseDragged((x, y, b) => this.renderWindow.mouseDrag(x, y, b));
this.gui.registerMouseReleased(() => this.renderWindow.mouseRelease());
this.gui.registerClicked((x,y,b) => this.postWindow.mouseClick(x,y,b));
this.gui.registerMouseDragged((x, y, b) => this.postWindow.mouseDrag(x, y, b));
this.gui.registerMouseReleased(() => this.postWindow.mouseRelease());

let overlayStatus = {
    mainUIContainer: false,
    testBlock: false
};

let testGUISelected = getkuudraValueOverlaySelected();
register("command", () => GuiHandler.openGui(gui)).setName("testnewhud");

register('renderOverlay', () => {
    // checkForSetting(getTestUI(), settings.attributeValueOverlay, overlayStatus, "render");
    guiMover();
    renderWindow.draw()
});
//zwei windows für beide fälle
register('postGuiRender', () => {
    // checkForSetting(getkuudraValueOverlay(), settings.attributeValueOverlay, overlayStatus, "post");
    postWindow.draw()
});
register('worldUnload', () => {
    closeEditing();
});

function checkForSetting(overlay, setting, statusObject, type){
    if(!overlay) return;
    if(setting && !statusObject[overlay]){
        if(type === "render") {
            renderWindow.addChild(overlay);
        }
        else if(type === "post"){
            postWindow.addChild(overlay);
        }
        statusObject[overlay] = true;
    }
    if(!setting && statusObject[overlay]){
        if(type === "render") {
            renderWindow.removeChild(overlay);
        }
        else if(type === "post"){
            postWindow.removeChild(overlay);
        }
        statusObject[overlay] = false;
    }
}

function closeEditing(){
    testGUISelected = false;
    gui.close();
}
let firstDraw = false;
function guiMover() {
    if (gui.isOpen()) {
        //Methode um examples zu setten hier hin
        if (firstDraw === false) {
            postWindow.draw();
            firstDraw = true;
        }
        Renderer.drawRect(
            Renderer.color(0, 0, 0, 70),
            0,
            0,
            Renderer.screen.getWidth(),
            Renderer.screen.getHeight()
        );
    }
    if (!gui.isOpen()) {
        firstDraw = false;
    }
}

// const Color = Java.type("java.awt.Color");
// const dragOffset = { x: 0, y: 0 };
// let guiIsSelected = false;
// var mainUIContainer = new UIRoundedRectangle(3)
// mainUIContainer.setX((5).pixels())
// mainUIContainer.setY((5).pixels())
// mainUIContainer.setWidth((370).pixels())
// mainUIContainer.setHeight((75).pixels())
// mainUIContainer.setColor(new ConstantColorConstraint(new Color(0.2,0.2,0.2,0.5)));
// mainUIContainer.onMouseClick((comp, event) => {
//     guiIsSelected = true;
//     dragOffset.x = event.absoluteX;
//     dragOffset.y = event.absoluteY;
// });
// mainUIContainer.onMouseRelease(() => {
//     guiIsSelected = false;
// });
// mainUIContainer.onMouseDrag((comp, mx, my) => {
//     if (!guiIsSelected) return;
//     const absoluteX = mx + comp.getLeft();
//     const absoluteY = my + comp.getTop();
//     const dx = absoluteX - dragOffset.x;
//     const dy = absoluteY - dragOffset.y;
//     dragOffset.x = absoluteX;
//     dragOffset.y = absoluteY;
//     const newX = mainUIContainer.getLeft() + dx;
//     const newY = mainUIContainer.getTop() + dy;
//     mainUIContainer.setX(newX.pixels());
//     mainUIContainer.setY(newY.pixels());
// })
// mainUIContainer.addChild(new UIText("Settings").setX(new CenterConstraint()).setY((5).pixels()))