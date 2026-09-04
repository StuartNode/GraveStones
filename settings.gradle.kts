rootProject.name = "GraveStone"

includeBuild("../NexisApi") {
    dependencySubstitution {
        substitute(module("dev.stuart:NexisApi")).using(project(":"))
    }
}
