dependencies {
    // 引入 服务端核心
//    compileOnly("ink.ptms.core:v12004:12004:mapped")
//    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("ink.ptms.core:v12100:12100:mapped")
    compileOnly("ink.ptms.core:v12100:12100:universal")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
}

// 子模块
taboolib {
    subproject = true
}
