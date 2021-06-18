package info.skyblond.telegram.icarus.bot

enum class BotCommand(
    val text: String,
    val description: String
) {
    HELP("help", "list available commands"),
    AUTH("auth", "start the protocol to make you an admin"),
    TIME("time", "get the UTC time on server")
}