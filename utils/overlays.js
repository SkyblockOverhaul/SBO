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
import { loadGuiSettings, saveGuiSettings } from "../utils/functions";
import { overlayExamples } from "../utils/guiExamples";

const dragOffset = {x: 0, y: 0};
//alle imports als export functions (setter) definieren
const Color = Java.type("java.awt.Color");
let guiSettings = loadGuiSettings();
class elementaOverlay {
    constructor(name, setting, example, type, locName) {
        this.name = name;
        this.setting = setting;
        this.example = example;
        this.type = type;
        this.locName = locName;
        this.renderGui = true;
        this.selected = false;
        this.overlay = new UIBlock(new Color(0.2, 0.2, 0.2, 0));
        this.overlay.setWidth(new ChildBasedRangeConstraint());
        this.overlay.setHeight(new ChildBasedRangeConstraint());

        this.overlay.onMouseClick((comp, event) => {
            this.selected = true;
            dragOffset.x = event.absoluteX;
            dragOffset.y = event.absoluteY;
        });

        this.overlay.onMouseRelease(() => {
            this.selected = false;
        }); 

        this.overlay.onMouseDrag((comp, mx, my) => {
            if(!this.selected) return;
            const absoluteX = mx + comp.getLeft()
            const absoluteY = my + comp.getTop()
            const dx = absoluteX - dragOffset.x;
            const dy = absoluteY - dragOffset.y;
            dragOffset.x = absoluteX;
            dragOffset.y = absoluteY;
            const newX = this.overlay.getLeft() + dx;
            const newY = this.overlay.getTop() + dy;
            this.overlay.setX(newX.pixels());
            this.overlay.setY(newY.pixels());
            guiSettings[this.locName]["x"] = newX;
            guiSettings[this.locName]["y"] = newY;
            saveGuiSettings(guiSettings);
        });
    }
}

let overLays = [];
export function newOverlay(name, setting, example, type, locName) {
    let overlay = new elementaOverlay(name, setting, example, type, locName);
    overLays.push(overlay);
    return overlay;
}

export function getGuiOpen(){
    return guiOpen;
}

let guiOpen = false;
const gui = new Gui();
const renderWindow = new Window()
const postWindow = new Window()
this.gui.registerClicked((x,y,b) => {
    this.renderWindow.mouseClick(x,y,b);
    this.postWindow.mouseClick(x,y,b);
});
this.gui.registerMouseDragged((x, y, b) => {
    this.renderWindow.mouseDrag(x, y, b);
    this.postWindow.mouseDrag(x, y, b);
});
this.gui.registerMouseReleased(() => {
    this.renderWindow.mouseRelease();
    this.postWindow.mouseRelease();
});

register("command", () => GuiHandler.openGui(gui)).setName("sboguis").setAliases("sbomoveguis");

register('renderOverlay', () => {
    overLays.forEach(overlay => {
        checkForSetting(overlay.overlay, overlay.setting, overlay.type, 0, false, overlay.renderGui);
    });
    guiMover();
    renderWindow.draw()
});

register('postGuiRender', () => {
    postWindow.draw()
});

register('worldUnload', () => {
    closeEditing();
});


function checkForSetting(overlay, setting, type, setting2, diana, renderBool){
    if(!overlay) return;
    if (renderBool) {
        if(setting || (setting2 > 0 && diana)){
            if(type === "render" && !renderWindow.children.includes(overlay)) {
                renderWindow.addChild(overlay);
            }
            else if(type === "post" && !postWindow.children.includes(overlay)){
                postWindow.addChild(overlay);
            }
        }
        if(!setting || (setting2 === 0 && diana)){
            if(type === "render" && renderWindow.children.includes(overlay)) {
                renderWindow.removeChild(overlay);
            }
            else if(type === "post" && postWindow.children.includes(overlay)){
                postWindow.removeChild(overlay);
            }
        }
    }
    else {
        if(type === "render" && renderWindow.children.includes(overlay)) {
            renderWindow.removeChild(overlay);
        }
        else if(type === "post" && postWindow.children.includes(overlay)){
            postWindow.removeChild(overlay);
        }
    }
}

function closeEditing(){
    overLays.forEach(overlay => {
        overlay.selected = false;
    });
    gui.close();
}

let clearState = false;
let firstDraw = false;
let refreshOverlays = false;
export function getRefreshOverlays() { return refreshOverlays; }
let refreshOverlaysTimeout;
function guiMover() {
    if (gui.isOpen()) {
        guiOpen = true;
        clearState = false;
        if (firstDraw === false) {
            drawExamples()
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
        if (clearState === false && guiOpen) {
            clearExamples();
            refreshOverlays = true;
            clearState = true;
            if (refreshOverlaysTimeout) clearTimeout(refreshOverlaysTimeout);
            refreshOverlaysTimeout = setTimeout(() => {
                refreshOverlays = false;
            }, 1000);
        }
        firstDraw = false;
        guiOpen = false;
    }
}

function drawExamples() {
    overLays.forEach(overlay => {
        if (overlay.name == "kuudraOverlay") {
            let example = overlay.example
            if (settings.lineSetting == 0) {
                example += "Two"
            }
            else {
                example += "One"
            }
            exampleMessage(overlayExamples[example], overlay.overlay);
        }
        else {
            exampleMessage(overlayExamples[overlay.example], overlay.overlay);
        }
    });
}

function exampleMessage(example, overlay){
    let exampleMSG = new UIWrappedText(example)
    overlay.clearChildren();
    exampleMSG.setX(new SiblingConstraint())
    exampleMSG.setY(new SiblingConstraint())
    maxStringWidth = example.split("\n").reduce((a, b) => a.length > b.length ? a : b).length
    exampleMSG.setWidth((maxStringWidth * 5.3).pixels());
    overlay.addChild(exampleMSG);
    overlay.setWidth(new ChildBasedRangeConstraint());
    overlay.setHeight(new ChildBasedRangeConstraint());
}

function clearExamples(){
    overLays.forEach(overlay => {
        overlay.overlay.clearChildren();
    });
}