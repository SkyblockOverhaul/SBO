import GuiHandler from "./GuiHandler";
import HandleGui from "../../../DocGuiLib/core/Gui";
import { UIBlock, UIText, UIWrappedText, OutlineEffect, CenterConstraint } from "../../../Elementa";
import { pastDianaEvents, dianaTrackerTotal } from "../../utils/variables";
import { calcTotalProfit, formatNumber, formatTime, toTitleCase } from "../../utils/functions";

let pastEventsGui = new HandleGui("data/DefaultColors.json", "sbo");
let pastEventsWindow = pastEventsGui.window;
let pastEventsCtGui = pastEventsGui.ctGui;
let pastEventsRegisters = pastEventsGui.registers;

let clickableElements = [];
let deleteClickableElements = [];
let currentPage = 0; 

pastEventsRegisters.onMouseClick((mouseX, mouseY, button) => {
    for (let elem of clickableElements) {
        if (elem.isClicked(mouseX, mouseY)) {
            elem.click(mouseX, mouseY, button);
            return true;
        }
    }
});

pastEventsRegisters.onOpen(() => {
    initMainUI();
});

function processDianaEvents() {
    let events = pastDianaEvents.events;
    if (events.length === 0) {
        return [];
    }
    return [...events].reverse();
}

// === MAIN GUI ===
function initMainUI() {
    pastEventsWindow.clearChildren();
    clickableElements = [];
    
    let background = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((100).percent())
        .setHeight((100).percent())
        .setX((0).percent())
        .setY((0).percent())
        .setChildOf(pastEventsWindow);
    
    let eventContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((65).percent())
        .setHeight((70).percent())
        .setX((20).percent())
        .setY((10).percent())
        .setChildOf(background);
    
    let eventsData = processDianaEvents();
    let eventBlockHeight = 10;
    let padding = 2;  
    let maxEventsPerPage = Math.floor((100 - padding) / (eventBlockHeight + padding));
    let startIndex = currentPage * maxEventsPerPage;
    let endIndex = Math.min(eventsData.length, startIndex + maxEventsPerPage);
        
    for (let i = startIndex; i < endIndex; i++) {
        let totalProfit = 0;
        let event = eventsData[i];
        let itemsArray = Object.entries(event.items);
        let chims = event.items["Chimera"] || 0;
        let chimsLs = event.items["ChimeraLs"] || 0;
        let totalChims = chims + chimsLs;
        let playtime = event.items["mayorTime"] || 0;
        playtime = formatTime(playtime);
        for (let j = 0; j < itemsArray.length; j++) {
            let item = itemsArray[j][0];
            let amount = itemsArray[j][1];
            let itemName = item.replaceAll("_", " ");
            itemName = toTitleCase(itemName);
            totalProfit += calcTotalProfit(itemName, amount);
        }
        let indexWithinPage = i - startIndex;
        
        // Erstelle nun einen Event-Button als Container, anstatt eines reinen UIBlocks:
        let eventButton = new GuiHandler.Button(
            "", // Kein Standardtext – der Button dient nur als Container
            (padding).percent(),
            ((padding + indexWithinPage * (eventBlockHeight + padding))).percent(),
            (85).percent(),
            (eventBlockHeight).percent(),
            [0, 0, 0, 0],
            null,
            new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1),
            eventContainer
        );
        GuiHandler.addHoverEffect(eventButton.Object, [0, 0, 0, 0]);
        
        // Füge alle Texte als Kinder des Buttons hinzu:
        new UIText("Year: " + event.year)
            .setX((2).percent())
            .setY((10).percent())
            .setColor(GuiHandler.Color([0, 255, 0, 255]))
            .setChildOf(eventButton.Object);
        new UIText("Chimeras: " + totalChims)
            .setX((2).percent())
            .setY((40).percent())
            .setColor(GuiHandler.Color([255, 85, 255, 255]))
            .setChildOf(eventButton.Object);
        new UIText("Total Profit: " + formatNumber(totalProfit))
            .setX((2).percent())
            .setY((70).percent())
            .setColor(GuiHandler.Color([255, 170, 0, 255]))
            .setChildOf(eventButton.Object);
        new UIText("Burrows: " + (formatNumber(event.items["Total Burrows"]) || ""))
            .setX((25).percent())
            .setY((10).percent())
            .setColor(GuiHandler.Color([170, 170, 170, 255]))
            .setChildOf(eventButton.Object);
        new UIText("Mobs: " + (formatNumber(event.mobs.TotalMobs) || ""))
            .setX((25).percent())
            .setY((40).percent())
            .setColor(GuiHandler.Color([170, 170, 170, 255]))
            .setChildOf(eventButton.Object);
        new UIText("Playtime: " + playtime)
            .setX((25).percent())
            .setY((70).percent())
            .setColor(GuiHandler.Color([255, 255, 85, 255]))
            .setChildOf(eventButton.Object);

        eventButton.setOnClick((mouseX, mouseY, button) => {
            showFullEventDetails(event, totalProfit);
            return true;
        });
        clickableElements.push(eventButton);
        
        // Ersetze den bisherigen "Delete"-Block durch die neue Button-Klasse:
        let deleteButton = new GuiHandler.Button(
            "Delete",
            (90).percent(),
            ((padding + indexWithinPage * (eventBlockHeight + padding))).percent(),
            (8).percent(),
            (eventBlockHeight).percent(),
            [0, 0, 0, 0],
            [255, 0, 0, 255],
            new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1),
            eventContainer
        );
        GuiHandler.addHoverEffect(deleteButton.Object, [0, 0, 0, 0]);
        deleteButton.setOnClick((mouseX, mouseY, button) => {
            openDeleteConfirmation(event, i);
            return true;
        });
        clickableElements.push(deleteButton);
    }
    
    let navContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((65).percent())
        .setHeight((5).percent())
        .setX((20).percent())
        .setY((82).percent())
        .enableEffect(new OutlineEffect(GuiHandler.Color([0, 0, 0, 150]), 1))
        .setChildOf(background);
    
    // Ersetze den Total Overview-Button:
    let totalOverviewButton = new GuiHandler.Button(
        "Total Overview",
        (31).percent(),
        (0).percent(),
        (38).percent(),
        (100).percent(),
        [0, 0, 0, 150],
        [255, 170, 0, 255],
        new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1),
        navContainer
    );
    GuiHandler.addHoverEffect(totalOverviewButton.Object, [0, 0, 0, 150]);
    totalOverviewButton.setOnClick((mouseX, mouseY, button) => {
        showTotalOverview();
        return true;
    });
    clickableElements.push(totalOverviewButton);
        
    if (currentPage > 0 && eventsData.length > maxEventsPerPage) {
        // Ersetze den Prev-Button:
        let prevButton = new GuiHandler.Button(
            "Prev",
            (0).percent(),
            (0).percent(),
            (30).percent(),
            (100).percent(),
            [20, 20, 20, 255],
            false,
            new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1),
            navContainer
        );
        GuiHandler.addHoverEffect(prevButton.Object, [20, 20, 20, 255]);
        prevButton.setOnClick((mouseX, mouseY, button) => {
            currentPage--;
            initMainUI();
            return true;
        });
        clickableElements.push(prevButton);
    }
    if (endIndex < eventsData.length) {
        // Ersetze den Next-Button:
        let nextButton = new GuiHandler.Button(
            "Next",
            (70).percent(),
            (0).percent(),
            (30).percent(),
            (100).percent(),
            [20, 20, 20, 255],
            false,
            new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1),
            navContainer
        );
        GuiHandler.addHoverEffect(nextButton.Object, [20, 20, 20, 255]);
        nextButton.setOnClick((mouseX, mouseY, button) => {
            currentPage++;
            initMainUI();
            return true;
        });
        clickableElements.push(nextButton);
    }
}

// === DETAIL GUI ===
let detailsGui = new HandleGui("data/DefaultColors.json", "detailsGui");
let detailsWindow = detailsGui.window;
let detailsCtGui = detailsGui.ctGui;
let detailsRegisters = detailsGui.registers;

detailsRegisters.onClose(() => {
    pastEventsCtGui.open();
    detailsWindow.clearChildren();
});

function showFullEventDetails(eventData, totalProfit) {
    clickableElements = [];
    detailsWindow.clearChildren();

    let itemsCount = Object.keys(eventData.items).length + 1;
    let mobsCount = Object.keys(eventData.mobs).length + 1;
    let lineHeight = 4; 

    let itemsHeight = itemsCount * lineHeight;
    let mobsHeight = mobsCount * lineHeight;
    let maxColumnHeight = Math.max(itemsHeight, mobsHeight);
    if (maxColumnHeight < 80) maxColumnHeight = 80; 

    let neededHeight = 10 + maxColumnHeight;

    if (neededHeight > 90) {
        neededHeight = 90;
    }

    let background = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((100).percent())
        .setHeight((100).percent())
        .setX((0).percent())
        .setY((0).percent())
        .setChildOf(detailsWindow);

    let bgWidth = 60;
    let bgHeight = neededHeight;
    let bgX = (100 - bgWidth) / 2;
    let bgY = (100 - bgHeight) / 2;

    let itemsBackground = new UIBlock(GuiHandler.Color([0, 0, 0, 0]))
        .setWidth((bgWidth).percent())
        .setHeight((bgHeight).percent())
        .setX((bgX).percent())
        .setY((bgY).percent())
        .setChildOf(detailsWindow)

    let margin = 3; // %
    let contentContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 0]))
        .setWidth((100 - 2 * margin).percent())
        .setHeight((100 - 2 * margin).percent())
        .setX((margin).percent())
        .setY((margin).percent())
        .setChildOf(itemsBackground);

    new UIWrappedText("Year: " + eventData.year)
        .setX((1).percent())
        .setY((6).percent())
        .setWidth((20).percent())
        .setTextScale((1.5).pixels())
        .setColor(GuiHandler.Color([0, 255, 0, 255]))
        .setChildOf(contentContainer);

    let columnsContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 0]))
        .setWidth((100).percent())
        .setHeight((88).percent())
        .setX((0).percent())
        .setY((10).percent()) 
        .setChildOf(contentContainer);

    let colHeight = maxColumnHeight;

    let itemsContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((48).percent())
        .setHeight((colHeight).percent())
        .setX((0).percent())
        .setY((0).percent())
        .enableEffect(new OutlineEffect(GuiHandler.Color([255, 255, 255, 255]), 1))
        .setChildOf(columnsContainer);

    let mobsContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((48).percent())
        .setHeight((colHeight).percent())
        .setX((52).percent())
        .setY((0).percent())
        .enableEffect(new OutlineEffect(GuiHandler.Color([255, 255, 255, 255]), 1))
        .setChildOf(columnsContainer);

    let itemsY = 4;
    new UIText("Items:")
        .setX((2).percent())
        .setY((itemsY).percent())
        .setColor(GuiHandler.Color([0, 255, 0, 255]))
        .setChildOf(itemsContainer);
    itemsY += lineHeight;

    for (let key in eventData.items) {
        let itemName = key.replaceAll("_", " ");
        itemName = toTitleCase(itemName);
        itemName = replaceNames(itemName);
        let amount = replaceKey(itemName, eventData.items[key]);
        new UIText(itemName + ": " + amount)
            .setX((2).percent())
            .setY((itemsY).percent())
            .setColor(GuiHandler.Color([0, 255, 0, 255]))
            .setChildOf(itemsContainer);
        itemsY += lineHeight;
    }
    new UIText("Total Profit: " + formatNumber(totalProfit))
        .setX((2).percent())
        .setY((itemsY).percent())
        .setColor(GuiHandler.Color([255, 170, 0, 255]))
        .setChildOf(itemsContainer);

    let mobsY = 4;
    new UIText("Mobs:")
        .setX((2).percent())
        .setY((mobsY).percent())
        .setColor(GuiHandler.Color([0, 255, 0, 255]))
        .setChildOf(mobsContainer);
    mobsY += lineHeight;

    for (let key in eventData.mobs) {
        let mobName = key.replaceAll("_", " ");
        mobName = toTitleCase(mobName);
        mobName = replaceNames(mobName);
        let amount = replaceKey(mobName, eventData.mobs[key]);
        new UIText(mobName + ": " + amount)
            .setX((2).percent())
            .setY((mobsY).percent())
            .setColor(GuiHandler.Color([0, 255, 0, 255]))
            .setChildOf(mobsContainer);
        mobsY += lineHeight;
    }

    detailsCtGui.open();
}

function showTotalOverview() {
    clickableElements = [];
    detailsWindow.clearChildren();

    let totalProfit = 0;
    for (let key in dianaTrackerTotal.items) {
        let itemName = key.replaceAll("_", " ");
        itemName = toTitleCase(itemName);
        totalProfit += calcTotalProfit(itemName, dianaTrackerTotal.items[key]);
    }

    let itemsCount = Object.keys(dianaTrackerTotal.items).length + 1;
    let mobsCount = Object.keys(dianaTrackerTotal.mobs).length + 1;
    let lineHeight = 4;

    let itemsHeight = itemsCount * lineHeight;
    let mobsHeight = mobsCount * lineHeight;
    let maxColumnHeight = Math.max(itemsHeight, mobsHeight);
    if (maxColumnHeight < 80) maxColumnHeight = 80; 

    let neededHeight = 10 + maxColumnHeight;
    if (neededHeight > 90) {
        neededHeight = 90;
    }

    let background = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((100).percent())
        .setHeight((100).percent())
        .setX((0).percent())
        .setY((0).percent())
        .setChildOf(detailsWindow);

    let bgWidth = 60;
    let bgHeight = neededHeight;
    let bgX = (100 - bgWidth) / 2;
    let bgY = (100 - bgHeight) / 2;

    let itemsBackground = new UIBlock(GuiHandler.Color([0, 0, 0, 0]))
        .setWidth((bgWidth).percent())
        .setHeight((bgHeight).percent())
        .setX((bgX).percent())
        .setY((bgY).percent())
        .setChildOf(detailsWindow);

    let margin = 3;
    let contentContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 0]))
        .setWidth((100 - 2 * margin).percent())
        .setHeight((100 - 2 * margin).percent())
        .setX((margin).percent())
        .setY((margin).percent())
        .setChildOf(itemsBackground);

    new UIWrappedText("Total Overview")
        .setX((1).percent())
        .setY((6).percent())
        .setWidth((20).percent())
        .setTextScale((1.5).pixels())
        .setColor(GuiHandler.Color([0, 255, 0, 255]))
        .setChildOf(contentContainer);

    let columnsContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 0]))
        .setWidth((100).percent())
        .setHeight((88).percent())
        .setX((0).percent())
        .setY((10).percent())
        .setChildOf(contentContainer);

    let colHeight = maxColumnHeight;

    let itemsContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((48).percent())
        .setHeight((colHeight).percent())
        .setX((0).percent())
        .setY((0).percent())
        .enableEffect(new OutlineEffect(GuiHandler.Color([255, 255, 255, 255]), 1))
        .setChildOf(columnsContainer);

    let mobsContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((48).percent())
        .setHeight((colHeight).percent())
        .setX((52).percent())
        .setY((0).percent())
        .enableEffect(new OutlineEffect(GuiHandler.Color([255, 255, 255, 255]), 1))
        .setChildOf(columnsContainer);

    let itemsY = 4;
    new UIText("Items:")
        .setX((2).percent())
        .setY((itemsY).percent())
        .setColor(GuiHandler.Color([0, 255, 0, 255]))
        .setChildOf(itemsContainer);
    itemsY += lineHeight;

    for (let key in dianaTrackerTotal.items) {
        let itemName = key.replaceAll("_", " ");
        itemName = toTitleCase(itemName);
        itemName = replaceNames(itemName);
        let amount = replaceKey(itemName, dianaTrackerTotal.items[key]);
        new UIText(itemName + ": " + amount)
            .setX((2).percent())
            .setY((itemsY).percent())
            .setColor(GuiHandler.Color([0, 255, 0, 255]))
            .setChildOf(itemsContainer);
        itemsY += lineHeight;
    }
    new UIText("Total Profit: " + formatNumber(totalProfit))
        .setX((2).percent())
        .setY((itemsY).percent())
        .setColor(GuiHandler.Color([255, 170, 0, 255]))
        .setChildOf(itemsContainer);

    let mobsY = 4;
    new UIText("Mobs:")
        .setX((2).percent())
        .setY((mobsY).percent())
        .setColor(GuiHandler.Color([0, 255, 0, 255]))
        .setChildOf(mobsContainer);
    mobsY += lineHeight;
    for (let key in dianaTrackerTotal.mobs) {
        let mobName = key.replaceAll("_", " ");
        mobName = toTitleCase(mobName);
        mobName = replaceNames(mobName)
        let amount = replaceKey(mobName, dianaTrackerTotal.mobs[key]);
        new UIText(mobName + ": " + amount)
            .setX((2).percent())
            .setY((mobsY).percent())
            .setColor(GuiHandler.Color([0, 255, 0, 255]))
            .setChildOf(mobsContainer);
        mobsY += lineHeight;
    }

    detailsCtGui.open();
}

// === DELETE GUI ===
let deleteGui = new HandleGui("data/DefaultColors.json", "deleteGui");
let deleteWindow = deleteGui.window;
let deleteCtGui = deleteGui.ctGui;
let deleteRegisters = deleteGui.registers;

deleteRegisters.onMouseClick((mouseX, mouseY, button) => {
    for (let elem of deleteClickableElements) {
        if (elem.isClicked(mouseX, mouseY)) {
            elem.click(mouseX, mouseY, button);
            return true;
        }
    }
});

function openDeleteConfirmation(event, eventIndex) {
    deleteWindow.clearChildren();
    
    let background = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((100).percent())
        .setHeight((100).percent())
        .setX((0).percent())
        .setY((0).percent())
        .setChildOf(deleteWindow);
    
    let containerWidth = 20;
    let containerHeight = 20;
    let containerX = (100 - containerWidth) / 2;
    let containerY = (100 - containerHeight) / 2;
    
    let confirmContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((containerWidth).percent())
        .setHeight((containerHeight).percent())
        .setX((containerX).percent())
        .setY((containerY).percent())
        .enableEffect(new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1))
        .setChildOf(background);
    
    new UIWrappedText("Are you sure you want to delete the event?")
        .setX(new CenterConstraint())
        .setY((20).percent())
        .setChildOf(confirmContainer);
    
    // Ersetze den Yes-Button:
    let yesButton = new GuiHandler.Button(
        "Yes",
        (8).percent(),
        (60).percent(),
        (40).percent(),
        (20).percent(),
        [0, 200, 0, 255],
        false,
        new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1),
        confirmContainer
    );
    GuiHandler.addHoverEffect(yesButton.Object, [0, 200, 0, 255]);
    yesButton.setOnClick((mouseX, mouseY, button) => {
        let originalIndex = pastDianaEvents.events.length - 1 - eventIndex;
        pastDianaEvents.events.splice(originalIndex, 1);
        deleteCtGui.close();
        pastDianaEvents.save();
        initMainUI();
        pastEventsCtGui.open();
        return true;
    });
    deleteClickableElements.push(yesButton);
    
    // Ersetze den No-Button:
    let noButton = new GuiHandler.Button(
        "No",
        (52).percent(),
        (60).percent(),
        (38).percent(),
        (20).percent(),
        [200, 0, 0, 255],
        false,
        new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1),
        confirmContainer
    );
    GuiHandler.addHoverEffect(noButton.Object, [200, 0, 0, 255]);
    noButton.setOnClick((mouseX, mouseY, button) => {
        deleteCtGui.close();
        pastEventsCtGui.open();
        return true;
    });
    deleteClickableElements.push(noButton);
    
    deleteCtGui.open();
}

register("command", () => {
    initMainUI();
    pastEventsCtGui.open();
}).setName("sbopastdianaevents").setAliases("sbopde");

function replaceNames(item) {
    switch (item) {
        case "Totaltime":
        case "Mayortime":
            return "Playtime";
        case "Total Burrows":
            return "Total Burrows";
        case "Totalmobs":
            return "Total Mobs";
        case "Scavengercoins":
            return "Scavenger";
        case "Fishcoins":
            return "Four-Eyed Fish";
        default:
            return item;
    }
}

function replaceKey(item, key) {
    switch (item) {
        case "Playtime":
            return formatTime(key);
        default:
            return formatNumber(key);
    }
}