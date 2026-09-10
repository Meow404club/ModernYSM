plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.141" apply false
}

stonecutter active "1.20.1-forge"

stonecutter parameters {
    // 版本项命名约定 "<mc>-<loader>"：取末段作为 loader 常量（1.20.1-forge -> forge）
    constants.match(node.metadata.project.substringAfterLast('-'), "forge")
}
