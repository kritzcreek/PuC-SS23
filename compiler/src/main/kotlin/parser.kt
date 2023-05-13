import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.CharStreams

fun parseString(input: String): Prog {
    val lexer = PucLexer(CharStreams.fromString(input))
    return parseInner(lexer)
}

fun parseFile(fileName: String): Prog {
    val lexer = PucLexer(CharStreams.fromFileName(fileName))
    return parseInner(lexer)
}

fun parseInner(lexer: PucLexer): Prog {
    val tokens = CommonTokenStream(lexer)
    val parser = PucParser(tokens)
    val tree = parser.init()
    val defs = mutableListOf<Def>()
    val expr = ExprVisitor(defs).visit(tree)
    return Prog(defs, expr)
}

class ExprVisitor(val defs: MutableList<Def>): PucBaseVisitor<Expr>() {

    override fun visitDef(ctx: PucParser.DefContext): Expr {
        defs.add(Def(ctx.NAME().text, this.visit(ctx.expr())))
        return super.visitDef(ctx)
    }

    override fun visitVar(ctx: PucParser.VarContext): Expr {
        return Expr.Var(ctx.NAME().text)
    }

    override fun visitIntLit(ctx: PucParser.IntLitContext): Expr {
        val int = ctx.INT().text.toInt()
        return Expr.Lit(Primitive.Integer(int))
    }

    override fun visitBoolLit(ctx: PucParser.BoolLitContext): Expr {
        val bool = ctx.BOOL_LIT().text == "true"
        return Expr.Lit(Primitive.Bool(bool))
    }

    override fun visitTextLit(ctx: PucParser.TextLitContext): Expr {
        val text = ctx.TEXT_LIT().text
            .trimStart('"')
            .trimEnd('"')
            .replace("\\n", "\n")

        return Expr.Lit(Primitive.Text(text))
    }

    override fun visitLambda(ctx: PucParser.LambdaContext): Expr {
        val param = ctx.param.text
        val body = this.visit(ctx.body)
        return Expr.Lambda(param, body)
    }

    override fun visitApp(ctx: PucParser.AppContext): Expr {
        val fn = this.visit(ctx.fn)
        val arg = this.visit(ctx.arg)
        return Expr.App(fn, arg)
    }

    override fun visitParenthesized(ctx: PucParser.ParenthesizedContext): Expr {
        return this.visit(ctx.inner)
    }

    override fun visitIf(ctx: PucParser.IfContext): Expr {
        val condition = this.visit(ctx.condition)
        val thenBranch = this.visit(ctx.thenBranch)
        val elseBranch = this.visit(ctx.elseBranch)
        return Expr.If(condition, thenBranch, elseBranch)
    }

    override fun visitLet(ctx: PucParser.LetContext): Expr {
        val name = ctx.NAME().text
        val bound = this.visit(ctx.bound)
        val body = this.visit(ctx.body)

        return Expr.Let(name, bound, body)
    }

    override fun visitBinary(ctx: PucParser.BinaryContext): Expr {
        val left = this.visit(ctx.left)
        val op = when(ctx.op.text) {
            "+" -> Operator.Add
            "-" -> Operator.Sub
            "*" -> Operator.Mul
            "==" -> Operator.Eq
            "||" -> Operator.Or
            "&&" -> Operator.And
            "++" -> Operator.Concat
            else -> throw Error("Unknown operator")
        }
        val right = this.visit(ctx.right)
        return Expr.Binary(left, op, right)
    }
}