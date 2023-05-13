fun main() {
    val parsed = parseFile("example.puc")
    println("${closureEval(parsed)}")
}