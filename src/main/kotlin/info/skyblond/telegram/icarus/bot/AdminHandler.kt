package info.skyblond.telegram.icarus.bot

import info.skyblond.telegram.icarus.utils.ConfigHelper
import info.skyblond.telegram.icarus.utils.Memory
import org.telegram.telegrambots.meta.api.objects.Message
import java.time.ZoneOffset
import java.time.ZonedDateTime

object AdminHandler : CommandHandler {
    private const val STATE_AUTH = "auth"
    private const val USER_ID_KEY_ADMIN = "admin"

    // KEY -> List<UserId>
    private val userIdMemory = Memory<String, MutableSet<Long>>()

    // <chatId, userId> -> <last message id, phase>
    private val authPhaseMemory = Memory<Pair<Long, Long>, Pair<Int?, Int>>()

    private fun addAdmin(userId: Long) {
        if (userIdMemory[USER_ID_KEY_ADMIN] == null) {
            userIdMemory[USER_ID_KEY_ADMIN] = mutableSetOf()
        }
        userIdMemory[USER_ID_KEY_ADMIN]!!.add(userId)
    }

    private fun getAdmins(): Set<Long> {
        if (userIdMemory[USER_ID_KEY_ADMIN] == null) {
            userIdMemory[USER_ID_KEY_ADMIN] = mutableSetOf()
        }
        return userIdMemory[USER_ID_KEY_ADMIN]!!
    }

    private val authCommand = fun(core: IcarusCore, msg: Message): String? {
        val phase = authPhaseMemory[Pair(msg.chatId, msg.from.id)] ?: Pair(null, -1)
        // delete received message
        core.deleteMessage(msg.chatId, msg.messageId)
        if (phase.first != null)
        // delete last sent message
            core.deleteMessage(msg.chatId, phase.first!!)

        val challenges = ConfigHelper.config.bot.authChallenge
        // check this phase
        if (phase.second == -1 || challenges[phase.second].compareAnswer(msg.text)) {
            return if (phase.second == challenges.size - 1) {
                // success
                addAdmin(msg.from.id)
                authPhaseMemory.remove(Pair(msg.chatId, msg.from.id))
                core.sendMessage(msg.chatId, "@${msg.from.userName} You have granted admin permission")
                IcarusCore.STATE_IDLE
            } else {
                // send challenge
                val messageId =
                    core.sendMessage(msg.chatId, "@${msg.from.userName} ${challenges[phase.second + 1].challenge}")
                // save next phase
                authPhaseMemory[Pair(msg.chatId, msg.from.id)] = Pair(messageId, phase.second + 1)
                // wait for next auth
                STATE_AUTH
            }
        } else {
            // wrong
            authPhaseMemory.remove(Pair(msg.chatId, msg.from.id))
            core.sendMessage(msg.chatId, "@${msg.from.userName} Authentication failed")
            return IcarusCore.STATE_IDLE
        }
    }

    private val timeCommand = fun(core: IcarusCore, msg: Message): String? {
        core.replyToMessage(
            msg.chatId, msg.messageId,
            ZonedDateTime.now(ZoneOffset.UTC).toString()
        )
        return IcarusCore.STATE_IDLE
    }

    private val helloCommand = fun(core: IcarusCore, msg: Message): String? {
        val message = if (getAdmins().contains(msg.from.id))
            "@${msg.from.userName} have a nice day!"
        else
            "Hi, nice to meet you!"
        core.replyToMessage(msg.chatId, msg.messageId, message)
        return IcarusCore.STATE_IDLE
    }

    override val stateHandlers: List<Pair<String, (IcarusCore, Message) -> String?>>
        get() = listOf(
            STATE_AUTH to authCommand
        )
    override val commandHandlers: List<Pair<BotCommand, (IcarusCore, Message) -> String?>>
        get() = listOf(
            BotCommand.TIME to timeCommand,
            BotCommand.AUTH to authCommand,
            BotCommand.HELLO to helloCommand
        )
}