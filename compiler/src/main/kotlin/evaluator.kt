import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentHashMap

typealias Env = PersistentMap<String, Value>

sealed class Value {
    data class Integer(val value: Int) : Value()
    data class Bool(val value: Boolean) : Value()
    data class Text(val value: String) : Value()
    data class Closure(val env: Env, val param: String, val body: Expr) : Value()
}

val emptyEnv: Env = persistentMapOf()

fun closureEval(prog: Prog): Value {
    return Evaluator(prog.defs).eval(emptyEnv, prog.expr)
}

data class BuiltIn(val name: String, val arity: Int)

val builtIns = listOf(
    BuiltIn("int_to_string", 1),
    BuiltIn("print", 1),
    BuiltIn("read_int", 1),
    // TODO: Besserer Name
    BuiltIn("str_eq", 2),
)

class Evaluator(defs: List<Def>) {

    var topLevel: PersistentMap<String, Value>

    init {
        val topLevelMut = mutableMapOf<String, Value>()
        builtIns.forEach { builtIn ->
            val value = (2..builtIn.arity)
                .map { "param$it" }
                .fold<String, Expr>(Expr.Builtin(builtIn.name)) { acc, param ->
                    Expr.Lambda(param, acc)
                }
            topLevelMut[builtIn.name] = Value.Closure(emptyEnv, "param1", value)
        }
        topLevel = topLevelMut.toPersistentHashMap()

        defs.forEach { topLevel = topLevel.put(it.name, eval(emptyEnv, it.expr)) }
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

                    is Value.Integer, is Value.Bool, is Value.Text -> throw Error("${closure} is not a function")
                }
            }

            is Expr.Lambda -> Value.Closure(env, expr.param, expr.body)
            is Expr.Lit -> when (val prim = expr.p) {
                is Primitive.Bool -> Value.Bool(prim.value)
                is Primitive.Integer -> Value.Integer(prim.value)
                is Primitive.Text -> Value.Text(prim.value)
            }

            is Expr.Var -> env[expr.n]
                ?: topLevel[expr.n]
                ?: throw Exception("Unbound variable ${expr.n}")

            is Expr.If -> {
                val cond = eval(env, expr.condition) as? Value.Bool ?: throw Error("Is not a boolean")
                if (cond.value) {
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
                        evalBinary<Value.Integer>(left, right) { l, r -> Value.Integer(l.value + r.value) }

                    Operator.Sub ->
                        evalBinary<Value.Integer>(left, right) { l, r -> Value.Integer(l.value - r.value) }

                    Operator.Mul ->
                        evalBinary<Value.Integer>(left, right) { l, r -> Value.Integer(l.value * r.value) }

                    Operator.Eq ->
                        evalBinary<Value.Integer>(left, right) { l, r -> Value.Bool(l.value == r.value) }

                    Operator.Or ->
                        evalBinary<Value.Bool>(left, right) { l, r -> Value.Bool(l.value || r.value) }

                    Operator.And ->
                        evalBinary<Value.Bool>(left, right) { l, r -> Value.Bool(l.value && r.value) }

                    Operator.Concat ->
                        evalBinary<Value.Text>(left, right) { l, r -> Value.Text(l.value + r.value) }
                }
            }

            is Expr.Builtin -> when (expr.name) {
                "int_to_string" -> {
                    val int = env["param1"]!! as? Value.Integer ?: throw Error("Expected an Int")
                    Value.Text(int.value.toString())
                }

                "print" -> {
                    val text = env["param1"]!! as? Value.Text ?: throw Error("Expected an Text")
                    println(text.value)
                    text
                }

                "read_int" -> {
                    val prompt = env["param1"]!! as? Value.Text ?: throw Error("Expected an Text")
                    print(prompt.value + ": ")
                    val line = readln()
                    Value.Integer(line.toInt())
                }

                "str_eq" -> {
                    val left = env["param1"]!! as? Value.Text ?: throw Error("Expected an Text")
                    val right = env["param2"]!! as? Value.Text ?: throw Error("Expected an Text")
                    Value.Bool(left.value == right.value)
                }

                else -> throw Error("Unknown built-in ${expr.name}")
            }

            is Expr.Let -> {
                val bound = eval(env, expr.bound)
                eval(env.put(expr.name, bound), expr.body)
            }
        }
    }

    inline fun <reified T> evalBinary(left: Value, right: Value, f: (T, T) -> Value): Value {
        val leftCasted = left as? T ?: throw Error("Expected a ${T::class.simpleName} but got $left")
        val rightCasted = right as? T ?: throw Error("Expected a ${T::class.simpleName} but got $right")
        return f(leftCasted, rightCasted)
    }
}


