// Beta Reduction
fun eval(expr: Expr): Expr {
    return when (expr) {
        is Expr.App -> {
            when (val left = eval(expr.func)) {
                is Expr.Lambda -> {
                    val arg = eval(expr.arg)
                    val result = substitute(left.param, arg, left.body)
                    eval(result)
                }

                else -> return Expr.App(left, expr.arg)
            }
        }

        else -> expr
    }
}

fun substitute(varName: String, replacement: Expr, expr: Expr): Expr {
    return when (expr) {
        is Expr.App -> Expr.App(
            substitute(varName, replacement, expr.func), substitute(varName, replacement, expr.arg)
        )

        is Expr.Lambda -> if (expr.param == varName) {
            expr
        } else {
            Expr.Lambda(expr.param, substitute(varName, replacement, expr.body))
        }

        is Expr.Var -> if (expr.n == varName) {
            replacement
        } else {
            expr
        }

        is Expr.Lit -> expr
        is Expr.Binary -> Expr.Binary(
            substitute(varName, replacement, expr.left), expr.op, substitute(varName, replacement, expr.right)
        )

        is Expr.If -> Expr.If(
            substitute(varName, replacement, expr.condition),
            substitute(varName, replacement, expr.thenBranch),
            substitute(varName, replacement, expr.elseBranch)
        )
    }
}