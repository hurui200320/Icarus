package info.skyblond.telegram.icarus.bot

enum class BotCommand(
    val text: String,
    val description: String
) {
    // default implementation
    HELP("help", "list available commands"),

    // Admin handler
    AUTH("auth", "start the protocol to make you an admin"),
    TIME("time", "get the UTC time on server"),
    HELLO("hello", "a gentle greeting")
}