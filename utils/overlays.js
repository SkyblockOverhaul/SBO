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
    ScaledTextConstraint,
    WindowScreen,
    UIRoundedRectangle,
    ChildBasedRangeConstraint,
    Window,
    RelativeConstraint
} from "../../Elementa";
import settings from "../settings";
import { loadGuiSettings, printDev, saveGuiSettings } from "../utils/functions";
import { isInSkyblock } from '../utils/functions';
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE, UNDERLINE} from "../utils/constants";


const dragOffset = {x: 0, y: 0};

const Color = Java.type("java.awt.Color");
let guiSettings = loadGuiSettings();

function loadOverlay(overlay, locName, obj) {
    if (guiSettings != undefined) {
        overlay.setX((guiSettings[locName]["x"]).pixels());
        overlay.setY((guiSettings[locName]["y"]).pixels());
        obj.X = guiSettings[locName]["x"];
        obj.Y = guiSettings[locName]["y"];
    }
}


class elementaOverlay {
    constructor(name, setting, example, type, locName) {
        this.name = name;
        this.setting = setting;
        this.example = example;
        this.type = type;
        this.locName = locName;
        this.renderGui = true;
        this.selected = false;
        this.scale = parseFloat(guiSettings[locName]["s"]);
        this.overlay = new UIBlock(new Color(0.2, 0.2, 0.2, 0));
        this.overlay.setWidth(new ChildBasedSizeConstraint());
        this.overlay.setHeight(new ChildBasedSizeConstraint());
        this.X = 0;
        this.Y = 0;

        this.overlay.onMouseClick((comp, event) => {
            this.selected = true;
            dragOffset.x = event.absoluteX;
            dragOffset.y = event.absoluteY;
            print("dragOffset.x: " + dragOffset.x + " dragOffset.y: " + dragOffset.y)
        });

        this.overlay.onMouseRelease(() => {
            this.selected = false;
        }); 

        this.overlay.onMouseDrag((comp, mx, my) => {
            if(!this.selected) return;
            const absoluteX = mx + comp.getLeft()
            const absoluteY = my + comp.getTop()
            print("absoluteX: " + absoluteX + " absoluteY: " + absoluteY)
            print("comp.getLeft(): " + comp.getLeft() + " comp.getTop(): " + comp.getTop())
            print("mx: " + mx + " my: " + my)
            const dx = absoluteX - dragOffset.x;
            const dy = absoluteY - dragOffset.y;
            dragOffset.x = absoluteX;
            dragOffset.y = absoluteY;
            const newX = this.overlay.getLeft() + dx;
            const newY = this.overlay.getTop() + dy;
            this.overlay.setX(newX.pixels());
            this.overlay.setY(newY.pixels());
            guiSettings[this.locName]["x"] = newX
            guiSettings[this.locName]["y"] = newY;
            this.X = newX;
            this.Y = newY;
            saveGuiSettings(guiSettings);
        });
        this.overlay.onMouseScroll((comp, x, y) => {
            delta = x.delta; // -1 = scrolled down, 1 = scrolled up
            scale = parseFloat(guiSettings[this.locName]["s"]);
            this.scale = scale;
            switch(delta){
                case -1:
                    if(this.scale <= 0.1) return;
                    this.scale -= 0.1;
                    break;
                case 1:
                    if(this.scale >= 1.0) return;
                    this.scale += 0.1;
                    break;
            }
            roundedScale = this.scale.toFixed(1);
            guiSettings[this.locName]["s"] = roundedScale;
            clearExamples();
            drawExamples();
            saveGuiSettings(guiSettings);
        });

        loadOverlay(this.overlay, this.locName, this);
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
const inventoryWindow = new Window()
this.gui.registerClicked((x,y,b) => {
    this.renderWindow.mouseClick(x,y,b);
    this.postWindow.mouseClick(x,y,b);
    this.inventoryWindow.mouseClick(x,y,b);
});
this.gui.registerMouseDragged((x, y, b) => {
    this.renderWindow.mouseDrag(x, y, b);
    this.postWindow.mouseDrag(x, y, b);
    this.inventoryWindow.mouseDrag(x, y, b);
});
this.gui.registerMouseReleased(() => {
    this.renderWindow.mouseRelease();
    this.postWindow.mouseRelease();
    this.inventoryWindow.mouseRelease();
});
this.gui.registerScrolled((mouseX, mouseY, scrollDirection) => {
    this.renderWindow.mouseScroll(scrollDirection);
    this.postWindow.mouseScroll(scrollDirection);
    this.inventoryWindow.mouseScroll(scrollDirection);
});

register("command", () => { 
    GuiHandler.openGui(gui)
}).setName("sboguis").setAliases("sbomoveguis"); 

register("tick", () => {
    overLays.forEach(overlay => {
        switch(overlay.name){
            case "dianaMobTracker":
                checkForSetting(overlay.overlay, settings[overlay.setting], overlay.type, settings.dianaMobTrackerView, true, overlay.renderGui);
                break;
            case "dianaLootTracker":
                checkForSetting(overlay.overlay, settings[overlay.setting], overlay.type, settings.dianaLootTrackerView, true, overlay.renderGui);
                break;
            default:
                checkForSetting(overlay.overlay, settings[overlay.setting], overlay.type, 0, false, overlay.renderGui);
                break;
        }
    });
});


register('renderOverlay', () => {
    if(!isInSkyblock()) return;
    guiMover();
    renderWindow.draw()
});

let isInInventory = false;
register('guiClosed', (gui) => {
    gui = gui.toString();
    if(gui.includes("Inventory")) {
        isInInventory = false;
    }
});


register('guiOpened', () => {
    setTimeout(() => {
        if (Client == undefined) return;
        if (Client.currentGui == undefined) return;
        if (Client.currentGui.get() == null) return;
        let openedgui = Client.currentGui.get().toString();
        if(openedgui.includes("Inventory")) {
            isInInventory = true;
        }
    }, 200);
});


register('postGuiRender', () => {
    if(!isInSkyblock()) return;
    postWindow.draw()
});

register('worldUnload', () => {
    closeEditing();
});



function checkForSetting(overlay, setting, type, setting2, diana, renderBool){
    if(!overlay) return;
    if (renderBool || type == "post") {
        if(setting || (setting2 > 0 && diana)){
            if((type === "render" || type === "inventory") && !renderWindow.children.includes(overlay)) {
                renderWindow.addChild(overlay);
            }
            else if(type === "post" && !postWindow.children.includes(overlay)){
                postWindow.addChild(overlay);
            }
            else if(type === "inventory" && !inventoryWindow.children.includes(overlay)){
                inventoryWindow.addChild(overlay);
            }
        }
        if(!setting || (setting2 === 0 && diana)){
            if((type === "render" || type === "inventory") && renderWindow.children.includes(overlay)) {
                renderWindow.removeChild(overlay);
            }
            else if(type === "post" && postWindow.children.includes(overlay)){
                postWindow.removeChild(overlay);
            }
            else if(type === "inventory" && inventoryWindow.children.includes(overlay)){
                inventoryWindow.removeChild(overlay);
            }
        }
    }
    else {
        if((type === "render" || type === "inventory") && renderWindow.children.includes(overlay)) {
            renderWindow.removeChild(overlay);
        }
        else if(type === "post" && postWindow.children.includes(overlay)){
            postWindow.removeChild(overlay);
        }
        else if(type === "inventory" && inventoryWindow.children.includes(overlay)){
            inventoryWindow.removeChild(overlay);
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
    overLays.forEach(gui => {
        if (gui.name == "kuudraOverlay") {
            let example = gui.example
            if (settings.lineSetting == 0) {
                example += "Two"
            }
            else {
                example += "One"
            }
            exampleMessage(overlayExamples[example], gui.overlay, gui.scale);
        }
        else {
            exampleMessage(overlayExamples[gui.example], gui.overlay, gui.scale);
        }
    });
}

function exampleMessage(example, overlay, scale){
    let exampleMSG = new UIWrappedText(example)
    overlay.clearChildren();
    exampleMSG.setX(new SiblingConstraint())
    exampleMSG.setY(new SiblingConstraint())
    maxStringWidth = example.split("\n").reduce((a, b) => a.length > b.length ? a : b).length
    exampleMSG.setWidth((maxStringWidth * 5.3).pixels());
    exampleMSG.setTextScale((scale).pixels());
    overlay.addChild(exampleMSG);
    overlay.setWidth(new ChildBasedRangeConstraint());
    overlay.setHeight(new ChildBasedRangeConstraint());
}

function clearExamples(){
    overLays.forEach(overlay => {
        overlay.overlay.clearChildren();
    });
}


function loadSettings(overlay) {
    if (guiSettings != undefined) {
        overlay.X = parseInt(guiSettings[overlay.locName]["x"]);
        overlay.Y = parseInt(guiSettings[overlay.locName]["y"]);
    }
}

//// new overlay without elementa
let overLaysNew = [];
let editGui = new Gui();

// if (this.gui.isOpen()) {
//     let txt = "Drag to move. Use +/- to increase/decrease gui size. Use arrow keys to set alignment.";
//     Renderer.drawStringWithShadow(txt, Renderer.screen.getWidth()/2 - Renderer.getStringWidth(txt)/2, Renderer.screen.getHeight()/2);
// }

editGui.registerScrolled((x, y, delta) => {
    if (editGui.isOpen()) {
        overLaysNew.forEach(overlay => {
            // check if mouse is over the overlay
            if (overlay.isInOverlay(x, y)) {
                overlay.scale = parseFloat(guiSettings[overlay.locName]["s"]);
                switch(delta){
                    case -1:
                        if(overlay.scale <= 0.1) return;
                        overlay.scale -= 0.1;
                        break;
                    case 1:
                        if(overlay.scale >= 10.0) return;
                        overlay.scale += 0.1;
                        break;
                }
                overlay.scale = overlay.scale.toFixed(1);
                guiSettings[overlay.locName]["s"] = overlay.scale;
                saveGuiSettings(guiSettings);
            }
        });
    }
});

editGui.registerMouseDragged((x, y) => {
    if (editGui.isOpen()) {
        overLaysNew.forEach(overlay => {
            if (overlay.selected) {
                const mx = x - overlay.X;
                const my = y - overlay.Y;
                const absoluteX = mx + overlay.X;
                const absoluteY = my + overlay.Y;
                const dx = absoluteX - dragOffset.x;
                const dy = absoluteY - dragOffset.y;
                dragOffset.x = absoluteX;
                dragOffset.y = absoluteY;
                const newX = overlay.X + dx;
                const newY = overlay.Y + dy;
                overlay.X = newX;
                overlay.Y = newY;
                guiSettings[overlay.locName]["x"] = newX;
                guiSettings[overlay.locName]["y"] = newY;
            }
        });
    }
});

editGui.registerClicked((x, y, button) => {
    if (editGui.isOpen()) {
        overLaysNew.forEach(overlay => {
            // check if mouse is over the overlay
            if (overlay.isInOverlay(x, y)) {
                overlay.selected = true;
            }
            else {
                overlay.selected = false;
            }
        });
        dragOffset.x = x;
        dragOffset.y = y;
    }
});

editGui.registerMouseReleased(() => {
    overLaysNew.forEach(overlay => {
        overlay.selected = false;
    });
    // save all gui settings
    saveGuiSettings(guiSettings);
});

export class OverlayTextLine {
    constructor(message, shadow = true) {
        this.text = new Text(message ?? "");
        this.text.setShadow(shadow);
        this.text.setScale(1.0);
        this.text.setAlign("LEFT")
        this.X = -1;
        this.Y = -1;
        this.scale = 1.0;
        this.lineBreak = true;

    }

    setText(message) {
        this.text.setString(message);
        return this;
    }

    getString() {
        return this.text.getString();
    }

    setX(x) {
        this.X = x;
        this.text.setX(x);
        return this;
    }

    setY(y) {
        this.Y = y;
        this.text.setY(y);
        return this;
    }

    setScale(scale) {
        this.scale = scale;
        this.text.setScale(scale);
        return this;
    }

    draw() {
        this.text.draw();
    }

    setAlign(align) {
        this.text.setAlign(align);
        return this;
    }

    setShadow(shadow) {
        this.text.setShadow(shadow);
        return this;
    }
}

let buttons = [];
register("guiMouseClick" , (cx, cy, button, gui) => {
    buttons.forEach(button => {
        button.clicked(cx, cy, button, gui);
    });
});

export class OverlayButton extends OverlayTextLine {
    constructor(message, shadow, button, lineBreak = true, delimiter = "&e | &r" ) {
        super(message, shadow);
        this.button = button;
        this.action = undefined;
        this.lineBreak = lineBreak; 

        this.delimiter = new Text(delimiter).setShadow(shadow);
        
        buttons.push(this);
    }
    
    onClick(action) {
        this.action = action;
        return this;
    }

    clicked(cx, cy, button, gui) {
        if (isInInventory) {
            if (this.button && this.action && this.X != -1 && this.Y != -1) {
                let stringCount = this.text.getString().split("\n").length;
                let longestLine = this.text.getString().split("\n").reduce((a, b) => a.length > b.length ? a : b);

                if (cx >= this.X && cx <= this.X + Renderer.getStringWidth(longestLine) * this.scale && cy >= this.Y && cy <= this.Y + 10 * this.scale * stringCount) {
                    this.action();
                }
            }
        }
    }

}


function drawText(overlay) {
    let lineCount = 0;
    let textLines = overlay.textLines;
    if (overlay.exampleText != undefined && editGui.isOpen()) { 
        textLines = [overlay.exampleText];
    }
    textLines.forEach((text, index) => {
        if (text.button && !isInInventory && !editGui.isOpen()) return;
        if (text.button && !text.lineBreak && index > 0) {
            let previousText = overlay.textLines[index - 1].text.getString() 
            text.delimiter.setX(overlay.X + overlay.offsetX + Renderer.getStringWidth(previousText) * overlay.scale)
            text.delimiter.setY(overlay.Y + overlay.offsetY + (10 * overlay.scale * (lineCount - 1)))
            text.delimiter.setScale(overlay.scale);
            text.delimiter.draw();

            text.setX(overlay.X + overlay.offsetX + Renderer.getStringWidth(previousText) * overlay.scale + Renderer.getStringWidth(text.delimiter.getString()) * overlay.scale)
            text.setY(overlay.Y + overlay.offsetY + (10 * overlay.scale * (lineCount - 1)))
            text.setScale(overlay.scale);
            
            text.draw();

        }
        else {
            text.setX(overlay.X + overlay.offsetX)
            text.setY(overlay.Y + overlay.offsetY + (10 * overlay.scale * (lineCount)))
            text.setScale(overlay.scale);
            text.draw();
            lineCount += text.getString().split("\n").length;
        }

    });
}

export class SboOverlay {
    constructor(name, setting, type, locName, example = "") {
        overLaysNew.push(this);
        this.name = name;
        this.setting = setting;
        this.example = example;
        this.type = type;
        this.locName = locName;
        this.renderGui = true;
        this.selected = false;
        this.scale = parseFloat(guiSettings[locName]["s"]);
        this.X = 0;
        this.Y = 0;
        this.offsetX = 0;
        this.offsetY = 0;
        this.longestString = 0;
        this.stringCount = 0;
        this.editParameters = new Text("");
        this.exampleText = overlayExamples[this.example] ?? undefined;
        
        this.textLines = [];    

        this.gui = new Gui();

        register("renderOverlay", () => {
            if (isInSkyblock() && settings[this.setting] && (this.renderGui || editGui.isOpen()) && (this.type == "render" || (this.type == "inventory" && !isInInventory))) {
                drawText(this);
                if (editGui.isOpen()) {
                    this.editParameters.setString("&oX: " + this.X + " Y: " + this.Y + " Scale: " + this.scale);
                    this.editParameters.setX(this.X)
                    this.editParameters.setY(this.Y - 10)
                    this.editParameters.draw();
                    // Renderer.drawRect(Renderer.color(0, 0, 0, 100), this.X, this.Y, Renderer.getStringWidth(this.longestString) * this.scale + this.offsetX, 10 * this.scale * this.stringCount + this.offsetY);
                }
            }
        });


        register("postGuiRender", () => {
            if (isInSkyblock() && settings[this.setting] && this.renderGui && this.type == "post") {
                drawText(this)
                if (editGui.isOpen()) {
                    this.editParameters.setString("&oX: " + this.X + " Y: " + this.Y + " Scale: " + this.scale);
                    this.editParameters.setX(this.X)
                    this.editParameters.setY(this.Y - 10)
                    this.editParameters.draw();
                    // Renderer.drawRect(Renderer.color(0, 0, 0, 100), this.X, this.Y, Renderer.getStringWidth(this.longestString) * this.scale + this.offsetX, 10 * this.scale * this.stringCount + this.offsetY);
                }
            }
        });

        register("guiRender", () => {
            if (isInSkyblock() && settings[this.setting] && this.renderGui && (this.type == "inventory" && isInInventory)) {
                drawText(this)
            }
        });

        register("guiKey", (char, keyCode, gui, event) => {
            const mouseX = Client.getMouseX();
            const mouseY = Client.getMouseY();
            if (editGui.isOpen() && this.isInOverlay(mouseX, mouseY)) {
                switch (keyCode) {
                    case Keyboard.KEY_LEFT:
                        this.X -= 1;
                        break;
                    case Keyboard.KEY_RIGHT:
                        this.X += 1;
                        break;
                    case Keyboard.KEY_UP:
                        this.Y -= 1;
                        break;
                    case Keyboard.KEY_DOWN:
                        this.Y += 1;
                        break;
                }
                guiSettings[this.locName]["x"] = this.X;
                guiSettings[this.locName]["y"] = this.Y;
                saveGuiSettings(guiSettings);
            }
        });
            
        loadSettings(this);
    }

    setLines(lines) {
        this.textLines = lines
        this.longestString = ""
        this.stringCount = 0;
        this.textLines.forEach(text => {
            let longestLine = text.getString().split("\n").reduce((a, b) => a.length > b.length ? a : b);
            if (longestLine.length > this.longestString.length) {
                this.longestString = longestLine;
            }
            if (text.lineBreak) {
                this.stringCount += text.getString().split("\n").length;
            }
        });
        
        // this.text.setString(message);
    };

    openGui() {
        return this.gui.open();
    }

    setOffsetX(offset) {
        this.offsetX = offset;
    }

    setOffsetY(offset) {
        this.offsetY = offset;
    }

    isInOverlay(x, y) {
        // with offset
        if (editGui.isOpen() && this.exampleText != undefined) {
            let longestString = ""
            let stringCount = 0;
            this.exampleText.getString().split("\n").forEach(line => {
                if (line.length > longestString.length) {
                    longestString = line;
                }
            });

            stringCount = this.exampleText.getString().split("\n").length;

            if (x >= this.X && x <= this.X + Renderer.getStringWidth(longestString) * this.scale + this.offsetX && y >= this.Y && y <= this.Y + 10 * this.scale * stringCount + this.offsetY) {
                return true;
            }
            return false;
        }
        else {
            if (x >= this.X && x <= this.X + Renderer.getStringWidth(this.longestString) * this.scale + this.offsetX && y >= this.Y && y <= this.Y + 10 * this.scale * this.stringCount + this.offsetY) {
                return true;
            }
            return false;
        }
    }

}

register("command", () => {
    // overLaysNew.forEach(overlay => {
    //     overlay.openGui();
    // });
    editGui.open();
}).setName("sbotestgui");


///////////// examples




export let blazeLootTrackerExample = 
`${YELLOW}${BOLD}Blaze Loot Tracker
-------------------
${WHITE}${BOLD}Ice-Flavored:
${WHITE}${BOLD}Fire Aspect III:
${GREEN}${BOLD}Flawed Opal Gems:
${BLUE}${BOLD}Lavatears Runes:
${BLUE}${BOLD}Mana Disintegrator:
${BLUE}${BOLD}Kelvin Inverter:
${BLUE}${BOLD}Blaze Rod Dist:
${BLUE}${BOLD}Glowstone Dist:
${BLUE}${BOLD}Magma Cream Dist:
${BLUE}${BOLD}Nether Wart Dist:
${BLUE}${BOLD}Gabagool Dist:
${DARK_PURPLE}${BOLD}Magma Arrows:
${DARK_PURPLE}${BOLD}Archfiend Dice
${GOLD}${BOLD}Fiery Burst Rune:
${GOLD}${BOLD}Scorched Power:
${GOLD}${BOLD}Engineering Plans:
${GOLD}${BOLD}Subzero Inverter:
${GOLD}${BOLD}High Class Dice:
${LIGHT_PURPLE}${BOLD}Duplex:
${LIGHT_PURPLE}${BOLD}Scorched Books:
${GRAY}${BOLD}Blaze Killed: 
`

const mythosMobHpExample = new OverlayTextLine(`&8[&7Lv750&8] &2Exalted Minos Inquisitor &a40M&f/&a40M`)
const fossilExample = new OverlayTextLine(`Possible Fossils: Unknown`)
const effectsGuiExample = new OverlayTextLine(`&6Active Effects\n&bWisp's Water: &f2520s`)
const kuudraExampleOne = new OverlayTextLine(`&6&l600.00K &cCrimson Chestplate &7(BL 5/BR 4 - &6600.00K/600.00K&7)\n&6&l2.50M &cTerror Boots &7(ER 5/DO 4 - &61.48M/2.50M&7)\n&7Total Value: &62.1M coins`)
const kuudraExampleTwo = new OverlayTextLine(`&6&l2.49M &cTerror Chestplate\n&7(BL 5/BR 4 - &6100.00K/2.49M)\n&6&l2.50M &cTerror Boots\n&7(ER 5/DO 4 - &61.48M/2.50M)\n&7Total Value: &64.99M coins`)


let overlayExamples = {
    kuudraExampleTwo: kuudraExampleTwo,
    kuudraExampleOne: kuudraExampleOne,
    fossilExample: fossilExample,
    effectsGuiExample: effectsGuiExample,
    mythosMobHpExample: mythosMobHpExample,
};