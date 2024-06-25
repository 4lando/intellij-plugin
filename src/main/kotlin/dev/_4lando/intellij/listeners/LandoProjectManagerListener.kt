package dev._4lando.intellij.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import dev._4lando.intellij.services.LandoProjectService

class LandoProjectManagerListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        LandoProjectService.getInstance(project)
    }
}