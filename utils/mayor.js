import { request } from "../../requestV2";


let perks = new Set([]);
let mayor = undefined;
let year = 0;
let lastMonth = 0;
let skyblockDate = "";

/**
 * Gets the array of mayor's perks.
 *
 * @returns {string[]} - An array containing the names of the mayor's perks.
 */
export function getPerks() {
    return perks;
}

export function getMayor() {
    return mayor;
}

export function getYear() {
    return year;
}

export function getSkyblockDate() {
    return skyblockDate;
}

register("worldLoad", () => {
    year, mayor = getYearMayorRequestV2();
});

function getYearMayorRequestV2() {
    request({
        url: "https://api.hypixel.net/resources/skyblock/election",
        json: true
    }).then((response)=>{
        mayor = response.mayor.name;
        year = response.current.year;
        perks = new Set([...response.mayor.perks.map(perk => perk.name)]);
    }).catch((error)=>{
        console.error(error);
    });

    return year, mayor;
}


function convertDate(dateStr) {
    if (dateStr === undefined) return "";
    var seasonToMonth = {
        'Early Spring': '1',
        "Spring": '2',
        "Late Spring": '3',
        "Early Summer": '4',
        "Summer": '5',
        "Late Summer": '6',
        "Early Autumn": '7',
        "Autumn": '8',
        "Late Autumn": '9',
        "Early Winter": '10',
        "Winter": '11',
        "Late Winter": '12'
    };

    dateStr = dateStr.toString().replace(/[^a-z0-9\s-]/gi, '');
    var parts = dateStr.split(' ');
    if (parts.length === 4) {
        season = parts[1] + ' ' + parts[2];
        day = parts[3];
    }
    else {
        season = parts[1];
        day = parts[2];
    }
    
    day = day.replace(/(st|nd|rd|th)$/, '');

    var month = seasonToMonth[season] || '';
    if (lastMonth === 12) {  // if the last month was December, and the current month is January, then the year has changed
        year++;
    }
    year = getYear();
    date = day + '.' + month + '.' + year;
    lastMonth = month;
    return date;
}

register("step", () => {
    skyblockDate = convertDate(Scoreboard.getLines()[7]);
    ChatLib.chat(skyblockDate);
}).setFps(1);
