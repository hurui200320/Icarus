package info.skyblond.telegram.icarus

import info.skyblond.telegram.icarus.bot.IcarusLongPollingBot
import info.skyblond.telegram.icarus.utils.ConfigHelper
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession


fun main() {
    val logger = LoggerFactory.getLogger("Application")

    val botOptions = DefaultBotOptions()
    botOptions.proxyHost = ConfigHelper.config.proxy.address
    botOptions.proxyPort = ConfigHelper.config.proxy.port
    botOptions.proxyType = ConfigHelper.config.proxy.parseProxyType()

    val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
    botsApi.registerBot(
        IcarusLongPollingBot(
            ConfigHelper.config.bot.username,
            ConfigHelper.config.bot.token,
            botOptions
        )
    )

    logger.info("Telegram bot registered")
}