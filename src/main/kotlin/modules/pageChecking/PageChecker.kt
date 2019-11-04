package modules.pageChecking

import api.Vk
import chatIds
import log
import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Paths
import modules.Module

class PageChecker : Module {
    override val callingType = 1
    override val millis = 10 * 1000L
    override val name = "Проверка обновления страницы"
    override var lastCalling = System.currentTimeMillis() + 3 * 60 * 60 * 1000L

    // Можно добавить сюда другие сайты
    private val pages = listOf(
        Link("Практика по дискретке [Сабонис]", "http://sergei-sabonis.ru/Student/20192020/dm2019.htm"),
        Link("Практика по вышмату [Давыдов]",
            "https://docs.google.com/spreadsheets/d/1r0US74YCVioZE0jf9-clpDmTQfO8q7Op/export?format=xlsx&gid=693072555",
            "https://docs.google.com/spreadsheets/d/1r0US74YCVioZE0jf9-clpDmTQfO8q7Op")
    )

    private fun getPath(page: String): String {
        val filePath = "/data/savedPages/" + page.replace(Regex("""[\\?|"/.:<>*]"""), "_") + ".txt"
        return Paths.get("").toAbsolutePath().toString() + filePath
    }

    private fun sendGet(address: String): String? {
        try {
            val url = URL(address)
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET
                log.info("Sent 'GET' request to $url, [$responseCode]")
                return inputStream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            log.warning("Can not get $address")
            return null
        }
    }

    private fun isUpdated(page: String): Boolean? {
        val path = getPath(page)
        val newPage = sendGet(page) ?: return null
        return try {
            if (newPage != File(path).readText()) {
                File(path).bufferedWriter().use { it.write(newPage) }
                log.info("Page $path was updated")
                true
            } else {
                false
            }
        } catch (e: FileNotFoundException) {
            log.warning("File $path not found")
            null
        }
    }

    fun createFiles() { //Следует запускать после добавления новых страниц
        for (page in pages) {
            val path = getPath(page.trueUrl)
            val text = sendGet(page.trueUrl)
            if (text != null) {
                File(path).writeText(text)
                log.info("File $path was created")
            }
        }
    }

    override fun call() {
        for (page in pages) {
            if (isUpdated(page.trueUrl) == true) {
                log.info("Page ${page.showingUrl} was updated")
                Vk().send("Обновление страницы ${page.showingUrl}", chatIds)
            }
        }
    }
}

data class Link(val name: String, val trueUrl: String, val showingUrl: String = trueUrl)