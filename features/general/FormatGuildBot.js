
register("chat", (player, message, event) =>{
    // cancel original message
    // cancel(event);
    // send new guildbot message
    if (!player.includes(" ")) {
        player = player.removeFormatting();
        ChatLib.chat("&r&2Guild > &b[DC] &b" + player + "&r:" + message);
        print("&r&2Guild > &b[DC] &b" + player + "&r:" + message);
    }
    else {
        print("contains space");
    }
}).setCriteria("&r&2Guild > &a[VIP] SlowDT &3[GM]&f: ${player}:${message}").setContains()

// &r&2Guild > &a[VIP] SlowDT &3[GM]&f: &rSuccesfully invited kenchika to the party!&r
// &r&2Guild > &b[MVP&2+&b] MasterNR &3[320]&f: &rnice&r