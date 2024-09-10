import { Achievement } from "../Diana/DianaAchievements";
import { data } from "../../utils/variables";
import { drawRectangleOutline } from "../../utils/functions";

const AchievementGui = new Gui();
let currentPage = 0;
let filterType = data.achievementFilter;
AchievementGui.registerDraw(achievementRender);
AchievementGui.registerClosed(onClose);

const rarityOrder = ["Common", "Uncommon", "Rare", "Epic", "Legendary", "Mythic", "Divine", "Impossible"];

function getLayoutData() {
    let displayX = Renderer.screen.getWidth();
    let displayY = Renderer.screen.getHeight();

    let boxWidth = 200;
    let boxHeight = 35;
    let spacingX = 20;
    let spacingY = 20;

    let columns = Math.floor((displayX * 0.75 - spacingX) / (boxWidth + spacingX));
    let rows = Math.floor((displayY * 0.6 - spacingY) / (boxHeight + spacingY));
    let achievementsPerPage = columns * rows;

    let totalWidth = columns * boxWidth + (columns - 1) * spacingX;
    let totalHeight = rows * boxHeight + (rows - 1) * spacingY;

    let startX = (displayX - totalWidth) / 2;
    let startY = (displayY - totalHeight) / 2;

    let buttonYPos = startY + totalHeight + 20;
    let buttonTextY = buttonYPos + 5;

    let buttonWidth = 30;
    let buttonHeight = 20;

    let filterButtonWidth = 85;
    let filterButtonHeight = 18;
    let filterY = startY - 23;
    let filterTextY = filterY + 5;
    let filterX = startX + totalWidth - filterButtonWidth;
    let filterTextX = filterX + 5;

    return {
        displayX,
        displayY,
        boxWidth,
        boxHeight,
        spacingX,
        spacingY,
        columns,
        rows,
        achievementsPerPage,
        totalWidth,
        totalHeight,
        startX,
        startY,
        buttonYPos,
        buttonTextY,
        buttonWidth,
        buttonHeight,
        filterButtonWidth,
        filterButtonHeight,
        filterY,
        filterTextY,
        filterX,
        filterTextX
    };
}

function updateTotalPages() {
    const { achievementsPerPage } = getLayoutData();
    let totalPages = Math.ceil(getFilteredAchievements().length / achievementsPerPage);
    
    if (currentPage >= totalPages) {
        currentPage = totalPages - 1;
    }
}

function onClose() {
    currentPage = 0;
}

function drawTitleAndSubtitle(startY, startX) {
    let title = "SBO Achievements";
    let titleText = new Text(title);
    titleText.setScale(2.25);
    titleText.setColor(Renderer.color(255, 255, 255, 255)); 
    titleText.setShadow(true); 

    let titleHeight = titleText.getHeight();
    let titleY = startY - titleHeight - 10; 

    titleText.setX(startX).setY(titleY).draw();

    let achievementPercentage = (Achievement.achievementsUnlocked / Achievement.list.length * 100).toFixed(2);
    let subtitle = "Unlocked: " + Achievement.achievementsUnlocked + "/" + Achievement.list.length + " (" + achievementPercentage + "%)";
    let subtitleText = new Text(subtitle);
    subtitleText.setScale(1.0);  
    subtitleText.setColor(Renderer.color(0, 255, 35, 224)); 
    subtitleText.setShadow(true); 

    let subtitleY = titleY + titleHeight - 2;

    subtitleText.setX(startX + 2).setY(subtitleY).draw();
}

function drawFilterDropdown(filterButtonWidth, filterButtonHeight, filterY, filterTextY, filterTextX, filterX) {
    Renderer.drawRect(Renderer.color(0, 0, 0, 150), filterX, filterY, filterButtonWidth, filterButtonHeight);
    Renderer.drawString("Filter: " + filterType.charAt(0).toUpperCase() + filterType.slice(1), filterTextX, filterTextY);
    drawRectangleOutline(Renderer.color(255, 255, 255, 255), filterX, filterY, filterButtonWidth, filterButtonHeight, 1);
}

function getFilteredAchievements() {
    let achievements = [...Achievement.list];
    if (filterType === "Rarity") {
        achievements.sort((a, b) => rarityOrder.indexOf(a.rarity) - rarityOrder.indexOf(b.rarity));
    }
    else if (filterType === "Locked") {
        achievements = achievements.filter(a => !a.isUnlocked());
    }
    else if (filterType === "Unlocked") {
        achievements = achievements.filter(a => a.isUnlocked());
    }
    return achievements;
}

function achievementRender() {
    const layout = getLayoutData();
    let { displayX, displayY, boxWidth, boxHeight, spacingX, spacingY, columns,
        achievementsPerPage, totalWidth, totalHeight, startX, startY, buttonYPos,
        buttonTextY, buttonWidth, buttonHeight, filterButtonWidth, filterButtonHeight,
        filterY, filterTextY, filterX, filterTextX
    } = layout;

    let achievementsToDisplay = getFilteredAchievements();
    let startAchievement = currentPage * achievementsPerPage;
    let endAchievement = Math.min(startAchievement + achievementsPerPage, achievementsToDisplay.length);

    Renderer.drawRect(Renderer.color(0, 0, 0, 200), 0, 0, displayX, displayY);

    drawTitleAndSubtitle(startY, startX);
    drawFilterDropdown(filterButtonWidth, filterButtonHeight, filterY, filterTextY, filterTextX, filterX);

    for (let i = startAchievement; i < endAchievement; i++) {
        let achievement = achievementsToDisplay[i];
        if (!achievement) continue;
        let index = i - startAchievement;
        let column = index % columns;
        let row = Math.floor(index / columns);

        let x = startX + column * (boxWidth + spacingX);
        let y = startY + row * (boxHeight + spacingY);

        Renderer.drawRect(Renderer.color(0, 0, 0, 150), x, y, boxWidth, boxHeight);

        let borderColor = achievement.isUnlocked() ? Renderer.color(0, 255, 0, 255) : Renderer.color(255, 0, 0, 255);
        let thickness = 1;

        drawRectangleOutline(borderColor, x, y, boxWidth, boxHeight, thickness);
        Renderer.drawString(`${achievement.getDisplayName()}`, x + 5, y + 5);
        Renderer.drawString(`&7${achievement.getDescription()}`, x + 5, y + 15);
        Renderer.drawString(`${achievement.color}${achievement.rarity}`, x + 5, y + boxHeight - 10);
    }

    Renderer.drawRect(Renderer.color(0, 0, 0, 150), startX, buttonYPos, buttonWidth, buttonHeight);
    Renderer.drawString("<-", startX + 10, buttonTextY + 2);
    drawRectangleOutline(Renderer.color(255, 255, 255, 255), startX, buttonYPos, buttonWidth, buttonHeight, 1);
    
    Renderer.drawRect(Renderer.color(0, 0, 0, 150), startX + totalWidth - buttonWidth, buttonYPos, buttonWidth, buttonHeight);
    Renderer.drawString("->", startX + totalWidth - buttonWidth + 10, buttonTextY + 2);
    drawRectangleOutline(Renderer.color(255, 255, 255, 255), startX + totalWidth - buttonWidth, buttonYPos, buttonWidth, buttonHeight, 1);

    Renderer.drawString(`Page ${currentPage + 1} of ${Math.ceil(achievementsToDisplay.length / achievementsPerPage)}`, startX, startY + totalHeight + 7);
}

AchievementGui.registerClicked((mouseX, mouseY, button) => {
    const layout = getLayoutData();
    let { startX, totalWidth, columns, rows, buttonYPos, buttonHeight,
        buttonWidth, filterButtonWidth, filterButtonHeight,
        filterY, filterX
    } = layout;
    if (mouseX >= filterX && mouseX <= filterX + filterButtonWidth && mouseY >= filterY && mouseY <= filterY + filterButtonHeight) {
        const filterOptions = ["Default", "Rarity", "Locked", "Unlocked"];
        let currentIndex = filterOptions.indexOf(filterType);
        if (button == 1) {
            filterType = filterOptions[(currentIndex + filterOptions.length - 1) % filterOptions.length];
            currentPage = 0;
            data.achievementFilter = filterType;
            data.save();
        }
        else if (button == 0) {
            filterType = filterOptions[(currentIndex + 1) % filterOptions.length];
            data.achievementFilter = filterType;
            currentPage = 0;
            data.save();
        }
        updateTotalPages();
    }

    if (mouseX >= startX && mouseX <= startX + buttonWidth && mouseY >= buttonYPos && mouseY <= buttonYPos + buttonHeight) {
        if (currentPage > 0) {
            currentPage--;
        }
    }
    
    if (mouseX >= startX + totalWidth - buttonWidth && mouseX <= startX + totalWidth && mouseY >= buttonYPos && mouseY <= buttonYPos + buttonHeight) {
        if ((currentPage + 1) * columns * rows < getFilteredAchievements().length) {
            currentPage++;
        }
    }
});

register("command", () => {
    currentPage = 0;
    AchievementGui.open();
}).setName("sboAchievements");

register("tick", () => {
    const { achievementsPerPage } = getLayoutData();
    let totalPages = Math.ceil(getFilteredAchievements().length / achievementsPerPage);
    if (AchievementGui.isOpen() && currentPage >= totalPages) {
        updateTotalPages();
    }
});

const button = new net.minecraft.client.gui.GuiButton(299999, 0, 0, 30, 20, "SBO");
register("postGuiRender", (mx, my, gui) => {
    if (!(gui instanceof net.minecraft.client.gui.GuiIngameMenu)) return;

    button.field_146128_h = (Renderer.screen.getWidth() / 2) + 104;
    button.field_146129_i = (Renderer.screen.getHeight() / 4) + 32;

    button.func_146112_a(Client.getMinecraft(), mx, my);
});

register("guiMouseClick", (mx, my, mb, gui) => {
    if (
        mx > button.field_146128_h && 
        mx < button.field_146128_h + button.field_146120_f &&
        my > button.field_146129_i &&
        my < button.field_146129_i + button.field_146121_g &&
        gui instanceof net.minecraft.client.gui.GuiIngameMenu
    ){
        currentPage = 0;
        AchievementGui.open();
    }
});