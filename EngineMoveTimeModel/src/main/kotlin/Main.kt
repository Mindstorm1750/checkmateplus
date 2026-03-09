package org.checkmateplus

import io.jhdf.HdfFile
import org.jetbrains.kotlinx.dl.api.core.Functional
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.api.inference.keras.loadWeights
import org.jetbrains.kotlinx.dl.api.summary.printSummary
import java.io.File


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val modelConfig = File("EngineMoveTimeModel/src/main/resources/model.json")
    val weights = File("EngineMoveTimeModel/src/main/resources/weights")

    val model = Functional.loadModelConfiguration(modelConfig)

    model.compile(Adam(), Losses.MSE, Metrics.MSE)
    model.loadWeights(HdfFile(weights))

    model.printSummary()

    val input = floatArrayOf(300.0F, 	0.0F, 	173.31F, 	1.36F, 	42.0F, 	0.0F)
    val output = model.predictSoftly(input)
    println("Model predicts ${output.contentToString()} seconds. In python it predicted 7.546401 seconds")
}