package org.checkmateplus

import org.deeplearning4j.ui.api.UIServer
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val uiServer = UIServer.getInstance()
    val statsStorage = InMemoryStatsStorage()
    uiServer.attach(statsStorage)
}