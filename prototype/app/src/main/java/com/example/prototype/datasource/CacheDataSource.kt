package com.example.prototype.datasource
//datasource文件夹只负责数据的访问
/*复制用户选中图片到APP缓存目录
返回本地路径
* */
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class CacheDataSource(private val context: Context){
    //数据源类，依赖一个Context
    fun saveToCache(uri: Uri):String{
        //传入一个Uri,返回一个String
        val inputStream=context.contentResolver.openInputStream(uri)
        //用Uri打开一个输入流
        //获取输入流
            ?: return "Failed to open stream"
        val cacheFile = File(
            context.cacheDir,
            // /data/data/你的包名/cache/
            //在cache这个文件夹下存储数据，将文件名命名为cached_image_现在的时间
            "cached_image_${System.currentTimeMillis()}.jpg"
        )

        val outputStream = FileOutputStream(cacheFile)

        inputStream.copyTo(outputStream)
        //将input流的拷贝到output，也就是cache文件夹下
        inputStream.close()
        outputStream.close()


        return cacheFile.absolutePath
        //返回绝对路径
    }
}