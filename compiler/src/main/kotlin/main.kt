fun main() {
    val parsed = parseFile("types.puc")
    val (type, errors) = Typechecker().inferProg(parsed)
    errors.forEach { println(it) }
    val evaled = closureEval(parsed)
    print("$evaled")
    println(": ${type.print()}")
}