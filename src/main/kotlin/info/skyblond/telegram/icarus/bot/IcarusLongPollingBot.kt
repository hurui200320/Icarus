package info.skyblond.telegram.icarus.bot

import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.Serializable
import java.util.*


class IcarusLongPollingBot(
    private val botUsername: String,
    private val botToken: String,
    botOptions: DefaultBotOptions = DefaultBotOptions()
) : TelegramLongPollingBot(botOptions), IcarusAbstractBot {

    private val logger = LoggerFactory.getLogger(IcarusLongPollingBot::class.java)
    private val core = IcarusCore(this)

    init {
        AdminHandler.register(core)
    }

    override fun getBotToken(): String = botToken

    override fun getBotUsername(): String = botUsername

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage()) {
            try {
                // query session state
                val sessionState = core.querySessionState(update.message.chatId)
                val newState = core.queryStateHandler(sessionState).invoke(core, update.message)
                core.setSessionState(update.message.chatId, newState)
            } catch (e: Throwable) {
                logger.error("Error when handling message: ", e)
                val message = SendMessage()
                message.chatId = update.message.chatId.toString()
                message.replyToMessageId = update.message.messageId
                message.text = "Error occurred when handling this message"
                safeExecute(message)
            }
        }
    }

    override fun <T : Serializable, Method : BotApiMethod<T>> safeExecute(method: Method): Optional<T> {
        return try {
            // not if this return null
            Optional.ofNullable(execute(method))
        } catch (e: Throwable) {
            logger.error("Error when deleting message: ", e)
            Optional.empty()
        }
    }
}