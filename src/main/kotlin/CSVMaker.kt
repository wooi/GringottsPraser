import bean.TradeInfo
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.QuoteMode
import java.io.File
import java.io.FileWriter
import java.math.BigDecimal
import java.nio.file.Paths

class CSVMaker(private val lang: String) {
    private val lanMap = mapOf(
        "CNY" to arrayOf(
            "命名", "当前余额", "账户", "转账", "描述", "交易对方", "分类", "日期", "时间", "备注", "金额", "货币", "支票号码", "标签"
        )
    )

    fun make(cvsMap: MutableMap<String, Pair<BigDecimal?, MutableList<TradeInfo>>>?) {
        if(!lanMap.containsKey(lang))
            throw Exception("not support $lang")
        val outputFileName = Paths.get("assets", "wacai.csv").toAbsolutePath().toString()
        val writer = FileWriter(File(outputFileName))
        val formatHeader = CSVFormat.DEFAULT.withHeader("sep=").withEscape('"').withQuoteMode(QuoteMode.NONE)
        val formatRecord = CSVFormat.DEFAULT.withQuoteMode(QuoteMode.ALL)
        val headerPrinter = CSVPrinter(writer, formatHeader)
        headerPrinter.flush()
        CSVPrinter(writer, formatRecord).use { csvPrinter ->
            csvPrinter.printRecord(lanMap[lang]?.asIterable())
            cvsMap?.forEach {
                val name = it.key
                val balance = it.value.first
                csvPrinter.printRecord(name, balance, "CNY", "", "", "", "", "", "", "", "", "", "", "")
                it.value.second.forEach { t->
                    val pair = getDateTime(t.date)

                    csvPrinter.printRecord( "", "",name, t.transfers, t.description, "", t.category, pair.first, pair.second, "", t.amount, lang, "")

                }
            }

            csvPrinter.flush()
            csvPrinter.close()
        }

        headerPrinter.close()
    }


}