package com.stepango.aar2jar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.transform.ArtifactTransform
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.internal.artifacts.ArtifactAttributes.ARTIFACT_FORMAT
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalArgumentException
import java.util.zip.ZipFile

class Aar2Jar : Plugin<Project> {

    override fun apply(project: Project) {

        val compileOnlyAar = project.configurations.register("compileOnlyAar")
        val implementationAar = project.configurations.register("implementationAar")

        // Assume all modules have test configuration
        val testCompileOnlyAar = project.configurations.register("testCompileOnlyAar")
        val testImplementationAar = project.configurations.register("testImplementationAar")

        project.pluginManager.withPlugin("idea") {

            val scopes = project.extensions
                    .getByType<IdeaModel>()
                    .module
                    .scopes

            scopes["TEST"]
                    ?.get("plus")
                    ?.apply {
                        add(testImplementationAar.get())
                        add(testCompileOnlyAar.get())
                    }

            scopes.filter { (s, _) -> s != "TEST" }
                    .forEach {
                        it.value["plus"]?.apply {
                            add(compileOnlyAar.get())
                        }
                    }

            scopes.forEach {
                        it.value["plus"]?.apply {
                            add(implementationAar.get())
                        }
                    }
        }

        project.dependencies {
            registerTransform {
                from.attribute(ARTIFACT_FORMAT, "aar")
                to.attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
                artifactTransform(AarToJarTransform::class.java)
            }
        }

        compileOnlyAar.configure {
            baseConfiguration(project, "main") {
                compileClasspath += this@configure
            }
        }

        implementationAar.configure {
            baseConfiguration(project, "main") {
                compileClasspath += this@configure
                runtimeClasspath += this@configure
            }
        }

        testCompileOnlyAar.configure {
            baseConfiguration(project, "test") {
                compileClasspath += this@configure
            }
        }

        testImplementationAar.configure {
            baseConfiguration(project, "test") {
                compileClasspath += this@configure
                runtimeClasspath += this@configure
            }
        }

    }
}

fun Configuration.baseConfiguration(project: Project, name: String, f: SourceSet.() -> Unit) {
    isTransitive = false
    attributes {
        attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
    }
    project.pluginManager.withPlugin("java") {
        val sourceSets = project.the<JavaPluginConvention>().sourceSets
        sourceSets.withName(name, f)
    }
}

fun SourceSetContainer.withName(name: String, f: SourceSet.() -> Unit) {
    this[name]?.apply { f(this) } ?: whenObjectAdded { if (this.name == name) f(this) }
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