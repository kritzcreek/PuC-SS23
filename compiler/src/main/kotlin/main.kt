val x = Expr.Var("x")

// \x => f (\v => (x x) v)
val innerZ = Expr.Lambda(
    "x", Expr.App(
        Expr.Var("f"), Expr.Lambda(
            "v", Expr.App(Expr.App(x, x), Expr.Var("v"))
        )
    )
)
// Z = \f => (\x => f (\v => x x v)) (\x => f (\v => x x v)))
val z = Expr.Lambda("f", Expr.App(innerZ, innerZ))

// (\x => x x)
val innerOmega = Expr.Lambda("x", Expr.App(x, x))
// (\x => x x) (\x => x x)
val omega = Expr.App(innerOmega, innerOmega)

val isZero = Expr.Lambda("x", eq(x, int(0)))
val isOne = Expr.Lambda("x", eq(x, int(1)))
val faculty: Expr = Expr.Lambda(
    "f", Expr.Lambda(
        "n", Expr.If(
            Expr.App(isZero, Expr.Var("n")),
            int(1),
            mul(Expr.Var("n"), Expr.App(Expr.Var("f"), sub(Expr.Var("n"), int(1))))
        )
    )
)

// fac 0 = 1
// fac n = n * fac (n - 1)
fun kotlinFaculty(n: Int): Int {
    if (n == 0) {
        return 1
    }
    return n * kotlinFaculty(n - 1)
}

val n = Expr.Var("n")

val fib: Expr = Expr.Lambda("f", Expr.Lambda(
    "n", Expr.If(Expr.App(isZero, n), int(1),
        Expr.If(Expr.App(isOne, n),
            int(1),
            add(
                Expr.App(Expr.Var("f"), sub(n, int(1))),
                Expr.App(Expr.Var("f"), sub(n, int(2)))
            ))
    )
))

// Aufgabe fuer naechste Stunde:
// fib 0 = 1
// fib 1 = 1
// fib n = fib (n - 1) + fib (n - 2)
//0 -> 1
//1 -> 1
//2 -> 1 + 1 = 2
//3 -> 1 + 2 = 3
//4 -> 2 + 3 = 5
//5 -> 3 + 5 = 8

// 1 * 2 * 3 * 4
fun main() {
    val parsedExpr = parse("if true then 10 else 20")

    val textFib = """
        fn f => fn n =>
          if n == 0 || n == 1 then 1
          else f(n - 1) + f(n - 2)
    """.trimIndent()

    val parsedFib = parse(textFib)
    val fibExpr = Expr.App(Expr.App(z, parsedFib), int(6))

    println("${closureEval(fibExpr)}")

    println("$parsedExpr = ${closureEval(parsedExpr)}")

}