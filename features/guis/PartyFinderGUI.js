import { drawRectangleOutline as outline, rect, color, line, TextClass, Button, getActiveUsers, getLayoutDataPartyFinder as getLayoutData, CheckBox } from "../../utils/functions";
import { createParty } from "../Diana/PartyFinder";
import { request } from "../../../requestV2";

const PartyFinderGUI = new Gui()
const CreatePartyGUI = new Gui()
const HdwiGUI = new Gui()
const PartyInfoGUI = new Gui()
let currentPage = 1
let pageCount = 0 
let onlineUsers = 0
let partyCount = 0
let originalPartyList = [];
let checkBoxList = []
let partyList = []
let joinButtons = []
let partyInfoButtons = []
let allPartyButtons = []
let buttonsPfwindow = []
let maxMembers = 6
let startDisplayWidth = Renderer.screen.getWidth()
let startDisplayHeight = Renderer.screen.getHeight()

PartyFinderGUI.registerDraw(partyFinderRender);
PartyFinderGUI.registerClosed(partyFinderClose);
CreatePartyGUI.registerDraw(createPartyRender);
PartyInfoGUI.registerDraw(partyInfoRender);
HdwiGUI.registerDraw(hdwiRender);

const filters = {
    "Eman9": (party) => party.emanreq >= 9
};

function filterPartyList() {
    partyList = [...originalPartyList];
    checkBoxList.forEach((checkBox) => {
        if (checkBox.checked) {
            let filterFunction = filters[checkBox.text];
            if (filterFunction) {
                partyList = partyList.filter(filterFunction);
            }
        }
    });
    updatePageButtons();
}

function getPartyFinderData(refresh = false) {
    getActiveUsers(true, (userCount) => {
        onlineUsers = userCount;
    });
    request({
        url: "https://api.skyblockoverhaul.com/getAllParties",
        json: true
    }).then((response) => {
        originalPartyList = response.Parties;
        partyList = [...originalPartyList];
        if (partyList.length === 0) {
            ChatLib.chat("&6[SBO] &eNo parties found. Try again later.");
        } else {
            partyCount = partyList.length;
            pageCount = Math.ceil(partyCount / 6);
            filterPartyList();
            updatePageButtons();
            if (refresh) ChatLib.chat("&6[SBO] &eRefreshed.");
        }
    }).catch((error) => {
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
    allPartyButtons = []

    partiesToDisplay.forEach((party, index) => {
        let partyBoxY = layoutData.partyBoxY + (layoutData.partyBoxHeight * index)
        let joinX = layoutData.hdwiX
        let joinY = partyBoxY + (layoutData.partyBoxHeight / 4);  
        let partyInfoButton = new Button(layoutData.partyBoxX, partyBoxY, layoutData.partyBoxWidth, layoutData.partyBoxHeight, "", false, true, true, color(255,255,255,255), "partyInfo", false).onClick(() => {
            openPartyInfo(party)
        }); 
        let joinButton = new Button(joinX, joinY, 90, 20, "Join Party", false, true, true, color(0,255,0,255), "join").onClick(() => {
            ChatLib.chat(`Joining party led by ${party.leaderName}`);
            print("Joining party led by " + party.leaderName)
        }).updateDimensions();
        joinButtons.push(joinButton);
        partyInfoButtons.push(partyInfoButton);
        allPartyButtons.push(joinButton);
        allPartyButtons.push(partyInfoButton);
    });
}

let currentPartyInfo
function openPartyInfo(party) {
    PartyInfoGUI.open()
    currentPartyInfo = party
}
 
PartyFinderGUI.registerOpened(() => {
    getPartyFinderData()
});

PartyFinderGUI.registerClicked((mouseX, mouseY, button) => {
    if (buttonClicked(mouseX, mouseY, button, buttonsPfwindow)) return;
    if (buttonClickedList(mouseX, mouseY, button, allPartyButtons)) return;
    if (emanCheckBox.isClicked(mouseX, mouseY)) return;
});
CreatePartyGUI.registerClicked((mouseX, mouseY, button) => {
    if (submitPartyButton.isClicked(mouseX, mouseY, button)) return;
});
HdwiGUI.registerClicked((mouseX, mouseY, button) => {
    if (backButton.isClicked(mouseX, mouseY, button)) return;
});
PartyInfoGUI.registerClicked((mouseX, mouseY, button) => {
    if (backButton.isClicked(mouseX, mouseY, button)) return;
});

function buttonClicked(mouseX, mouseY, Button, buttons) {
    for (let button of buttons) {
        if (button.isClicked(mouseX, mouseY, Button)) {
            return true;
        }
    }
    return false;
}

function buttonClickedList(mouseX, mouseY, Button, list) {
    for (let button of list) {
        if (button.isClicked(mouseX, mouseY, Button)) {
            return true;
        }
    }
    return false;
}

function drawButtonsMain(layoutData) {
    const [mouseX, mouseY] = [Client.getMouseX(), Client.getMouseY()]
    hdiwButton.customize({
        x: layoutData.hdwiX, y: layoutData.hdwiY,
        width: layoutData.hdwiWidth, height: layoutData.buttonHeight1
    }).draw(mouseX, mouseY)
    refreshButton.customize({
        x: layoutData.refreshX, y: layoutData.hdwiY,
        width: layoutData.refreshWidth, height: layoutData.buttonHeight1
    }).draw(mouseX, mouseY)
    pageBackButton.customize({
        x: layoutData.pageBackX, y: layoutData.createPartyY,
        width: layoutData.pageBackWidth, height: layoutData.buttonHeight1
    }).draw(mouseX, mouseY)
    pageNextButton.customize({
        x: layoutData.pageNextX, y: layoutData.createPartyY,
        width: layoutData.pageNextWidth, height: layoutData.buttonHeight1
    }).draw(mouseX, mouseY)
    createPartyButton.customize({
        x: layoutData.createPartyX, y: layoutData.createPartyY,
        width: layoutData.createPartyWidth, height: layoutData.buttonHeight1
    }).draw(mouseX, mouseY)
    deQueueButton.customize({
        x: layoutData.deQueueX, y: layoutData.createPartyY,
        width: layoutData.deQueueWidth, height: layoutData.buttonHeight1
    }).draw(mouseX, mouseY)
}
function drawButtonsCreate(layoutData) {
    const [mouseX, mouseY] = [Client.getMouseX(), Client.getMouseY()]
    submitPartyButton.customize({
        x: layoutData.createPartyX, y: layoutData.createPartyY,
        width: layoutData.createPartyWidth, height: layoutData.buttonHeight1
    }).draw(mouseX, mouseY)
}
function drawButtonsHdwi(layoutData) {
    const [mouseX, mouseY] = [Client.getMouseX(), Client.getMouseY()]
    backButton.customize({
        x: layoutData.createPartyX, y: layoutData.createPartyY,
        width: layoutData.createPartyWidth, height: layoutData.buttonHeight1
    }).draw(mouseX, mouseY)
}
function drawButtonsPartyInfo(layoutData) {
    const [mouseX, mouseY] = [Client.getMouseX(), Client.getMouseY()]
    backButton.customize({
        x: layoutData.createPartyX, y: layoutData.createPartyY,
        width: layoutData.createPartyWidth, height: layoutData.buttonHeight1
    }).draw(mouseX, mouseY)
}

const hdiwButton = new Button(0, 0, 90, 20, "How does it Work", false, true, true, color(255,165,0,255)).onClick(() => {
    HdwiGUI.open()
});
buttonsPfwindow.push(hdiwButton)
const refreshButton = new Button(0, 0, 90, 20, "Refresh", false, true, true, color(0,196,255,255)).onClick(() => {
    getPartyFinderData(true)
});
buttonsPfwindow.push(refreshButton)
const pageBackButton = new Button(0, 0, 90, 20, "<=", false, true, true, color(0,196,255,255)).onClick(() => {
    if (currentPage > 1) {
        currentPage -= 1
        updatePageButtons()
    }
});
buttonsPfwindow.push(pageBackButton)
const pageNextButton = new Button(20, 20, 90, 20, "=>", false, true, true, color(0,196,255,255)).onClick(() => {
    if (currentPage < pageCount) {
        currentPage += 1
        updatePageButtons()
    }
});
buttonsPfwindow.push(pageNextButton)
const createPartyButton = new Button(0, 0, 90, 20, "Create Party", false, true, true, color(0,196,255,255)).onClick(() => {
    CreatePartyGUI.open()
});
buttonsPfwindow.push(createPartyButton)
const deQueueButton = new Button(0, 0, 90, 20, "Dequeue", false, true, true, color(255,0,0,255)).onClick(() => {
    ChatLib.chat("Dequeueing")
});
buttonsPfwindow.push(deQueueButton)
const submitPartyButton = new Button(0, 0, 90, 20, "Create", false, true, true, color(255,255,255,255)).onClick(() => {
    createParty()
    PartyFinderGUI.open()
});
const backButton = new Button(0, 0, 90, 20, "Back", false, true, true, color(255,255,255,255)).onClick(() => {
    PartyFinderGUI.open()
});

const pfText = new TextClass(color(255, 255, 255, 255), 0, 0, "", 1.75, true)
const onlineUserText = new TextClass(color(233, 233, 233, 255), 0, 0, ``, 1, false)
const partyCountText = new TextClass(color(233, 233, 233, 255), 0, 0, ``, 1, false)
const pageCountText = new TextClass(color(233, 233, 233, 255), 0, 0, ``, 1, false)
const leaderText = new TextClass(color(255, 255, 255, 255), 0, 0, ``, 1, false)
const membersText = new TextClass(color(255, 255, 255, 255), 0 + 5, 0, ``, 1, false)
const partyReqs = new TextClass(color(255, 255, 255, 255), 0, 0, ``, 1, false)

const emanCheckBox = new CheckBox(0, 0, 0, 0, "", color(255, 255, 255, 255), color(255, 255, 255, 255), () => {
    filterPartyList()
});
checkBoxList.push(emanCheckBox)

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
            button.draw(Client.getMouseX(), Client.getMouseY(), allPartyButtons);
        });
        joinButtons.forEach((button) => {
            button.customize({
                width: layoutData.joinPartyWidth, height: layoutData.buttonHeight2,
            }).draw(Client.getMouseX(), Client.getMouseY(), allPartyButtons);
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
    let partyCountX = onlineUserText.width + layoutData.onlineUserX + 10
    partyCountText.draw().setX(partyCountX).setY(layoutData.onlineUserY).setText(`Partys: ${partyCount}`)
    pageCountText.draw().setX(layoutData.pageCountX).setY(layoutData.pageCountY).setText(`Page ${currentPage}/${pageCount}`)
    drawButtonsMain(layoutData);
    let checkBoxX = pfText.width + layoutData.titleX + 20
    emanCheckBox.draw().setText("Eman9")
    .setX(checkBoxX).setY(layoutData.pfWindowY * 1.15)
    .setHeight(7).setWidth(7);
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

function partyInfoRender() {
    let layoutData = getLayoutData()
    rect(color(80, 80, 80, 245), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight);
    outline(color(0, 173, 255, 255), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight, 1);
    Renderer.drawString(`Leader: ${currentPartyInfo.leaderName}`, layoutData.partyBoxX + 5, layoutData.partyBoxY + (layoutData.partyBoxHeight / 5.5));
    drawButtonsPartyInfo(layoutData)
}

function partyFinderClose() {
    currentPage = 1
}

PartyInfoGUI.registerKeyTyped((char, keyCode) => {
    if (keyCode === Keyboard.KEY_ESCAPE) {
        PartyFinderGUI.open()
    }
});
CreatePartyGUI.registerKeyTyped((char, keyCode) => {
    if (keyCode === Keyboard.KEY_ESCAPE) {
        PartyFinderGUI.open()
    }
});
HdwiGUI.registerKeyTyped((char, keyCode) => {
    if (keyCode === Keyboard.KEY_ESCAPE) {
        PartyFinderGUI.open()
    }
});

register("command", () => {
    currentPage = 1
    PartyFinderGUI.open()
}).setName("sbopartyfinder").setAliases("sbopf")