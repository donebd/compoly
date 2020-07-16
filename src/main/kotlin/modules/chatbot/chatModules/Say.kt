package modules.chatbot.chatModules

import api.VkPlatform
import modules.Active
import modules.chatbot.CommandPermission
import modules.chatbot.MessageNewObj
import modules.chatbot.OnCommand
import modules.chatbot.chatBotEvents.LongPollNewMessageEvent

@Active
class Say {
    private val vk = VkPlatform()
    private val regex = Regex("(/say) (\\d+) ([\\w\\W\\d]*)")

    @OnCommand(["say"],
        "отправить сообщение в чат с ID: /say ID СООБЩЕНИЕ",
        CommandPermission.ADMIN_ONLY
    )
    fun say(event: LongPollNewMessageEvent) {
        val text = event.text
        val regexed = regex.find(text)
        val receiverChatId = regexed?.groupValues?.get(2)?.let { Integer.parseInt(it) }
        val messageText = regexed?.groupValues?.get(3)

        if (receiverChatId == null) {
            vk.send("Неверный параметр ID", event.chatId)
            return
        }
        if (messageText == null) {
            vk.send("Неверный параметр СООБЩЕНИЕ", event.chatId)
            return
        }

        vk.send(messageText, receiverChatId)
    }
}