import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

typealias Env = PersistentMap<String, Value>

sealed class Value {
    data class Integer(val n: Int) : Value()
    data class Bool(val b: Boolean) : Value()
    data class Closure(val env: Env, val param: String, val body: Expr) : Value()
}

fun closureEval(expr: Expr): Value {
    return eval(persistentMapOf(), expr)
}

fun eval(env: Env, expr: Expr): Value {
    return when (expr) {
        is Expr.App -> {
            when (val closure = eval(env, expr.func)) {
                is Value.Closure -> {
                    val arg = eval(env, expr.arg)
                    val newEnv = closure.env.put(closure.param, arg)
                    eval(newEnv, closure.body)
                }

                is Value.Integer, is Value.Bool -> throw Error("${closure} is not a function")
            }
        }

        is Expr.Lambda -> Value.Closure(env, expr.param, expr.body)
        is Expr.Lit -> when (val prim = expr.p) {
            is Primitive.Bool -> Value.Bool(prim.b)
            is Primitive.Integer -> Value.Integer(prim.n)
        }

        is Expr.Var -> env[expr.n] ?: throw Exception("Unbound variable ${expr.n}")
        is Expr.If -> {
            val cond = eval(env, expr.condition) as? Value.Bool ?: throw Error("Is not a boolean")
            if (cond.b) {
                eval(env, expr.thenBranch)
            } else {
                eval(env, expr.elseBranch)
            }

        }

        is Expr.Binary -> {
            val left = eval(env, expr.left)
            val right = eval(env, expr.right)
            when (expr.op) {
                Operator.Add ->
                    evalBinary<Value.Integer>(left, right) { l, r -> Value.Integer(l.n + r.n) }

                Operator.Sub ->
                    evalBinary<Value.Integer>(left, right) { l, r -> Value.Integer(l.n - r.n) }

                Operator.Mul ->
                    evalBinary<Value.Integer>(left, right) { l, r -> Value.Integer(l.n * r.n) }

                Operator.Eq ->
                    evalBinary<Value.Integer>(left, right) { l, r -> Value.Bool(l.n == r.n) }

                Operator.Or ->
                    evalBinary<Value.Bool>(left, right) { l, r -> Value.Bool(l.b || r.b) }
            }
        }
    }
}

inline fun <reified T> evalBinary(left: Value, right: Value, f: (T, T) -> Value): Value {
    val leftCasted = left as? T ?: throw Error("Expected a ${T::class.simpleName} but got $left")
    val rightCasted = right as? T ?: throw Error("Expected a ${T::class.simpleName} but got $right")
    return f(leftCasted, rightCasted)
}