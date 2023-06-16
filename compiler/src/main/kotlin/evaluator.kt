import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentHashMap

typealias Env = PersistentMap<String, Value>

sealed class Value {
    data class Integer(val value: Int) : Value()
    data class Bool(val value: Boolean) : Value()
    data class Text(val value: String) : Value()
    data class Closure(val env: Env, val param: String, val body: Expr) : Value()
    data class Struct(val tag: String, val fields: List<Value>) : Value()

    inline fun <reified T> castAs(): T {
        return this as? T
            ?: throw Error("Expected a ${T::class.simpleName} but got ${this.javaClass.simpleName}")
    }
}

val emptyEnv: Env = persistentMapOf()

fun closureEval(prog: Prog): Value {
    return Evaluator(prog.fnDefs).eval(emptyEnv, prog.expr)
}

data class BuiltIn(val name: String, val arity: Int, val type: Monotype)

val builtIns = listOf(
    BuiltIn("int_to_string", 1, parseType("Integer -> Text")),
    BuiltIn("print", 1, parseType("Text -> Text")),
    BuiltIn("read_int", 1, parseType("Text -> Integer")),
    // TODO: Besserer Name
    BuiltIn("str_eq", 2, parseType("Text -> Text -> Bool")),
)

class Evaluator(fnDefs: List<FnDef>) {

    var topLevel: PersistentMap<String, Value>

    init {
        val topLevelMut = mutableMapOf<String, Value>()
        builtIns.forEach { builtIn ->
            val value = (2..builtIn.arity)
                .map { "param$it" }
                .fold<String, Expr>(Expr.Builtin(builtIn.name)) { acc, param ->
                    Expr.Lambda(param, Monotype.Integer, acc)
                }
            topLevelMut[builtIn.name] = Value.Closure(emptyEnv, "param1", value)
        }
        topLevel = topLevelMut.toPersistentHashMap()

        fnDefs.forEach { topLevel = topLevel.put(it.name, eval(emptyEnv, it.expr)) }
    }

    fun eval(env: Env, expr: Expr): Value {
        return when (expr) {
            is Expr.App -> {
                val closure = eval(env, expr.func).castAs<Value.Closure>()
                val arg = eval(env, expr.arg)
                val newEnv = closure.env.put(closure.param, arg)
                eval(newEnv, closure.body)
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
                val cond = eval(env, expr.condition).castAs<Value.Bool>()
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

                    Operator.Div ->
                        evalBinary<Value.Integer>(left, right) { l, r -> Value.Integer(l.value / r.value) }

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
                    val int = env["param1"]!!.castAs<Value.Integer>()
                    Value.Text(int.value.toString())
                }

                "print" -> {
                    val text = env["param1"]!!.castAs<Value.Text>()
                    println(text.value)
                    text
                }

                "read_int" -> {
                    val prompt = env["param1"]!!.castAs<Value.Text>()
                    print(prompt.value + ": ")
                    val line = readln()
                    Value.Integer(line.toInt())
                }

                "str_eq" -> {
                    val left = env["param1"]!!.castAs<Value.Text>()
                    val right = env["param2"]!!.castAs<Value.Text>()
                    Value.Bool(left.value == right.value)
                }

                else -> throw Error("Unknown built-in ${expr.name}")
            }

            is Expr.Let -> {
                val bound = eval(env, expr.bound)
                eval(env.put(expr.name, bound), expr.body)
            }

            is Expr.Construction -> {
                val fields = expr.fields.map { eval(env, it) }
                Value.Struct("${expr.type}.${expr.name}", fields)
            }

            is Expr.Case -> {
                val scrutinee = eval(env, expr.scrutinee).castAs<Value.Struct>()
                for (branch in expr.branches) {
                    val bindings = matchPattern(branch.pattern, scrutinee)
                    if (bindings != null) {
                        val newEnv =
                            bindings.fold(env) { acc, (name, value) ->
                                acc.put(name, value)
                            }
                        return eval(newEnv, branch.body)
                    }
                }
                throw Error("Failed to match $scrutinee")
            }
        }
    }

    fun matchPattern(pattern: Pattern, scrutinee: Value.Struct): List<Pair<String, Value>>? {
        when (pattern) {
            is Pattern.Constructor -> {
                val tag = "${pattern.type}.${pattern.name}"
                if (tag == scrutinee.tag) {
                    if (scrutinee.fields.size == pattern.fields.size) {
                        return pattern.fields.zip(scrutinee.fields)
                    } else {
                        throw Error("Mismatched field count in pattern: expected: ${pattern.fields.size}, actual: ${scrutinee.fields.size}")
                    }
                }
            }
        }
        return null
    }

    inline fun <reified T> evalBinary(left: Value, right: Value, f: (T, T) -> Value): Value =
        f(left.castAs<T>(), right.castAs<T>())
}


