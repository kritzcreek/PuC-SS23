import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import java.lang.Exception

typealias Env = PersistentMap<String, Value>

sealed class Value {
    data class Integer(val n: Int): Value()
    data class Closure(val env: Env, val param: String, val body: Expr): Value()
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
                is Value.Integer -> throw Exception("${closure.n} is not a function")
            }
        }
        is Expr.Lambda -> Value.Closure(env, expr.param, expr.body)
        is Expr.Lit -> Value.Integer(expr.n)
        is Expr.Var -> env[expr.n] ?: throw Exception("Unbound variable ${expr.n}")
    }
}