package com.stepango.aar2jar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.ArtifactTransform
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.internal.artifacts.ArtifactAttributes.ARTIFACT_FORMAT
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

class Aar2Jar : Plugin<Project> {

    override fun apply(project: Project) {

        project.pluginManager.apply(IdeaPlugin::class.java)
        project.pluginManager.apply(JavaBasePlugin::class.java)

        project.dependencies {
            registerTransform {
                from.attribute(ARTIFACT_FORMAT, "aar")
                to.attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
                artifactTransform(AarToJarTransform::class.java)
            }
        }

        val compileOnlyAar = project.configurations.register("compileOnlyAar")
        val implementationAar = project.configurations.register("implementationAar")

        compileOnlyAar.configure {
            attributes {
                attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
            }
            project.afterEvaluate {
                forEach { jarFile ->
                    project.dependencies.add("compileOnly", project.files(jarFile))
                }
            }
        }

        implementationAar.configure {
            attributes {
                attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
            }
            project.afterEvaluate {
                forEach { jarFile ->
                    project.dependencies.add("implementation", project.files(jarFile))
                }
            }
        }

        project.extensions
                .getByType<IdeaModel>()
                .module
                .scopes["PROVIDED"]
                ?.get("plus")
                ?.apply {
                    add(implementationAar.get())
                    add(compileOnlyAar.get())
                }

    }
}

class AarToJarTransform : ArtifactTransform() {

    override fun transform(input: File): List<File> {
        val file = File(outputDirectory, input.name.replace(".aar", ".jar"))
        ZipFile(input).use { zipFile ->
            zipFile.entries()
                    .toList()
                    .first { it.name == "classes.jar" }
                    .let(zipFile::getInputStream)
                    .use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
        }

        return listOf(file)
    }

}