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
const gui = new Gui();
const renderWindow = new Window()
const postWindow = new Window()
const Color = Java.type("java.awt.Color");
this.gui.registerClicked((x,y,b) => this.window.mouseClick(x,y,b));
this.gui.registerMouseDragged((x, y, b) => this.window.mouseDrag(x, y, b));
this.gui.registerMouseReleased((x, y, b) => this.window.mouseRelease(x,y,b));

let testGUISelected = false;

const dragOffset2 = { x: 0, y: 0 };
let textString = "Hello, World!";
let testText = new UIText(textString)
let testBlock = new UIBlock(new Color(0.2, 0.2, 0.2, 0.5));
testBlock.setX((5).pixels());
testBlock.setY((5).pixels());
testBlock.setWidth((100).pixels());
testBlock.setHeight((100).pixels());
testBlock.onMouseClick((comp, event) => {
    print("clicked");
    testGUISelected = true;
    dragOffset2.x = event.absoluteX;
    dragOffset2.y = event.absoluteY;
});
testBlock.onMouseRelease(() => {
    testGUISelected = false;
});
testBlock.onMouseDrag((comp, mx, my) => {
    if (!testGUISelected) return;
    const absoluteX = mx + comp.getLeft();
    const absoluteY = my + comp.getTop();
    const dx = absoluteX - dragOffset2.x;
    const dy = absoluteY - dragOffset2.y;
    dragOffset2.x = absoluteX;
    dragOffset2.y = absoluteY;
    const newX = testBlock.getLeft() + dx;
    const newY = testBlock.getTop() + dy;
    testBlock.setX(newX.pixels());
    testBlock.setY(newY.pixels());
});
testText.onMouseEnter((comp) => {
    testText.setText("Goodbye, World!");
});
testText.onMouseLeave((comp) => {
    testText.setText("Hello, World!");
});


renderWindow.addChildren(testBlock);
testText.setX(new CenterConstraint());
testText.setY(new CenterConstraint());
testBlock.addChild(testText)

const dragOffset = { x: 0, y: 0 };

let guiIsSelected = false;
var mainUIContainer = new UIRoundedRectangle(3)
mainUIContainer.setX((5).pixels())
mainUIContainer.setY((5).pixels())
mainUIContainer.setWidth((370).pixels())
mainUIContainer.setHeight((75).pixels())
mainUIContainer.setColor(new ConstantColorConstraint(new Color(0.2,0.2,0.2,0.5)));
mainUIContainer.onMouseClick((comp, event) => {
    print("clicked");
    guiIsSelected = true;
    dragOffset.x = event.absoluteX;
    dragOffset.y = event.absoluteY;
});
mainUIContainer.onMouseRelease(() => {
    guiIsSelected = false;
});
mainUIContainer.onMouseDrag((comp, mx, my) => {
    if (!guiIsSelected) return;
    const absoluteX = mx + comp.getLeft();
    const absoluteY = my + comp.getTop();
    const dx = absoluteX - dragOffset.x;
    const dy = absoluteY - dragOffset.y;
    dragOffset.x = absoluteX;
    dragOffset.y = absoluteY;
    const newX = mainUIContainer.getLeft() + dx;
    const newY = mainUIContainer.getTop() + dy;
    mainUIContainer.setX(newX.pixels());
    mainUIContainer.setY(newY.pixels());
})
mainUIContainer.addChild(new UIText("Settings").setX(new CenterConstraint()).setY((5).pixels()))

renderWindow.addChild(mainUIContainer);


function guiMover() {
    if (gui.isOpen()) {
        //Methode um examples zu setten hier hin
        // postWindow.draw();
        Renderer.drawRect(
            Renderer.color(0, 0, 0, 70),
            0,
            0,
            Renderer.screen.getWidth(),
            Renderer.screen.getHeight()
        );
    }
}
register("command", () => GuiHandler.openGui(gui)).setName("testnewhud");
register('renderOverlay', () => {
    guiMover();
    renderWindow.draw()
});
//zweiwindows für beidefälle
// register('postGuiRender', () => {
//     postWindow.draw()
// });

register('worldUnload', () => {
    closeEditing();
});

function closeEditing(){
    testGUISelected = false;
    guiIsSelected = false;
    gui.close();
}


// let textString = "Hello, World!";
// let testText = new UIText(textString)
// let testBlock = new UIBlock(new Color(0.2, 0.2, 0.2, 0.5));
// testBlock.setX((5).pixels());
// testBlock.setY((5).pixels());
// testBlock.setWidth((100).pixels());
// testBlock.setHeight((100).pixels());
// testText.onMouseClick((comp) => {
//     //replace hellow world text with new text
//     print("clicked");
//     testText.setText("Goodbye, World!");
// });


// window.addChild(testBlock);
// testText.setX(new CenterConstraint());
// testText.setY(new CenterConstraint());
// testBlock.addChild(testText)


// function dragFunction(x, y) {
//     testBlock.setX((x).pixels());
//     testBlock.setY((y).pixels());
// }
// testGUI.registerMouseDragged(dragFunction);

// register("command", function () {
//     testGUI.open()
// }).setName("testgui");