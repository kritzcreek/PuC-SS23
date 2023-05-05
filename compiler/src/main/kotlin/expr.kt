// Syntax
sealed class Expr {
    data class Var(val n: String) : Expr()
    data class Lambda(val param: String, val body: Expr) : Expr()
    data class App(val func: Expr, val arg: Expr) : Expr()
    data class If(val condition: Expr, val thenBranch: Expr, val elseBranch: Expr) : Expr()
    data class Binary(val left: Expr, val op: Operator, val right: Expr) : Expr()

    data class Lit(val p: Primitive) : Expr()
}

sealed class Primitive {
    data class Integer(val n: Int) : Primitive()
    data class Bool(val b: Boolean) : Primitive()
}

enum class Operator {
    Add, Sub, Mul, Eq, Or, And
}

// Helpers for Expr construction
fun int(x: Int): Expr {
    return Expr.Lit(Primitive.Integer(x))
}

fun bool(x: Boolean): Expr {
    return Expr.Lit(Primitive.Bool(x))
}

fun eq(x: Expr, y: Expr): Expr {
    return Expr.Binary(x, Operator.Eq, y)
}

fun add(x: Expr, y: Expr): Expr {
    return Expr.Binary(x, Operator.Add, y)
}

fun sub(x: Expr, y: Expr): Expr {
    return Expr.Binary(x, Operator.Sub, y)
}

fun mul(x: Expr, y: Expr): Expr {
    return Expr.Binary(x, Operator.Mul, y)
}

fun or(x: Expr, y: Expr): Expr {
    return Expr.Binary(x, Operator.Or, y)
}