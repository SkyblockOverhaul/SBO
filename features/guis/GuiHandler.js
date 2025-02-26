import { CenterConstraint, UIBlock, UIText, UIWrappedText } from "../../../Elementa";

export default class GuiHandler {
    static JavaColor = java.awt.Color

    static Color(color = [255, 255, 255, 255]) {
        const [r, g, b, a] = color
        return new this.JavaColor(r / 255, g / 255, b / 255, a / 255)
    }

    static percentToPixel(percent, value) {
        return (percent / 100) * value
    }

    static addHoverEffect(comp, baseColor, hoverColor = [50, 50, 50, 200]) {
        comp.onMouseEnter((comp, event) => {
            comp.setColor(GuiHandler.Color(hoverColor));
        }).onMouseLeave((comp, event) => {
            comp.setColor(GuiHandler.Color(baseColor));
        });
    }

    static Button = class {
        constructor(text, x, y, width, height, color, textColor = false, outline = false, comp = false, wrapped = false) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = color;
            this.textColor = textColor;
            this.outline = outline;
            this.comp = comp;
            this.callback = undefined;
    
            this.Object = new UIBlock();
            this.textObject = wrapped ? new UIWrappedText(text) : new UIText(text);
            this._create();
        }
        
        setOnClick(callback) {
            this.callback = callback;
            return this;
        }
        
        click(mouseX, mouseY, button) {
            if (typeof this.callback === "function") {
                return this.callback(mouseX, mouseY, button);
            }
            return false;
        }
        
        isClicked(mouseX, mouseY) {
            const absX = this.Object.getLeft();
            const absY = this.Object.getTop();
            const absWidth = this.Object.getWidth();
            const absHeight = this.Object.getHeight();
            return (
                mouseX >= absX &&
                mouseX <= absX + absWidth &&
                mouseY >= absY &&
                mouseY <= absY + absHeight
            );
        }
        

        _create() {
            this.Object
                .setX(this.x)
                .setY(this.y)
                .setWidth(this.width)
                .setHeight(this.height)
                .setColor(GuiHandler.Color(this.color))
            if (this.outline) {
                this.Object.enableEffect(this.outline)
            }
            if (this.comp) {
                this.Object.setChildOf(this.comp)
            }

            this.textObject
                .setX(new CenterConstraint())
                .setY(new CenterConstraint())
                .setChildOf(this.Object)
            if (this.textColor) {
                this.textObject.setColor(GuiHandler.Color(this.textColor))
            }
        }
    }    
}
