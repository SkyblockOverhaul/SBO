// Import the Java threading library
const Threading = Java.type("gg.essential.api.utils.Multithreading");

/**
 * Adds a delay before executing a function or runs the function asynchronously.
 *
 * @param {function} func - The function to be executed after the delay.
 * @param {number} time - The delay time in milliseconds (optional). If not provided, the function will run asynchronously.
 */
export function delay(func, time) {
    if (time) {
        // Schedule the function to be executed after the specified delay.
        // The time value is converted to milliseconds using java.util.concurrent.TimeUnit.MILLISECONDS.
        Threading.schedule(() => { func() }, time, java.util.concurrent.TimeUnit.MILLISECONDS);
    } else {
        // Run the function asynchronously without any delay.
        Threading.runAsync(() => { func() });
    }
}