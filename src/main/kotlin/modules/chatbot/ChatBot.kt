package modules.chatbot

import api.VkPlatform
import com.google.gson.Gson
import com.google.gson.JsonArray
import group_id
import io.github.classgraph.ClassGraph
import log
import mainChatPeerId
import modules.chatbot.chatModules.Gulag
import modules.chatbot.listeners.CommandListener
import modules.chatbot.listeners.MessageListener
import modules.chatbot.chatModules.RatingSystem
import testChatId
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

data class JsonVK(val response: Response?, val error: Error?) {
    data class Response(
            val key: String,
            val server: String,
            val ts: String
    )

    data class Error(
            val error_code: Int,
            val error_msg: String,
            val request_params: JsonArray
    )
}

data class JsonAnswer(
        val ts: String,
        val updates: List<UpdatePart>,
        val failed: Int?
) {
    data class UpdatePart(
            val type: String,
            val `object`: MessageNewObj,
            val group_id: Int,
            val event_id: String
    )

}

data class MessageNewObj(
        val date: Int,
        val from_id: Int,
        val id: Int,
        val out: Int,
        val peer_id: Int,
        val text: String,
        val conversation_message_id: Int,
        val fwd_messages: List<Any>,
        val important: Boolean,
        val random_id: Int,
        val attachments: List<Any>,
        val is_hidden: Boolean,
        val action: Action?
) {
    data class Action(
        val type: String,
        val member_id: Int,
        val text: String,
        val email: String,
        val photo: Any
    )
}

object ChatBot: Thread() {
    private lateinit var server: String
    private lateinit var key: String
    private lateinit var ts: String
    private lateinit var commandListeners: List<CommandListener>
    private lateinit var messageListeners: List<MessageListener>
    private var isInit = false
    private val vk = VkPlatform()


    private fun initLongPoll() {
        val response = vk.post(
            "groups.getLongPollServer",
            mutableMapOf(
                "group_id" to group_id
            )
        )

        val jsonVK = Gson().fromJson(response, JsonVK::class.java)

        if (jsonVK.error != null || jsonVK.response == null) {
            log.severe("vk long poll connection error. ${jsonVK.error}\n")
            return
        }

        val responseBody = jsonVK.response

        server = responseBody.server
        key = responseBody.key
        ts = responseBody.ts

        ClassGraph().enableAllInfo().whitelistPackages("modules.chatbot.chatModules")
            .scan().use { scanResult ->
                val classes = scanResult.allClasses
                commandListeners = classes.flatMap {
                    it.methodAndConstructorInfo.filter { method ->
                        method.hasAnnotation(OnCommand::class.java.name)
                    }.map { method ->
                        val loadedMethod = method.loadClassAndGetMethod()
                        val annotation = loadedMethod.getAnnotation(OnCommand::class.java)
                        CommandListener(
                            annotation.commands,
                            annotation.description,
                            it.loadClass().getConstructor().newInstance(),
                            loadedMethod,
                            annotation.permissions,
                            annotation.cost
                        )
                    }
                }

                messageListeners = classes.flatMap {
                    it.methodAndConstructorInfo.filter { method ->
                        method.hasAnnotation(OnMessage::class.java.name)
                    }.map { method ->
                        val loadedMethod = method.loadClassAndGetMethod()
                        MessageListener(
                            it.loadClass().getConstructor().newInstance(),
                            loadedMethod
                        )
                    }
                }

                isInit = true
            }
    }

    private fun longPollRequest(): HttpResponse<String?> {
        val wait = 25
        val request = HttpRequest.newBuilder()
                .uri(URI.create(server))
                .POST(HttpRequest.BodyPublishers.ofString("act=a_check&key=$key&ts=${ts}&wait=$wait"))
                .build()
        val client = HttpClient.newHttpClient()
        try {
            val response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            )
            log.info("response: ${response.body()}")
            return response
        } catch (e: IOException) {
            return longPollRequest()
        }
    }

    override fun run() {
        log.info("Initialising ChatBot...")
        initLongPoll()
        while (!isInit) {
            sleep(10 * 1000) // 10 seconds
            initLongPoll()
        }

        while (true) {
            val response = longPollRequest().body()
            val jsonAnswer = Gson().fromJson(response, JsonAnswer::class.java)
            if (jsonAnswer.failed != null) {
                log.severe("long poll error: $response")
                initLongPoll()
                continue
            }
            ts = jsonAnswer.ts
            for (update in jsonAnswer.updates) {
                if (update.type == "message_new" && (!testChatId || update.`object`.peer_id != mainChatPeerId)) {
                    if (update.`object`.text.startsWith("/")) {
                        log.info("Message which starts with \"/\" found")
                        commandProcessor(update.`object`)
                    }
                    messageProcessor(update.`object`)
                }

                if (update.type == "message_new" && (update.`object`.action != null)) {
                    if (update.`object`.action.type == "chat_invite_user"
                        || update.`object`.action.type == "chat_invite_user_by_link") {
                        log.info("Somebody was added to chat")
                        val targetId = update.`object`.action.member_id
                        val peerId = update.`object`.peer_id
                        if (Gulag.gulagKickTime.containsKey(targetId to peerId)) {
                            val dif = Gulag.gulagKickTime[targetId to peerId]!! - System.currentTimeMillis()
                            if (dif > 0) {
                                val fullSec = dif / 1000
                                val hours = fullSec / (60 * 60)
                                val minutes = (fullSec % 3600) / 60
                                val seconds = fullSec % 60
                                val message = "Еще наказан ${String.format("%02d:%02d:%02d", hours, minutes, seconds)}"

                                vk.send(message, peerId)
                                sleep(400)
                                vk.kickUserFromChat(targetId, peerId)
                            } else Gulag.gulagKickTime.remove(targetId to peerId)
                        } else vk.send("Приветствуем ${vk.getUserNameById(targetId)}", peerId)
                    }
                }
            }
        }
    }

    private fun commandProcessor(message: MessageNewObj) {
        val commandName = message.text.split(" ")[0].removePrefix("/")
        for (command in commandListeners) {
            if (command.commands.any{ it == commandName }) {
                val userPermission = getPermission(message)
                val chatId = message.peer_id
                val userId = message.from_id
                val userCanUseCommand = userPermission.ordinal >= command.permission.ordinal
                val userScoreEnough = command.cost == 0 ||
                        RatingSystem.buyCommand(chatId, userId, command.cost)

                if (userCanUseCommand && userScoreEnough) {
                    try {
                        command.call.invoke(command.baseClass, message)
                    } catch (e: Exception) {
                        log.severe("message: ${message.text}")
                        e.printStackTrace()
                    }
                } else if (!userCanUseCommand) {
                    val domain = vk.getUserNameById(message.from_id)
                    vk.send("""
                        @${domain}, у Вас недостаточно прав для использования команды /${commandName}
                    """.trimIndent(), message.peer_id
                    )
                } else if (!userScoreEnough) {
                    val domain = vk.getUserNameById(message.from_id)
                    vk.send("""
                        @${domain}, у Вас недостаточно средств для использования команды /${commandName}
                    """.trimIndent(), message.peer_id
                    )
                }
            }
        }
    }

    fun getPermission(message: MessageNewObj): CommandPermission {
        val profiles = vk.getChatMembers(message.peer_id, listOf()) ?: throw IllegalStateException()
        //Find Admin
        for (profile in profiles) {
            if (profile.is_admin && profile.member_id == message.from_id) {
                return CommandPermission.ADMIN_ONLY
            }
        }
        //Если не высшие права(админ), то что-то из рейтинговой системы
        return CommandPermission.ALL
    }

    fun getCommands() = commandListeners

    private fun messageProcessor(message: MessageNewObj) {
        for (messageListener in messageListeners) {
            try {
                messageListener.call.invoke(messageListener.baseClass, message)
            } catch (e: Exception) {
                log.severe("message: ${message.text}")
                e.printStackTrace()
            }
        }
    }
}