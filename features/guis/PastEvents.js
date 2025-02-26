import GuiHandler from "./GuiHandler";
import HandleGui from "../../../DocGuiLib/core/Gui";
import { UIBlock, UIText, UIWrappedText, OutlineEffect } from "../../../Elementa";
import { pastDianaEvents, dianaTrackerTotal } from "../../utils/variables";
import { calcTotalProfit, formatNumber, formatTime, toTitleCase } from "../../utils/functions";

let pastEventsGui = new HandleGui("data/DefaultColors.json", "sbo");
let pastEventsWindow = pastEventsGui.window;
let pastEventsCtGui = pastEventsGui.ctGui;
let pastEventsRegisters = pastEventsGui.registers;

let clickableElements = [];
let currentPage = 0; 

pastEventsRegisters.onMouseClick((mouseX, mouseY, button) => {
    for (let elem of clickableElements) {
        let abs = elem.getAbsolute();
        if (
            mouseX >= abs.x &&
            mouseX <= abs.x + abs.width &&
            mouseY >= abs.y &&
            mouseY <= abs.y + abs.height
        ) {
            elem.onClick(mouseX, mouseY, button);
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

function getScreenSize() {
    return { width: Renderer.screen.getWidth(), height: Renderer.screen.getHeight() };
}

function getMainContainerAbsolute() {
    let { width, height } = getScreenSize();
    return {
        x: GuiHandler.percentToPixel(20, width),
        y: GuiHandler.percentToPixel(10, height),
        width: GuiHandler.percentToPixel(65, width),
        height: GuiHandler.percentToPixel(70, height)
    };
}

function getEventBlockAbsolute(indexWithinPage, paddingPercent, eventBlockHeightPercent, blockWidthPercent) {
    let parent = getMainContainerAbsolute();
    return {
        x: parent.x + (paddingPercent / 100) * parent.width,
        y: parent.y + ((paddingPercent + indexWithinPage * (eventBlockHeightPercent + paddingPercent)) / 100) * parent.height,
        width: parent.width * (blockWidthPercent / 100),
        height: parent.height * (eventBlockHeightPercent / 100)
    };
}

function getDeleteButtonAbsolute(indexWithinPage, paddingPercent, eventBlockHeightPercent) {
    let container = getMainContainerAbsolute();
    return {
       x: container.x + container.width * 0.9, 
       y: container.y + ((paddingPercent + indexWithinPage * (eventBlockHeightPercent + paddingPercent)) / 100) * container.height,
       width: container.width * 0.08,
       height: container.height * eventBlockHeightPercent / 100
    };
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
        for (let i = 0; i < itemsArray.length; i++) {
            let item = itemsArray[i][0];
            let amount = itemsArray[i][1];
            let itemName = item.replaceAll("_", " ");
            itemName = toTitleCase(itemName);
            totalProfit += calcTotalProfit(itemName, amount);
        }
        let indexWithinPage = i - startIndex;
        let eventBlock = new UIBlock(GuiHandler.Color([0, 0, 0, 0]))
            .setWidth((85).percent())
            .setHeight((eventBlockHeight).percent())
            .setX((padding).percent())
            .setY(((padding + indexWithinPage * (eventBlockHeight + padding))).percent())
            .enableEffect(new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1))
            .setChildOf(eventContainer);
        GuiHandler.addHoverEffect(eventBlock, [0, 0, 0, 0]);
        let extraBlock = new UIBlock(GuiHandler.Color([0, 0, 0, 0]))
            .setWidth((8).percent())
            .setHeight((eventBlockHeight).percent())
            .setX((90).percent())
            .setY(((padding + indexWithinPage * (eventBlockHeight + padding))).percent())
            .enableEffect(new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1))
            .setChildOf(eventContainer);

        let deleteBlock = new UIBlock(GuiHandler.Color([0, 0, 0, 0]))
            .setWidth((100).percent())
            .setHeight((100).percent())
            .setX((0).percent())
            .setY((0).percent())
            .setChildOf(extraBlock);
        GuiHandler.addHoverEffect(deleteBlock, [0, 0, 0, 0]);
        let delteButton = new UIText("Delete")
            .setX((25).percent())
            .setY((40).percent())
            .setColor(GuiHandler.Color([255, 0, 0, 255]))
            .setChildOf(deleteBlock);

            (function(event, eventIndex, indexWithinPage) {
                clickableElements.push({
                    getAbsolute: function() {
                        return getDeleteButtonAbsolute(indexWithinPage, padding, eventBlockHeight);
                    },
                    onClick: function(mouseX, mouseY, button) {
                        openDeleteConfirmation(event, eventIndex);
                        return true;
                    }
                });
            })(event, i, indexWithinPage);


        new UIText("Year: " + event.year)
            .setX((2).percent())
            .setY((10).percent())
            .setColor(GuiHandler.Color([0, 255, 0, 255]))
            .setChildOf(eventBlock);
        new UIText("Chimeras: " + totalChims)
            .setX((2).percent())
            .setY((40).percent())
            .setColor(GuiHandler.Color([255, 85, 255, 255]))
            .setChildOf(eventBlock);
        new UIText("Total Profit: " + formatNumber(totalProfit))
            .setX((2).percent())
            .setY((70).percent())
            .setColor(GuiHandler.Color([255, 170, 0, 255]))
            .setChildOf(eventBlock);
        new UIText("Burrows: " + (formatNumber(event.items["Total Burrows"]) || ""))
            .setX((25).percent())
            .setY((10).percent())
            .setColor(GuiHandler.Color([170, 170, 170, 255]))
            .setChildOf(eventBlock);
        new UIText("Mobs: " + (formatNumber(event.mobs.TotalMobs) || ""))
            .setX((25).percent())
            .setY((40).percent())
            .setColor(GuiHandler.Color([170, 170, 170, 255]))
            .setChildOf(eventBlock);
        new UIText("Playtime: " + playtime)
            .setX((25).percent())
            .setY((70).percent())
            .setColor(GuiHandler.Color([255, 255, 85, 255]))
            .setChildOf(eventBlock);

        clickableElements.push({
            getAbsolute: function() {
                return getEventBlockAbsolute(indexWithinPage, padding, eventBlockHeight, 85);
            },
            onClick: function(mouseX, mouseY, button) {
                showFullEventDetails(event, totalProfit);
                return true;
            }
        });
    }
    
    let navContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((65).percent())
        .setHeight((5).percent())
        .setX((20).percent())
        .setY((82).percent())
        .enableEffect(new OutlineEffect(GuiHandler.Color([0, 0, 0, 150]), 1))
        .setChildOf(background);
    
    let totalEventsContainer = new UIBlock(GuiHandler.Color([0, 0, 0, 150]))
        .setWidth((38).percent())
        .setHeight((100).percent())
        .setX((31).percent())
        .setY((0).percent())
        .enableEffect(new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1))
        .setChildOf(navContainer);
        GuiHandler.addHoverEffect(totalEventsContainer, [0, 0, 0, 150]);
    new UIText("Total Overview")
        .setX((37).percent())
        .setY((40).percent())
        .setColor(GuiHandler.Color([255, 170, 0, 255]))
        .setChildOf(totalEventsContainer);
    clickableElements.push({
        getAbsolute: function() {
            let s = getScreenSize();
            let containerX = GuiHandler.percentToPixel(20, s.width);
            let containerY = GuiHandler.percentToPixel(82, s.height);
            let containerWidth = GuiHandler.percentToPixel(65, s.width);
            let containerHeight = GuiHandler.percentToPixel(5, s.height);
            return { x: containerX + containerWidth * 0.3, y: containerY, width: containerWidth * 0.4, height: containerHeight };
        },
        onClick: function(mouseX, mouseY, button) {
            showTotalOverview();
            return true;
        }
    });
        
    if (currentPage > 0 && eventsData.length > maxEventsPerPage) {
        let prevButton = new UIBlock(GuiHandler.Color([20, 20, 20, 255]))
            .setWidth((30).percent())
            .setHeight((100).percent())
            .setX((0).percent())
            .setY((0).percent())
            .enableEffect(new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1))
            .setChildOf(navContainer);
        GuiHandler.addHoverEffect(prevButton, [20, 20, 20, 255]);
        new UIText("Prev")
            .setX((45).percent())
            .setY((40).percent())
            .setChildOf(prevButton);
        clickableElements.push({
            getAbsolute: function() {
                let s = getScreenSize();
                let containerX = GuiHandler.percentToPixel(20, s.width);
                let containerY = GuiHandler.percentToPixel(82, s.height);
                let containerWidth = GuiHandler.percentToPixel(65, s.width);
                let containerHeight = GuiHandler.percentToPixel(5, s.height);
                return { x: containerX, y: containerY, width: containerWidth * 0.3, height: containerHeight };
            },
            onClick: function(mouseX, mouseY, button) {
                currentPage--;
                initMainUI();
                return true;
            }
        });
    }
    if (endIndex < eventsData.length) {
        let nextButton = new UIBlock(GuiHandler.Color([20, 20, 20, 255]))
            .setWidth((30).percent())
            .setHeight((100).percent())
            .setX((70).percent())
            .setY((0).percent())
            .enableEffect(new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1))
            .setChildOf(navContainer);
        GuiHandler.addHoverEffect(nextButton, [20, 20, 20, 255]);
        new UIText("Next")
            .setX((45).percent())
            .setY((40).percent())
            .setChildOf(nextButton);
        clickableElements.push({
            getAbsolute: function() {
                let s = getScreenSize();
                let containerX = GuiHandler.percentToPixel(20, s.width);
                let containerY = GuiHandler.percentToPixel(82, s.height);
                let containerWidth = GuiHandler.percentToPixel(65, s.width);
                let containerHeight = GuiHandler.percentToPixel(5, s.height);
                return { x: containerX + containerWidth * 0.7, y: containerY, width: containerWidth * 0.3, height: containerHeight };
            },
            onClick: function(mouseX, mouseY, button) {
                currentPage++;
                initMainUI();
                return true;
            }
        });
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
        .setX((52).percent()) // 48 + 4 = 52 (% Abstand)
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
    for (let elem of clickableElements) {
        let abs = elem.getAbsolute();
        if (
            mouseX >= abs.x &&
            mouseX <= abs.x + abs.width &&
            mouseY >= abs.y &&
            mouseY <= abs.y + abs.height
        ) {
            elem.onClick(mouseX, mouseY, button);
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
        .setX((8).percent())
        .setY((20).percent())
        .setWidth((90).percent())
        .setChildOf(confirmContainer);
    
    let yesButton = new UIBlock(GuiHandler.Color([0, 200, 0, 255]))
        .setWidth((40).percent())
        .setHeight((20).percent())
        .setX((8).percent())
        .setY((60).percent())
        .enableEffect(new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1))
        .setChildOf(confirmContainer);
    GuiHandler.addHoverEffect(yesButton, [0, 200, 0, 255]);
    new UIText("Yes")
        .setX((40).percent())
        .setY((40).percent())
        .setChildOf(yesButton);
    
    let noButton = new UIBlock(GuiHandler.Color([200, 0, 0, 255]))
        .setWidth((38).percent())
        .setHeight((20).percent())
        .setX((52).percent())
        .setY((60).percent())
        .enableEffect(new OutlineEffect(GuiHandler.Color([85, 255, 255, 255]), 1))
        .setChildOf(confirmContainer);
    GuiHandler.addHoverEffect(noButton, [200, 0, 0, 255]);
    new UIText("No")
        .setX((40).percent())
        .setY((40).percent())
        .setChildOf(noButton);

    clickableElements = [];
    clickableElements.push({
        getAbsolute: function() {
            let s = getScreenSize();
            let absX = GuiHandler.percentToPixel(containerX, s.width);
            let absY = GuiHandler.percentToPixel(containerY, s.height);
            let absWidth = GuiHandler.percentToPixel(containerWidth, s.width);
            let absHeight = GuiHandler.percentToPixel(containerHeight, s.height);
            return {
                x: absX + GuiHandler.percentToPixel(8, absWidth),
                y: absY + GuiHandler.percentToPixel(60, absHeight),
                width: GuiHandler.percentToPixel(40, absWidth),
                height: GuiHandler.percentToPixel(20, absHeight)
            };
        },
        onClick: function(mouseX, mouseY, button) {
            let originalIndex = pastDianaEvents.events.length - 1 - eventIndex;
            pastDianaEvents.events.splice(originalIndex, 1);
            deleteCtGui.close();
            pastDianaEvents.save();
            initMainUI();
            pastEventsCtGui.open();
            return true;
        }
    });
    clickableElements.push({
        getAbsolute: function() {
            let s = getScreenSize();
            let absX = GuiHandler.percentToPixel(containerX, s.width);
            let absY = GuiHandler.percentToPixel(containerY, s.height);
            let absWidth = GuiHandler.percentToPixel(containerWidth, s.width);
            let absHeight = GuiHandler.percentToPixel(containerHeight, s.height);
            return {
                x: absX + GuiHandler.percentToPixel(52, absWidth),
                y: absY + GuiHandler.percentToPixel(60, absHeight),
                width: GuiHandler.percentToPixel(40, absWidth),
                height: GuiHandler.percentToPixel(20, absHeight)
            };
        },
        onClick: function(mouseX, mouseY, button) {
            deleteCtGui.close();
            pastEventsCtGui.open();
            return true;
        }
    });
    
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