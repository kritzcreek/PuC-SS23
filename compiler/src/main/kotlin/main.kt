fun main() {
    val parsed = parseFile("example.puc")
    val (type, errors) = Typechecker().inferProg(parsed)
    errors.forEach { println(it) }
    println("${closureEval(parsed)} : ${type.print()}")
}