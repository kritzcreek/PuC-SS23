import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf

typealias Context = PersistentMap<String, Polytype>
typealias Solution = Map<Int, Monotype>

class Typechecker {

    var typeDefs: List<TypeDef> = listOf()

    val errors: MutableList<String> = mutableListOf()
    fun error(msg: String) = errors.add(msg)

    var unknownSupply: Int = 0
    val solution: MutableMap<Int, Monotype> = mutableMapOf()

    fun freshUnknown(): Monotype = Monotype.Unknown(++unknownSupply)
    fun applySolution(ty: Monotype, solution: Solution = this.solution): Monotype {
        return when (ty) {
            Monotype.Bool, is Monotype.Constructor, is Monotype.Var, Monotype.Integer, Monotype.Text -> ty
            is Monotype.Function -> Monotype.Function(
                applySolution(ty.arg, solution),
                applySolution(ty.result, solution)
            )

            is Monotype.Unknown -> solution[ty.u]?.let { applySolution(it) } ?: ty
        }
    }

    fun equalType(msg: String, actual: Monotype, expected: Monotype) {
        try {
            unify(actual, expected)
        } catch (e: Error) {
            error("$msg, ${e.message}")
        }
    }

    fun unify(ty1: Monotype, ty2: Monotype) {
        val ty1 = applySolution(ty1)
        val ty2 = applySolution(ty2)

        if (ty1 == ty2) return
        if (ty1 is Monotype.Function && ty2 is Monotype.Function) {
            unify(ty1.arg, ty2.arg)
            unify(ty1.result, ty2.result)
        } else if (ty1 is Monotype.Unknown) {
            if (ty2.unknowns().contains(ty1.u)) {
                throw Error("Can't resolve infinite type ${ty1.print()} ~ ${ty2.print()}")
            }
            solution[ty1.u] = ty2
        } else if (ty2 is Monotype.Unknown) {
            if (ty1.unknowns().contains(ty2.u)) {
                throw Error("Can't resolve infinite type ${ty2.print()} ~ ${ty1.print()}")
            }
            solution[ty2.u] = ty1
        } else {
            throw Error("Can't unify ${ty1.print()} with ${ty2.print()}")
        }
    }

    fun inferProg(prog: Prog): Pair<Monotype, List<String>> {
        typeDefs = prog.typeDefs
        val builtinCtx: Context = builtIns.fold(persistentHashMapOf()) { acc, def ->
            acc.put(def.name, Polytype.fromMono(def.type))
        }
        val ctx: Context = prog.fnDefs.fold(builtinCtx) { acc, def ->
            acc.put(def.name, def.ty)
        }
        prog.fnDefs.forEach { def ->
            val tyExpr = infer(ctx, def.expr)
            equalType("When inferring a definition", tyExpr, instantiate(def.ty))
        }
        ctx.forEach { println(it.key + " : " + it.value.pretty()) }

        val tyProg = infer(ctx, prog.expr)
        return applySolution(tyProg) to errors
    }

    fun infer(ctx: Context, expr: Expr): Monotype {
        return when (expr) {
            is Expr.App -> {
                val tyFun = infer(ctx, expr.func)
                val tyArg = infer(ctx, expr.arg)
                val tyResult = freshUnknown()
                equalType("when applying a function", tyFun, Monotype.Function(tyArg, tyResult))
                tyResult
            }

            is Expr.Binary -> {
                val (left, right, result) = when (expr.op) {
                    Operator.Add,
                    Operator.Sub,
                    Operator.Mul,
                    Operator.Div -> Triple(Monotype.Integer, Monotype.Integer, Monotype.Integer)

                    Operator.Eq -> Triple(Monotype.Integer, Monotype.Integer, Monotype.Bool)

                    Operator.Or,
                    Operator.And -> Triple(Monotype.Bool, Monotype.Bool, Monotype.Bool)

                    Operator.Concat -> Triple(Monotype.Text, Monotype.Text, Monotype.Text)
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
                equalType("In an If condition", tyCond, Monotype.Bool)
                val tyThen = infer(ctx, expr.thenBranch)
                val tyElse = infer(ctx, expr.elseBranch)
                equalType("In if branches", tyElse, tyThen)
                tyThen
            }

            is Expr.Lambda -> {
                val tyParam = expr.tyParam ?: freshUnknown()
                val newCtx = ctx.put(expr.param, Polytype.fromMono(tyParam))
                val tyBody = infer(newCtx, expr.body)
                Monotype.Function(tyParam, tyBody)
            }

            is Expr.Let -> {
                val tyBound = generalize(ctx, infer(ctx, expr.bound))
                println(expr.name + " : " + tyBound.pretty())
                val newCtx = ctx.put(expr.name, tyBound)
                val tyBody = infer(newCtx, expr.body)
                tyBody
            }

            is Expr.Var -> ctx[expr.n]?.let { instantiate(it) } ?: throw Error("Unknown variable ${expr.n}")

            is Expr.Lit -> when (expr.p) {
                is Primitive.Bool -> Monotype.Bool
                is Primitive.Integer -> Monotype.Integer
                is Primitive.Text -> Monotype.Text
            }

            is Expr.Construction -> {
                val tyFields = expr.fields.map { infer(ctx, it) }
                lookupConstructor(expr.type, expr.name, tyFields).forEach { (actual, expected) ->
                    equalType("", actual, expected)
                }
                return Monotype.Constructor(expr.type)
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

    private fun instantiate(ty: Polytype): Monotype {
        return ty.vars.fold(ty.type) { t, v -> t.substitute(v, freshUnknown()) }
    }

    private fun generalize(ctx: Context, ty: Monotype): Polytype {
        val ty = applySolution(ty)
        val unknowns = ty.unknowns().filterNot { u -> ctx.any { (_, ty) -> ty.unknowns().contains(u) } }
        val unknownVars = unknowns.zip('a'..'z')
        val solution: Solution = unknownVars.associate { (u, v) -> u to Monotype.Var(v.toString()) }
        return Polytype(unknownVars.map { (_, v) -> v.toString() }, applySolution(ty, solution))
    }

    private fun matchPattern(ctx: Context, pattern: Pattern, ty: Monotype): Context {
        return when (pattern) {
            is Pattern.Constructor -> {
                equalType("", Monotype.Constructor(pattern.type), ty)
                lookupConstructor(pattern.type, pattern.name, pattern.fields).fold(ctx) { acc, (field, ty) ->
                    acc.put(field, Polytype.fromMono(ty))
                }
            }
        }
    }

    private fun <X> lookupConstructor(type: String, name: String, xs: List<X>): List<Pair<X, Monotype>> {
        val typeDef = typeDefs.find { it.name == type } ?: throw Error("Unknown type $type")
        val constr = typeDef.constructors.find { it.name == name } ?: throw Error("Unknown constructor $type.$name")
        if (xs.size != constr.fields.size) {
            throw Error("Mismatched fields for $type.$name, expected ${constr.fields.size} but got ${xs.size}")
        }
        return xs.zip(constr.fields)
    }

}