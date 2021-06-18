package info.skyblond.telegram.icarus.bot

import info.skyblond.telegram.icarus.utils.ConfigHelper
import info.skyblond.telegram.icarus.utils.Memory
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Message
import java.time.ZoneOffset
import java.time.ZonedDateTime

class IcarusCore(
    private val bot: IcarusAbstractBot
) {
    private val logger = LoggerFactory.getLogger(IcarusCore::class.java)
    private val userIdMemory = Memory<String, MutableSet<Long>>()

    // <chatId, userId> -> <last message id, phase>
    private val authPhaseMemory = Memory<Pair<Long, Long>, Pair<Int?, Int>>()

    companion object {
        private const val USER_ID_KEY_USER = "USER"
        private const val USER_ID_KEY_ADMIN = "ADMIN"

        private val STATE_IDLE = null
        private const val STATE_AUTH = "auth"


        private fun parseCommand(text: String): BotCommand? {
            val cmd = text.split(" ")[0].removePrefix("/")
            return BotCommand.values().find { it.text == cmd }
        }
    }

    private fun addAdmin(userId: Long) {
        if (userIdMemory[USER_ID_KEY_ADMIN] == null) {
            userIdMemory[USER_ID_KEY_ADMIN] = mutableSetOf()
        }
        userIdMemory[USER_ID_KEY_ADMIN]!!.add(userId)
    }

    private val authCommand = fun(msg: Message): String? {
        val phase = authPhaseMemory[Pair(msg.chatId, msg.from.id)] ?: Pair(null, -1)
        // delete received message
        deleteMessage(msg.chatId, msg.messageId)
        if (phase.first != null)
            // delete last sent message
            deleteMessage(msg.chatId, phase.first!!)

        val challenges = ConfigHelper.config.bot.authChallenge
        // check this phase
        if (phase.second == -1 || challenges[phase.second].compareAnswer(msg.text)) {
            return if (phase.second == challenges.size - 1) {
                // success
                addAdmin(msg.from.id)
                authPhaseMemory.remove(Pair(msg.chatId, msg.from.id))
                sendMessage(msg.chatId, "@${msg.from.userName} You have granted admin permission")
                STATE_IDLE
            } else {
                // send challenge
                val messageId = sendMessage(msg.chatId, "@${msg.from.userName} ${challenges[phase.second + 1].challenge}")
                // save next phase
                authPhaseMemory[Pair(msg.chatId, msg.from.id)] = Pair(messageId, phase.second + 1)
                // wait for next auth
                STATE_AUTH
            }
        } else {
            // wrong
            authPhaseMemory.remove(Pair(msg.chatId, msg.from.id))
            sendMessage(msg.chatId, "@${msg.from.userName} Authentication failed")
            return STATE_IDLE
        }
    }

    private val timeCommand = fun(msg: Message): String? {
        replyToMessage(
            msg.chatId, msg.messageId,
            ZonedDateTime.now(ZoneOffset.UTC).toString()
        )
        return STATE_IDLE
    }

    private val helpCommand = fun(msg: Message): String? {
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

    private val identifyCommand = fun(msg: Message): String? {
        if (msg.hasText()) {
            return when (parseCommand(msg.text)) {
                BotCommand.AUTH -> authCommand(msg)
                BotCommand.TIME -> timeCommand(msg)
                BotCommand.HELP -> helpCommand(msg)
                null -> {
                    replyToMessage(
                        msg.chatId, msg.messageId,
                        "Command not recognized, you can use /help to list all available commands"
                    )
                    STATE_IDLE
                }
            }
        }
        return STATE_IDLE
    }

    val stateHandlerMap = mapOf(
        STATE_IDLE to identifyCommand,
        STATE_AUTH to authCommand
    )

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
    private fun deleteMessage(chatId: Long, messageId: Int): Boolean {
        val deleteMessage = DeleteMessage()
        deleteMessage.chatId = chatId.toString()
        deleteMessage.messageId = messageId

        return bot.safeExecute(deleteMessage).orElse(false)
    }

    private fun replyToMessage(chatId: Long, messageId: Int, text: String) {
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
    private fun sendMessage(chatId: Long, text: String): Int? {
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