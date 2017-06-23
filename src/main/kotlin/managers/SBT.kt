package com.here.provenanceanalyzer.managers

import com.here.provenanceanalyzer.model.Dependency
import com.here.provenanceanalyzer.PackageManager

import java.io.File

object SBT : PackageManager(
        "http://www.scala-sbt.org/",
        "Scala",
        listOf("build.sbt", "build.scala")
) {
    override fun resolveDependencies(definitionFiles: List<File>): Map<File, Dependency> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
