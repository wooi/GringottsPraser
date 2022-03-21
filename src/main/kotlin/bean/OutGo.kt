package bean

data class OutGo(
    val uuid: String,
    val name: String,
    val orderNo: Int,
    var parentUUid: String = ""
)







