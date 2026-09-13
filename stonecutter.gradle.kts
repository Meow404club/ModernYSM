plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.147" apply false
}

stonecutter active "1.20.1-forge"

stonecutter parameters {
    // 版本项命名约定 "<mc>-<loader>"：取末段作为 loader 常量（1.20.1-forge -> forge）；
    // M3 批二 a 起 neoforge 线注册，choices 加 "neoforge"（stonecutter 0.7
    // data/dsl/Containers.kt:13 match(sample, vararg choices) 选择器形态实证）
    constants.match(node.metadata.project.substringAfterLast('-'), "forge", "neoforge")
}
