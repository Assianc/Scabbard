package com.assiance.convert.logic

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 存储单位换算的纯逻辑（不依赖 Android）。
 *
 * 两种口径都支持：1024（KiB 系，但界面仍沿用 KB/MB 的叫法）与 1000（十进口径）。
 * 全程用 [BigDecimal]，避免 Double 在 GB/TB 量级上的显示误差。
 */

/** 单位与它相对字节的幂次（base^power）。 */
enum class StorageUnit(val label: String, val power: Int) {
    B("B", 0),
    KB("KB", 1),
    MB("MB", 2),
    GB("GB", 3),
    TB("TB", 4),
    PB("PB", 5),
}

/** 界面上的两种换算口径。 */
enum class StorageBase(val label: String, val value: Int) {
    BINARY("1024", 1024),
    DECIMAL("1000", 1000),
}

/** 把 [value] 从 [from] 换算成全部单位（含自身）。 */
fun convertStorage(
    value: BigDecimal,
    from: StorageUnit,
    base: Int,
): Map<StorageUnit, BigDecimal> {
    val bytes = value.multiply(BigDecimal(base).pow(from.power))
    return StorageUnit.entries.associateWith { unit ->
        bytes.divide(BigDecimal(base).pow(unit.power), 12, RoundingMode.HALF_UP)
    }
}

/**
 * 结果格式化：最多 6 位小数、去掉尾零，且不用科学计数法。
 *
 * `stripTrailingZeros()` 对整数会得到 `1E+3` 这种表示，所以最后统一走 `toPlainString()`。
 */
fun formatStorageNumber(value: BigDecimal): String =
    value
        .setScale(6, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
