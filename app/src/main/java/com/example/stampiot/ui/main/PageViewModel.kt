package com.example.stampiot.ui.main

import android.provider.ContactsContract
import android.view.contentcapture.DataRemovalRequest
import androidx.lifecycle.*
import kotlinx.coroutines.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Date

class PageViewModel : ViewModel() {

    private val _index = MutableLiveData<Int>()
    val text: LiveData<String> = Transformations.map(_index) {
        "Hello world from section: $it"
    }

    fun setIndex(index: Int) {
        _index.value = index
    }
}