rootProject.name = "ModTemplate"
include("LoaderModPlugin")
findProject(":LoaderModPlugin")?.name = "Loader"
include("Mod")

