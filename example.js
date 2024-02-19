register("command", showGui).setName("showgui")
register("command", hideGui).setName("hidegui")



function showGui() {
    SimpleDisplay.show()
}
function hideGui() {
    SimpleDisplay.hide()
}

var mygui = new Gui()
mygui.registerDraw(drawMyGui)
function drawMyGui() {
    Renderer.drawRect(
        0xffff0000,
        Renderer.screen.getWidth() / 2 - 50,
        Renderer.screen.getHeight() / 2 - 50,
        100, 100
    )
}

var SimpleDisplay = new Display();
SimpleDisplay.hide();
SimpleDisplay.setBackground("full");
SimpleDisplay.setRenderLoc(150,150);
SimpleDisplay.setLine(0, "&cDiana Mob Kills");
SimpleDisplay.setLine(1, "&cInquis: ");