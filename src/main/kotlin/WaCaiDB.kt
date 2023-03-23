import bean.AccountInfo
import bean.InCome
import bean.OutGo
import bean.TradeInfo
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Paths
import java.sql.*

class WaCaiDB {
    fun process(): MutableMap<String, Pair<BigDecimal?, MutableList<TradeInfo>>>? {
        val conn = connect()
        return conn?.run {
            val accountMap = getAccountList(this)
            val outGoMap = getOutGoCateList(this)
            val incomeMap = getIncomeCateList(this)
            val historyBalance = getBalanceHistory(this)
            val tradeInfoMap = getTradList(this, accountMap, outGoMap, incomeMap, historyBalance)
            disConnect(conn)
            convertToCSV(accountMap, tradeInfoMap)
        }
    }

    private fun convertToCSV(
        accountMap: Map<String, AccountInfo>,
        tradeInfoMap: MutableMap<String, MutableList<TradeInfo>>): MutableMap<String, Pair<BigDecimal?, MutableList<TradeInfo>>> {
        val cvsMap = mutableMapOf<String, Pair<BigDecimal?, MutableList<TradeInfo>>>()
        accountMap.filterNot { it.value.isDelete }.forEach { it ->
            val account = it.value.name
            val accountId = it.value.uuid
            val originList = tradeInfoMap[accountId]
            if (originList != null) {
                var total = 0.0
                for(trade in originList) {
                    if (trade.isHistory) {
                        val adjustAmount = BigDecimal(trade.amount) - BigDecimal(total)
                        total = trade.amount
                        trade.amount = adjustAmount.toDouble()
                    } else {
                        total += BigDecimal(trade.amount).toDouble()
                    }
                }
            }
            val balanceList = originList?.map { BigDecimal(it.amount) }
            val balance =
                balanceList?.fold(BigDecimal.ZERO) { acc, e -> acc + e }.let { it?.setScale(2, RoundingMode.HALF_EVEN) }
            println("account:$account balance:${balance}")
            if (balance != null) {
                val pair = Pair(balance, originList!!)
                cvsMap[account] = pair
            }
        }
        return cvsMap
    }

    private fun connect(): Connection? {
        val path = Paths.get("assets", "wacai.db").toAbsolutePath().toString()
        var conn: Connection? = null
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:$path")
        } catch (e: SQLException) {
            println(e.message.toString())
        }
        return conn
    }

    private fun disConnect(conn: Connection?) {
        try {
            conn?.close()
        } catch (ex: SQLException) {
            println(ex.message.toString())
        }
    }


    private fun getTradList(
        conn: Connection,
        accountMap: Map<String, AccountInfo>,
        outGoMap: Map<String, OutGo>,
        incomeMap: Map<String, InCome>,
        historyBalance: MutableMap<String, MutableList<TradeInfo>>
    ): MutableMap<String, MutableList<TradeInfo>> {
        val tradeInfoMap = mutableMapOf<String, MutableList<TradeInfo>>()
        val transferTargetList = mutableListOf<TradeInfo>()
        accountMap.forEach {
            val accountId = it.key
            val sql = "SELECT * FROM  TBL_TRADEINFO WHERE accountUuid='$accountId' AND isdelete=0"
            val stmt: Statement = conn.createStatement()
            val rs: ResultSet = stmt.executeQuery(sql)
            val list = mutableListOf<TradeInfo>()
            while (rs.next()) {
                val tradeType = rs.getInt("tradetype")
                val date = rs.getLong("date")
                var amount = rs.getLong("money") / 100.00
                val amount2 = rs.getLong("money2") / 100.00
                val accountUuid2 = rs.getString("accountUuid2")
                var transfers = accountMap[accountUuid2]?.name ?: ""
                val description = rs.getString("comment")
//            val peyee = rs.getString("TODO")
                var typeUuid = rs.getString("typeUuid") //1-支出 2-收入 3-转账 4-借入/借出 5-收款/还款
                var cate = ""
                var account = it.value.name
                when (tradeType) {
                    1 -> {
                        amount = -amount
                        while (outGoMap.containsKey(typeUuid)) {
                            val outgo = outGoMap[typeUuid]
                            cate = if (outgo!!.parentUUid.isEmpty()) {
                                typeUuid = ""
                                outgo.name + cate
                            } else {
                                typeUuid = outgo.parentUUid
                                "${cate}▶${outgo.name}"
                            }
                        }
                    }
                    2 -> {
                        cate = incomeMap[typeUuid]?.name.toString()
                    }
                    3 -> {
                        amount = -amount
                        transfers = accountMap[accountUuid2]!!.name
                    }
                    4 -> {
                        if (typeUuid == "1") { //借出
                            amount = -amount
                            transfers = accountMap[accountUuid2]!!.name
                        } else if (typeUuid == "0") {//借入
                            transfers = accountMap[accountUuid2]!!.name
                        }
                    }
                    5 -> {
                        if (typeUuid == "1") {//还款
                            amount = -amount
                            transfers = accountMap[accountUuid2]!!.name
                        } else if (typeUuid == "0") {//收款
                            transfers = accountMap[accountUuid2]!!.name
                        }
                    }
                }
                val tradeInfo = TradeInfo(
                    accountId = accountId,
                    account = account,
                    tradetype = tradeType,
                    date = date,
                    amount = amount,
                    transfers = transfers,
                    description = description,
                    category = cate
                )
                if (tradeType == 3 || tradeType == 4 || tradeType == 5) {
                    account = transfers.also { transfers = account }
                    amount = if (amount2 > 0) amount2 else -amount
                    val targetTrade = tradeInfo.copy(
                        accountId = accountUuid2, account = account, transfers = transfers, amount = amount
                    )
                    transferTargetList.add(targetTrade)
                }
                list.add(tradeInfo)
            }
            if (list.isNotEmpty()) {
                val historyList = historyBalance[accountId]
                if (historyList != null) {
                    list.addAll(historyList)
                }
                tradeInfoMap[accountId] = list
            }
        }
        //inset transfer record
        transferTargetList.forEach { tradeinfo ->
            if (tradeInfoMap.containsKey(tradeinfo.accountId)) {
                tradeInfoMap[tradeinfo.accountId]?.add(tradeinfo)
            } else {
                tradeInfoMap[tradeinfo.accountId] = mutableListOf<TradeInfo>().also { it.add(tradeinfo) }
            }
        }
        tradeInfoMap.forEach { it ->
            it.value.sortBy { it.date }
        }
        return tradeInfoMap

    }

    private fun getAccountList(conn: Connection): Map<String, AccountInfo> {
        val sql = "SELECT * FROM TBL_ACCOUNTINFO"
        val stmt: Statement = conn.createStatement()
        val rs: ResultSet = stmt.executeQuery(sql)
        val accountMap = mutableMapOf<String, AccountInfo>()
        while (rs.next()) {
            val uuid = rs.getString("uuid")
            val name = rs.getString("name")
            val isDelete = rs.getBoolean("isDelete")
            val balance = rs.getLong("balance")
            val accountInfo = AccountInfo(uuid, name, isDelete, balance / 100.00)
            accountMap[uuid] = accountInfo
//        println(accountInfo)
        }
        return accountMap
    }


    private fun getOutGoCateList(conn: Connection): Map<String, OutGo> {
        val sql = "SELECT * FROM TBL_OUTGOMAINTYPEINFO"
        val stmt: Statement = conn.createStatement()
        val rs: ResultSet = stmt.executeQuery(sql)
        val outGoMap = mutableMapOf<String, OutGo>()
        while (rs.next()) {
            val uuid = rs.getString("uuid")
            val name = rs.getString("name")
            val orderno = rs.getInt("orderno")
            val outGo = OutGo(uuid, name, orderno)
            outGoMap[uuid] = outGo
        }

        val subsql = "SELECT * FROM TBL_OUTGOSUBTYPEINFO"
        val substmt: Statement = conn.createStatement()
        val subrs: ResultSet = substmt.executeQuery(subsql)
        while (subrs.next()) {
            val uuid = subrs.getString("uuid")
            val name = subrs.getString("name")
            val orderno = subrs.getInt("orderno")
            val parentUuid = subrs.getString("parentUuid")
            val outGo = OutGo(uuid, name, orderno, parentUuid)
            outGoMap[uuid] = outGo
        }
        return outGoMap
    }

    private fun getIncomeCateList(conn: Connection): Map<String, InCome> {
        val sql = "SELECT * FROM TBL_INCOMEMAINTYPEINFO"
        val stmt: Statement = conn.createStatement()
        val rs: ResultSet = stmt.executeQuery(sql)
        val inComeMap = mutableMapOf<String, InCome>()
        while (rs.next()) {
            val uuid = rs.getString("uuid")
            val name = rs.getString("name")
            val orderno = rs.getInt("orderno")
            val income = InCome(uuid, name, orderno)
            inComeMap[uuid] = income
        }
        return inComeMap
    }

    private fun getBalanceHistory(conn: Connection): MutableMap<String, MutableList<TradeInfo>> {
        val sql = "SELECT * FROM  TBL_BALANCE_HISTORY WHERE isdelete=0"
        val stmt: Statement = conn.createStatement()
        val rs: ResultSet = stmt.executeQuery(sql)
        val balanceHistoryMap = mutableMapOf<String, MutableList<TradeInfo>>()
        while (rs.next()) {
            val account = rs.getString("account")
            val date = rs.getLong("balancedate")
            val balance = rs.getLong("balance") / 100.0
            val tradeInfo = TradeInfo(
                accountId = account, tradetype = 6, date = date, amount = balance, isHistory = true, description = "新余额"
            )
            if (balanceHistoryMap.containsKey(account)) {
                balanceHistoryMap[account]?.add(tradeInfo)
            } else {
                val list = mutableListOf<TradeInfo>()
                list.add(tradeInfo)
                balanceHistoryMap[account] = list
            }
        }
        return balanceHistoryMap
    }

}