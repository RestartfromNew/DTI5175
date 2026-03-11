package com.example.prototype.viewmodel
//UI 状态管理中心
//ViewModel 是 UI 的状态控制中心，同时是 UI 与数据层之间的桥梁。
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototype.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PhotoRepository(application)
    //创建一个repository类

    private val _cachedPath = MutableStateFlow<String?>(null)
    //private 让UI组件没办法访问获得的cachePath
    val cachedPath: StateFlow<String?> = _cachedPath

    fun onPhotoSelected(uri: Uri) {

        viewModelScope.launch(Dispatchers.IO) {
            //用后台线程Dispatchers来进行文件操作
            val path = repository.savePhotoToCache(uri)
            _cachedPath.value = path
        }
    }
}
