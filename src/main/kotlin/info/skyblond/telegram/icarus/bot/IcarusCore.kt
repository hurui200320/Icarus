package info.skyblond.telegram.icarus.bot

import info.skyblond.telegram.icarus.utils.Memory
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Message


class IcarusCore(
    private val bot: IcarusAbstractBot
) {
    private val logger = LoggerFactory.getLogger(IcarusCore::class.java)

    companion object {
        val STATE_IDLE = null

        private fun parseCommand(text: String): BotCommand? {
            val cmd = text.split(" ")[0].removePrefix("/")
            return BotCommand.values().find { it.text == cmd }
        }
    }

    private val helpCommand = fun(_: IcarusCore, msg: Message): String? {
        replyToMessage(
            msg.chatId, msg.messageId,
            BotCommand.values()
                .sortedBy { it.text }
                .joinToString("\n") {
                    it.text + " - " + it.description
                }
        )
        return STATE_IDLE
    }

    private val commandHandlerMap = mutableMapOf(
        BotCommand.HELP to helpCommand
    )

    fun registerCommandHandler(command: BotCommand, handler: (IcarusCore, Message) -> String?) {
        require(!commandHandlerMap.contains(command)) { "Duplicate command '$command'" }
        commandHandlerMap[command] = handler
    }

    private val identifyCommand = fun(core: IcarusCore, msg: Message): String? {
        if (msg.hasText()) {
            val handler = commandHandlerMap[parseCommand(msg.text)]
            return if (handler != null) {
                handler.invoke(core, msg)
            } else {
                replyToMessage(
                    msg.chatId, msg.messageId,
                    "Command not recognized, use /help to list all available commands"
                )
                STATE_IDLE
            }
        } else {
            replyToMessage(
                msg.chatId, msg.messageId,
                "Text command please."
            )
            return STATE_IDLE
        }
    }

    private val stateHandlerMap = mutableMapOf<String?, (IcarusCore, Message) -> String?>(
        STATE_IDLE to identifyCommand
    )

    fun registerStateHandler(state: String, handler: (IcarusCore, Message) -> String?) {
        require(!stateHandlerMap.contains(state)) { "Duplicate state '$state'" }
        stateHandlerMap[state] = handler
    }

    fun queryStateHandler(state: String?): (IcarusCore, Message) -> String? {
        require(stateHandlerMap.containsKey(state)) { "Invalid state '$state'" }
        return stateHandlerMap[state]!!
    }

    private val sessionStateMap = Memory<Long, String>()
    fun querySessionState(chatId: Long): String? {
        return sessionStateMap[chatId]
    }

    fun setSessionState(chatId: Long, state: String?) {
        if (state == null) {
            sessionStateMap.remove(chatId)
        } else {
            sessionStateMap[chatId] = state
        }
    }

    /**
     * Delete message.
     *
     * @return true if the message is deleted, false if something goes wrong.
     * */
    fun deleteMessage(chatId: Long, messageId: Int): Boolean {
        val deleteMessage = DeleteMessage()
        deleteMessage.chatId = chatId.toString()
        deleteMessage.messageId = messageId

        return bot.safeExecute(deleteMessage).orElse(false)
    }

    fun replyToMessage(chatId: Long, messageId: Int, text: String) {
        val message = SendMessage()
        message.chatId = chatId.toString()
        message.replyToMessageId = messageId
        message.text = text

        bot.safeExecute(message)
    }

    /**
     * Send message.
     *
     * @return true if the message is sent, false if something goes wrong.
     * */
    fun sendMessage(chatId: Long, text: String): Int? {
        val message = SendMessage()
        message.chatId = chatId.toString()
        message.text = text

        val optional = bot.safeExecute(message)

        return if (optional.isPresent)
            optional.get().messageId
        else
            null
    }
}