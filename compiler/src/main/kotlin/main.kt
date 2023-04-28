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

// Aufgabe fuer naechste Stunde:
// fib 0 = 1
// fib 1 = 1
// fib n = fib (n - 1) + fib (n - 2)

// 1 * 2 * 3 * 4
fun main() {
    val expr = Expr.App(Expr.App(z, faculty), int(10))

    val brokenExpr = or(bool(true), int(10))

    println("${closureEval(brokenExpr)}")
    val closureResult = closureEval(expr)
    println("$closureResult == ${kotlinFaculty(10)}")

}