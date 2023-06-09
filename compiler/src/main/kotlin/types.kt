import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf

typealias Context = PersistentMap<String, Type>

class Typechecker {

    var typeDefs: List<TypeDef> = listOf()

    val errors: MutableList<String> = mutableListOf()
    fun error(msg: String) = errors.add(msg)

    var unknownSupply: Int = 0
    val solution: MutableMap<Int, Type> = mutableMapOf()

    fun freshUnknown(): Type = Type.Unknown(++unknownSupply)
    fun applySolution(ty: Type): Type {
        return when (ty) {
            Type.Bool, is Type.Constructor, Type.Integer, Type.Text -> ty
            is Type.Function -> Type.Function(applySolution(ty.arg), applySolution(ty.result))
            is Type.Unknown -> solution[ty.u]?.let { applySolution(it) } ?: ty
        }
    }

    fun equalType(msg: String, actual: Type, expected: Type) {
        try {
            unify(actual, expected)
        } catch (e : Error) {
            error("$msg, ${e.message}")
        }
    }

    fun unify(ty1: Type, ty2: Type) {
        val ty1 = applySolution(ty1)
        val ty2 = applySolution(ty2)

        if (ty1 == ty2) return
        if (ty1 is Type.Function && ty2 is Type.Function) {
            unify(ty1.arg, ty2.arg)
            unify(ty1.result, ty2.result)
        } else if (ty1 is Type.Unknown) {
            if (ty2.unknowns().contains(ty1.u)) {
                throw Error("Can't resolve infinite type ${ty1.print()} ~ ${ty2.print()}")
            }
            solution[ty1.u] = ty2
        } else if (ty2 is Type.Unknown) {
            if (ty1.unknowns().contains(ty2.u)) {
                throw Error("Can't resolve infinite type ${ty2.print()} ~ ${ty1.print()}")
            }
            solution[ty2.u] = ty1
        } else {
            throw Error("Can't unify ${ty1.print()} with ${ty2.print()}")
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
        val tyProg = infer(ctx, prog.expr)
        return applySolution(tyProg) to errors
    }

    fun infer(ctx: Context, expr: Expr): Type {
        return when (expr) {
            is Expr.App -> {
                val tyFun = infer(ctx, expr.func)
                val tyArg = infer(ctx, expr.arg)
                val tyResult = freshUnknown()
                equalType("when applying a function", tyFun, Type.Function(tyArg, tyResult))
                tyResult
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
                val tyParam = expr.tyParam ?: freshUnknown()
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

    private fun <X> lookupConstructor(type: String, name: String, xs: List<X>): List<Pair<X, Type>> {
        val typeDef = typeDefs.find { it.name == type } ?: throw Error("Unknown type $type")
        val constr = typeDef.constructors.find { it.name == name } ?: throw Error("Unknown constructor $type.$name")
        if (xs.size != constr.fields.size) {
            throw Error("Mismatched fields for $type.$name, expected ${constr.fields.size} but got ${xs.size}")
        }
        return xs.zip(constr.fields)
    }

}