import settings from "../settings";
import { loadGuiSettings, printDev, saveGuiSettings, setTimeout } from "../utils/functions";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE, UNDERLINE} from "../utils/constants";
import { registerWhen } from "./variables";


const dragOffset = {x: 0, y: 0};

let guiSettings = loadGuiSettings();

let isInInventory = false;
let guiOpened = false;
export function isGuiOpened() {
    if (Client == undefined) return false;
    if (Client.currentGui == undefined) return false;
    if (Client.currentGui.get() == null) return false;
    return true
}
let currentGui = null;
register('guiClosed', (gui) => {
    guiOpened = false;
    gui = gui.toString();
    currentGui = null;
    if(gui.includes("Inventory")) {
        isInInventory = false;
    }
});

register('guiOpened', () => {
    guiOpened = true;
    setTimeout(() => {
        if (Client == undefined) return;
        if (Client.currentGui == undefined) return;
        if (Client.currentGui.get() == null) return;
        let openedgui = Client.currentGui.get().toString();
        currentGui = openedgui;
        if(openedgui.includes("Inventory")) {
            isInInventory = true;
        }
    }, 200);
});

function loadSettings(overlay) {
    if (guiSettings != undefined) {
        overlay.X = parseInt(guiSettings[overlay.locName]["x"]);
        overlay.Y = parseInt(guiSettings[overlay.locName]["y"]);
    }
}

let overLays = [];
let editGui = new Gui();

register('worldUnload', () => {
    closeEditing();
});

function closeEditing() {
    overLays.forEach(overlay => {
        overlay.selected = false;
    });
    editGui.close();
}

editGui.registerScrolled((x, y, delta) => {
    if (editGui.isOpen()) {
        overLays.forEach(overlay => {
            if (overlay.isInOverlay(x, y)) {
                overlay.scale = parseFloat(guiSettings[overlay.locName]["s"]);
                switch(delta) {
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
        overLays.forEach(overlay => {
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
        overLays.forEach(overlay => {
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
    overLays.forEach(overlay => {
        overlay.selected = false;
    });
    // save all gui settings
    saveGuiSettings(guiSettings);
});

export class OverlayTextLine {
    constructor(message, shadow = true, hoverable = false) {
        this.text = new Text(message ?? "");
        this.text.setShadow(shadow);
        this.text.setScale(1.0);
        this.text.setAlign("LEFT")
        this.X = -1;
        this.Y = -1;
        this.scale = 1.0;
        this.lineBreak = true;
        this.hoverable = hoverable;
        this.isHovered = false;
        this.mouseEnterAction = undefined;
        this.mouseLeaveAction = undefined;
        this.hoverAction = undefined;

    }

    onMouseEnter(action) {
        this.mouseEnterAction = action;
        return this;
    }

    onMouseLeave(action) {
        this.mouseLeaveAction = action;
        return this;
    }

    onHover(action) { 
        this.hoverAction = action;
        return this;
    }

    hover(overlay) {
        if (this.hoverAction)
            this.hoverAction(overlay); 
    }

    mouseEnter() {
        if (this.mouseEnterAction)
            this.mouseEnterAction();
    }

    mouseLeave() {
        if (this.mouseLeaveAction)
            this.mouseLeaveAction();
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

    isOverString(x, y) {
        let stringCount = this.text.getString().split("\n").length;
        let longestLine = this.text.getString().split("\n").reduce((a, b) => a.length > b.length ? a : b);

        if (x >= this.X && x <= this.X + Renderer.getStringWidth(longestLine) * this.scale && y >= this.Y && y <= this.Y + 9 * this.scale * stringCount) {
            return true;
        }
        return false;
    }
}

export class OverlayButton extends OverlayTextLine {
    constructor(message, shadow, button, lineBreak = true, hoverable = false, delimiter = "&e | &r" ) {
        super(message, shadow, hoverable);
        this.button = button;
        this.action = undefined;
        this.lineBreak = lineBreak; 
        this.delimiter = new Text(delimiter).setShadow(shadow);
    }
    
    onClick(action) {
        this.action = action;
        return this;
    }

    clicked(cx, cy, button, gui) {
        if (this.action && this.X != -1 && this.Y != -1 && !editGui.isOpen()) {
            let stringCount = this.text.getString().split("\n").length;
            let longestLine = this.text.getString().split("\n").reduce((a, b) => a.length > b.length ? a : b);

            if (cx >= this.X && cx <= this.X + Renderer.getStringWidth(longestLine) * this.scale && cy >= this.Y && cy <= this.Y + 9 * this.scale * stringCount) {
                this.action();
            }
        }
    }

}

function drawText(overlay) {
    let lineCount = 0;
    let textLines = overlay.textLines;
    if (overlay.exampleText != undefined && editGui.isOpen()) { 
        if (overlay.name == "kuudraOverlay") {
            if (settings.lineSetting == 0) {
                overlay.exampleText = overlayExamples[overlay.example + "Two"];
            }
            else {
                overlay.exampleText = overlayExamples[overlay.example + "One"];
            }
        }
        textLines = [overlay.exampleText];
    }
    
    
    let hovered = [];
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
            if (text.getString().substring(0, 4) == "§7§m") {
                if (!editGui.isOpen()) {
                    text.draw();
                    lineCount += text.getString().split("\n").length;
                }
            }
            else {
                text.draw();
                lineCount += text.getString().split("\n").length;
            }
        }
        
        if (text.isHovered) {
            hovered.push(text);
        }
    });
    hovered.forEach(text => {
        text.hover(overlay);
    });
    
}

export class hoverText {
    constructor(message) {
        this.text = new Text(message);
        this.text.setShadow(false);
        this.text.setScale(1.0);
        this.text.setAlign("LEFT")
        this.X = 0;
        this.Y = 0;
        this.scale = 1.0;
    }

    draw() {
        this.text.draw();
        // draw ractangle around text
        Renderer.translate(0, 0, 10)
        Renderer.drawRect(Renderer.color(0, 0, 0, 100), this.X, this.Y, Renderer.getStringWidth(this.text.getString()) * this.scale, 10 * this.scale);
        // print("drawing")

    }

    setText(message) {
        this.text.setString(message);
        return this;
    }

    setXYScale(x, y, scale) {
        this.X = x;
        this.Y = y;
        this.scale = scale;
        this.text.setX(x);
        this.text.setY(y);
        this.text.setScale(scale);
        return this;
    }
}

export class SboOverlay {
    constructor(name, setting, type, locName, example = "", hoverable = false, allowedGuis = ["GuiInventory"]) {
        overLays.push(this);
        this.name = name;
        this.setting = setting;
        this.example = example;
        this.type = type;
        this.locName = locName;
        this.renderGui = true;
        this.selected = false;
        this.allowedGuis = allowedGuis;
        this.scale = parseFloat(guiSettings[locName]["s"]);
        this.X = 0;
        this.Y = 0;
        this.offsetX = 0;
        this.offsetY = 0;
        this.longestString = 0;
        this.stringCount = 0;
        this.editParameters = new Text("");
        this.exampleText = undefined;
        this.hoverable = hoverable;
        this.someTextIsHovered = false;
        if (example != "") {
            this.exampleText = overlayExamples[example];
            if (this.exampleText == undefined) {
                this.exampleText = new OverlayTextLine("example text not found");
            }
        }
        
        this.textLines = [];    

        this.gui = new Gui();

        registerWhen(register("renderOverlay", () => {
            if ((this.renderGui || editGui.isOpen()) && (this.type == "render" || (this.type == "inventory" && !isInInventory))) {
                drawText(this);
                if (editGui.isOpen()) {
                    this.editParameters.setString("&oX: " + this.X + ", Y: " + this.Y + ", S: " + this.scale);
                    this.editParameters.setX(this.X)
                    this.editParameters.setY(this.Y - 10)
                    this.editParameters.draw();
                    // Renderer.drawRect(Renderer.color(0, 0, 0, 100), this.X, this.Y, Renderer.getStringWidth(this.longestString) * this.scale + this.offsetX, 10 * this.scale * this.stringCount + this.offsetY);
                }
            }
        }), () => settings[this.setting]);

        registerWhen(register("postGuiRender", () => {
            if ((this.renderGui || editGui.isOpen()) && this.type == "post") {
                drawText(this)
                if (editGui.isOpen()) {
                    this.editParameters.setString("&oX: " + this.X + ", Y: " + this.Y + ", S: " + this.scale);
                    this.editParameters.setX(this.X)
                    this.editParameters.setY(this.Y - 10)
                    this.editParameters.draw();
                    // Renderer.drawRect(Renderer.color(0, 0, 0, 100), this.X, this.Y, Renderer.getStringWidth(this.longestString) * this.scale + this.offsetX, 10 * this.scale * this.stringCount + this.offsetY);
                }
            }
        }), () => settings[this.setting]);

        registerWhen(register("guiRender", () => {
            if ((this.renderGui || editGui.isOpen()) && (this.type == "inventory" && isInInventory)) {
                drawText(this)
            }
        }), () => settings[this.setting]);

        registerWhen(register("guiKey", (char, keyCode, gui, event) => {
            if (editGui.isOpen() ) {
                const mouseX = Client.getMouseX();
                const mouseY = Client.getMouseY();
                if (this.isInOverlay(mouseX, mouseY)) {
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
            }
        }), () => settings[this.setting]);
        
        registerWhen(register("tick", () => {
            if (this.renderGui) {
                if (this.textLines.length > 0 && !editGui.isOpen() && (this.isInAllowedGui() || this.someTextIsHovered)) {
                    const mouseX = Client.getMouseX();
                    const mouseY = Client.getMouseY();
                    this.textLines.forEach(text => {
                        if (text.hoverable) {
                            if (text.X != -1 && text.Y != -1 && (text.mouseEnterAction || text.mouseLeaveAction || text.hoverAction)) {
                                if (text.isOverString(mouseX, mouseY) && this.isInAllowedGui()) {
                                    if (!text.isHovered) {
                                        text.mouseEnter();
                                        text.isHovered = true;
                                        this.someTextIsHovered = true;
                                    }
                                }
                                else {
                                    if (text.isHovered) {
                                        text.mouseLeave();
                                        text.isHovered = false;
                                        this.someTextIsHovered = false;
                                    }
                                }
                            }
                        }
                    });
                }
            }
        }), () => settings[this.setting]) && this.hoverable;
        
        registerWhen(register("guiMouseClick" , (cx, cy, button, gui) => {
            if (this.renderGui) {
                if (this.textLines.length > 0 && !editGui.isOpen() && this.isInAllowedGui()) {
                    this.textLines.forEach(text => {
                        if (text.action) {
                            text.clicked(cx, cy, button, gui);
                        }
                    });
                }
            }
        }), () => settings[this.setting]);

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

    isInAllowedGui() {
        if (Client == undefined) return false;
        if (Client.currentGui == undefined) return false;
        if (Client.currentGui.get() == null) return false;
        let currentGui = Client.currentGui.get().toString();
        // currentGui = currentGui.toString() and the last string bevor the @ symbol and after the last dot
        currentGui = currentGui.substring(currentGui.lastIndexOf(".") + 1, currentGui.lastIndexOf("@"));
        return this.allowedGuis.includes(currentGui);
    }

    isInOverlay(x, y) {
        if (this.renderGui || editGui.isOpen()) {
            if (this.exampleText != undefined) {
                let longestString = ""
                let stringCount = 0;
                this.exampleText.getString().split("\n").forEach(line => {
                    if (line.length > longestString.length) {
                        longestString = line;
                    }
                });

                stringCount = this.exampleText.getString().split("\n").length;

                if (x >= this.X && x <= this.X + Renderer.getStringWidth(longestString) * this.scale + this.offsetX && y >= this.Y && y <= this.Y + 9 * this.scale * stringCount + this.offsetY) {
                    return true;
                }
                return false;
            }
            else {
                if (x >= this.X && x <= this.X + Renderer.getStringWidth(this.longestString) * this.scale + this.offsetX && y >= this.Y && y <= this.Y + 9 * this.scale * this.stringCount + this.offsetY) {
                    return true;
                }
                return false;
            }
        }
        else {
            return false;
        }
    }

}

register("command", () => {
    // overLaysNew.forEach(overlay => {
    //     overlay.openGui();
    // });
    editGui.open();
}).setName("sboguis").setAliases("sbomoveguis"); 

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
const kuudraExampleOne = new OverlayTextLine(`&6600.00k &eCrimson Chestplate &b(BL 5/BR 4 - &6600.00k/600.00k&7&b)\n&62.50m &eTerror Boots &b(ER 5/DO 4 - &61.48m/2.50m&7&b)\n&eTotal Value: &62.1m coins`)
const kuudraExampleTwo = new OverlayTextLine(`&62.49m &eTerror Chestplate\n&b(BL 5/BR 4 - &6100.00k/2.49m&b)\n&62.50m &eTerror Boots\n&b(ER 5/DO 4 - &61.48m/2.50m&b)\n&eTotal Value: &64.99m coins`)
const pickupLogExample = new OverlayTextLine(`&a+ 1x &fRotten Flesh \n&c- 1x &5Empty Thunder Bottle`)

let overlayExamples = {
    kuudraExampleTwo: kuudraExampleTwo,
    kuudraExampleOne: kuudraExampleOne,
    fossilExample: fossilExample,
    effectsGuiExample: effectsGuiExample,
    mythosMobHpExample: mythosMobHpExample,
    pickupLogExample: pickupLogExample
};