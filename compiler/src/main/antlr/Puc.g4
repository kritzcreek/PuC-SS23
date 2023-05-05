grammar Puc;

init: expr;

atom
  : NAME # Var
  | INT # IntLit
  | BOOL_LIT # BoolLit
  | 'fn' NAME '=>' expr # Lambda
  | atom '(' expr ')' # App
  | '(' expr ')' # Parenthesized
  | 'if' expr 'then' expr 'else' expr # If
  ;

expr
  : atom # Unary
  | expr '*' expr # Binary
  | expr ('+' | '-') expr # Binary
  | expr '==' expr # Binary
  | expr '&&' expr # Binary
  | expr '||' expr # Binary
  ;

BOOL_LIT: 'false' | 'true';
INT: [0-9]+;
NAME: [a-zA-Z]+;
WS: [ \t\r\n] -> skip;