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
}
