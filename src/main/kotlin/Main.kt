fun main() {
    val cvsMap = WaCaiDB().process()
    CSVMaker("CNY").make(cvsMap)
}



