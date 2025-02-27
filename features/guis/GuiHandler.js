import { CenterConstraint, UIBlock, UIText, UIWrappedText } from "../../../Elementa";

export default class GuiHandler {
    static JavaColor = java.awt.Color
    static myComponentList = []

    static Color(color = [255, 255, 255, 255]) {
        const [r, g, b, a] = color
        return new this.JavaColor(r / 255, g / 255, b / 255, a / 255)
    }

    static percentToPixel(percent, value) {
        return (percent / 100) * value
    }

    static addHoverEffect(comp, baseColor, hoverColor = [50, 50, 50, 200]) {
        GuiHandler.myComponentList.push([comp, baseColor, hoverColor]);
        comp.onMouseEnter((comp, event) => {
            comp.setColor(GuiHandler.Color(hoverColor));
        }).onMouseLeave((comp, event) => {
            comp.setColor(GuiHandler.Color(baseColor));
        });
    }

    /**
     * @param {string} text // The text of the button
     * @param {number} x // The x position of the button
     * @param {number} y // The y position of the button
     * @param {number} width // The width of the button
     * @param {number} height // The height of the button
     * @param {string} color // The color of the button
     * @param {string} textColor // The color of the text
     * @param {...} outline // The outline of the button
     * @param {...} comp // The component the button should be a child of
     * @param {boolean} wrapped // If the text should be wrapped
     */
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
            this._registers();
        }
        
        setOnClick(callback) {
            this.callback = callback;
            return this;
        }

        _registers() {
            this.Object.onMouseClick((comp, event) => {
                if (this.callback) {
                    this.callback();
                }
            });
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

// === Fixes Overlapping Hover Effects ===

// register("tick", () => {
//     const hoveredComponents = GuiHandler.myComponentList.filter(
//         ([comp, baseColor, hoverColor]) => comp.isHovered()
//     );

//     let topComp = null;
//     if (hoveredComponents.length > 0) {
//         topComp = hoveredComponents.reduce((prev, curr) => {
//             return curr[0].depth() > prev[0].depth() ? curr : prev;
//         })[0];
//     }

//     GuiHandler.myComponentList.forEach(([comp, baseColor, hoverColor]) => {
//         if (comp.isHovered() && comp === topComp) {
//             comp.setColor(GuiHandler.Color(hoverColor));
//         } else {
//             comp.setColor(GuiHandler.Color(baseColor));
//         }
//     });
// });