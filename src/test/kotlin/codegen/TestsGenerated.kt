package codegen

import base.runTest
import base.afterTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

//DO NOT MODIFY THIS FILE MANUALLY!!!

class TestsGenerated {
    @AfterEach
    fun after() {
        afterTest()
    }
    @Test
    @Order(1)
    fun cats() {
        runTest("src/test/kotlin/testData/cats")
    }
    
    @Test
    @Order(2)
    fun defenceFromGulag() {
        runTest("src/test/kotlin/testData/defenceFromGulag")
    }
    
    @Test
    @Order(3)
    fun gulag() {
        runTest("src/test/kotlin/testData/gulag")
    }
    
    @Test
    @Order(4)
    fun help() {
        runTest("src/test/kotlin/testData/help")
    }
    
    @Test
    @Order(5)
    fun mute() {
        runTest("src/test/kotlin/testData/mute")
    }
    
    @Test
    @Order(6)
    fun ratingSystem() {
        runTest("src/test/kotlin/testData/ratingSystem")
    }
    
    @Test
    @Order(7)
    fun reward() {
        runTest("src/test/kotlin/testData/reward")
    }
    
    @Test
    @Order(8)
    fun virtualcommands() {
        runTest("src/test/kotlin/testData/virtualcommands")
    }
    
    @Test
    @Order(9)
    fun virtualTargets() {
        runTest("src/test/kotlin/testData/virtualTargets")
    }
    
}
