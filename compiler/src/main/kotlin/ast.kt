data class Prog(val typeDefs: List<TypeDef>, val fnDefs: List<FnDef>, val expr: Expr)

data class FnDef(val name: String, val expr: Expr, val ty: Polytype)
data class TypeDef(val name: String, val constructors: List<TypeConstructor>)
data class TypeConstructor(val name: String, val fields: List<Monotype>)

sealed class Expr {
    data class Var(val n: String) : Expr()
    data class Lambda(val param: String, val tyParam: Monotype?, val body: Expr) : Expr()
    data class App(val func: Expr, val arg: Expr) : Expr()
    data class If(val condition: Expr, val thenBranch: Expr, val elseBranch: Expr) : Expr()
    data class Binary(val left: Expr, val op: Operator, val right: Expr) : Expr()
    data class Let(val name: String, val bound: Expr, val body: Expr) : Expr()
    data class Lit(val p: Primitive) : Expr()
    data class Construction(val type: String, val name: String, val fields: List<Expr>) : Expr()
    data class Case(val scrutinee: Expr, val branches: List<CaseBranch>) : Expr()

    // Used to implement built-in functions in the evaluator,
    // doesn't exist at the syntax level
    data class Builtin(val name: String) : Expr()
}

data class CaseBranch(val pattern: Pattern, val body: Expr)
sealed class Pattern {
    data class Constructor(val type: String, val name: String, val fields: List<String>) : Pattern()
}

sealed class Primitive {
    data class Integer(val value: Int) : Primitive()
    data class Bool(val value: Boolean) : Primitive()
    data class Text(val value: String) : Primitive()
}

enum class Operator {
    Add, Sub, Mul, Div, Eq, Or, And, Concat
}

sealed class Monotype {
    object Integer : Monotype()
    object Text : Monotype()
    object Bool : Monotype()
    data class Constructor(val name: String) : Monotype()
    data class Var(val name: String) : Monotype()
    data class Function(val arg: Monotype, val result: Monotype) : Monotype()
    data class Unknown(val u: Int) : Monotype()

    fun print(): String = printInner(false)

    private fun printInner(nested: Boolean): String {
        return when (this) {
            Bool -> "Bool"
            Integer -> "Integer"
            Text -> "Text"
            is Constructor -> name
            is Var -> name
            is Function -> {
                val inner = "${arg.printInner(true)} -> ${result.printInner(false)}"
                if (nested) "($inner)" else inner
            }

            is Unknown -> "u$u"
        }
    }

    fun substitute(v: String, replacement: Monotype): Monotype {
        return when (this) {
            Bool, is Constructor, Integer, Text, is Unknown -> this
            is Function -> Function(arg.substitute(v, replacement), result.substitute(v, replacement))
            is Var -> if (v == name) { replacement } else { this }
        }
    }

    fun unknowns(): Set<Int> {
        return when (this) {
            Bool, is Constructor, is Var, Integer, Text -> setOf()
            is Function -> arg.unknowns().union(result.unknowns())
            is Unknown -> setOf(u)
        }
    }
}

data class Polytype(val vars: List<String>, val type: Monotype) {
    fun unknowns(): Set<Int> = type.unknowns()

    fun pretty(): String {
        if (vars.isEmpty()) return type.print()
        return "forall " + vars.joinToString(" ") + ". " + type.print()
    }

    companion object {
        fun fromMono(type: Monotype): Polytype {
            return Polytype(listOf(), type)
        }
    }
}



















