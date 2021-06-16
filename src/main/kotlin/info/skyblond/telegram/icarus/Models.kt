package info.skyblond.telegram.icarus

import org.telegram.telegrambots.bots.DefaultBotOptions

data class ConfigPojo(
    val proxy: ProxyConfigPojo = ProxyConfigPojo(),
    val bot: BotConfigPojo = BotConfigPojo()
)

data class ProxyConfigPojo(
    val type: String = "NONE",
    val address: String = "127.0.0.1",
    val port: Int = 1080
) {
    fun parseProxyType(): DefaultBotOptions.ProxyType {
        return when (type.uppercase()) {
            "HTTP" -> DefaultBotOptions.ProxyType.HTTP
            "SOCKS4" -> DefaultBotOptions.ProxyType.SOCKS4
            "SOCKS5" -> DefaultBotOptions.ProxyType.SOCKS5
            else -> DefaultBotOptions.ProxyType.NO_PROXY
        }
    }
}

data class BotConfigPojo(
    val username: String = "Icarus",
    val token: String = "1234567890:FILL_YOUR_BOT_TOKEN_HERE"
)