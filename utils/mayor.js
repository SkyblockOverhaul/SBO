import { request } from "../../requestV2";


// The URL of the API endpoint to fetch information about the mayor.
const MAYOR_API = "https://api.hypixel.net/resources/skyblock/election";

// Variable to store the mayor's name, initially set to undefined.
let mayor = undefined;

/**
 * Gets the current mayor's name.
 *
 * @returns {string|undefined} - The name of the mayor, or undefined if not set yet.
 */
export function getMayor() {
    return mayor;
}

// An array to store the names of the mayor's perks.
let perks = new Set([]);

/**
 * Gets the array of mayor's perks.
 *
 * @returns {string[]} - An array containing the names of the mayor's perks.
 */
export function getPerks() {
    return perks;
}

/**
 * Makes a PULL request to get mayor info from Hypixel API.
 */
request({
    url: MAYOR_API,
    json: true
}).then((response)=>{
    mayor = response.mayor.name;
    perks = new Set([...response.mayor.perks.map(perk => perk.name)]);
}).catch((error)=>{
    console.error(error);
});