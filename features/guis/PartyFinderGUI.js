import { drawRectangleOutline as outline, rect, formatPlayerInfo, color, getDianaStats, line, TextClass, Button, getActiveUsers, getLayoutDataPartyFinder as getLayoutData, 
    CheckBox, isInSkyblock, pMmMColor, requirementsFormat, filterTextInput
} from "../../utils/functions";
import { createParty, removePartyFromQueue, getInQueue, sendJoinRequest, isInParty } from "../Diana/PartyFinder";
import { mainInputFields } from "../../utils/variables";
import { request } from "../../../requestV2";
import ElementUtils from "../../../DocGuiLib/core/Element"
import TextInputElement from "../../../DocGuiLib/elements/TextInput";
import HandleGui from "../../../DocGuiLib/core/Gui";
import { UIBlock, PixelConstraint } from "../../../Elementa"
import { isDataLoaded } from "../../utils/checkData";

const PartyFinderGUI = new Gui()
const HdwiGUI = new Gui()
const CreatePartyGUI = new HandleGui("data/DefaultColors.json", "sbo")
const PartyInfoGUI = new Gui()
let currentPage = 1
let pageCount = 0 
let onlineUsers = 0
let partyCount = 0
let originalPartyList = [];
let checkBoxList = []
let checkBoxListCreate = []
let partyList = []
let joinButtons = []
let partyInfoButtons = []
let allPartyButtons = []
let buttonsPfwindow = []
let startDisplayWidth = Renderer.screen.getWidth()
let startDisplayHeight = Renderer.screen.getHeight()

PartyFinderGUI.registerDraw(partyFinderRender);
PartyFinderGUI.registerClosed(partyFinderClose);
CreatePartyGUI.registers.onDraw(createPartyRender);
PartyInfoGUI.registerDraw(partyInfoRender);
HdwiGUI.registerDraw(helpRender);

const filters = {
    "Eman9": (party) => {
        const requirements = party.reqs;
        return requirements.eman9 === true;
    },
    "MVP+": (party) => {
        const requirements = party.reqs;
        return requirements.mvpplus === true;
    },
    "Looting 5": (party) => {
        const requirements = party.reqs;
        return requirements.looting5 === true;
    },
    "Can I Join": (party) => {
        const requirements = party.reqs;
        let myReqs = dianaStats
        if (myReqs === undefined) return false;
        return (
            (requirements.lvl === undefined || myReqs.sbLvl >= requirements.lvl) &&
            (requirements.kills === undefined || myReqs.mythosKills >= requirements.kills) &&
            (requirements.eman9 === undefined || myReqs.eman9 === requirements.eman9) &&
            (requirements.looting5 === undefined || myReqs.looting5daxe === requirements.looting5)
        );
    }
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
    partyCount = partyList.length;
    pageCount = Math.ceil(partyCount / 6);
    if (currentPage > pageCount) {
        currentPage = pageCount;
    }
    else if (currentPage < 1) {
        currentPage = 1;
    }
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
            partyCount = partyList.length;
            pageCount = Math.ceil(partyCount / 6);
            if (refresh) ChatLib.chat("&6[SBO] &eRefreshed.");
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
            if (!getInQueue() && !isInParty()) {
                sendJoinRequest(party.leaderName, party.reqs)
            }
            else {
                let leaderCheck = party.leaderName === Player.getName()
                if (getInQueue() && !isInParty() && !leaderCheck) ChatLib.chat("&6[SBO] &eYou are already in queue.")
                if (isInParty() && !getInQueue() && !leaderCheck) ChatLib.chat("&6[SBO] &eYou are already in a party.")
                if (leaderCheck) ChatLib.chat("&6[SBO] &eYou can't join your own party.")
            }
        }).updateDimensions()
        joinButtons.push(joinButton);
        partyInfoButtons.push(partyInfoButton);
        allPartyButtons.push(joinButton);
        allPartyButtons.push(partyInfoButton);
    });
}

let currentMemberList = []
function openPartyInfo(party) {
    currentMemberList = []
    PartyInfoGUI.open()
    party.partyinfo.forEach((memberObject) => {
        let memberButton = new Button(0, 0, 90, 20, memberObject.name, false, true, true, color(255,255,255,255), "", true, 2)
        let object = {
            button: memberButton,
            memberObject: memberObject
        }
        currentMemberList.push(object)
    })
}
 
let dianaStats
PartyFinderGUI.registerOpened(() => {
    textObject.setText("")
    dianaStats = getDianaStats()
    getPartyFinderData()
});

CreatePartyGUI.registers.onOpen(() => {
    loadInputFieldState()
});

CreatePartyGUI.registers.onClose(() => {
    saveInputFieldState()
});

PartyFinderGUI.registerClicked((mouseX, mouseY, button) => {
    if (buttonClicked(mouseX, mouseY, button, buttonsPfwindow)) return;
    if (buttonClickedList(mouseX, mouseY, button, allPartyButtons)) return;
    if (checkBoxClicked(mouseX, mouseY, button, checkBoxList)) return;
});
CreatePartyGUI.registers.onMouseClick((mouseX, mouseY, button) => {
    if (submitPartyButton.isClicked(mouseX, mouseY, button)) return;
    if (checkBoxClicked(mouseX, mouseY, button, checkBoxListCreate)) return;
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

function checkBoxClicked(mouseX, mouseY, button, checkBoxes) {
    for (let checkBox of checkBoxes) {
        if (checkBox.isClicked(mouseX, mouseY, button)) {
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
        x: layoutData.createWindowX, y: layoutData.createPartyButtonY,
        width: layoutData.createWindowWidth, height: layoutData.createPartyButtonHeight
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
        x: layoutData.partyBackButtoX, y: layoutData.createPartyY,
        width: layoutData.createPartyWidth, height: layoutData.buttonHeight1
    }).draw(mouseX, mouseY)
}

const hdiwButton = new Button(0, 0, 90, 20, "Help", false, true, true, color(255,165,0,255)).onClick(() => {
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
    CreatePartyGUI.ctGui.open()
});
buttonsPfwindow.push(createPartyButton)
let dequeued
const deQueueButton = new Button(0, 0, 90, 20, "Dequeue", false, true, true, color(255,0,0,255)).onClick(() => {
    if (getInQueue()) {
        removePartyFromQueue(true, (response) => {
            dequeued = response
            if (dequeued)
                getPartyFinderData()
        });
    }
});
buttonsPfwindow.push(deQueueButton)
const submitPartyButton = new Button(0, 0, 90, 20, "Create", false, false, true, color(255,255,255,255)).onClick(() => {
    let reqs = getRequirements()
    let reqsString = ""
    Object.entries(reqs).forEach(([key, value]) => {
        if (key === "MVP+") key = "mvpplus"
        if (value === 0 || value === false) return;
        if (value === true) value = ""
        reqsString += `${key}${value},`.toLowerCase()
    })
    createParty(reqsString)
    PartyFinderGUI.open()
});
const backButton = new Button(0, 0, 90, 20, "Back", false, true, true, color(255,255,255,255)).onClick(() => {
    PartyFinderGUI.open()
});

function getRequirements() {
    let reqs = {}
    Object.entries(inputFields).forEach(([key, object]) => {
        let text = object.text
        if (text === "") reqs[key] = 0
        else reqs[key] = text
    })
    checkBoxListCreate.forEach((checkBox) => {
        reqs[checkBox.text.replace(" ","")] = checkBox.checked
    })
    return reqs
}

function loadInputFieldState() {
    Object.entries(inputFields).forEach(([key, object]) => {
        object.text = mainInputFields[key]
        object.textInput.setText(mainInputFields[key])
    })
    mainInputFields.save()
}

function saveInputFieldState() {
    Object.entries(inputFields).forEach(([key, object]) => {
        mainInputFields[key] = object.text
    })
    mainInputFields.save()
}

const pfText = new TextClass(color(255, 255, 255, 255), 0, 0, "", 1.75, true)
const onlineUserText = new TextClass(color(233, 233, 233, 255), 0, 0, ``, 1, false)
const partyCountText = new TextClass(color(233, 233, 233, 255), 0, 0, ``, 1, false)
const pageCountText = new TextClass(color(233, 233, 233, 255), 0, 0, ``, 1, false)
const leaderText = new TextClass(color(255, 255, 255, 255), 0, 0, ``, 1, false)
const membersText = new TextClass(color(255, 255, 255, 255), 0 + 5, 0, ``, 1, false)
const partyReqs = new TextClass(color(255, 255, 255, 255), 0, 0, ``, 1, false)
function drawCheckBoxesMain(param = {}) {
    let layoutData = getLayoutData()
    emanCheckBox.draw().setText("Eman9")
    .setX(param.x).setY(layoutData.pfWindowY + layoutData.pfWindowHeight / 50)
    .setHeight(layoutData.checkBoxHeight).setWidth(layoutData.checkBoxWidth);
    // mvpPlusCheckBox.draw().setText("MVP+")
    // .setX(param.x).setY(layoutData.pfWindowY + layoutData.pfWindowHeight / 20)
    // .setHeight(layoutData.checkBoxHeight).setWidth(layoutData.checkBoxWidth);
    looting5CheckBox.draw().setText("Looting 5")
    .setX(param.x + emanCheckBox.width + emanCheckBox.textWidth + 20).setY(layoutData.pfWindowY + layoutData.pfWindowHeight / 50)
    .setHeight(layoutData.checkBoxHeight).setWidth(layoutData.checkBoxWidth);
    canIJoinCheckBox.draw().setText("Can I Join")
    .setX(param.x).setY(layoutData.pfWindowY + layoutData.pfWindowHeight / 20)
    .setHeight(layoutData.checkBoxHeight).setWidth(layoutData.checkBoxWidth);
}

const emanCheckBox = new CheckBox(0, 0, 0, 0, "", color(255, 255, 255, 255), color(255, 255, 255, 255), "eman9").onClick(() => {
    filterPartyList()
});
checkBoxList.push(emanCheckBox)
// const mvpPlusCheckBox = new CheckBox(0, 0, 0, 0, "", color(255, 255, 255, 255), color(255, 255, 255, 255), "mvpplus").onClick(() => {
//     filterPartyList()
// });
// checkBoxList.push(mvpPlusCheckBox)
const looting5CheckBox = new CheckBox(0, 0, 0, 0, "", color(255, 255, 255, 255), color(255, 255, 255, 255), "looting5").onClick(() => {
    filterPartyList()
});
checkBoxList.push(looting5CheckBox)
const canIJoinCheckBox = new CheckBox(0, 0, 0, 0, "", color(255, 255, 255, 255), color(255, 255, 255, 255), "canIjoin").onClick(() => {
    filterPartyList()
});
checkBoxList.push(canIJoinCheckBox)

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
            let requirements = requirementsFormat(party.reqs, dianaStats)
            let pMmMcolor = pMmMColor(party.partymembers)
            outline(color(0, 173, 255, 255), layoutData.partyBoxX, partyBoxY, layoutData.partyBoxWidth, layoutData.partyBoxHeight, 1);
            leaderText.draw().setX(layoutData.partyBoxX + 5).setY(row1y).setText(`&b${party.leaderName}'s Party`)
            membersText.draw().setX(layoutData.partyBoxX + 5).setY(row2y).setText(`&9Members: &r${party.partymembers}/6`).setColor(pMmMcolor)
            partyReqs.draw().setX(layoutData.partyBoxX + 5).setY(row3y).setText(`&9Requirements: &r\n${requirements}`)
        });
    }

    pfText.draw().setX(layoutData.titleX).setY(layoutData.titleY).setText("Diana Party Finder")
    onlineUserText.draw().setX(layoutData.onlineUserX).setY(layoutData.onlineUserY).setText(`Online User: ${onlineUsers}`)
    let partyCountX = onlineUserText.width + layoutData.onlineUserX + 10
    partyCountText.draw().setX(partyCountX).setY(layoutData.onlineUserY).setText(`Partys: ${partyCount}`)
    pageCountText.draw().setX(layoutData.pageCountX).setY(layoutData.pageCountY).setText(`Page ${currentPage}/${pageCount}`)
    drawButtonsMain(layoutData);
    let checkBoxX = pfText.width + layoutData.titleX + 20
    drawCheckBoxesMain({x: checkBoxX})
}

let dianaKillsText = new TextClass(color(255, 255, 255, 255), 0, 0, "", 1, false)
let sbLevelText = new TextClass(color(255, 255, 255, 255), 0, 0, "", 1, false)
let looting5box = new CheckBox(0, 0, 0, 0, "", color(255, 255, 255, 255), color(255, 255, 255, 255), "l5ReqCreate")
let eman9ReqsBox = new CheckBox(0, 0, 0, 0, "", color(255, 255, 255, 255), color(255, 255, 255, 255), "eman9ReqCreate")
// let mvpPlusBox = new CheckBox(0, 0, 0, 0, "", color(255, 255, 255, 255), color(255, 255, 255, 255), 1)
checkBoxListCreate.push(eman9ReqsBox)
checkBoxListCreate.push(looting5box)
// checkBoxListCreate.push(mvpPlusBox)

function drawCheckBoxesCreate(layoutData) {
    eman9ReqsBox.draw().setText("Eman9").setX(layoutData.createWindowX * 1.012).setY(layoutData.createWindowY + layoutData.createWindowHeight / 2)
    .setHeight(layoutData.checkBoxHeight).setWidth(layoutData.checkBoxWidth)
    looting5box.draw().setText("Looting 5").setX(layoutData.createWindowX * 1.012).setY(layoutData.createWindowY + layoutData.createWindowHeight / 1.66)
    .setHeight(layoutData.checkBoxHeight).setWidth(layoutData.checkBoxWidth)
    // mvpPlusBox.draw().setText("MVP+").setX(layoutData.createWindowX * 1.012).setY(layoutData.createWindowY + layoutData.createWindowHeight / 1.4)
    // .setHeight(layoutData.checkBoxHeight).setWidth(layoutData.checkBoxWidth)
}

const hintText = new TextClass(color(233, 233, 233, 255), 0, 0, `Enable private messages (guide in Help)`, 0.9, false)
function createPartyRender() {
    let layoutData = getLayoutData()
    createPartyBlock.setX(new PixelConstraint(layoutData.createWindowX))
    createPartyBlock.setY(new PixelConstraint(layoutData.createWindowY))
    createPartyBlock.setWidth(new PixelConstraint(layoutData.createWindowWidth))
    createPartyBlock.setHeight(new PixelConstraint(layoutData.createWindowHeight))
    dianaKillsText.draw().setX(layoutData.createWindowX * 1.012).setY(layoutData.createWindowY + layoutData.createWindowHeight / 20).setText("Diana Kills")
    sbLevelText.draw().setX(layoutData.createWindowX * 1.012).setY(layoutData.createWindowY + layoutData.createWindowHeight / 4).setText("Skyblock Level")
    drawCheckBoxesCreate(layoutData)
    drawButtonsCreate(layoutData);
    line(color(0, 173, 255, 255), layoutData.createWindowX, layoutData.createPartyButtonY, layoutData.createWindowX + layoutData.createWindowWidth, layoutData.createPartyButtonY, 1);
    outline(color(0, 173, 255, 255), layoutData.createWindowX, layoutData.createWindowY, layoutData.createWindowWidth, layoutData.createWindowHeight, 1);
    hintText.draw().setX(submitPartyButton.x + 5).setY(submitPartyButton.y - hintText.height)
}
let privateMessageTitle = new TextClass(color(255, 255, 255, 255), 0, 0, "Enabling private Messages:", 1.5, true)
let privateMessageGuide = new TextClass(color(200, 200, 200, 255), 0, 0, "/settings -> Social Settings -> Set Private Message privacy to None", 1.25, false)
let privateMessageSubTitle = new TextClass(color(200, 200, 200, 255), 0, 0, "It's essential to see party join requests", 1.25, false)
let reqsRefreshTitle = new TextClass(color(255, 255, 255, 255), 0, 0, "Updating Requirements:", 1.5, true)
let reqsRefreshGuide = new TextClass(color(200, 200, 200, 255), 0, 0, "do /ct reload to update ur requirements (could take 10mins!)", 1.25, false)
function helpRender() {
    let layoutData = getLayoutData()
    rect(color(80, 80, 80, 245), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight);
    outline(color(0, 173, 255, 255), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight, 1);
    drawButtonsHdwi(layoutData);
    privateMessageTitle.draw().setX(layoutData.pfWindowX + 10).setY(layoutData.pfWindowY + 10)
    privateMessageGuide.draw().setX(layoutData.pfWindowX + 10).setY(layoutData.pfWindowY + 10 + privateMessageTitle.height)
    privateMessageSubTitle.draw().setX(layoutData.pfWindowX + 10).setY(layoutData.pfWindowY + 10 + privateMessageTitle.height + privateMessageGuide.height)
    reqsRefreshTitle.draw().setX(layoutData.pfWindowX + 10).setY(layoutData.pfWindowY + 10 + privateMessageTitle.height + privateMessageGuide.height + privateMessageSubTitle.height)
    reqsRefreshGuide.draw().setX(layoutData.pfWindowX + 10).setY(layoutData.pfWindowY + 10 + privateMessageTitle.height + privateMessageGuide.height + privateMessageSubTitle.height + reqsRefreshTitle.height)
}

let textObject = new TextClass(color(255, 255, 255, 255), 0, 0, "", 1, false)
function partyInfoRender() {
    let layoutData = getLayoutData()
    rect(color(80, 80, 80, 245), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight);
    outline(color(0, 173, 255, 255), layoutData.pfWindowX, layoutData.pfWindowY, layoutData.pfWindowWidth, layoutData.pfWindowHeight, 1);
    rect(color(0, 0, 0, 155), layoutData.partyBoxX + layoutData.pfListWidth / 2, layoutData.partyBoxY, layoutData.partyBoxWidth / 2.1, layoutData.pfListHeight);
    outline(color(0, 173, 255, 255), layoutData.partyBoxX + layoutData.pfListWidth / 2, layoutData.partyBoxY, layoutData.partyBoxWidth / 2.1, layoutData.pfListHeight, 1);
    currentMemberList.forEach((member, index) => {
        let memberButton = member.button
        let memberObject = member.memberObject
        memberButton.customize({
            x: layoutData.partyBoxX + 20, y: (layoutData.partyBoxY + 10) + (layoutData.partyBoxHeight * 6 / 5 * index),
            width: layoutData.partyBoxWidth / 2.2, height: layoutData.partyBoxHeight / 6 * 5
        }).draw(Client.getMouseX(), Client.getMouseY(), allPartyButtons).onHover(() => {
            let text = formatPlayerInfo(memberObject)
            textObject.setText(text).setX(layoutData.partyBoxX + layoutData.pfListWidth / 2 + 20).setY(layoutData.partyBoxY + 20)
        });
    })
    textObject.draw()
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
CreatePartyGUI.registers.onKeyType((char, keyCode) => {
    if (keyCode !== Keyboard.KEY_ESCAPE)
        filterTextInput(inputFields)
    if (keyCode === Keyboard.KEY_ESCAPE) 
        PartyFinderGUI.open();
});
HdwiGUI.registerKeyTyped((char, keyCode) => {
    if (keyCode === Keyboard.KEY_ESCAPE) {
        PartyFinderGUI.open()
    }
});
let inputFields = {}
let createLayoutData = getLayoutData()
let createPartyColor = ElementUtils.getJavaColor([80, 80, 80, 245])
let createPartyBlock = new UIBlock(createPartyColor)
    .setX(new PixelConstraint(createLayoutData.createWindowX))
    .setY(new PixelConstraint(createLayoutData.createWindowY))
    .setWidth(new PixelConstraint(createLayoutData.createWindowWidth))
    .setHeight(new PixelConstraint(createLayoutData.createWindowHeight))
    .setChildOf(CreatePartyGUI.window)
let SbLevel = new TextInputElement("", 0, 33, 0, 12)
SbLevel.onMouseEnterEvent(() => {}, true);
SbLevel._create().setChildOf(createPartyBlock)
SbLevel.bgBox.setWidth((100).percent())
inputFields["lvl"] = SbLevel
let DianaKills = new TextInputElement("", 0, 11, 0, 12)
DianaKills.onMouseEnterEvent(() => {}, true);
DianaKills._create().setChildOf(createPartyBlock)
DianaKills.bgBox.setWidth((100).percent())
inputFields["kills"] = DianaKills

register("command", () => {
    if (isDataLoaded() && isInSkyblock()) {
        currentPage = 1
        PartyFinderGUI.open()
    }   
}).setName("sbopartyfinder").setAliases("sbopf")