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
    UIWrappedText,
    WindowScreen,
    UIRoundedRectangle,
    Window
} from "../../Elementa";
import settings from "../settings";
import { getkuudraValueOverlay, getkuudraValueOverlaySelected } from "../features/Kuudra";
import { fossilOverlay, fossilGUISelected } from "../features/general/fossilSolver";

// siehe https://github.com/EssentialGG/Elementa für mehr 

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
    kuudraOverlay: false,
    fossilOverlay: false,
};
let overlayExamples = {
    kuudraExampleOne: `&r&62.49M &r&eTerror Chestplate&r
&r&b(BL 5/BR 4 - &r&6100.00K/2.49M&b)
&r&62.50M &r&eTerror Boots&r
&r&b(ER 5/DO 4 - &r&61.48M/2.50M&b)
&r&eTotal Value: &r&64.99M coins`,
kuudraExampleTwo: `&r&6600.00K &r&eCrimson Chestplate&r &r&b(BL 5/BR 4 - &r&6100.00K/600.00K&b)
&r&62.50M &r&eTerror Boots&r &r&b(ER 5/DO 4 - &r&61.48M/2.50M&b)
&r&eTotal Value: &r&63.1M coins`,
fossilExample: `&r&bFossil Solver&r`
};
let kuudraValueOverlaySelected = getkuudraValueOverlaySelected();


register("command", () => GuiHandler.openGui(gui)).setName("testnewhud");

register('renderOverlay', () => {
    // checkForSetting(getTestUI(), settings.attributeValueOverlay, overlayStatus, "render");
    guiMover();
    renderWindow.draw()
});

register('postGuiRender', () => {
    checkForSetting(getkuudraValueOverlay(), settings.attributeValueOverlay, overlayStatus, "post", "kuudraOverlay");
    checkForSetting(fossilOverlay, settings.fossilOverlay, overlayStatus, "post");
    postWindow.draw()
});

register('worldUnload', () => {
    closeEditing();
});


function checkForSetting(overlay, setting, statusObject, type, name){
    if(!overlay) return;
    if(setting && !statusObject[name]){
        if(type === "render") {
            renderWindow.addChild(overlay);
        }
        else if(type === "post"){
            postWindow.addChild(overlay);
        }
        statusObject[overlay] = true;
    }
    if(!setting && statusObject[name]){
        if(type === "render") {
            renderWindow.removeChild(overlay);
        }
        else if(type === "post"){
            postWindow.removeChild(overlay);
        }
        statusObject[name] = false;
    }
}

function closeEditing(){
    kuudraValueOverlaySelected = false;
    fossilGUISelected = false;
    gui.close();
}

let firstDraw = false;
function guiMover() {
    if (gui.isOpen()) {
        if (firstDraw === false) {
            drawExamples();
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

function drawExamples(){
    switch(settings.lineSetting){
        case 0:
            exampleMessage(overlayExamples["kuudraExampleOne"], getkuudraValueOverlay());
            break;
        case 1:
            exampleMessage(overlayExamples["kuudraExampleTwo"], getkuudraValueOverlay());
            break;
    }
    if(settings.fossilOverlay){
        exampleMessage(overlayExamples["fossilExample"], fossilOverlay);
    }
}

function exampleMessage(example, overlay){
    let exampleMSG = new UIWrappedText(example)
    exampleMSG.setX(new SiblingConstraint())
    exampleMSG.setY(new SiblingConstraint())
    maxStringWidth = example.split("\n").reduce((a, b) => a.length > b.length ? a : b).length
    print(maxStringWidth);
    print(example)
    exampleMSG.setWidth((maxStringWidth * 4.5).pixels());
    overlay.addChild(exampleMSG);
}