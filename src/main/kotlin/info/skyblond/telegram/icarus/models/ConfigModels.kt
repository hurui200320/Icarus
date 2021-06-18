package info.skyblond.telegram.icarus.models

import com.fasterxml.jackson.annotation.JsonIgnore
import io.neow3j.wallet.Account
import io.neow3j.wallet.Wallet
import org.apache.commons.codec.digest.DigestUtils
import org.telegram.telegrambots.bots.DefaultBotOptions

data class ConfigPojo(
    val proxy: ProxyConfigPojo = ProxyConfigPojo(),
    val bot: BotConfigPojo = BotConfigPojo(),
    val neo: NeoConfig = NeoConfig()
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
    val token: String = "1234567890:FILL_YOUR_BOT_TOKEN_HERE",
    val authChallenge: List<AuthChallenge> = listOf(AuthChallenge())
)

data class AuthChallenge(
    val challenge: String = "challenge",
    val md5Answer: String = DigestUtils.md5Hex("Answer").uppercase()
) {
    fun compareAnswer(answer: String): Boolean {
        return md5Answer.uppercase() == DigestUtils.md5Hex(answer).uppercase()
    }
}

data class NeoConfig(
    val rpcServer: String = "http://127.0.0.1:50012",
    val accountWIF: String = Account.create().ecKeyPair.exportAsWIF()
) {
    @JsonIgnore
    val account: Account = Account.fromWIF(accountWIF)

    @JsonIgnore
    val wallet: Wallet = Wallet.withAccounts(account)
}
