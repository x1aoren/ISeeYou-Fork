package cn.xor7.iseeyou.utils

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import java.io.File
import java.io.IOException

class TomlEx<T>(private val filePath: String, private val clazz: Class<T>) {
    var data: T
        private set

    init {
        val file = File(filePath)
        if (!file.exists()) {
            // 如果文件不存在，创建默认配置
            file.parentFile.mkdirs()
            data = clazz.getDeclaredConstructor().newInstance()
            save()
        } else {
            // 读取现有配置
            try {
                data = Toml().read(file).to(clazz)
            } catch (e: Exception) {
                // 如果读取失败，创建默认配置
                data = clazz.getDeclaredConstructor().newInstance()
                save()
            }
        }
    }

    fun save() {
        try {
            val file = File(filePath)
            file.parentFile.mkdirs()
            TomlWriter().write(data, file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
} 