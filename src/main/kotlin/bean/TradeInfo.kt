package bean

data class TradeInfo(
    val accountId: String,
    val account:String = "",
    val tradetype: Int,
    val date: Long,
    var amount: Double = 0.0,
    var transfers: String? = "",
    var description: String? = "",
    var payee: String? = "",
    var category: String = "",
    val currency: String = "",
    val isHistory:Boolean = false,
    var accountBalance:Double = 0.0

)
