package com.github.saloed.customdiffplugin.services

import com.github.saloed.customdiffplugin.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
