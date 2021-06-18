package info.skyblond.telegram.icarus.bot

import org.telegram.telegrambots.meta.api.objects.Message

/**
 * Offer interaction with Neo blockchain.
 * Specifically the Cat Token and WCA contract.
 *
 * Functions:
 *  + /neo info - give some info about Cat Token and WCA Contract, address etc.
 *  + /neo query - choose what to query: CAT/GAS balance or WCA details
 *  + /neo wallet - open a wallet by giving a WIF string or exit
 *  + /neo transfer - transfer cat token to some address
 *  + /neo create - create a WCA
 *  + /neo milestone - finish a milestone
 *  + /neo finish - finish a WCA
 *  + /neo refund - make a refund
 *
 * TODO:
 *  + Query CAT Token balance by giving a address
 *  + Query GAS Token balance by giving a address
 *  + Open a wallet by WIF string *STORE IN MEMORY*
 *  + Transfer CAT Token to a given address with or without a string id
 *  + Query WCA contract info by giving a string id
 *  + Create a WCA by asking some info
 *  + Finish a WCA milestone by asking the id and proof of work
 *  + Finish a WCA if last milestone is expired
 *  + Make a refund by asking the id
 *  + Cache for queried WCAs, like milestone info, etc.
 * */
object NeoHandler : CommandHandler {
    override val stateHandlers: List<Pair<String, (IcarusCore, Message) -> String?>>
        get() = listOf()
    override val commandHandlers: List<Pair<BotCommand, (IcarusCore, Message) -> String?>>
        get() = listOf()
}