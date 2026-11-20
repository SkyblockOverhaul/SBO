plugins {
    `java-gradle-plugin`
}

group = "net.sbo"
version = "1.0"

gradlePlugin {
    plugins {
        create("sboBuildLogic") {
            id = "net.sbo.build-logic"
            implementationClass = "buildlogic.SboBuildLogicPlugin"
        }
    }
}
