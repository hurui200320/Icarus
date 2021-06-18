package info.skyblond.telegram.icarus.bot

import org.telegram.telegrambots.meta.api.objects.Message

interface CommandHandler {
    val stateHandlers: List<Pair<String, (IcarusCore, Message) -> String?>>
    val commandHandlers: List<Pair<BotCommand, (IcarusCore, Message) -> String?>>

    fun register(core: IcarusCore) {
        stateHandlers.forEach {
            core.registerStateHandler(it.first, it.second)
        }
        commandHandlers.forEach {
            core.registerCommandHandler(it.first, it.second)
        }
    }
}