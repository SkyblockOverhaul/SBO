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
    ChildBasedRangeConstraint,
    Window
} from "../../Elementa";
import settings from "../settings";
import { kuudraValueOverlaySelected, kuudraOverlay } from "../features/Kuudra";
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
let overlayExamples = {
    kuudraExampleOne: `&r&62.49M &r&eTerror Chestplate&r
&r&b(BL 5/BR 4 - &r&6100.00K/2.49M&b)
&r&62.50M &r&eTerror Boots&r
&r&b(ER 5/DO 4 - &r&61.48M/2.50M&b)
&r&eTotal Value: &r&64.99M coins`,
kuudraExampleTwo: `&r&6600.00K &r&eCrimson Chestplate&r &r&b(BL 5/BR 4 - &r&6100.00K/600.00K&b)
&r&62.50M &r&eTerror Boots&r &r&b(ER 5/DO 4 - &r&61.48M/2.50M&b)
&r&eTotal Value: &r&63.1M coins`,
fossilExample: `Fossil: Unknown `
};


register("command", () => GuiHandler.openGui(gui)).setName("testnewhud");

register('renderOverlay', () => {
    // checkForSetting(getTestUI(), settings.attributeValueOverlay, overlayStatus, "render");
    guiMover();
    renderWindow.draw()
});

register('postGuiRender', () => {
    checkForSetting(kuudraOverlay, settings.attributeValueOverlay, "post");
    checkForSetting(fossilOverlay, settings.fossilOverlay, "post");
    postWindow.draw()
});

register('worldUnload', () => {
    closeEditing();
});


function checkForSetting(overlay, setting, type){
    if(!overlay) return;
    if(setting){
        if(type === "render" && !renderWindow.children.includes(overlay)) {
            renderWindow.addChild(overlay);
        }
        else if(type === "post" && !postWindow.children.includes(overlay)){
            postWindow.addChild(overlay);
        }
    }
    if(!setting){
        if(type === "render" && renderWindow.children.includes(overlay)) {
            renderWindow.removeChild(overlay);
        }
        else if(type === "post" && postWindow.children.includes(overlay)){
            postWindow.removeChild(overlay);
        }
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
            exampleMessage(overlayExamples["kuudraExampleOne"], kuudraOverlay, true);
            break;
        case 1:
            exampleMessage(overlayExamples["kuudraExampleTwo"], kuudraOverlay, true);
            break;
    }
    if(settings.fossilOverlay){
        exampleMessage(overlayExamples["fossilExample"], fossilOverlay, false);
    }
}

function exampleMessage(example, overlay, split){
    let exampleMSG = new UIWrappedText(example)
    overlay.clearChildren();
    exampleMSG.setX(new SiblingConstraint())
    exampleMSG.setY(new SiblingConstraint())
    if(split){
        maxStringWidth = example.split("\n").reduce((a, b) => a.length > b.length ? a : b).length
        exampleMSG.setWidth((maxStringWidth * 4.5).pixels());
    }
    overlay.addChild(exampleMSG);
    overlay.setWidth(new ChildBasedRangeConstraint());
    overlay.setHeight(new ChildBasedRangeConstraint());
}