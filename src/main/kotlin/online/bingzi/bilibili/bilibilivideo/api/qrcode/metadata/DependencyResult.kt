package online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata

data class DependencyResult(
    val satisfied: Boolean,                                     // 依赖是否满足
    val missingDependencies: List<String>,                      // 缺失的必需依赖
    val missingSoftDependencies: List<String>,                  // 缺失的可选依赖
    val details: Map<String, DependencyStatus>                  // 每个依赖的详细状态
)