package com.example.stampiot.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.InputType.*
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.stampiot.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * A placeholder fragment containing a simple view.
 */
@Suppress("DEPRECATION")
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private var tabIndex = 0
    private var tabName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabIndex = arguments?.getInt(ARG_SECTION_NUMBER) ?: 1
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(tabIndex)
        }
        tabIndex -= 1
        var tabList: Array<String> = resources.getStringArray(R.array.stamps_array)
        tabName = tabList[tabIndex]
    }

    private var infoID = 0

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val net: ConnectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        net.getNetworkCapabilities(net.activeNetwork)?.run {
            if (!hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                context?.let { it1 ->
                    AlertDialog.Builder(it1)
                        .setTitle("无网络连接！")
                        .setNegativeButton("確認") { _, _ ->
                            "确定"
                        }
                        //设置内容文本
                        .setMessage("请首先检查无线网络连接，然后点击[刷新]按钮").create().show()
                }
        }

        val root = inflater.inflate(R.layout.fragment_main, container, false)
        val textView: TextView = root.findViewById(R.id.section_label)
//        val progressBar: ProgressBar = root.findViewById(R.id.progressBar)
//        val textViewProgress: TextView = root.findViewById(R.id.textViewProgress)
//        val textViewMStatus: TextView = root.findViewById(R.id.textViewMStatus)
//        val textViewPdName: TextView = root.findViewById(R.id.textViewPdName)
//        val textViewMaterial: TextView = root.findViewById(R.id.textViewMaterial)
//        val textViewPlanNum: TextView = root.findViewById(R.id.textViewPlanNum)
        //val textViewCompleteNum: TextView = root.findViewById(R.id.textViewCompleteNum)
        val inputCounter: TextInputEditText = root.findViewById(R.id.inputCounter)

        pageViewModel.text.observe(viewLifecycleOwner, Observer<String> {
            textView.text = it
        })

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        var serverIP = sharedPreferences.getString("serverIP", "192.168.31.94")
        if(serverIP == null) serverIP = "192.168.31.94"
        StampInfoLiveData.get(serverIP).message.observe(viewLifecycleOwner, { str: String ->
            this.view?.let { it ->
                Snackbar.make(it, str, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                var dialog = this.context?.let { it1 ->
                    AlertDialog.Builder(it1)
                        .setTitle(str)
                        .setNegativeButton("確認") { _, _ ->
                            ("确定")
                        }
                        //设置内容文本
                        .setMessage("请首先检查无线网络连接，然后点击[刷新]按钮").create().show()
                }
            }

        })

        StampInfoLiveData.get(serverIP).observe(
            viewLifecycleOwner,
            Observer<MutableList<StampInfo>> { stampInfo: MutableList<StampInfo> ->

                println("--------index: $tabIndex----------")
                if (tabIndex > stampInfo.size) return@Observer
                println("--------name: $tabName----------")
                var info = stampInfo.firstOrNull{
                    it.name == tabName
                }
                info = info ?: stampInfo[tabIndex]
                tabName = info.name
                infoID = info.infoID
                val statusStr = when(info.stampStatus){
                    "红" -> arrayOf("故障中", Color.RED)
                    "黄" -> arrayOf("段取中", Color.YELLOW)
                    "绿" -> arrayOf("生产中", Color.GREEN)
                    else -> arrayOf("计画停止中", Color.GRAY)
                }

                textViewMStatus.setBackgroundColor(statusStr[1] as Int)
                textViewMStatus.text = "冲压机工作状态：${statusStr[0]}"
                textViewPdName.text = "品名図番：" + info.productInfo
                textViewMaterial.text = "材料：" + info.material
                textViewPlanNum.text = "计划数：" + info.planNum.toString()
                textViewCompleteNum.text = "完成数：" + info.completeNum.toString()

                val progress = (info.todayProgress * 100).toInt()
                progressBar.progress = progress
                textViewProgress.text = "本日进度：$progress%"
                val id = when (info.speed) {
                    "正常" -> R.drawable.main_pb_bg_green
                    "较慢" -> R.drawable.main_pb_bg
                    else -> R.drawable.main_pb_bg_yellow
                }
                progressBar.progressDrawable = getDrawable(resources, id, null)

                textViewPlan.text = info.nextPlan
                //if(!inputCounter.text.isNullOrBlank()) inputCounter.text!!.clear()
                if(info.over.isNotBlank()){
                    workerID.setText(info.workID)
                    inputCounter.setText(info.completeNum.toString())
                    //root.findViewById(R.id.buttonOver)
                    buttonOver.isEnabled = false
                }
            })

        val btnOver: android.widget.Button = root.findViewById(R.id.buttonOver)
        btnOver.setOnClickListener { view ->
            println("-----------$view---------")
            if (inputCounter.text.isNullOrBlank()) inputCounter.setText(textViewCompleteNum.text.subSequence(4,textViewCompleteNum.text.length))
            if (workerID.text != null && workerID.text.toString().isNotBlank()){
                println("-----------" + workerID.text.toString())
                this.context?.let { it1 ->
                    AlertDialog.Builder(it1)
                        .setTitle("请再次确认实际加工的数量：${inputCounter.text}")
                        .setPositiveButton("確認"){ dialog, id ->
                            // User clicked OK button
                            view.isEnabled = false
                            try {
                                GlobalScope.launch {
                                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                                        context
                                    )
                                    val serverIP = sharedPreferences.getString("serverIP", "") ?: throw Exception(
                                        "请先设置服务器IP地址"
                                    )
                                    val db = DatabaseUtility(serverIP)
                                    //workerID.text.toString()
                                    val sqlStr = "UPDATE 冲压部品信息表 SET 加工完成数 = ${inputCounter.text.toString().toInt()}, 加工人员ID = ${workerID.text.toString()}, 完工日期 = GETDATE() WHERE ID = $infoID"
                                    db.update(sqlStr)
                                }
                            }
                            catch (e: Exception){
                                println("!!!!!!!!!!!!!!!!!!${e.message}")
                                this.context?.let { it1 ->
                                    AlertDialog.Builder(it1)
                                        .setTitle(e.toString())
                                        .setNegativeButton("确定") { _, _ ->
                                        }
                                        //设置内容文本
                                        .setMessage("请首先检查无线网络连接，然后点击[完工]按钮").create().show()
                                }
                            }
                        }
                        .setNegativeButton("重填") { _, _ ->
                            inputCounter.text?.clear()
                            inputCounter.requestFocus()
                            view.isEnabled = true
                        }
                        //.setMessage(inputCounter.text)设置内容文本
                        .create().show()
                }

            }else{
                view.isEnabled = true
                Snackbar.make(view, "请输入加工人员ID！", Snackbar.LENGTH_LONG)
                    .setAction("实际加工的数量", null).show()
                this.context?.let { it1 ->
                    AlertDialog.Builder(it1)
                        .setTitle("请输入员工编号！")
                        .setNegativeButton("取消") { _, _ ->
                            ("")
                        }
                        //设置内容文本
                        .setMessage("请先输入加工人员ID，然后点击[完工]按钮").create().show()
                }
            }
        }

        val btnRefresh: android.widget.Button = root.findViewById(R.id.btnRefresh)
        btnRefresh.setOnClickListener {
            println("-----------btnRefresh---------")
            StampInfoLiveData.get(serverIP).isRefresh = true
        }

        val btnError: android.widget.Button = root.findViewById(R.id.buttonError)
        btnError.setOnClickListener { view ->
            println("-----------$view---------")
            view.isEnabled = false
                println("-----上报设备故障------")
                try {
                    //val index = arguments?.getInt(ARG_SECTION_NUMBER) ?: 1
                    //val stampList = resources.getStringArray(R.array.stamps_array)
                    //val name = stampList[index - 1]
                    //val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                    val ip = sharedPreferences.getString(tabName, "") ?: throw Exception(
                        "请先设置与$tabName 相关联的PLC的IP地址"
                    )
                    GlobalScope.launch {
                        PlcSocket().sendComm(
                            ip, 507, "red light"
                        )
                        //val db = DatabaseUtility(serverIP)
                        //workerID.text.toString()
                        //val sqlStr = "UPDATE 冲压设备状态表 SET 状态 = '红' WHERE 机器名 = $tabName"
                        //db.update(sqlStr)
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                    this.context?.let {
                        AlertDialog.Builder(it)
                            .setTitle(e.toString())
                            .setNegativeButton("確認") { _, _ ->
                                ("确定")
                            }
                            //设置内容文本
                            .setMessage("请首先检查无线网络连接，然后点击[故障]按钮").create().show()
                    }
                }
                finally {
                    println("--------socket close--------")
                }
        }
        return root
    }


    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val context = preferenceManager.context

            val serverIPEdit = preferenceManager.findPreference<EditTextPreference>("serverIP")
            serverIPEdit?.setOnBindEditTextListener {
                it.inputType = TYPE_CLASS_NUMBER
                it.keyListener = DigitsKeyListener.getInstance("0123456789.:")
                it.isSingleLine = true
            }

            val stampList = resources.getStringArray(R.array.stamps_array)//arrayOf("60ー6", "80Tー1", "80Tー2", "80Tー3", "80Tー4", "110T", "200T")
            for (stamp in stampList.indices) {
                "設置押し抜き機IPアドレス"
                val numberPreference = EditTextPreference(context)
                numberPreference.key = "stamp_${stampList[stamp]}"
                numberPreference.title = stampList[stamp]
                numberPreference.setOnBindEditTextListener {
                    it.inputType = TYPE_CLASS_NUMBER
                    it.keyListener = DigitsKeyListener.getInstance("0123456789.:")
                    it.isSingleLine = true
                }

                numberPreference.text = "192.168.1.1"
                numberPreference.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
                preferenceScreen.addPreference(numberPreference)
            }

        }

        private val listener: SharedPreferences.OnSharedPreferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                 if (key == "serverIP") {
                     var ip =
                         sharedPreferences.getString(key, "") ?: throw Exception("请先设置服务器IP地址")
                     StampInfoLiveData.get(ip).serverIPListener = ip
                     println("---------------onSharedPreferenceChanged:$ip----------------------")
                 }
            }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        }

        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }


    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}