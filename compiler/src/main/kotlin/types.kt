import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf

typealias Context = PersistentMap<String, Type>

class Typechecker {

    var typeDefs: List<TypeDef> = listOf()
    val errors: MutableList<String> = mutableListOf()

    fun error(msg: String) = errors.add(msg)

    fun equalType(msg: String, actual: Type, expected: Type) {
        if (actual != expected) {
            error("$msg, Expected ${expected.print()} but got ${actual.print()}")
        }
    }

    fun inferProg(prog: Prog): Pair<Type, List<String>> {
        typeDefs = prog.typeDefs
        val builtinCtx: Context = builtIns.fold(persistentHashMapOf()) { acc, def ->
            acc.put(def.name, def.type)
        }
        val ctx: Context = prog.fnDefs.fold(builtinCtx) { acc, def ->
            acc.put(def.name, def.ty)
        }
        prog.fnDefs.forEach { def ->
            val tyExpr = infer(ctx, def.expr)
            equalType("When inferring a definition", tyExpr, def.ty)
        }
        return infer(ctx, prog.expr) to errors
    }

    fun infer(ctx: Context, expr: Expr): Type {
        return when (expr) {
            is Expr.App -> {
                val tyFun = infer(ctx, expr.func)
                val tyArg = infer(ctx, expr.arg)
                when (tyFun) {
                    is Type.Function -> {
                        equalType("when applying a function", tyArg, tyFun.arg)
                        tyFun.result
                    }

                    else -> throw Error("${tyFun.print()} is not a function")
                }
            }

            is Expr.Binary -> {
                val (left, right, result) = when (expr.op) {
                    Operator.Add,
                    Operator.Sub,
                    Operator.Mul,
                    Operator.Div -> Triple(Type.Integer, Type.Integer, Type.Integer)

                    Operator.Eq -> Triple(Type.Integer, Type.Integer, Type.Bool)

                    Operator.Or,
                    Operator.And -> Triple(Type.Bool, Type.Bool, Type.Bool)

                    Operator.Concat -> Triple(Type.Text, Type.Text, Type.Text)
                }
                val tyLeft = infer(ctx, expr.left)
                val tyRight = infer(ctx, expr.right)
                equalType("as the left operand of ${expr.op}", tyLeft, left)
                equalType("as the right operand of ${expr.op}", tyRight, right)
                result
            }

            is Expr.Builtin -> throw Error("Should not need to infer a Builtin")
            is Expr.If -> {
                val tyCond = infer(ctx, expr.condition)
                equalType("In an If condition", tyCond, Type.Bool)
                val tyThen = infer(ctx, expr.thenBranch)
                val tyElse = infer(ctx, expr.elseBranch)
                equalType("In if branches", tyElse, tyThen)
                tyThen
            }

            is Expr.Lambda -> {
                val tyParam = expr.tyParam
                val newCtx = ctx.put(expr.param, tyParam)
                val tyBody = infer(newCtx, expr.body)
                Type.Function(tyParam, tyBody)
            }

            is Expr.Let -> {
                val tyBound = infer(ctx, expr.bound)
                val newCtx = ctx.put(expr.name, tyBound)
                val tyBody = infer(newCtx, expr.body)
                tyBody
            }

            is Expr.Lit -> when (expr.p) {
                is Primitive.Bool -> Type.Bool
                is Primitive.Integer -> Type.Integer
                is Primitive.Text -> Type.Text
            }

            is Expr.Var -> ctx[expr.n] ?: throw Error("Unknown variable ${expr.n}")
            is Expr.Construction -> {
                val tyFields = expr.fields.map { infer(ctx, it) }
                lookupConstructor(expr.type, expr.name, tyFields).forEach { (actual, expected) ->
                    equalType("", actual, expected)
                }
                return Type.Constructor(expr.type)
            }
            is Expr.Case -> {
                val tyScrutinee = infer(ctx, expr.scrutinee)
                expr.branches.map {
                    val newCtx: Context = matchPattern(ctx, it.pattern, tyScrutinee)
                    infer(newCtx, it.body)
                }.reduce { ty1, ty2 ->
                    equalType("", ty1, ty2)
                    ty1
                }
            }
        }
    }

    private fun matchPattern(ctx: Context, pattern: Pattern, ty: Type): Context {
        return when (pattern) {
            is Pattern.Constructor -> {
                equalType("", Type.Constructor(pattern.type), ty)
                lookupConstructor(pattern.type, pattern.name, pattern.fields).fold(ctx) { acc, (field, ty) ->
                    acc.put(field, ty)
                }
            }
        }
    }

    private fun <X> lookupConstructor(type: String, name: String, xs : List<X>): List<Pair<X, Type>> {
        val typeDef = typeDefs.find { it.name == type } ?: throw Error("Unknown type $type")
        val constr = typeDef.constructors.find { it.name == name } ?: throw Error("Unknown constructor $type.$name")
        if (xs.size != constr.fields.size) {
            throw Error("Mismatched fields for $type.$name, expected ${constr.fields.size} but got ${xs.size}")
        }
        return xs.zip(constr.fields)
    }

}