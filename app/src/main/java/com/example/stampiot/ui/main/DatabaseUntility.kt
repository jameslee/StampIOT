package com.example.stampiot.ui.main

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.example.stampiot.R
import kotlinx.coroutines.*
import net.sourceforge.jtds.jdbc.DateTime
import java.sql.*
import java.time.DateTimeException

data class StampProductDescribe( //冲压部品信息表
    var id:	Int,                 //NOT NULL PRIMARY KEY IDENTITY,--流水号
    var mName: String,           //机器名	 nvarchar	(20),
    var orderNum: String,        //订单编号  nvarchar	(40),
    var rawNum: String,          //物料编号  nvarchar	(40),--(图番)
    var classification: String,  //品名	 nvarchar	(100),
    var material: String,        //材料 	 nvarchar	(100),
    var dayPlanQuantity: Int,    //日计划数 int,
    var completeQuantity: Int,   //加工完成数 int,
    var rotationSpeed: Float,    //设备转速  decimal(6,4),--0.0001
    var personHour: Float,       //工时  decimal(6,1),
    var ST: Float,               //decimal(6,4),
    var date: DateTime,           //计划日期 datetime,
    var overTime: String
)

class DatabaseUtility(serverIP: String) {

    var serverIP = serverIP

    suspend fun update(sqlStr: String): Boolean{
        var dbConnect: Connection? = null
        var stmt: Statement? = null
        try {
            println("-----------update------serverIP:$serverIP----------------")
            dbConnect = DriverManager.getConnection("jdbc:jtds:sqlserver://$serverIP:1433/fujiPG;user=sa;password=123456;")
            stmt = dbConnect.createStatement()
            println("----------update--------$stmt---------------------")
            val ref = stmt.execute(sqlStr)
            stmt.clearBatch()
            return ref
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
        finally {
            stmt?.close()
            dbConnect?.close()
        }
        return false
    }

    suspend fun getStampInfoSet(): MutableList<StampInfo> {
        //val sqlStr = "SELECT * from 冲压部品信息表 where 计划日期 = CONVERT(varchar(100), GETDATE(), 23)"
        var sqlStr = "SELECT * from 冲压设备状态表"
        //val statement = DbSettings.dbConnect.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE,ResultSet.HOLD_CURSORS_OVER_COMMIT)
        var dbConnect: Connection? = null
        var statementRe: Statement? = null
        var statementRs: Statement? = null
        var refSet: ResultSet? = null
        val rsList = mutableListOf<StampInfo>()
        var infoSet: ResultSet? = null
        try {
            println("----------getStampInfoSet-------serverIP:$serverIP----------------")
            dbConnect =
                DriverManager.getConnection("jdbc:jtds:sqlserver://$serverIP:1433/fujiPG;user=sa;password=123456;")
            statementRe = dbConnect.createStatement()
            println("----------getStampInfoSet--------$statementRe---------------------")
            refSet = statementRe.executeQuery(sqlStr)
            println("----------getStampInfoSet--------${refSet.isClosed}---------------------")

            while (refSet.next()) {
                val id = refSet.getInt("当前加工id")
                if (id == 0) {
                    rsList.add(
                        StampInfo(
                            refSet.getString("机器名"),
                            refSet.getString("状态"),
                            " ",
                            " ",
                            0,
                            0,
                            refSet.getFloat("本日进度"),
                            refSet.getString("次回预定段取"),
                            refSet.getString("进度快慢"),
                            " ",
                            id,
                            "",
                        )
                    )
                } else {
                    sqlStr = "select * from 冲压部品信息表 where id = $id"
                    statementRs = dbConnect.createStatement()
                    infoSet = statementRs.executeQuery(sqlStr)
                    while (infoSet.next()) {
                        rsList.add(
                            StampInfo(
                                refSet.getString("机器名"),
                                refSet.getString("状态"),
                                infoSet.getString("品名") + infoSet.getString("物料编号"),
                                infoSet.getString("材料"),
                                infoSet.getInt("日计划数"),
                                infoSet.getInt("加工完成数"),
                                refSet.getFloat("本日进度"),
                                refSet.getString("次回预定段取"),
                                refSet.getString("进度快慢"),
                                infoSet.getString("加工人员ID")?:"",
                                id,
                                infoSet.getString("完工日期")?:""
                            )
                        )
                    }
                }
            }
        }
        catch (e: SQLException){
            e.printStackTrace()
            throw(e)
        }
        finally {
            infoSet?.close()
            refSet?.close()
            statementRe?.close()
            statementRs?.close()
            dbConnect?.close()
        }
        return rsList
    }
}

data class StampInfo(
    var name: String,
    var stampStatus: String,
    val productInfo: String,
    val material: String,
    val planNum: Int,
    val completeNum: Int,
    val todayProgress: Float,
    val nextPlan: String,
    val speed: String,
    val workID: String,
    val infoID: Int,
    val over: String
)

class StampInfoLiveData(serverIP: String) : LiveData<MutableList<StampInfo>>() {

    private val listener = { stampInfo: MutableList<StampInfo> ->
        value = stampInfo
    }

    companion object {
        private lateinit var sInstance: StampInfoLiveData

        @MainThread
        fun get(serverIP: String): StampInfoLiveData {
            sInstance = if (::sInstance.isInitialized) sInstance else StampInfoLiveData(serverIP)
            return sInstance
        }
    }

    val message =  MutableLiveData<String>()
    var isRefresh = false
    var serverIPListener = serverIP

    init {
        try {
            GlobalScope.launch{
                val db = DatabaseUtility(serverIP)
                while (true) {
                    try {
                        postValue(db.getStampInfoSet())
                    }
                    catch (e: SQLException){
                        message.postValue(e.toString())
                    }
                    for (i in 1..5){
                        if(isRefresh) {
                            isRefresh = false
                            break
                        }
                        if(serverIPListener.isNotBlank() && !serverIPListener.equals(db.serverIP)) {
                            println("-----------------global launch set server IP :$serverIPListener----------------")
                            db.serverIP = serverIPListener
                            break
                        }
                        delay(1000)
                    }
                }

            }
        }
        catch (e: Exception){
            message.postValue(e.toString())
        }
    }
}
