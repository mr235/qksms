package com.moez.qksms.common.util

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination

object PinyinUtil {
    // 初始化拼音输出格式（大写、无音调）
    private val pinyinFormat = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.UPPERCASE // 首字母大写
        toneType = HanyuPinyinToneType.WITHOUT_TONE // 无音调（如 "张" → "ZHANG"，而非 "ZHANG1"）
    }

    /**
     * 中文字符串转首字母串（如 "张三" → "ZS"，"李四" → "LS"）
     * @param chinese 输入中文（可含字母、数字，会保留原样）
     * @return 首字母串（非中文字符直接保留，如 "张三abc123" → "ZSABC123"）
     */
    fun getFirstLetter(chinese: String?): String {
        if (chinese.isNullOrEmpty()) return ""

        val sb = StringBuilder()
        for (char in chinese.toCharArray()) {
            // 处理中文字符
            if (char.isChinese()) {
                try {
                    // 获取该汉字的所有拼音（多音字会返回多个，取第一个）
                    val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(char, pinyinFormat)
                    if (pinyinArray.isNotEmpty()) {
                        // 取拼音首字母（如 "ZHANG" → "Z"）
                        sb.append(pinyinArray[0][0])
                    }
                } catch (e: BadHanyuPinyinOutputFormatCombination) {
                    e.printStackTrace()
                    sb.append(char) // 异常时保留原字符
                }
            } else {
                // 非中文字符（字母、数字、符号）直接保留
                sb.append(char.uppercaseChar())
            }
        }
        return sb.toString()
    }
    /**
     * 中文字符串转全拼（如 "张三" → "ZHANGSAN"，"吕" → "LYU"）
     * @param chinese 输入中文（可含字母、数字、符号）
     * @param caseType 大小写类型（默认大写）
     * @param toneType 音调类型（默认无音调）
     * @param vCharType ü 显示方式（默认用 v 代替）
     * @param separator 拼音之间的分隔符（默认空字符串，如 "张三" → "ZHANGSAN"；传 "-" 则为 "ZHANG-SAN"）
     * @return 全拼字符串（非中文字符直接保留）
     */
    fun getFullPinyin(
        chinese: String?
    ): String {
        if (chinese.isNullOrEmpty()) return ""

        val sb = StringBuilder()
        for (char in chinese.toCharArray()) {
            when {
                // 中文字符：转全拼（多音字取第一个拼音）
                char.isChinese() -> {
                    try {
                        val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(char, pinyinFormat)
                        if (pinyinArray.isNotEmpty()) {
                            val str = pinyinArray[0]
                            sb.append(str) // 多音字默认取第一个拼音（可根据需求扩展）
                            repeat(7- str.length) {
                                sb.append("0")
                            }
//                            sb.append("_")
                        } else {
                            sb.append(char) // 无对应拼音时保留原字符
                        }
                    } catch (e: BadHanyuPinyinOutputFormatCombination) {
                        e.printStackTrace()
                        sb.append(char)
                    }
                }
                // 非中文字符：直接保留
                else -> {
                    sb.append(char)
                    // 非中文字符后不添加分隔符（避免如 "张三123" 变成 "ZHANGSAN-123"）
                }
            }
        }

        return sb.toString()
    }

    /**
     * 判断字符是否为中文字符（含简繁体）
     */
    private fun Char.isChinese(): Boolean {
        return Character.toString(this).matches(Regex("[\\u4E00-\\u9FA5\\u3400-\\u4DBF\\uF900-\\uFAFF]"))
    }

    /**
     * 单个汉字转首字母（如 "张" → "Z"，"李" → "L"）
     * @param chineseChar 单个中文字符
     * @return 首字母（非中文返回原字符大写）
     */
    fun getSingleFirstLetter(chineseChar: Char): Char {
        if (Character.toString(chineseChar).matches(Regex("[\\u4E00-\\u9FA5]"))) {
            try {
                val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(chineseChar, pinyinFormat)
                if (pinyinArray.isNotEmpty()) {
                    return pinyinArray[0][0]
                }
            } catch (e: BadHanyuPinyinOutputFormatCombination) {
                e.printStackTrace()
            }
        }
        return chineseChar.uppercaseChar()
    }
}