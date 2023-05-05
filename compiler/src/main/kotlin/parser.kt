import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.CharStreams

fun parse(input: String): Expr {
    val lexer = PucLexer(CharStreams.fromString(input))
    val tokens = CommonTokenStream(lexer)
    val parser = PucParser(tokens)
    val tree = parser.init()
    return ExprVisitor().visit(tree)
}

class ExprVisitor: PucBaseVisitor<Expr>() {
    override fun visitVar(ctx: PucParser.VarContext): Expr {
        return Expr.Var(ctx.NAME().text)
    }

    override fun visitIntLit(ctx: PucParser.IntLitContext): Expr {
        val int = ctx.INT().text.toInt();
        return Expr.Lit(Primitive.Integer(int))
    }

    override fun visitBoolLit(ctx: PucParser.BoolLitContext): Expr {
        val bool = ctx.BOOL_LIT().text == "true"
        return Expr.Lit(Primitive.Bool(bool))
    }

    override fun visitLambda(ctx: PucParser.LambdaContext): Expr {
        val param = ctx.NAME().text
        val body = this.visit(ctx.expr())
        return Expr.Lambda(param, body)
    }

    override fun visitApp(ctx: PucParser.AppContext): Expr {
        val fn = this.visit(ctx.atom())
        val arg = this.visit(ctx.expr())
        return Expr.App(fn, arg)
    }

    override fun visitParenthesized(ctx: PucParser.ParenthesizedContext): Expr {
        return this.visit(ctx.expr())
    }

    override fun visitIf(ctx: PucParser.IfContext): Expr {
        val condition = this.visit(ctx.expr(0))
        val thenBranch = this.visit(ctx.expr(1))
        val elseBranch = this.visit(ctx.expr(2))
        return Expr.If(condition, thenBranch, elseBranch)
    }

    override fun visitBinary(ctx: PucParser.BinaryContext): Expr {
        val left = this.visit(ctx.expr(0))
        val op = when(ctx.getChild(1).text) {
            "+" -> Operator.Add
            "-" -> Operator.Sub
            "*" -> Operator.Mul
            "==" -> Operator.Eq
            "||" -> Operator.Or
            "&&" -> Operator.And
            else -> throw Error("Unknown operator")
        }
        val right = this.visit(ctx.expr(1))
        return Expr.Binary(left, op, right)
    }
}