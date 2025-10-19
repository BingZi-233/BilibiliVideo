package online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata

/**
 * 依赖检查结果
 * 
 * 包含依赖检查的完整结果信息，包括总体满足状态和详细的依赖项状态。
 * 
 * @property satisfied 依赖是否满足，只有所有必需依赖都存在且可用时才为true
 * @property missingDependencies 缺失的必需依赖列表，这些依赖必须安装才能正常工作
 * @property missingSoftDependencies 缺失的可选依赖列表，这些依赖缺失不影响基本功能
 * @property details 每个依赖的详细状态映射，键为依赖名称，值为对应的状态
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class DependencyResult(
    val satisfied: Boolean,
    val missingDependencies: List<String>,
    val missingSoftDependencies: List<String>,
    val details: Map<String, DependencyStatus>
)