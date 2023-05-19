data class Prog(val defs: List<Def>, val expr: Expr)

data class Def(val name: String, val expr: Expr, val ty: Type)

sealed class Expr {
    data class Var(val n: String) : Expr()
    data class Lambda(val param: String, val tyParam: Type, val body: Expr) : Expr()
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

sealed class Type {
    object Integer: Type()
    object Text: Type()
    object Bool: Type()
    data class Function(val arg: Type, val result: Type): Type()

    fun print(): String = printInner(false)

    private fun printInner(nested: Boolean): String {
        return when(this) {
            Bool -> "Bool"
            Integer -> "Integer"
            Text -> "Text"
            is Function -> {
                val inner = "${arg.printInner(true)} -> ${result.printInner(false)}"
                if (nested) "($inner)" else inner
            }
        }
    }
}