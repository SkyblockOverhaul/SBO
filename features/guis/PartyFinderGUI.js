import { drawRectangleOutline as outline, text, rect, color, line, Button, getActiveUsers } from "../../utils/functions";

const PartyFinderGUI = new Gui()
let currentPage = 0
let onlineUsers = 0
let partyCount = 0
PartyFinderGUI.registerDraw(partyFinderRender);
PartyFinderGUI.registerClosed(partyFinderClose);
PartyFinderGUI.registerOpened(() => {
    getActiveUsers(true, (userCount) => {
        onlineUsers = userCount
    });
});

function getLayoutData() {
    let displayX = Renderer.screen.getWidth()
    let displayY = Renderer.screen.getHeight()

    let pfWindowWidth = displayX * 0.65
    let pfWindowHeight = displayY * 0.7
    let pfWindowX = (displayX - pfWindowWidth) / 2
    let pfWindowY = (displayY - pfWindowHeight) / 2

    let pfListWidth = pfWindowWidth * 0.9
    let pfListHeight = pfWindowHeight * 0.85
    let pfListX = pfWindowX + (pfWindowWidth - pfListWidth) / 2
    let pfListY = pfWindowY + (pfWindowHeight - pfListHeight) / 2

    let titleX = pfWindowX * 1.05
    let titleY = pfWindowY * 1.05

    let onlineUserX = titleX
    let onlineUserY = titleY * 1.18

    let boxWidth = 200
    let boxHeight = 35
    let spacingY = 10
    let spacingX = 0
    
    let columns = Math.floor((displayX * 0.75 - spacingX) / (boxWidth + spacingX));
    let rows = Math.floor((displayY * 0.6 - spacingY) / (boxHeight + spacingY));

    let totalWidth = columns * boxWidth + (columns - 1) * spacingX
    let totalHeight = rows * boxHeight + (rows - 1) * spacingY

    let startX = (displayX - totalWidth) / 2
    let startY = (displayY - totalHeight) / 2


    return {
        displayX, displayY, 
        boxWidth, boxHeight, spacingX, spacingY, columns, rows, totalWidth, totalHeight, startX, startY, 
        pfWindowWidth, pfWindowHeight, pfWindowX, pfWindowY, 
        pfListWidth, pfListHeight, pfListX, pfListY,
        titleX, titleY, onlineUserX, onlineUserY
    }
}

function partyFinderRender() {
    const layoutData = getLayoutData()

    rect(color(80, 80, 80, 245), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight);
    outline(color(0, 173, 255, 255), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight, 1);
    line(color(0, 173, 255, 255), layoutData.pfWindowX, layoutData.pfWindowY * 1.35, layoutData.pfWindowX + layoutData.pfWindowWidth, layoutData.pfWindowY * 1.35, 1)

    rect(color(0, 0, 0, 0), layoutData.pfListX, layoutData.pfListY, layoutData.pfListWidth, layoutData.pfListHeight);
    outline(color(0, 173, 255, 255), layoutData.pfListX, layoutData.pfListY, layoutData.pfListWidth, layoutData.pfListHeight, 1);

    text(color(255, 255, 255, 255), layoutData.titleX, layoutData.titleY, "Diana Party Finder", 2.2, true)
    text(color(255, 255, 255, 255), layoutData.onlineUserX, layoutData.onlineUserY, `Online User: ${onlineUsers}`, 1, false)
    text(color(255, 255, 255, 255), layoutData.onlineUserX * 1.38, layoutData.onlineUserY, `Party Count: ${partyCount}`, 1, false)

    textButton.draw()
}

function partyFinderClose() {
    currentPage = 0
}

register("command", () => {
    currentPage = 0
    PartyFinderGUI.open()
}).setName("sbopartyfinder").setAliases("sbopf")

const textButton = new Button(0, 0, 100, 30, "Click me", false, (button) => {
    ChatLib.chat("Button clicked");
});

PartyFinderGUI.registerClicked((mouseX, mouseY, button) => {
    if (textButton.isClicked(mouseX, mouseY, button)) return;
});