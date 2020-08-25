/*
 * Copyright (C) 2020 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file

import org.ossreviewtoolkit.helper.common.writeAsYaml
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.OrtResult
import org.ossreviewtoolkit.model.Provenance
import org.ossreviewtoolkit.model.VcsType
import org.ossreviewtoolkit.model.config.PackageConfiguration
import org.ossreviewtoolkit.model.config.VcsMatcher
import org.ossreviewtoolkit.model.readValue
import org.ossreviewtoolkit.utils.expandTilde
import org.ossreviewtoolkit.utils.safeMkdirs

internal class GeneratePackageConfigurationsCommand : CliktCommand(
    help = "Generates one package configuration for the source artifact scan and one for the VCS scan, if " +
            "a corresponding scan result exists in the given ORT result for the respective provenance. The output " +
            "package configuration YAML files are written to the given output directory."
) {
    private val ortResultFile by option(
        "--ort-result-file",
        help = "The input ORT file containing the package for which the package configurations shall be generated."
    ).convert { it.expandTilde() }
        .file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = false)
        .convert { it.absoluteFile.normalize() }
        .required()

    private val packageId by option(
        "--package-id",
        help = "The target package for which the package configuration shall be generated."
    ).convert { Identifier(it) }
        .required()

    private val outputDir by option(
        "--output-dir",
        help = "The output directory to write the package configurations to."
    ).convert { it.expandTilde() }
        .file(mustExist = false, canBeFile = false, canBeDir = true, mustBeWritable = false, mustBeReadable = false)
        .convert { it.absoluteFile.normalize() }
        .required()

    override fun run() {
        outputDir.safeMkdirs()

        val ortResult = ortResultFile.readValue<OrtResult>()
        val scanResults = ortResult.getScanResultsForId(packageId)

        scanResults.find { it.provenance.vcsInfo != null }?.provenance
            ?.writePackageConfigurationFile("vcs.yml")
        scanResults.find { it.provenance.sourceArtifact != null }?.provenance
            ?.writePackageConfigurationFile("source-artifact.yml")
    }

    private fun Provenance.writePackageConfigurationFile(filename: String) {
        val packageConfiguration = createPackageConfiguration(packageId, this)
        val outputFile = outputDir.resolve(filename)

        packageConfiguration.writeAsYaml(outputFile)
        println("Wrote a package configuration to '${outputFile.absolutePath}'.")
    }
}

private fun createPackageConfiguration(id: Identifier, provenance: Provenance): PackageConfiguration =
    provenance.vcsInfo?.let { vcsInfo ->
        PackageConfiguration(
            id = id,
            vcs = VcsMatcher(
                type = vcsInfo.type,
                url = vcsInfo.url,
                revision = vcsInfo.resolvedRevision!!,
                path = vcsInfo.path.takeIf { vcsInfo.type == VcsType.GIT_REPO }.orEmpty()
            )
        )
    } ?: PackageConfiguration(
        id = id,
        sourceArtifactUrl = provenance.sourceArtifact!!.url
    )
