package api

interface PlatformApiInterface {
    fun send(text: String, chatId: Long, attachments: List<String> = listOf())

    fun getUserNameById(id: Long): String?

    fun getUserIdByName(username: String): Long?

    fun sendCat(id: Long)

    fun kickUserFromChat(chatId: Long, userId: Long)
}
