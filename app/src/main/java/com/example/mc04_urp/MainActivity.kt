/*
* Преобразование длины линии и диаметра жилы в сопротивление шлейфа R=ρ*l/S, ρ=0.01724..0.018 Ом*мм^2/м, l-м, S = πd^2/4 мм^2
* Уточнить минимальную нагрузку, при которой ДП отключается по недогрузу
* Выход в настройку источника ДП по клику, а не из контекстного меню
* Ограничение сверху по току кроме мощности (ток разный в зависимости от напряжения источника ДП)
* */

package com.example.mc04_urp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import kotlin.math.round

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var rpVoltage: Int = 210      // Напряжение ДП
    private var count:Int = 0       // счетчик регенераторов
    private val rgVoltageMin = 100        // минимальное напряжение питания регенератора
//    private val uPSmin = 120
    private var error = 0           // номер ошибки. 0 - нет ошибки
    private var rpPWRmax = 75            // мощность источника ДП
    private var rpCurrent: Double = 0.0       // Ток источника ДП

    val REQUEST_CODE_URP = 1
    val REQUEST_CODE_PRP = 2


    private var listRgName: MutableList<String> = mutableListOf("РЛ №0")       // Список имен регенераторов
    private var listRgPower: MutableList<Double> = mutableListOf(5.5)      // Мощность каждого регенератора
    private var listRgVoltage: MutableList<Double> = mutableListOf(rgVoltageMin.toDouble())    // Падение напряжения на регенераторе
    private var listRgCurrent: MutableList<Double> = mutableListOf(0.0)    // Потребляемый регенератором ток
    private var listRgResist: MutableList<Int> = mutableListOf(16)          // Сопротивление дросселя ДП (0й - для платы SM - 16 Ом, Для РЛ - 24 Ом)
    private var listLnVoltage: MutableList<Double> = mutableListOf(0.0)    // Падение напряжения на линии
    private var listLnCurrent: MutableList<Double> = mutableListOf(0.0)    // Ток через линию
    private var listLnResist: MutableList<Int> = mutableListOf(159)        // Сопротивление линии
    private var listStr: MutableList<String> = mutableListOf()          // Массив для засовывания в строку listView

    private lateinit var adapter1: ArrayAdapter<String>      // адаптер для listView
    private lateinit var btnAddRG: Button
    private lateinit var lvList :ListView
    private lateinit var tvRP: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.e("tagg", "onCreate")

        btnAddRG = findViewById(R.id.btnAddRG)
        val btnDelOneRG = findViewById<Button>(R.id.btnDelOneRG)
        val btnDelAllRG = findViewById<Button>(R.id.btnDelAllRG)
        lvList = findViewById(R.id.lvList)
        tvRP = findViewById(R.id.tvRP)

        btnAddRG.setOnClickListener(this)
        btnDelOneRG.setOnClickListener(this)
        btnDelAllRG.setOnClickListener(this)

        registerForContextMenu(tvRP)
        registerForContextMenu(lvList)

        // По нажатию на список открываю настройки РЛ или линии
        lvList.setOnItemClickListener { parent, view, position, id ->
            var i: Intent
            if (position % 2 == 1) {
                i = Intent(this, activity_settings_rg::class.java)
                i.putExtra("rgRESIST", listRgResist[(position+1)/2].toString())
                i.putExtra("rgPWR", listRgPower[(position+1)/2].toString())
                startActivityForResult(i, id.toInt())
            }
            if (position % 2 == 0) {
                i = Intent(this, activity_settings_ln::class.java)
                i.putExtra("lnRESIST", listLnResist[(position+2)/2].toString())
                startActivityForResult(i, id.toInt())
            }
        }

        tvRP.text = "Источник ДП:\nU = $rpVoltage В;  Pmax = $rpPWRmax Вт;  I = 0 мА"
    }

// Действия при нажатии кнопок
    override fun onClick(v: View?) {
        Log.e("tagg", "onClick")
        when (v?.id) {
            R.id.btnAddRG -> {
                addRG()         // Добавляем массивам элемент
                calculate()     // Подбираем значения напряжений и токов
                output()        // Выводим список
            }

            R.id.btnDelOneRG -> {
                delOneRg()
            }

            R.id.btnDelAllRG -> {
                cleanAll()
                checkError()
            }
        }
        Log.e("tagg", "end onClick")
    }

    // Добавляем регенератор в массивы (увеличиваем массивы на 1)
    private fun addRG() {
        Log.e("tagg", "addRG()")
        count += 1
        if (count > 49) return
        listRgPower.add(listRgPower[count-1])
        listRgResist.add(listRgResist[count-1])
        listRgVoltage.add(0.0)
        listRgCurrent.add(0.0)
        listLnVoltage.add(0.0)
        listLnCurrent.add(0.0)
        listLnResist.add(listLnResist[count-1])
        listRgName.add("РЛ №$count")
        listStr.add("")
        listStr.add("")
        Log.e("tagg", "end addRG()")
    }

    // Вычисляем параметры
    private fun calculate () {
        Log.e("tagg", "calculate")
        if (count == 0) return
//        listRgVoltage[1] = 0.0
//        listLnVoltage[1] = 0.0
            listRgVoltage[count] = rgVoltageMin.toDouble()
            var changeVoltage: Double
            do {
                for (i in count downTo 1) {
                    if (i != count) {
                        listRgVoltage[i] = listRgVoltage[i+1]+listLnVoltage[i+1]
                    }
                    listRgCurrent[i] = (listRgPower[i] * 1000) / listRgVoltage[i]       // Получаем ток на линии и на РЛ
                    listLnCurrent[i] = if (i != count) {
                        listLnCurrent[i+1] + listRgCurrent[i]
                    } else listRgCurrent[i]
                    listLnVoltage[i] = (listLnCurrent[i] * (listLnResist[i]+listRgResist[i]+listRgResist[i-1])) / 1000      // Вычисляем падение напряжения на линии
                }
                checkError()
                if (error != 0) {       // Проверка на ошибки
                    return
                }
                changeVoltage = (rpVoltage-(listRgVoltage[1]+listLnVoltage[1]))/2
                listRgVoltage[count] = listRgVoltage[count] + changeVoltage
            } while (changeVoltage>0.1 || changeVoltage<-0.1)     // делаем потом проверяем что приращение напряжения не стало меньше 0,1 (минимальная дискретность)
        Log.e("tagg", "end calculate")
    }

    // Вывод списка регенераторов и линий
    private fun output() {
        Log.e("tagg", "output")
        if (count > 0) {
            checkError()
            if (error == 1) {
                count -= 1
                calculate()
                btnAddRG.isEnabled = false
            }
            for (i in 1..count) {
                listStr[i * 2 - 2] ="      Линия:  R=(${listLnResist[i]}+${listRgResist[i]+listRgResist[i-1]})Ом, ΔU=${"%.1f".format(listLnVoltage[i])}В, I=${"%.1f".format(listLnCurrent[i])}мА"
                listStr[i * 2 - 1] ="${listRgName[i]}:  P=${listRgPower[i]}Вт, Uвх=${"%.1f".format(listRgVoltage[i])}В, I=${"%.1f".format(listRgCurrent[i])}мА, LR=${listRgResist[i]}Ом"
            }
            adapter1 = ArrayAdapter(this, android.R.layout.simple_list_item_1, listStr)
            lvList.adapter = adapter1
            Log.e("tagg", "!!!!!!!!!!!!! ${"%.1f".format(listLnCurrent[1])}")
//            rpCurrent = ("%.1f".format(listLnCurrent[1])).toString().toDouble()
            tvRP.text = "Источник ДП:\nU = ${rpVoltage}В;  Pmax = ${rpPWRmax}Вт;  I = ${"%.1f".format(listLnCurrent[1])}мА"
            Log.e("tagg", "??????????????")
        } else {
            tvRP.text = "Источник ДП:\nU = $rpVoltage В;  Pmax = $rpPWRmax Вт;  I = $rpCurrent мА"
        }
        Log.e("tagg", "end output")
    }

    // Удаление последнего регенератора
    private fun delOneRg() {
        Log.e("tagg", "delOneRg")
        if (count > 1) {
            listRgPower.removeAt(count)
            listRgVoltage.removeAt(count)
            listRgCurrent.removeAt(count)
            listLnVoltage.removeAt(count)
            listLnCurrent.removeAt(count)
            listLnResist.removeAt(count)
            listRgName.removeAt(count)
            listStr.removeAt(count*2-1)
            listStr.removeAt(count*2-2)
            count -= 1
            calculate()
            output()
        } else {
            if (count == 1) {
                cleanAll()
            }
        }
        Log.e("tagg", "end delOneRg")
    }

    // Удаление записей из массивов
    private fun cleanAll() {
        Log.e("tagg", "cleanAll")
        if (count>0) {
            this.adapter1.clear()
            init()
        }
        Log.e("tagg", "end cleanAll")
    }

    // Округление
//    private fun rnd(num:Double): String {
//        Log.e("tagg", "rnd $num")
//        return "%.1f".format(num)
//        return round(num).toString()
//    }

    //    Проверка на ошибки
    private fun checkError () {
        if (count>0) {
            if (listLnVoltage[1] + listRgVoltage[1] > rpVoltage+0.1) {
                tvRP.setTextColor(resources.getColor(R.color.red))
                Toast.makeText(this, "Низкое напряжение ДП (1)", Toast.LENGTH_SHORT).show()
                error = 1
                btnAddRG.isEnabled=false
                return
            } else {
                if (listRgVoltage[count] < rgVoltageMin) {
                    tvRP.setTextColor(resources.getColor(R.color.red))
                    Toast.makeText(this, "Низкое напряжение ДП (2)", Toast.LENGTH_SHORT).show()
                    error = 2
                    btnAddRG.isEnabled = false
                    return
                } else {
                    if (rpVoltage * listLnCurrent[1] / 1000 > rpPWRmax) {
                        tvRP.setTextColor(resources.getColor(R.color.red))
                        Toast.makeText(this, "Перегрузка источника ДП (3)", Toast.LENGTH_SHORT)
                            .show()
                        error = 3
                        btnAddRG.isEnabled = false
                        return
                    } else {
                        error = 0
                        tvRP.setTextColor(resources.getColor(R.color.black))
                        btnAddRG.isEnabled=true
                        return
                    }
                }
            }
        }
        error = 0
        tvRP.setTextColor(resources.getColor(R.color.black))
        btnAddRG.isEnabled=true
    }

    // Инициализация
    private fun init() {
        Log.e("tagg", "init")
        listRgName = mutableListOf("РЛ №0")
        listRgPower = mutableListOf(5.5)
        listRgVoltage = mutableListOf(rgVoltageMin.toDouble())
        listRgCurrent = mutableListOf(0.0)
        listLnVoltage = mutableListOf(0.0)
        listLnCurrent = mutableListOf(0.0)
        listLnResist = mutableListOf(159)
        listStr = mutableListOf()

        count = 0
        error = 0

        tvRP.setTextColor(resources.getColor(R.color.black))
        btnAddRG.isEnabled=true
        tvRP.text = "Источник ДП:\nU = $rpVoltage В;  Pmax = $rpPWRmax Вт;  I = 0 мА"
        Log.e("tagg", "end init")
    }

// Создание главного меню
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "Справка")
        menu?.add(0, 2, 0, "Очистить")
        menu?.add(0, 3, 0, "Выход")

        return super.onCreateOptionsMenu(menu)
    }

// Выбор пунктов главного меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                val i = Intent(this, activity_settings_rp::class.java)
                startActivity(Intent(this, about::class.java))
            }
            2 -> {
            }
            3 -> {
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

// Создание контекстного меню
    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        when (v?.id) {
            R.id.tvRP -> {
                menu?.add (0, 1, 0, "Настроить параметры ДП")
            }
            R.id.lvList -> {
                menu?.add(0, 2, 0, "Настроить параметры РЛ")
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo)
    }

// Действия при выборе пунктов контекстного меню
    override fun onContextItemSelected(item: MenuItem): Boolean {
        var i: Intent
        when(item.itemId) {
            1 -> {
                i = Intent(this, activity_settings_rp::class.java)
                i.putExtra("URP", rpVoltage.toString())
                i.putExtra("PRP", rpPWRmax.toString())
                startActivityForResult(i, 101)
            }
            2 -> {
                i = Intent(this, activity_settings_rg::class.java)
                i.putExtra("rgRESIST", listRgResist[count].toString())
                i.putExtra("rgPWR", listRgPower[count].toString())
                startActivityForResult(i, 102)
            }
        }
        return super.onContextItemSelected(item)
    }

    // Приём данных из других активити
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) {
            return
        }
        when(requestCode) {
            101 -> {
                rpVoltage = data.getStringExtra("URP")!!.toInt()
                rpPWRmax = data.getStringExtra("PRP")!!.toInt()
            }
            102 -> {
                listRgResist[count] = data.getStringExtra("rgRESIST")!!.toInt()
                listRgPower[count] = data.getStringExtra("rgPWR")!!.toDouble()
            }
        }
        if (requestCode < 51 && requestCode % 2 == 1) {
            listRgPower[(requestCode+1)/2] = data.getStringExtra("rgPWR")!!.toDouble()
            listRgResist[(requestCode+1)/2] = data.getStringExtra("rgRESIST")!!.toInt()
        }
        if (requestCode < 51 && requestCode % 2 == 0) {
            listLnResist[(requestCode+2)/2] = data.getStringExtra("lnRESIST")!!.toInt()
        }
        calculate()
        checkError()
        output()
    }
}