package com.example.prototype.repository
/*
* 负责：决定用哪个数据源,合多个数据源,处理缓存策略,做数据转换
* 此处创建一个访问图片的datasource对象，对外暴露savePhotoCache对象*/
import android.content.Context
import android.net.Uri
import com.example.prototype.datasource.CacheDataSource

class PhotoRepository(private val context: Context){
    private val cacheDataSource= CacheDataSource(context)
    //创建具体的数据来源对象，使用datasource
    fun savePhotoToCache(uri: Uri): String {
        return cacheDataSource.saveToCache(uri)
    }
}