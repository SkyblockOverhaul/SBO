import { Achivement } from "../Diana/DianaAchievements";

const AchivementGui = new Gui();
let currentPage = 0;

register("command", () => {
    currentPage = 0;
    AchivementGui.open();
}).setName("sboAchievements");

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
        buttonHeight
    };
}

function updateTotalPages() {
    const { achievementsPerPage } = getLayoutData();
    let totalPages = Math.ceil(Achivement.list.length / achievementsPerPage);
    
    if (currentPage >= totalPages) {
        currentPage = totalPages - 1;
    }
}

register("tick", () => {
    const { achievementsPerPage } = getLayoutData();
    let totalPages = Math.ceil(Achivement.list.length / achievementsPerPage);
    if (AchivementGui.isOpen() && currentPage >= totalPages) {
        updateTotalPages();
    }
});

AchivementGui.registerDraw(achievementRender);
AchivementGui.registerClosed(onClose);

function onClose() {
    currentPage = 0;
}

function achievementRender() {
    const layout = getLayoutData();
    let { displayX, displayY, boxWidth, boxHeight, spacingX, spacingY, columns, rows, achievementsPerPage, totalWidth, totalHeight, startX, startY, buttonYPos, buttonTextY, buttonWidth, buttonHeight } = layout;

    let startAchievement = currentPage * achievementsPerPage;
    let endAchievement = Math.min(startAchievement + achievementsPerPage, Achivement.list.length);

    Renderer.drawRect(Renderer.color(0, 0, 0, 200), 0, 0, displayX, displayY);

    let title = "SBO Achievements";
    let titleText = new Text("");
    titleText.setString(title);
    titleText.setScale(2.25);
    titleText.setColor(Renderer.color(255, 255, 255, 255)); 
    titleText.setShadow(true); 

    let titleHeight = titleText.getHeight();
    let titleY = startY - titleHeight - 10; 

    titleText.setX(startX).setY(titleY).draw();


    for (let i = startAchievement; i < endAchievement; i++) {
        let achievement = Achivement.list[i];
        let index = i - startAchievement;
        let column = index % columns;
        let row = Math.floor(index / columns);

        let x = startX + column * (boxWidth + spacingX);
        let y = startY + row * (boxHeight + spacingY);

        Renderer.drawRect(Renderer.color(0, 0, 0, 150), x, y, boxWidth, boxHeight);

        let borderColor = achievement.isUnlocked() ? Renderer.color(0, 255, 0, 255) : Renderer.color(255, 0, 0, 255);
        let thickness = 1;
        
        Renderer.drawLine(borderColor, x, y, x + boxWidth, y, thickness);
        Renderer.drawLine(borderColor, x, y, x, y + boxHeight, thickness);
        Renderer.drawLine(borderColor, x + boxWidth, y, x + boxWidth, y + boxHeight, thickness);
        Renderer.drawLine(borderColor, x, y + boxHeight, x + boxWidth, y + boxHeight, thickness);

        Renderer.drawString(`${achievement.getDisplayName()}`, x + 5, y + 5);
        Renderer.drawString(`&7${achievement.getDescription()}`, x + 5, y + 15);
        Renderer.drawString(`${achievement.color}${achievement.rarity}`, x + 5, y + boxHeight - 10);
    }

    Renderer.drawRect(Renderer.color(0, 0, 0, 150), startX, buttonYPos, buttonWidth, buttonHeight);
    Renderer.drawString("<-", startX + 10, buttonTextY);
    
    Renderer.drawRect(Renderer.color(0, 0, 0, 150), startX + totalWidth - buttonWidth, buttonYPos, buttonWidth, buttonHeight);
    Renderer.drawString("->", startX + totalWidth - buttonWidth + 10, buttonTextY);

    Renderer.drawString(`Page ${currentPage + 1} of ${Math.ceil(Achivement.list.length / achievementsPerPage)}`, startX, startY + totalHeight + 7);
}

AchivementGui.registerClicked((mouseX, mouseY) => {
    const layout = getLayoutData();
    let { startX, totalWidth, columns, rows, buttonYPos, buttonHeight, buttonWidth } = layout;

    if (mouseX >= startX && mouseX <= startX + buttonWidth && mouseY >= buttonYPos && mouseY <= buttonYPos + buttonHeight) {
        if (currentPage > 0) {
            currentPage--;
        }
    }
    
    if (mouseX >= startX + totalWidth - buttonWidth && mouseX <= startX + totalWidth && mouseY >= buttonYPos && mouseY <= buttonYPos + buttonHeight) {
        if ((currentPage + 1) * columns * rows < Achivement.list.length) {
            currentPage++;
        }
    }
});
