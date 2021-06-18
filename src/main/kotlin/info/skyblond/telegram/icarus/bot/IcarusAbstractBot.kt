package info.skyblond.telegram.icarus.bot

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.Serializable
import java.util.*
import java.util.concurrent.CompletableFuture

interface IcarusAbstractBot {

    fun <T : Serializable, Method : BotApiMethod<T>> safeExecute(method: Method): Optional<T>
}