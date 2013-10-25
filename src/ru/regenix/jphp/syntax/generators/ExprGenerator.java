package ru.regenix.jphp.syntax.generators;

import ru.regenix.jphp.lexer.tokens.SemicolonToken;
import ru.regenix.jphp.lexer.tokens.Token;
import ru.regenix.jphp.lexer.tokens.expr.BraceExprToken;
import ru.regenix.jphp.lexer.tokens.expr.ExprToken;
import ru.regenix.jphp.lexer.tokens.stmt.*;
import ru.regenix.jphp.syntax.SyntaxAnalyzer;
import ru.regenix.jphp.syntax.generators.manually.BodyGenerator;
import ru.regenix.jphp.syntax.generators.manually.SimpleExprGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ExprGenerator extends Generator<ExprStmtToken> {

    public ExprGenerator(SyntaxAnalyzer analyzer) {
        super(analyzer);
    }

    public ExprStmtToken getInBraces(BraceExprToken.Kind kind, ListIterator<Token> iterator){
        Token next = nextToken(iterator);
        if (!isOpenedBrace(next, kind))
            unexpectedToken(next, BraceExprToken.Kind.toOpen(kind));

        ExprStmtToken result = analyzer.generator(SimpleExprGenerator.class)
                .getToken(nextToken(iterator), iterator, false, kind);

        if (!isClosedBrace(nextToken(iterator), kind))
            unexpectedToken(next, BraceExprToken.Kind.toClose(kind));

        return result;
    }

    protected void processIf(IfStmtToken result, ListIterator<Token> iterator){
        ExprStmtToken condition = getInBraces(BraceExprToken.Kind.SIMPLE, iterator);
        BodyStmtToken body = analyzer.generator(BodyGenerator.class).getToken(
                nextToken(iterator), iterator, EndifStmtToken.class
        );
        result.setCondition(condition);
        result.setBody(body);
    }

    protected void processWhile(WhileStmtToken result, ListIterator<Token> iterator){
        ExprStmtToken condition = getInBraces(BraceExprToken.Kind.SIMPLE, iterator);
        BodyStmtToken body = analyzer.generator(BodyGenerator.class).getToken(
                nextToken(iterator), iterator, EndwhileStmtToken.class
        );
        result.setCondition(condition);
        result.setBody(body);
    }

    protected void processDo(DoStmtToken result, ListIterator<Token> iterator){
        BodyStmtToken body = analyzer.generator(BodyGenerator.class).getToken(nextToken(iterator), iterator);
        result.setBody(body);

        Token next = nextToken(iterator);
        if (next instanceof WhileStmtToken){
            result.setCondition(getInBraces(BraceExprToken.Kind.SIMPLE, iterator));

            next = nextToken(iterator);
            if (next instanceof SemicolonToken)
                return;
        }
        unexpectedToken(next);
    }

    protected List<Token> processSimpleExpr(Token current, ListIterator<Token> iterator){
        ExprStmtToken token = analyzer.generator(SimpleExprGenerator.class).getToken(current, iterator);
        return token.getTokens();
    }

    @SuppressWarnings("unchecked")
    public ExprStmtToken getToken(Token current, ListIterator<Token> iterator,
                                  Class<? extends EndStmtToken> endToken) {
        List<Token> tokens = new ArrayList<Token>();
        do {
            if (current instanceof EndStmtToken){
                if (endToken == null)
                    unexpectedToken(current);

                if (isTokenClass(current, endToken))
                    break;

                unexpectedToken(current);
            } else if (current instanceof IfStmtToken){
                processIf((IfStmtToken)current, iterator);
                tokens.add(current);
            } else if (current instanceof WhileStmtToken){
                processWhile((WhileStmtToken)current, iterator);
                tokens.add(current);
            } else if (current instanceof DoStmtToken){
                processDo((DoStmtToken)current, iterator);
                tokens.add(current);
            } else if (current instanceof ExprToken || current instanceof FunctionStmtToken){
                if (isClosedBrace(current, BraceExprToken.Kind.BLOCK)){
                    if (endToken != null)
                        unexpectedToken(current);

                    break;
                }

                List<Token> tmp = processSimpleExpr(current, iterator);
                tokens.addAll(tmp);
                break;
            } else {
                if (!tokens.isEmpty())
                    unexpectedToken(current);

                break;
            }

            if (iterator.hasNext())
                current = nextToken(iterator);
            else
                current = null;
        } while (current != null);

        if (tokens.isEmpty())
            return null;

        return new ExprStmtToken(tokens);
    }

    @Override
    public ExprStmtToken getToken(Token current, ListIterator<Token> iterator) {
        return getToken(current, iterator, null);
    }
}