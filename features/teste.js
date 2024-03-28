import {
    SiblingConstraint,
    FillConstraint,
    CenterConstraint,
    SubtractiveConstraint,
    AdditiveConstraint,
    PixelConstraint,
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
    UIRoundedRectangle,
} from "../../Elementa";

export function getTestUI(){
    return testBlock;
}
export function getTestGUISelected(){
    return testGUISelected;
}
let testGUISelected = false;
const Color = Java.type("java.awt.Color");
const dragOffset2 = { x: 0, y: 0 };
let textString = "Hello, World!";
let testText = new UIWrappedText(textString)
let testBlock = new UIBlock(new Color(0.2, 0.2, 0.2, 0));
testBlock.setX((5).pixels());
testBlock.setY((5).pixels());
testBlock.setWidth(new AdditiveConstraint(new ChildBasedSizeConstraint(), new PixelConstraint(2)));
testBlock.setHeight(new AdditiveConstraint(new ChildBasedSizeConstraint(), new PixelConstraint(2)));
testBlock.onMouseClick((comp, event) => {
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
testText.setX(new SiblingConstraint());
testText.setY(new SiblingConstraint());
testBlock.addChild(testText)