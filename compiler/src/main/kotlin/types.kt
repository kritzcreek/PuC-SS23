import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf

typealias Context = PersistentMap<String, Type>

class Typechecker {

    val errors: MutableList<String> = mutableListOf()

    fun error(msg: String) = errors.add(msg)

    fun equalType(msg: String, actual: Type, expected: Type) {
        if (actual != expected) {
            error("$msg, Expected ${expected.print()} but got ${actual.print()}")
        }
    }

    fun inferProg(prog: Prog): Pair<Type, List<String>> {
        val builtinCtx: Context = builtIns.fold(persistentHashMapOf()) { acc, def ->
            acc.put(def.name, def.type)
        }
        val ctx: Context = prog.defs.fold(builtinCtx) { acc, def ->
            acc.put(def.name, def.ty)
        }
        prog.defs.forEach { def ->
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
                when (expr.op) {
                    Operator.Add,
                    Operator.Sub,
                    Operator.Mul,
                    Operator.Eq -> {
                        val tyLeft = infer(ctx, expr.left)
                        val tyRight = infer(ctx, expr.right)
                        equalType("as the left operand of ${expr.op}", tyLeft, Type.Integer)
                        equalType("as the right operand of ${expr.op}", tyRight, Type.Integer)
                        Type.Integer
                    }

                    Operator.Or,
                    Operator.And -> {
                        val tyLeft = infer(ctx, expr.left)
                        val tyRight = infer(ctx, expr.right)
                        equalType("as the left operand of ${expr.op}", tyLeft, Type.Bool)
                        equalType("as the right operand of ${expr.op}", tyRight, Type.Bool)
                        Type.Bool
                    }

                    Operator.Concat -> {
                        val tyLeft = infer(ctx, expr.left)
                        val tyRight = infer(ctx, expr.right)
                        equalType("as the left operand of ${expr.op}", tyLeft, Type.Text)
                        equalType("as the right operand of ${expr.op}", tyRight, Type.Text)
                        Type.Text
                    }
                }
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
        }
    }

}