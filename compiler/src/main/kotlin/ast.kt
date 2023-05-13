data class Prog(val defs: List<Def>, val expr: Expr)

data class Def(val name: String, val expr: Expr)

sealed class Expr {
    data class Var(val n: String) : Expr()
    data class Lambda(val param: String, val body: Expr) : Expr()
    data class App(val func: Expr, val arg: Expr) : Expr()
    data class If(val condition: Expr, val thenBranch: Expr, val elseBranch: Expr) : Expr()
    data class Binary(val left: Expr, val op: Operator, val right: Expr) : Expr()
    data class Let(val name: String, val bound: Expr, val body: Expr) : Expr()
    data class Lit(val p: Primitive) : Expr()

    // Used to implement built-in functions in the evaluator,
    // doesn't exist at the syntax level
    data class Builtin(val name: String) : Expr()
}

sealed class Primitive {
    data class Integer(val value: Int) : Primitive()
    data class Bool(val value: Boolean) : Primitive()
    data class Text(val value: String) : Primitive()
}

enum class Operator {
    Add, Sub, Mul, Eq, Or, And, Concat
}