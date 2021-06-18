package info.skyblond.telegram.icarus.bot

import info.skyblond.telegram.icarus.utils.ContractHelper
import info.skyblond.telegram.icarus.utils.Memory
import io.neow3j.wallet.Account
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.objects.Message
import java.lang.Exception
import java.util.concurrent.atomic.AtomicLong


/**
 * Offer interaction with Neo blockchain.
 * Specifically the Cat Token and WCA contract.
 *
 * TODO:
 *  + Query CAT Token balance by giving a address
 *  + Query WCA contract info by giving a string id
 * */
object NeoHandler : CommandHandler {
    private val logger = LoggerFactory.getLogger(NeoHandler::class.java)
    private const val STATE_QUERY_WAIT_TYPE = "neo_query_wait_type"
    private const val STATE_QUERY_WAIT_CONTENT = "neo_query_wait_content"
    private const val STATE_AIRDROP_WAIT_ADDRESS = "neo_airdrop_wait_address"

    private val infoCommandHandler = fun(core: IcarusCore, message: Message): String? {
        // just reply some static info
        core.replyToMessage(
            message.chatId, message.messageId,
            "Useful infos:\n" +
                    "Cat Token contract address: 0x${ContractHelper.catToken.scriptHash}\n" +
                    "WCA contract address: 0x${ContractHelper.wcaContract.scriptHash}\n" +
                    "Bot wallet address: ${ContractHelper.account.address}\n" +
                    "If you found this bot helpful, please considering donate some GAS to this address. Thanks!"
        )
        return IcarusCore.STATE_IDLE
    }

    // <chatId, userId> -> <phase,type>
    private val queryPhaseMemory = Memory<Pair<Long, Long>, Pair<String, String?>>()
    private val queryCommandHandler = fun(core: IcarusCore, message: Message): String? {
        val phase = queryPhaseMemory[Pair(message.chatId, message.from.id)] ?: Pair(null, null)
        if (phase.first == null) {
            // ask what to query
            core.replyToMessage(
                message.chatId, message.messageId,
                "What to query: cat, gas or wca?"
            )
            queryPhaseMemory[Pair(message.chatId, message.from.id)] = Pair(STATE_QUERY_WAIT_TYPE, null)
            return STATE_QUERY_WAIT_TYPE
        } else if (phase.first == STATE_QUERY_WAIT_TYPE) {
            // get type, ask content
            val content = when {
                message.text.lowercase().contains("gas") -> {
                    core.replyToMessage(message.chatId, message.messageId, "Give me a valid account address.")
                    "gas"
                }
                message.text.lowercase().contains("cat") -> {
                    core.replyToMessage(message.chatId, message.messageId, "Give me a valid account address.")
                    "cat"
                }
                message.text.lowercase().contains("wca") -> {
                    core.replyToMessage(message.chatId, message.messageId, "Give me a WCA identifier.")
                    "wca"
                }
                else -> {
                    core.replyToMessage(message.chatId, message.messageId, "Invalid reply. Query is canceled.")
                    null
                }
            }
            return if (content == null) {
                queryPhaseMemory.remove(Pair(message.chatId, message.from.id))
                IcarusCore.STATE_IDLE
            } else {
                queryPhaseMemory[Pair(message.chatId, message.from.id)] = Pair(STATE_QUERY_WAIT_CONTENT, content)
                STATE_QUERY_WAIT_CONTENT
            }
        } else if (phase.first == STATE_QUERY_WAIT_CONTENT && phase.second != null) {
            // get content, do the query
            val content = message.text
            when (phase.second) {
                "gas" -> {
                    try {
                        core.replyToMessage(
                            message.chatId, message.messageId,
                            "Gas balance of $content: ${ContractHelper.getGasBalance(content).toPlainString()}"
                        )
                    } catch (e: Exception) {
                        core.replyToMessage(
                            message.chatId, message.messageId,
                            "Cannot do the query: ${e.message}"
                        )
                    }
                }
                "cat" -> {
                    try {
                        core.replyToMessage(
                            message.chatId, message.messageId,
                            "Cat balance of $content: ${ContractHelper.getCatBalance(content).toPlainString()}"
                        )
                    } catch (e: Exception) {
                        core.replyToMessage(
                            message.chatId, message.messageId,
                            "Cannot do the query: ${e.message}"
                        )
                    }
                }
                "wca" -> {
                    // TODO make this async, otherwise it will block the bot
                    core.replyToMessage(message.chatId, message.messageId, "Querying WCA details. This may take 15s.")
                    try {
                        val result = ContractHelper.queryWCAJson(content, ContractHelper.wallet)
                        core.replyToMessage(
                            message.chatId, message.messageId,
                            "Details of WCA contract $content: \n\n" +
                                    "${result.third}\n\n" +
                                    "Query tx: 0x${result.first}\n\n" +
                                    "Gas usage: ${result.second}"
                        )
                    } catch (e: Exception) {
                        core.replyToMessage(
                            message.chatId, message.messageId,
                            "Cannot do the query: ${e.localizedMessage}"
                        )
                        e.printStackTrace()
                    }
                }
                else -> {
                    core.replyToMessage(message.chatId, message.messageId, "Invalid reply. Query is canceled.")
                }
            }
            queryPhaseMemory.remove(Pair(message.chatId, message.from.id))
            return IcarusCore.STATE_IDLE
        } else {
            // something goes wrong, cannot do the query
            core.replyToMessage(message.chatId, message.messageId, "Something goes wrong. Cannot do the query.")
            queryPhaseMemory.remove(Pair(message.chatId, message.from.id))
            return IcarusCore.STATE_IDLE
        }
    }

    private val atomicLong = AtomicLong(0)

    // <chatId, userId> -> <phase>
    private val airDropPhaseMemory = Memory<Pair<Long, Long>, String>()
    private val airDropCommandHandler = fun(core: IcarusCore, message: Message): String? {
        when (airDropPhaseMemory[Pair(message.chatId, message.from.id)]) {
            null -> {
                // ask address
                core.replyToMessage(
                    message.chatId, message.messageId,
                    "Send me your account address"
                )
                airDropPhaseMemory[Pair(message.chatId, message.from.id)] = STATE_AIRDROP_WAIT_ADDRESS
                return STATE_AIRDROP_WAIT_ADDRESS
            }
            STATE_AIRDROP_WAIT_ADDRESS -> {
                // do the airdrop
                try {
                    ContractHelper.transferToken(
                        ContractHelper.catToken,
                        ContractHelper.wallet,
                        Account.fromAddress(message.text),
                        500_00
                    )
                    core.replyToMessage(
                        message.chatId, message.messageId,
                        "Send 500.00 CAT to your account."
                    )
                } catch (e: Exception) {
                    core.replyToMessage(
                        message.chatId, message.messageId,
                        "Cannot do the transfer: ${e.message}"
                    )
                }
                airDropPhaseMemory.remove(Pair(message.chatId, message.from.id))
                return IcarusCore.STATE_IDLE
            }
            else -> {
                // something goes wrong, cannot do the query
                core.replyToMessage(message.chatId, message.messageId, "Something goes wrong. Cannot do the airdrop.")
                airDropPhaseMemory.remove(Pair(message.chatId, message.from.id))
                return IcarusCore.STATE_IDLE
            }
        }
    }

    private val rootCommandHandler = fun(core: IcarusCore, message: Message): String? {
        val cli = message.text.split(" ")
        return if (cli.size < 2) {
            core.replyToMessage(
                message.chatId, message.messageId,
                "Sub commands:\n" +
                        "neo info - give some info about contract and bot wallet\n" +
                        "neo query - query CAT balance, GAS balance or WCA details\n" +
                        "neo airdrop - get some cat token on testnet\n" +
                        "Please DO NOT send your wallet private key or WIF to anyone"
            )
            IcarusCore.STATE_IDLE
        } else {
            when (cli[1].lowercase()) {
                "info" -> infoCommandHandler(core, message)
                "query" -> queryCommandHandler(core, message)
                "airdrop" -> airDropCommandHandler(core, message)
                else -> {
                    core.replyToMessage(
                        message.chatId, message.messageId,
                        "Unknown command, use /neo to see what sub commands are available"
                    )
                    IcarusCore.STATE_IDLE
                }
            }
        }
    }

    override val stateHandlers: List<Pair<String, (IcarusCore, Message) -> String?>>
        get() = listOf(
            STATE_AIRDROP_WAIT_ADDRESS to airDropCommandHandler,
            STATE_QUERY_WAIT_TYPE to queryCommandHandler,
            STATE_QUERY_WAIT_CONTENT to queryCommandHandler,

        )
    override val commandHandlers: List<Pair<BotCommand, (IcarusCore, Message) -> String?>>
        get() = listOf(
            BotCommand.NEO to rootCommandHandler
        )
}