import kotlinx.collections.immutable.persistentMapOf

// Syntax
sealed class Expr {
    data class Var(val n: String) : Expr()
    data class Lambda(val param: String, val body: Expr) : Expr()
    data class App(val func: Expr, val arg: Expr) : Expr()

    // Nicht zwingend notwendig
    data class Lit(val n: Int) : Expr()
}

// (\f => \y => f y)(\x => y)
// (\y => (\x => y) y)

fun eval(expr: Expr): Expr {
    return when (expr) {
        is Expr.Lit, is Expr.Var, is Expr.Lambda -> expr
        is Expr.App -> {
            when (val left = eval(expr.func)) {
                is Expr.Lambda -> {
                    val arg = eval(expr.arg);
                    val result = substitute(left.param, arg, left.body)
                    eval(result)
                }
                is Expr.Lit, is Expr.Var, is Expr.App -> return Expr.App(left, expr.arg)
            }
        }
    }
}

fun substitute(var_name: String, replacement: Expr, expr: Expr): Expr {
    return when (expr) {
        is Expr.App -> Expr.App(
            substitute(var_name, replacement, expr.func),
            substitute(var_name, replacement, expr.arg)
        )
        is Expr.Lambda -> if (expr.param == var_name) {
            expr
        } else {
            Expr.Lambda(expr.param, substitute(var_name, replacement, expr.body))
        }

        is Expr.Lit -> expr
        is Expr.Var -> if (expr.n == var_name) {
            replacement
        } else {
            expr
        }
    }
}

fun main() {
    val expr = Expr.App(
        Expr.App(
            Expr.Lambda("x",
                Expr.Lambda("y",
                    Expr.Var("y")
                )
            ), Expr.Lit(4)
        ), Expr.Lit(2)
    )

    val brokenExpr = Expr.App(Expr.Lit(4), Expr.Lit(2))

    val result = eval(expr)
    val closureResult = closureEval(expr)
    println("${result} = ${closureResult}")

}