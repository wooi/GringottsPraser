public inline fun <T> List<T>.takeLastWhileWithInclusive(predicate: (T) -> Boolean): List<T> {
    if (isEmpty()) return emptyList()
    val iterator = listIterator(size)
    var inclusive = false
    while (iterator.hasPrevious()) {
        val predicateCondition = predicate(iterator.previous())
        if (inclusive) {
            var next = iterator.next()
            val expectedSize = size - iterator.nextIndex()
            if (expectedSize == 0) return emptyList()
            return ArrayList<T>(expectedSize).apply {
                while (iterator.hasNext()) add(iterator.next())
            }
        }
        inclusive = !predicateCondition
    }
    return toList()
}
