import { pastDianaEvents } from "../../utils/variables";
import { toTitleCase, formatNumber, drawRectangleOutline, formatTime, getDianaAhPrice, getBazaarPriceDiana } from "../../utils/functions";

const PastDianaEventsGui = new Gui();
const ConfirmDeleteGui = new Gui();
let currentPage = 0;
let showConfirmDelete = false;
let deleteYear = 0;
let deletePage = 0;
let pastDianaEventsList;

PastDianaEventsGui.registerDraw(pastDianaEventsRender);
PastDianaEventsGui.registerClosed(onClose);
PastDianaEventsGui.registerOpened(() => {
    pastDianaEventsList = processDianaEvents();
});
ConfirmDeleteGui.registerDraw(confirmDeleteRender);
ConfirmDeleteGui.registerClosed(onCloseConfirmDelete);

function processDianaEvents() {
    let events = pastDianaEvents.events;
    if (events.length === 0) {
        return [];
    }
    return [...events].reverse();
}

function layoutData() {
    // Get the current GUI scale
    let guiScale = Client.settings.video.getGuiScale();
    
    // Define the standard scale that we want to simulate (2.0)
    const targetScale = 2.0;
    
    // Calculate the compensation factor
    let compensationFactor = targetScale / guiScale;

    // Get the screen dimensions
    let displayX = Renderer.screen.getWidth();
    let displayY = Renderer.screen.getHeight();

    // Adjust dimensions based on the compensation factor
    let boxWidth = 300 * compensationFactor;
    let boxHeight = calculateBoxHeight() * compensationFactor;
    let spacingX = 20 * compensationFactor;
    let spacingY = 20 * compensationFactor;
    let totalWidth = boxWidth * 2 + spacingX;
    let totalHeight = boxHeight + spacingY;
    let startX = (displayX - totalWidth) / 2;
    let startY = (displayY - totalHeight) / 2;
    let titleY = displayY * 0.15 * compensationFactor;
    let boxY = titleY + 3 * compensationFactor;

    let buttonWidth = 30 * compensationFactor;
    let buttonHeight = 20 * compensationFactor;
    let buttonTextY = boxY + 10 * compensationFactor;

    if (guiScale === 1) {
        startY -= 65;
        titleY -= 65;
        boxY -= 65;
        buttonTextY -= 65;
    }
    if (guiScale === 3) {
        startY += 10;
        titleY += 10;
        boxY += 10;
        buttonTextY += 10;
    }

    return {
        displayX,
        displayY,
        boxWidth,
        spacingX,
        spacingY,
        startX,
        titleY,
        buttonWidth,
        buttonHeight,
        boxHeight,
        totalWidth,
        startY,
        buttonSpacing: 20 * compensationFactor,
        buttonTextY,
        boxY
    };
}


function calculateBoxHeight() {
    const events = pastDianaEventsList
    if (events.length === 0) return 300;

    let itemHeight = 16;
    let mobHeight = 16;
    let maxItemHeight = 0;
    let maxMobHeight = 0;

    for (let item in events[currentPage].items) {
        maxItemHeight += itemHeight;
    }

    for (let mob in events[currentPage].mobs) {
        maxMobHeight += mobHeight;
    }

    return Math.max(maxItemHeight, maxMobHeight) + 50;
}

function onClose() {
    currentPage = 0;
}

function onCloseConfirmDelete() {
    showConfirmDelete = false;
    PastDianaEventsGui.open();
    deleteYear = 0;
    deletePage = 0;
}

function drawTitle() {
    let title = "SBO PastEvents";
    
    let { startX, titleY } = layoutData();
    
    let guiScale = Client.settings.video.getGuiScale();
    let scale = 2.25 * (2.0 / guiScale);

    Renderer.scale(scale, scale);
    
    let adjustedX = startX / scale;
    let adjustedY = (titleY - 34) / scale;

    if (guiScale === 1)
        adjustedY = (titleY - 60) / scale

    Renderer.drawStringWithShadow(title, adjustedX, adjustedY);
    Renderer.scale(1, 1);
}

function pastDianaEventsRender() {
    let events = pastDianaEventsList;
    const layout = layoutData();
    let { displayX, boxWidth, startX, boxY, titleY, buttonWidth, buttonHeight, boxHeight, totalWidth, buttonSpacing, buttonTextY } = layout;

    let event = events[currentPage];
    let lineColor = Renderer.color(255, 255, 255, 255);
    let boxColor = Renderer.color(0, 0, 0, 150);
    let guiScale = Client.settings.video.getGuiScale();
    let scale = 2.0 / guiScale;
    let adjustedX = 0;
    let adjustedY = 0;

    Renderer.drawRect(Renderer.color(0, 0, 0, 200), 0, 0, displayX, Renderer.screen.getHeight());

    drawTitle();
    if (events.length === 0) {
        adjustedX = startX / scale;
        adjustedY = boxY / scale;
        Renderer.scale(scale, scale);
        Renderer.drawStringWithShadow(`&aNo Events Found`, adjustedX, adjustedY);
        Renderer.scale(1, 1);

        adjustedX = startX / scale;
        adjustedY = (titleY - 12) / scale;
        Renderer.scale(scale, scale);
        Renderer.drawString(`&aPage &7${currentPage} &aof &7${events.length}`, adjustedX, adjustedY);
        Renderer.scale(1, 1);
        return;
    }
    let yearX = startX + 75;
    let yearY = titleY - 12;
    if (guiScale === 1) {
        adjustedX = yearX / scale;
        adjustedY = (yearY - 2) / scale;
        Renderer.scale(scale, scale);
        Renderer.drawString(`&aYear: &7` + event.year, adjustedX + 30, adjustedY);
        Renderer.scale(1, 1);
    }
    else {
        Renderer.drawStringWithShadow(`&aYear: &7` + event.year, yearX, yearY);
    }

    Renderer.drawRect(boxColor, startX, boxY, boxWidth, boxHeight);
    if (guiScale === 1) {
        adjustedX = (startX + 10) / scale;
        adjustedY = (boxY + 8) / scale;
        Renderer.scale(scale, scale);
        Renderer.drawString(`&aItems`, adjustedX, adjustedY - 2);
        Renderer.scale(1, 1);
    }
    else {
        Renderer.drawStringWithShadow(`&aItems`, startX + 10, boxY + 8);
    }
    Renderer.drawLine(lineColor, startX, boxY + 23, startX + boxWidth, boxY + 23, 1);
    drawRectangleOutline(lineColor, startX, boxY, boxWidth, boxHeight, 1);
    let itemYPos = boxY + 30;
    drawItems(event, startX, itemYPos);
    
    let mobsX = startX + boxWidth + layout.spacingX;
    Renderer.drawRect(boxColor, mobsX, boxY, boxWidth, boxHeight);
    if (guiScale === 1) {
        adjustedX = (mobsX + 10) / scale;
        adjustedY = (boxY + 8) / scale;
        Renderer.scale(scale, scale);
        Renderer.drawString(`&aMobs`, adjustedX, adjustedY - 2);
        Renderer.scale(1, 1);
    }
    else {
        Renderer.drawStringWithShadow(`&aMobs`, mobsX + 10, boxY + 8);
    }
    Renderer.drawLine(lineColor, mobsX, boxY + 23, mobsX + boxWidth, boxY + 23, 1);
    drawRectangleOutline(lineColor, mobsX, boxY, boxWidth, boxHeight, 1);
    let mobYPos = boxY + 30;
    drawMobs(event, mobsX, mobYPos);

    let prevButtonX = startX - buttonWidth - buttonSpacing;
    adjustedX = (prevButtonX + 10) / scale;
    adjustedY = (buttonTextY - 3) / scale;
    Renderer.drawRect(Renderer.color(0, 0, 0, 150), prevButtonX, boxY, buttonWidth, buttonHeight);
    Renderer.scale(scale, scale);
    Renderer.drawString("<-", adjustedX, adjustedY);
    Renderer.scale(1, 1);
    drawRectangleOutline(lineColor, prevButtonX, boxY, buttonWidth, buttonHeight, 1);
    
    let nextButtonX = startX + totalWidth + buttonSpacing;
    adjustedX = (nextButtonX + 10) / scale;
    Renderer.drawRect(Renderer.color(0, 0, 0, 150), nextButtonX, boxY, buttonWidth, buttonHeight);
    Renderer.scale(scale, scale);
    Renderer.drawString("->", adjustedX, adjustedY);
    Renderer.scale(1, 1);
    drawRectangleOutline(lineColor, nextButtonX, boxY, buttonWidth, buttonHeight, 1);


    if (guiScale === 1) {
        Renderer.drawRect(Renderer.color(0, 0, 0, 150), startX, titleY + boxHeight + 7, buttonWidth + 73, buttonHeight);
        drawRectangleOutline(Renderer.color(255, 0, 0, 255), startX, titleY + boxHeight + 7, buttonWidth + 73, buttonHeight, 1);
    }
    else {
        Renderer.drawRect(Renderer.color(0, 0, 0, 150), startX, titleY + boxHeight + 7, buttonWidth + 43, buttonHeight);
        drawRectangleOutline(Renderer.color(255, 0, 0, 255), startX, titleY + boxHeight + 7, buttonWidth + 43, buttonHeight, 1);
    }
    adjustedX = (startX + 5) / scale;
    adjustedY = (titleY + boxHeight + 13) / scale;
    Renderer.scale(scale, scale);
    Renderer.drawString("Delete Event",  adjustedX, adjustedY);
    Renderer.scale(1, 1);


    if (guiScale === 1) {
        adjustedX = startX / scale;
        adjustedY = (titleY - 14) / scale;
        Renderer.scale(scale, scale);
        Renderer.drawString(`&aPage &7${currentPage + 1} &aof &7${events.length}`, adjustedX, adjustedY);
        Renderer.scale(1, 1);
    }
    else {
        Renderer.drawString(`&aPage &7${currentPage + 1} &aof &7${events.length}`, startX, titleY - 12);
    }
    if (showConfirmDelete) {
        ConfirmDeleteGui.open();
        deleteYear = event.year;
        deletePage = currentPage;
    }
}

function drawItems(event, startX, itemYPos) {
    let totalProfit = 0;
    let guiScale = Client.settings.video.getGuiScale();
    let scale = 2.0 / guiScale;

    const sortOrder = ["Chimera", "Chimerals", "Minos Relic", "Daedalus Stick", "Crown Of Greed", 
                       "Washed-up Souvenir", "Griffin Feather", "Dwarf Turtle Shelmet", 
                       "Crochet Tiger Plushie", "Antique Remedies", "Ancient Claw", 
                       "Enchanted Ancient Claw", "Enchanted Gold", "Enchanted Iron", 
                       "Coins", "Fishcoins", "Scavengercoins", "Mayortime"];

    let itemsArray = Object.entries(event.items);

    itemsArray.sort((a, b) => {
        let itemNameA = toTitleCase(a[0].replaceAll("_", " "));
        let itemNameB = toTitleCase(b[0].replaceAll("_", " "));

        let indexA = sortOrder.indexOf(itemNameA);
        let indexB = sortOrder.indexOf(itemNameB);

        if (indexA === -1) indexA = sortOrder.length;
        if (indexB === -1) indexB = sortOrder.length;

        return indexA - indexB;
    });

    for (let i = 0; i < itemsArray.length; i++) {
        let item = itemsArray[i][0];
        let amount = itemsArray[i][1];
        let itemName = item.replaceAll("_", " ");
        let itemAmount = 0;
        itemName = toTitleCase(itemName);
        totalProfit += calcTotalProfit(itemName, amount);
        if (itemName === "Mayortime") {
            itemName = "Playtime";
            itemAmount = formatTime(amount);
        }
        else if (itemName === "Coins" && event.items["fishCoins"] > 0 && event.items["scavengerCoins"] > 0) {
            itemAmount = formatNumber(amount - event.items["fishCoins"] - event.items["scavengerCoins"]);
            itemName = "Treasure";
        }
        else {
            itemAmount = formatNumber(amount);
        }
        Renderer.scale(scale, scale);
        let adjustedX = (startX + 10) / scale;
        let adjustedY = itemYPos / scale;
        Renderer.drawString(`&a${itemName}: &7${itemAmount}`, adjustedX, adjustedY);
        Renderer.scale(1, 1);
        if (guiScale === 3) {
            itemYPos += 8;
        }
        else if (guiScale === 1) {
            itemYPos += 34;
        }
        else {
            itemYPos += 16;
        }
    }
    Renderer.scale(scale, scale);
    let adjustedX = (startX + 10) / scale;
    let adjustedY = itemYPos / scale;
    Renderer.drawString(`&6Total Profit: ` + formatNumber(totalProfit), adjustedX, adjustedY);
    Renderer.scale(1, 1);
}

function drawMobs(event, mobsX, mobYPos) {
    let guiScale = Client.settings.video.getGuiScale();
    let scale = 2.0 / guiScale;

    const sortOrder = [
        "Minos Inquisitor", "Minos Inquisitor Ls"
    ]

    let mobsArray = Object.entries(event.mobs);

    mobsArray.sort((a, b) => {
        let mobNameA = toTitleCase(a[0].replaceAll("_", " "));
        let mobNameB = toTitleCase(b[0].replaceAll("_", " "));

        let indexA = sortOrder.indexOf(mobNameA);
        let indexB = sortOrder.indexOf(mobNameB);

        if (indexA === -1) indexA = sortOrder.length;
        if (indexB === -1) indexB = sortOrder.length;

        return indexA - indexB;
    });

    for (let i = 0; i < mobsArray.length; i++) {
        Renderer.scale(scale, scale);
        let adjustedX = (mobsX + 10) / scale;
        let adjustedY = mobYPos / scale;
        Renderer.drawString(`&a${mobsArray[i][0]}: &7${mobsArray[i][1]}`, adjustedX, adjustedY);
        Renderer.scale(1, 1);
        if (guiScale === 3) {
            mobYPos += 8;
        }
        else if (guiScale === 1) {
            mobYPos += 34;
        }
        else {
            mobYPos += 16;
        }
    }
}


function calcTotalProfit(item, amount) {
    let totalProfit = 0;
    switch (item) {
        case "Coins":
            totalProfit += amount;
            break;
        case "Griffin Feather":
            totalProfit += getBazaarPriceDiana("GRIFFIN_FEATHER") * amount;
            break;
        case "Ancient Claws":
            totalProfit += getBazaarPriceDiana("ANCIENT_CLAW") * amount;
            break;
        case "Enchanted Ancient Claws":
            totalProfit += getBazaarPriceDiana("ENCHANTED_ANCIENT_CLAW") * amount;
            break;
        case "Enchanted Gold":
            totalProfit += getBazaarPriceDiana("ENCHANTED_GOLD") * amount;
            break;
        case "Enchanted Iron":
            totalProfit += getBazaarPriceDiana("ENCHANTED_IRON") * amount;
            break;
        case "Chimera":
            totalProfit += getBazaarPriceDiana("ENCHANTMENT_ULTIMATE_CHIMERA_1") * amount;
            break;
        case "Minos Relic":
            totalProfit += getDianaAhPrice("MINOS_RELIC") * amount;
            break;
        case "Daedalus Stick":
            totalProfit += getBazaarPriceDiana("DAEDALUS_STICK") * amount;
            break;
        case "Crown of Greed":
            totalProfit += getDianaAhPrice("CROWN_OF_GREED") * amount;
            break;
        case "Washed-up Souvenir":
            totalProfit += getDianaAhPrice("WASHED_UP_SOUVENIR") * amount;
            break;
        case "Dwarf Turtle Shelmet":
            totalProfit += getDianaAhPrice("DWARF_TURTLE_SHELMET") * amount;
            break;
        case "Crochet Tiger Plushie":
            totalProfit += getDianaAhPrice("CROCHET_TIGER_PLUSHIE") * amount;
            break;
        case "Antique Remedies":
            totalProfit += getDianaAhPrice("ANTIQUE_REMEDIES") * amount;
            break;
        case "Chimerals":
            totalProfit += getBazaarPriceDiana("ENCHANTMENT_ULTIMATE_CHIMERA_1") * amount;
            break;
    }
    return totalProfit;
}

function confirmDeleteRender() {
    const layout = layoutData();
    let { displayX, displayY } = layout;

    Renderer.drawRect(Renderer.color(0, 0, 0, 200), 0, 0, displayX, Renderer.screen.getHeight());

    let boxWidth = 200;
    let boxHeight = 100;
    let buttonWidth = 70;
    let buttonHeight = 20;

    let boxColor = Renderer.color(0, 0, 0, 200);
    let buttonColor = Renderer.color(0, 0, 0, 150);
    let textColor = Renderer.color(255, 255, 255, 255);

    let centerX = (displayX - boxWidth) / 2;
    let centerY = (displayY - boxHeight) / 2;

    Renderer.drawRect(boxColor, centerX, centerY, boxWidth, boxHeight);
    drawRectangleOutline(textColor, centerX, centerY, boxWidth, boxHeight, 1);

    Renderer.drawStringWithShadow("Do you want to delete event " + deleteYear + "?", centerX + 10, centerY + 10);
    
    Renderer.drawRect(buttonColor, centerX + 20, centerY + 60, buttonWidth, buttonHeight);
    Renderer.drawString("Yes", centerX + 30, centerY + 66);
    drawRectangleOutline(textColor, centerX + 20, centerY + 60, buttonWidth, buttonHeight, 1);
    
    Renderer.drawRect(buttonColor, centerX + 110, centerY + 60, buttonWidth, buttonHeight);
    Renderer.drawString("No", centerX + 120, centerY + 66);
    drawRectangleOutline(textColor, centerX + 110, centerY + 60, buttonWidth, buttonHeight, 1);
}

function deleteEvent() {
    const events = pastDianaEventsList;
    if (events.length === 0) return;

    events.splice(deletePage, 1);

    if (deletePage >= events.length) {
        deletePage = events.length - 1;
    }

    pastDianaEvents.events = [...events].reverse();
    pastDianaEvents.save();
    showConfirmDelete = false;
}

PastDianaEventsGui.registerClicked((mouseX, mouseY, button) => {
    let events = pastDianaEventsList;
    const layout = layoutData();
    let { startX, buttonWidth, buttonHeight, buttonSpacing, totalWidth, titleY, boxHeight } = layout;

    if (mouseX >= startX - buttonWidth - buttonSpacing &&
        mouseX <= startX - buttonSpacing &&
        mouseY >= titleY && mouseY <= titleY + buttonHeight) {
        if (currentPage > 0) {
            currentPage--;
        }
    }

    if (mouseX >= startX + totalWidth + buttonSpacing &&
        mouseX <= startX + totalWidth + buttonWidth + buttonSpacing &&
        mouseY >= titleY && mouseY <= titleY + buttonHeight) {
        if (currentPage < events.length - 1) {
            currentPage++;
        }
    }

    let deleteButtonWidth = buttonWidth + 43;
    if (mouseX >= startX &&
        mouseX <= startX + deleteButtonWidth &&
        mouseY >= titleY + boxHeight + 7 && mouseY <= titleY + boxHeight + 7 + buttonHeight) {
        showConfirmDelete = true;
    }
});

ConfirmDeleteGui.registerClicked((mouseX, mouseY, button) => {
    const layout = layoutData();
    let { displayX, displayY } = layout;

    let boxWidth = 200;
    let boxHeight = 100;
    let buttonWidth = 70;
    let buttonHeight = 20;

    let centerX = (displayX - boxWidth) / 2;
    let centerY = (displayY - boxHeight) / 2;

    if (mouseX >= centerX + 20 &&
        mouseX <= centerX + 20 + buttonWidth &&
        mouseY >= centerY + 60 &&
        mouseY <= centerY + 60 + buttonHeight) {
        deleteEvent();
        ConfirmDeleteGui.close();
    }

    if (mouseX >= centerX + 110 &&
        mouseX <= centerX + 110 + buttonWidth &&
        mouseY >= centerY + 60 &&
        mouseY <= centerY + 60 + buttonHeight) {
        ConfirmDeleteGui.close();
    }
});

register("command", () => {
    currentPage = 0;
    PastDianaEventsGui.open();
}).setName("sbopastdianaevents").setAliases("sbopde");