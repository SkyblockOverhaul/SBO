import { drawRectangleOutline as outline, rect, color, line, TextClass, Button, getActiveUsers } from "../../utils/functions";

const PartyFinderGUI = new Gui()
let currentPage = 0
let pageCount = 100 // This will be calculated based on the number of parties
let onlineUsers = 0
let partyCount = 0
PartyFinderGUI.registerDraw(partyFinderRender);
PartyFinderGUI.registerClosed(partyFinderClose);

PartyFinderGUI.registerOpened(() => {
    getActiveUsers(true, (userCount) => {
        onlineUsers = userCount
    });
});

PartyFinderGUI.registerClicked((mouseX, mouseY, button) => {
    if (hdiwButton.isClicked(mouseX, mouseY, button)) return;
    if (refreshButton.isClicked(mouseX, mouseY, button)) return;
    if (pageBackButton.isClicked(mouseX, mouseY, button)) return;
    if (pageNextButton.isClicked(mouseX, mouseY, button)) return;
    if (createPartyButton.isClicked(mouseX, mouseY, button)) return;
});

function drawButtons(layoutData) {
    const [mouseX, mouseY] = [Client.getMouseX(), Client.getMouseY()]
    hdiwButton.customize({
        x: layoutData.hdwiX, y: layoutData.hdwiY,
        width: layoutData.hdwiWidth, height: layoutData.hdwiHeight
    }); hdiwButton.draw(mouseX, mouseY)
    refreshButton.customize({
        x: layoutData.refreshX, y: layoutData.refreshY,
        width: layoutData.refreshWidth, height: layoutData.refreshHeight
    }); refreshButton.draw(mouseX, mouseY)
    pageBackButton.customize({
        x: layoutData.pageBackX, y: layoutData.pageBackY,
        width: layoutData.pageBackWidth, height: layoutData.pageBackHeight
    }); pageBackButton.draw(mouseX, mouseY)
    pageNextButton.customize({
        x: layoutData.pageNextX, y: layoutData.pageNextY,
        width: layoutData.pageNextWidth, height: layoutData.pageNextHeight
    }); pageNextButton.draw(mouseX, mouseY)
    createPartyButton.customize({
        x: layoutData.createPartyX, y: layoutData.createPartyY,
        width: layoutData.createPartyWidth, height: layoutData.createPartyHeight
    }); createPartyButton.draw(mouseX, mouseY)
}

const hdiwButton = new Button(0, 0, 90, 20, "How does it Work", false, true, true, (button) => {
    ChatLib.chat("Button clicked");
});
const refreshButton = new Button(0, 0, 90, 20, "Refresh", false, true, true, (button) => {
    ChatLib.chat("Button clicked");
});
const pageBackButton = new Button(0, 0, 90, 20, "<=", false, true, true, (button) => {
    if (currentPage > 0)
        currentPage -= 1
});
const pageNextButton = new Button(20, 20, 90, 20, "=>", false, true, true, (button) => {
    if (currentPage < pageCount)
        currentPage += 1
});
const createPartyButton = new Button(0, 0, 90, 20, "Create Party", false, true, true, (button) => {
    ChatLib.chat("Button clicked");
});

function getGuiScaleData() {
    let guiScale = Client.settings.video.getGuiScale();
    let partyCountComp;
    if (guiScale === 1) {
        partyCountComp = 1.85;
    } else if (guiScale === 3) {
        partyCountComp = 0.65;
    } else {
        partyCountComp = 1;
    }
    return {
        partyCountComp
    }
}

function getLayoutData() {
    let displayX = Renderer.screen.getWidth()
    let displayY = Renderer.screen.getHeight()
    let guiScaleData = getGuiScaleData()

    let pfWindowWidth = displayX * 0.7
    let pfWindowHeight = displayY * 0.8
    let pfWindowX = (displayX - pfWindowWidth) / 2
    let pfWindowY = (displayY - pfWindowHeight) / 2

    let pfListWidth = pfWindowWidth * 0.9
    let pfListHeight = pfWindowHeight * 0.85
    let pfListX = pfWindowX + (pfWindowWidth - pfListWidth) / 2
    let pfListY = pfWindowY + (pfWindowHeight - pfListHeight) / 2

    let titleX = pfWindowX * 1.05
    let titleY = pfWindowY * 1.1

    let onlineUserX = titleX
    let onlineUserY = pfWindowY * 1.4

    let partyCountX = pfWindowX + 90 * guiScaleData.partyCountComp
    let partyCountY = pfWindowY * 1.4

    let hdwiX = (pfWindowX + pfWindowWidth) * 0.847
    let hdwiY = pfWindowY * 1.1
    let hdwiWidth = displayX * 0.095
    let hdwiHeight = displayY * 0.04

    let refreshX = (pfWindowX + pfWindowWidth) * 0.76
    let refreshY = pfWindowY * 1.1
    let refreshWidth = displayX * 0.07
    let refreshHeight = displayY * 0.04

    let pageBackX = pfWindowX * 1.233
    let pageBackY = (pfWindowY + pfWindowHeight) * 0.945
    let pageBackWidth = displayX * 0.05
    let pageBackHeight = displayY * 0.04

    let pageNextX = pfWindowX * 1.6
    let pageNextY = (pfWindowY + pfWindowHeight) * 0.945
    let pageNextWidth = displayX * 0.05
    let pageNextHeight = displayY * 0.04

    let createPartyX = (pfWindowX + pfWindowWidth) * 0.847
    let createPartyY = (pfWindowY + pfWindowHeight) * 0.945
    let createPartyWidth = displayX * 0.095
    let createPartyHeight = displayY * 0.04

    let pageCountX = (pfWindowX + pfWindowWidth) * 0.55
    let pageCountY = (pfWindowY + pfWindowHeight) * 0.95

    return {
        displayX, displayY, 
        pfWindowWidth, pfWindowHeight, pfWindowX, pfWindowY, 
        pfListWidth, pfListHeight, pfListX, pfListY,
        titleX, titleY, onlineUserX, onlineUserY,
        hdwiX, hdwiY, hdwiWidth, hdwiHeight,
        partyCountX, partyCountY,
        refreshX, refreshY, refreshWidth, refreshHeight,
        pageBackX, pageBackY, pageBackWidth, pageBackHeight,
        pageNextX, pageNextY, pageNextWidth, pageNextHeight,
        createPartyX, createPartyY, createPartyWidth, createPartyHeight,
        pageCountX, pageCountY
    }
}

function partyFinderRender() {
    let layoutData = getLayoutData()

    rect(color(80, 80, 80, 245), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight);
    outline(color(0, 173, 255, 255), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight, 1);
    line(color(0, 173, 255, 255), layoutData.pfWindowX, layoutData.pfWindowY * 1.6, layoutData.pfWindowX + layoutData.pfWindowWidth, layoutData.pfWindowY * 1.6, 1)

    rect(color(0, 0, 0, 0), layoutData.pfListX, layoutData.pfListY, layoutData.pfListWidth, layoutData.pfListHeight);
    outline(color(0, 173, 255, 255), layoutData.pfListX, layoutData.pfListY, layoutData.pfListWidth, layoutData.pfListHeight, 1);

    const pfText = new TextClass(color(255, 255, 255, 255), layoutData.titleX, layoutData.titleY, "Diana Party Finder", 1.75, true); pfText.draw()
    const onlineUserText = new TextClass(color(255, 255, 255, 255), layoutData.onlineUserX, layoutData.onlineUserY, `Online User: ${onlineUsers}`, 1, false); onlineUserText.draw()
    const partyCountText = new TextClass(color(255, 255, 255, 255), layoutData.partyCountX, layoutData.partyCountY, `Party Count: ${partyCount}`, 1, false); partyCountText.draw()
    Renderer.drawString(`Page ${currentPage}/${pageCount}`, layoutData.pageCountX, layoutData.pageCountY)

    drawButtons(layoutData)
}

function partyFinderClose() {
    currentPage = 0
}

register("command", () => {
    currentPage = 0
    PartyFinderGUI.open()
}).setName("sbopartyfinder").setAliases("sbopf")