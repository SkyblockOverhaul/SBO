import { drawRectangleOutline as outline, rect, color, line, TextClass, Button, getActiveUsers } from "../../utils/functions";
import { createParty, getAllParties } from "../Diana/PartyFinder";
import { request } from "../../../requestV2";

const PartyFinderGUI = new Gui()
const CreatePartyGUI = new Gui()
const HdwiGUI = new Gui()
const PartyInfoGUI = new Gui()
let currentPage = 1
let pageCount = 0 
let onlineUsers = 0
let partyCount = 0
let partyList = []
let joinButtons = []
let partyInfoButtons = []
let allButtons = []
let maxMembers = 6
let startDisplayWidth = Renderer.screen.getWidth()
let startDisplayHeight = Renderer.screen.getHeight()

PartyFinderGUI.registerDraw(partyFinderRender);
PartyFinderGUI.registerClosed(partyFinderClose);
CreatePartyGUI.registerDraw(createPartyRender);
HdwiGUI.registerDraw(hdwiRender);
// PartyInfoGUI.registerDraw();

function getPartyFinderData(refresh = false) {
    getActiveUsers(true, (userCount) => {
        onlineUsers = userCount
    });
    request({
        url: "https://api.skyblockoverhaul.com/getAllParties",
        json: true
    }).then((response)=> {
        partyList = response.Parties;
        if (partyList.length == 0) {
            ChatLib.chat("&6[SBO] &eNo parties found. Try again later.");
        }
        else {
            partyCount = partyList.length;
            pageCount = Math.ceil(partyCount / 6);
            updatePageButtons()
            if (refresh) ChatLib.chat("&6[SBO] &eRefreshed.")
        }
    }).catch((error)=> {
        if (error.detail) {
            ChatLib.chat("&6[SBO] &4Error: " + error.detail);
        } else {
            console.error(JSON.stringify(error));
            ChatLib.chat("&6[SBO] &4Unexpected error occurred while getting all parties");
        }
    });
}

function updatePageButtons() {
    let layoutData = getLayoutData()
    let startIndex = (currentPage - 1) * 6;
    let endIndex = startIndex + 6;
    let partiesToDisplay = partyList.slice(startIndex, endIndex);
    joinButtons = []
    partyInfoButtons = []
    allButtons = []

    partiesToDisplay.forEach((party, index) => {
        let partyBoxY = layoutData.partyBoxY + (layoutData.partyBoxHeight * index)
        let joinX = layoutData.partyBoxX + (layoutData.partyBoxWidth / 1.21)
        let joinY = partyBoxY + (layoutData.partyBoxHeight / 4);  
        let partyInfoButton = new Button(layoutData.partyBoxX, partyBoxY, layoutData.partyBoxWidth, layoutData.partyBoxHeight, "", false, true, true, "partyInfo", false).onClick(() => {
            ChatLib.chat(`Leader: ${party.leaderName}`)
        }); 
        // let joinButton = new Button(joinX, joinY, 90, 20, "Join Party", false, true, true, "join", () => {
        //     ChatLib.chat(`Joining party led by ${party.leaderName}`);
        //     print("Joining party led by " + party.leaderName)
        // });
        let joinButton = new Button(joinX, joinY, 90, 20, "Join Party", false, true, true, "join").onClick(() => {
            ChatLib.chat(`Joining party led by ${party.leaderName}`);
            print("Joining party led by " + party.leaderName)
        }).updateDimensions();
        joinButtons.push(joinButton);
        partyInfoButtons.push(partyInfoButton);
        allButtons.push(joinButton);
        allButtons.push(partyInfoButton);
    });
}
 
PartyFinderGUI.registerOpened(() => {
    getPartyFinderData()
});

PartyFinderGUI.registerClicked((mouseX, mouseY, button) => {
    if (hdiwButton.isClicked(mouseX, mouseY, button)) return;
    if (refreshButton.isClicked(mouseX, mouseY, button)) return;
    if (pageBackButton.isClicked(mouseX, mouseY, button)) return;
    if (pageNextButton.isClicked(mouseX, mouseY, button)) return;
    if (createPartyButton.isClicked(mouseX, mouseY, button)) return;
    for (let joinButton of joinButtons) {
        if (joinButton.isClicked(mouseX, mouseY, button)) {
            return;
        }
    }
    for (let partyInfoButton of partyInfoButtons) {
        if (partyInfoButton.isClicked(mouseX, mouseY, button)) {
            return;
        }
    }
});
CreatePartyGUI.registerClicked((mouseX, mouseY, button) => {
    if (submitPartyButton.isClicked(mouseX, mouseY, button)) return;
});
HdwiGUI.registerClicked((mouseX, mouseY, button) => {
    if (backButton.isClicked(mouseX, mouseY, button)) return;
});

function drawButtonsMain(layoutData) {
    const [mouseX, mouseY] = [Client.getMouseX(), Client.getMouseY()]
    hdiwButton.customize({
        x: layoutData.hdwiX, y: layoutData.hdwiY,
        width: layoutData.hdwiWidth, height: layoutData.hdwiHeight
    }).draw(mouseX, mouseY)
    refreshButton.customize({
        x: layoutData.refreshX, y: layoutData.refreshY,
        width: layoutData.refreshWidth, height: layoutData.refreshHeight
    }).draw(mouseX, mouseY)
    pageBackButton.customize({
        x: layoutData.pageBackX, y: layoutData.pageBackY,
        width: layoutData.pageBackWidth, height: layoutData.pageBackHeight
    }).draw(mouseX, mouseY)
    pageNextButton.customize({
        x: layoutData.pageNextX, y: layoutData.pageNextY,
        width: layoutData.pageNextWidth, height: layoutData.pageNextHeight
    }).draw(mouseX, mouseY)
    createPartyButton.customize({
        x: layoutData.createPartyX, y: layoutData.createPartyY,
        width: layoutData.createPartyWidth, height: layoutData.createPartyHeight
    }).draw(mouseX, mouseY)
}
function drawButtonsCreate(layoutData) {
    const [mouseX, mouseY] = [Client.getMouseX(), Client.getMouseY()]
    submitPartyButton.customize({
        x: layoutData.createPartyX, y: layoutData.createPartyY,
        width: layoutData.createPartyWidth, height: layoutData.createPartyHeight
    }).draw(mouseX, mouseY)
}
function drawButtonsHdwi(layoutData) {
    const [mouseX, mouseY] = [Client.getMouseX(), Client.getMouseY()]
    backButton.customize({
        x: layoutData.createPartyX, y: layoutData.createPartyY,
        width: layoutData.createPartyWidth, height: layoutData.createPartyHeight
    }).draw(mouseX, mouseY)
}

const hdiwButton = new Button(0, 0, 90, 20, "How does it Work", false, true, true).onClick(() => {
    HdwiGUI.open()
});
const refreshButton = new Button(0, 0, 90, 20, "Refresh", false, true, true).onClick(() => {
    getPartyFinderData(true)
});
const pageBackButton = new Button(0, 0, 90, 20, "<=", false, true, true).onClick(() => {
    if (currentPage > 1) {
        currentPage -= 1
        updatePageButtons()
    }
});
const pageNextButton = new Button(20, 20, 90, 20, "=>", false, true, true).onClick(() => {
    if (currentPage < pageCount) {
        currentPage += 1
        updatePageButtons()
    }
});
const createPartyButton = new Button(0, 0, 90, 20, "Create Party", false, true, true).onClick(() => {
    CreatePartyGUI.open()
});
const submitPartyButton = new Button(0, 0, 90, 20, "Create", false, true, true).onClick(() => {
    createParty()
    PartyFinderGUI.open()
});
const backButton = new Button(0, 0, 90, 20, "Back", false, true, true).onClick(() => {
    PartyFinderGUI.open()
});

function getLayoutData() {
    let displayX = Renderer.screen.getWidth()
    let displayY = Renderer.screen.getHeight()

    let pfWindowWidth = displayX * 0.6
    let pfWindowHeight = displayY * 0.8
    let pfWindowX = (displayX - pfWindowWidth) / 2
    let pfWindowY = (displayY - pfWindowHeight) / 2

    let pfListWidth = pfWindowWidth
    let pfListHeight = pfWindowHeight * 0.85
    let pfListX = pfWindowX + (pfWindowWidth - pfListWidth) / 2
    let pfListY = pfWindowY + (pfWindowHeight - pfListHeight) / 2

    let titleX = pfWindowX * 1.05
    let titleY = pfWindowY * 1.1

    let onlineUserX = titleX
    let onlineUserY = pfWindowY * 1.4

    let partyCountX = pfWindowX + pfWindowWidth
    let partyCountY = pfWindowY * 1.4

    let pageCountX = (pfWindowX + pfWindowWidth) * 0.6
    let pageCountY = (pfWindowY + pfWindowHeight) * 0.95

    let hdwiX = (pfWindowX + pfWindowWidth) * 0.869
    let hdwiY = pfWindowY * 1.1
    let hdwiWidth = displayX * 0.095
    let hdwiHeight = displayY * 0.04

    let refreshX = (pfWindowX + pfWindowWidth) * 0.77
    let refreshY = pfWindowY * 1.1
    let refreshWidth = displayX * 0.07
    let refreshHeight = displayY * 0.04

    let pageBackX = (pfWindowY + pfWindowWidth) * 0.334
    let pageBackY = (pfWindowY + pfWindowHeight) * 0.945
    let pageBackWidth = displayX * 0.05
    let pageBackHeight = displayY * 0.04

    let pageNextX = (pfWindowY + pfWindowWidth) * 0.43
    let pageNextY = (pfWindowY + pfWindowHeight) * 0.945
    let pageNextWidth = displayX * 0.05
    let pageNextHeight = displayY * 0.04

    let createPartyX = (pfWindowX + pfWindowWidth) * 0.869
    let createPartyY = (pfWindowY + pfWindowHeight) * 0.945
    let createPartyWidth = displayX * 0.095
    let createPartyHeight = displayY * 0.04

    let joinPartyWidth = displayX * 0.095
    let joinPartyHeight = displayY * 0.04

    let partyBoxWidth = pfListWidth
    let partyBoxHeight = pfListHeight / 6
    let partyBoxX = pfListX
    let partyBoxY = pfListY

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
        pageCountX, pageCountY,
        partyBoxWidth, partyBoxHeight, partyBoxX, partyBoxY,
        joinPartyWidth, joinPartyHeight
    }
}

const pfText = new TextClass(color(255, 255, 255, 255), 0, 0, "", 1.75, true)
const onlineUserText = new TextClass(color(255, 255, 255, 255), 0, 0, ``, 1, false)
const partyCountText = new TextClass(color(255, 255, 255, 255), 0, 0, ``, 1, false)
const pageCountText = new TextClass(color(255, 255, 255, 255), 0, 0, ``, 1, false)
const leaderText = new TextClass(color(255, 255, 255, 255), 0, 0, ``, 1, false)
const membersText = new TextClass(color(255, 255, 255, 255), 0 + 5, 0, ``, 1, false)
const partyReqs = new TextClass(color(255, 255, 255, 255), 0, 0, ``, 1, false)



function partyFinderRender() {
    let layoutData = getLayoutData()

    if (startDisplayWidth !== Renderer.screen.getWidth() || startDisplayHeight !== Renderer.screen.getHeight()) {
        startDisplayWidth = Renderer.screen.getWidth()
        startDisplayHeight = Renderer.screen.getHeight()
        updatePageButtons()
    }

    rect(color(80, 80, 80, 245), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight);
    outline(color(0, 173, 255, 255), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight, 1);
    line(color(0, 173, 255, 255), layoutData.pfWindowX, layoutData.pfWindowY * 1.6, layoutData.pfWindowX + layoutData.pfWindowWidth, layoutData.pfWindowY * 1.6, 1)

    rect(color(0, 0, 0, 0), layoutData.pfListX, layoutData.pfListY, layoutData.pfListWidth, layoutData.pfListHeight);
    outline(color(0, 173, 255, 255), layoutData.pfListX, layoutData.pfListY, layoutData.pfListWidth, layoutData.pfListHeight, 1);

    if (partyList && partyList.length !== 0) {
        const startIndex = (currentPage - 1) * 6;
        const endIndex = startIndex + 6;
        const partiesToDisplay = partyList.slice(startIndex, endIndex);
        partyInfoButtons.forEach((button) => {
            button.draw(Client.getMouseX(), Client.getMouseY(), allButtons);
        });
        joinButtons.forEach((button) => {
            button.customize({
                width: layoutData.joinPartyWidth, height: layoutData.joinPartyHeight
            }).draw(Client.getMouseX(), Client.getMouseY(), allButtons);
        });
        partiesToDisplay.forEach((party, index) => {
            let partyBoxY = layoutData.partyBoxY + (layoutData.partyBoxHeight * index)
            let row1y = partyBoxY + (layoutData.partyBoxHeight / 5.5)
            let row2y = partyBoxY + (layoutData.partyBoxHeight / 5.5) * 2
            let row3y = partyBoxY + (layoutData.partyBoxHeight / 5.5) * 3
            outline(color(0, 173, 255, 255), layoutData.partyBoxX, partyBoxY, layoutData.partyBoxWidth, layoutData.partyBoxHeight, 1);
            leaderText.draw().setX(layoutData.partyBoxX + 5).setY(row1y).setText(`Leader: ${party.leaderName}`)
            membersText.draw().setX(layoutData.partyBoxX + 5).setY(row2y).setText(`Members: ${party.partymembers}/${maxMembers}`)
            partyReqs.draw().setX(layoutData.partyBoxX + 5).setY(row3y).setText(`Requirements: \nEman: ${party.emanreq}, lvl: ${party.lvlreq}`)
        });
    }

    pfText.draw().setX(layoutData.titleX).setY(layoutData.titleY).setText("Diana Party Finder")
    onlineUserText.draw().setX(layoutData.onlineUserX).setY(layoutData.onlineUserY).setText(`Online User: ${onlineUsers}`)
    let partyCountX = onlineUserText.width + layoutData.onlineUserX + 2
    partyCountText.draw().setX(partyCountX).setY(layoutData.partyCountY).setText(`Party Count: ${partyCount}`)
    pageCountText.draw().setX(layoutData.pageCountX).setY(layoutData.pageCountY).setText(`Page ${currentPage}/${pageCount}`)
    drawButtonsMain(layoutData);
}

function createPartyRender() {
    let layoutData = getLayoutData()
    rect(color(80, 80, 80, 245), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight);
    outline(color(0, 173, 255, 255), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight, 1);
    drawButtonsCreate(layoutData);
}

function hdwiRender() {
    let layoutData = getLayoutData()
    rect(color(80, 80, 80, 245), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight);
    outline(color(0, 173, 255, 255), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight, 1);
    drawButtonsHdwi(layoutData);
}

function partyFinderClose() {
    currentPage = 1
}

register("command", () => {
    currentPage = 1
    PartyFinderGUI.open()
}).setName("sbopartyfinder").setAliases("sbopf")