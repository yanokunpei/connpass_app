package item.java_conf.gr.jp.connpass_viewer.entity

import android.util.Log
import item.java_conf.gr.jp.connpass_viewer.Http
import item.java_conf.gr.jp.connpass_viewer.Setting
import java.io.Serializable
import java.util.*


val REQUEST_COUNT = 10
class ConnpassRequest : Serializable {
  enum class SearchRange(var days: Int) {
    ONE_WEEK(7),
    TWO_WEEK(14),
    ONE_MONTH(30),
    UNLIMITED(0)
  }

  var event_id: Array<Int>? = null
  var keyword: List<String>? = null
  var keyword_is_or: Boolean = false
  var start_date: Calendar? = null
  var date_range: SearchRange = SearchRange.UNLIMITED
  var nickname: List<String>? =null
  var series_id: Array<Int>? = null
  var start: Int = 1
  var order: Int = 2   //1: 更新日時順,2: 開催日時順,3: 新着順

  var finished = false

  private val baseUrl = "https://connpass.com/api/v1/event/?"


  interface Result {
    fun onSuccess(list: List<Event>)
    fun onError()
  }
  fun getList(cb: Result) {
    if(finished) return
    val http = Http()
    http.setCallback(object : Http.Callback {
      override fun onSuccess(body: ConnpassResponse) {
        if(body.events.size < REQUEST_COUNT) finished = true
        else start += REQUEST_COUNT
        val list = ArrayList<Event>()
        body.events.forEach {
          if(!Setting.blackList.contains(it.series?.id)) list.add(it)
          else System.out.println("deleted!!!")
        }
        cb.onSuccess(list)
      }
      override fun onError() {
        cb.onError()
      }
    })
    http.execute(getQuery())
  }

  private fun getQuery(): String {
    return baseUrl +
        getQueryAttr(event_id, "event_id")+
        getQueryAttr(keyword, if(!keyword_is_or) "keyword" else "keyword_or") +
        getDateQuery() +
        getQueryAttr(nickname, "nickname") +
        getQueryAttr(series_id, "series_id") +
        if(start != 1) "start=" + start.toString() + "&" else {""} +
        "order=" + order.toString() + "&" +
        "count=" + REQUEST_COUNT.toString()
  }

  private fun getDateQuery(): String {
    fun getDateRange(range: SearchRange, start: Calendar = Calendar.getInstance()): List<String>? {
      if(range == SearchRange.UNLIMITED) return null
        fun getDateString(cal: Calendar): String {
          val month_ = "0" + (cal.get(Calendar.MONTH) +1).toString()
          val day_ = "0" + cal.get(Calendar.DAY_OF_MONTH).toString()
          return cal.get(Calendar.YEAR).toString() + month_.slice(month_.length-2..month_.length-1) + day_.slice(day_.length-2..day_.length-1)
        }
        val list: MutableList<String> = mutableListOf()
        for(i in 0..range.days) {
          list.add(getDateString(start))
          start.add(Calendar.DAY_OF_MONTH, 1)
        }
      return list
    }
    if(date_range == SearchRange.UNLIMITED) return ""
    if(start_date != null) return getQueryAttr(getDateRange(date_range, start_date!!), "ymd")
    return getQueryAttr(getDateRange(date_range), "ymd")
  }

  private fun getQueryAttr(list: List<String>?, name: String): String {
    if(isNotEmptyList(list)) {
      var query = name + "="
      list?.forEach { if(it != "")query += it + "," }
      if (query.last() == ',') query = query.substring(0, query.length - 1)
      return query + "&"
    }
    return ""
  }
  private fun isNotEmptyList(array: List<String>?): Boolean {
    array?.forEach { if(it != "") return true }
    return false
  }
  private fun getQueryAttr(array: Array<Int>?, name: String): String {
    if(array != null && array.isNotEmpty()) {
      var query = name + "="
      array.forEach { query += it.toString() + "," }
      if (query.last() == ',') query = query.substring(0, query.length - 1)
      return query + "&"
    }
    return ""
  }
}