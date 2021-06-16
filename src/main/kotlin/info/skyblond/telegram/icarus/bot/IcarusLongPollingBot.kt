package info.skyblond.telegram.icarus.bot

import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException


class IcarusLongPollingBot(
    private val botUsername: String,
    private val botToken: String,
    botOptions: DefaultBotOptions = DefaultBotOptions()
) : TelegramLongPollingBot(botOptions) {

    private val logger = LoggerFactory.getLogger(IcarusLongPollingBot::class.java)

    override fun getBotToken(): String = botToken

    override fun getBotUsername(): String = botUsername

    override fun onUpdateReceived(update: Update) {
        // We check if the update has a message and the message has text
        if (update.hasMessage()) {
            val message = SendMessage() // Create a SendMessage object with mandatory fields
            message.chatId = update.message.chatId.toString()
            message.text = "Hello, world!"
            try {
                execute(message)
            } catch (e: TelegramApiException) {
                logger.error("Error when handling updates: ", e)
            }
        }
    }
}