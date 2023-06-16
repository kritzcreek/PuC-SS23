grammar Puc;

init: prog;

prog: (fnDef | typeDef)* expr;

tyVars: '<' (NAME (',' NAME)*)? '>';
fnDef: 'def' tyVars? name=NAME '(' fnParam (',' fnParam)* ')' ':' tyResult=type '=>' body=expr;
typeDef: 'type' name=UP_NAME '=' typeConstructor ('|' typeConstructor)*;
typeConstructor: constr=UP_NAME '(' (type (',' type)*)? ')';
fnParam: param=NAME ':' tyParam=type;

atom
  : NAME # Var
  | INT # IntLit
  | BOOL_LIT # BoolLit
  | TEXT_LIT # TextLit
  | 'fn' param=NAME (':' tyParam=type)? '=>' body=expr # Lambda
  | fn=atom '(' arg=expr ')' # App
  | '(' inner=expr ')' # Parenthesized
  | 'if' condition=expr 'then' thenBranch=expr 'else' elseBranch=expr # If
  | 'let' NAME '=' bound=expr 'in' body=expr # Let
  | typ=UP_NAME '.' constr=UP_NAME '(' (expr (',' expr)*)? ')' # Construction
  | 'case' scrutinee=expr '{' branches=caseBranch+ '}' # Case
  ;

caseBranch: 'of' pattern '=>' body=expr;
pattern: typ=UP_NAME '.' constr=UP_NAME '(' (NAME (',' NAME)*)? ')';

expr
  : atom # Unary
  | left=expr op=('*' | '/') right=expr # Binary
  | left=expr op=('+' | '-' | '++') right=expr # Binary
  | left=expr op='==' right=expr # Binary
  | left=expr op='&&' right=expr # Binary
  | left=expr op='||' right=expr # Binary
  ;

type
  : 'Integer' # TyInt
  | 'Text' # TyText
  | 'Bool' # TyBool
  | '(' inner=type ')' # TyParenthesized
  | NAME # TyVar
  | UP_NAME # TyConstructor
  | <assoc=right> arg=type '->' result=type # TyFun
  ;

TEXT_LIT: '"' ~('"')* '"';
BOOL_LIT: 'false' | 'true';
INT: [0-9]+;
NAME: [a-z_][a-zA-Z_]*;
UP_NAME: [A-Z][a-zA-Z_]*;
WS: [ \t\r\n] -> skip;