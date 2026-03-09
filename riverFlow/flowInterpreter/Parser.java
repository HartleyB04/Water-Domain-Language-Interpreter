package flowInterpreter;

import static flowInterpreter.TokenType.*;

import java.util.ArrayList;
import java.util.List;

/*
 * River Flow Language Grammar with Dams and Policy System:
 * 
 * program        → declaration* EOF ;
 * 
 * declaration    → varDecl
 *                | damDecl
 *                | statement ;
 * 
 * varDecl        → "var" IDENTIFIER "=" expression ";" ;
 * 
 * damDecl        → "dam" IDENTIFIER "|" STRING damParams "policy" policyBlock "|" inflowList ";" ;
 * 
 * damParams      → ( "capacity" MULTIPLIER )?
 *                  ( "maxflow" MULTIPLIER )?
 *                  ( "start" NUMBER )?
 *                  ( "rainfall" MULTIPLIER "[" numberList "]" )?
 *                  ( "simulate" NUMBER )? ;
 * 
 * policyBlock    → "{" policyRule* "}" ;
 * 
 * policyRule     → "threshold" NUMBER "release" NUMBER ";"
 *                | "default" "release" NUMBER ";" ;
 * 
 * inflowList     → expression ( "," expression )* ;
 * 
 * statement      → exprStmt
 *                | printStmt
 *                | block ;
 * 
 * exprStmt       → expression ";" ;
 * printStmt      → "print" expression ";" ;
 * block          → "{" declaration* "}" ;
 * 
 * expression     → assignment ;
 * 
 * assignment     → IDENTIFIER "=" assignment
 *                | flow_chain ;
 * 
 * flow_chain     → dam_or_river ( "~" dam_or_river )* ;
 * 
 * dam_or_river   → river
 *                | IDENTIFIER
 *                | "(" flow_chain ")" ;
 * 
 * river          → "@" STRING MULTIPLIER "[" numberList "]" ;
 * 
 * numberList     → NUMBER ( "," NUMBER )* ;
 * 
 * Lexical Grammar:
 * NUMBER         → DIGIT+ ( "." DIGIT+ )? ;
 * STRING         → "'" <any char except "'">* "'" ;
 * MULTIPLIER     → NUMBER "L" ;
 * IDENTIFIER     → ALPHA ( ALPHA | DIGIT )* ;
 * ALPHA          → "a" ... "z" | "A" ... "Z" | "_" ;
 * DIGIT          → "0" ... "9" ;
 * 
 * Keywords: "var", "dam", "capacity", "maxflow", "start", "rainfall", 
 *           "simulate", "policy", "threshold", "release", "default", "print"
 * 
 * Notes:
 * - Rivers/lakes use var declaration with @ prefix and string name
 * - Dams use dam declaration with | delimiters and named parameters
 * - All dam parameters are optional with sensible defaults
 * - Flow operator ~ connects rivers/lakes (water flows right)
 * - Decay pattern: 50%, 25%, 12.5%, 6.25%, 3.125% over 5 days
 * - Policy thresholds evaluated from highest to lowest
 */

class Parser {
  private static class ParseError extends RuntimeException {}
  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements; 
  }

  private Expr river_system() {
    return assignment();
  }

  private Stmt declaration() {
    try {
      if (match(VAR)) return varDeclaration();
      if (match(DAM)) return damDeclaration();  // ADD THIS LINE

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt damDeclaration() {
    // We already consumed 'dam' keyword
    Token name = consume(IDENTIFIER, "Expect dam variable name.");
    
    // Parse the rest as a dam expression
    current--; // Step back so dam() can see the IDENTIFIER
    Expr damExpr = dam();
    
    // The dam() method already consumed the semicolon
    return new Stmt.Var(name, damExpr);
  }

  private Stmt statement() {
    if (match(PRINT)) return printStatement();
    if (match(LEFT_BRACE)) return new Stmt.Block(block());

    return expressionStatement();
  }

  private Stmt printStatement() {
    Expr value = river_system();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = river_system();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  private Stmt expressionStatement() {
    Expr expr = river_system();
    consume(SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }

  private Expr assignment() {
    Expr expr = flow_chain();  // Skip combination, go straight to flow_chain

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target."); 
    }

    return expr;
  }

  private Expr flow_chain() {
    Expr expr = dam_or_river();

    while (match(TILDE)) {
      Token operator = previous();
      Expr right = dam_or_river();
      expr = new Expr.FlowChain(expr, operator, right);
    }

    return expr;
  }

  private Expr dam_or_river() {
    if (match(AT)) {
      return river();
    }
    
    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(LEFT_PAREN)) {
      Expr expr = flow_chain();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect river expression starting with '@', '|', variable, or '('.");
  }

  private Expr dam() {
    // We already consumed 'dam' keyword
    Token damVarName = consume(IDENTIFIER, "Expect dam variable name.");
    consume(PIPE, "Expect '|' after dam name.");
    
    Token damDisplayName = consume(STRING, "Expect quoted dam display name.");
    
    // Optional parameters with defaults
    Token capacity = null;
    Token maxFlow = null; 
    Token startLevel = null;
    Token rainfall = null;
    List<Object> rainfallArray = new ArrayList<>();
    Token simulateDays = null;
    List<Expr.PolicyRule> policyRules = new ArrayList<>();
    
    // Parse optional parameters (in any order)
    while (!check(PIPE)) {
    if (match(CAPACITY)) {
      capacity = consume(MULTIPLIER, "Expect capacity amount with 'L' (e.g., 50000L).");
    } else if (match(MAXFLOW)) {  // ADD THIS BLOCK
      maxFlow = consume(MULTIPLIER, "Expect max flow rate with 'L' (e.g., 100L).");
    } else if (match(START)) {
      startLevel = consume(NUMBER, "Expect starting level (0.0-1.0).");
    } else if (match(RAINFALL)) {
      rainfall = consume(MULTIPLIER, "Expect rainfall multiplier (e.g., 2L).");
      consume(LEFT_BRACKET, "Expect '[' after rainfall multiplier.");
      rainfallArray = number_list();
      consume(RIGHT_BRACKET, "Expect ']' after rainfall values.");
    } else if (match(SIMULATE)) {
      simulateDays = consume(NUMBER, "Expect number of days to simulate.");
    } else if (match(POLICY)) {
      consume(LEFT_BRACE, "Expect '{' to start policy block.");
      policyRules = parsePolicyRules();
      consume(RIGHT_BRACE, "Expect '}' to end policy block.");
    } else {
      throw error(peek(), "Unexpected token in dam declaration. Expected 'capacity', 'maxflow', 'start', 'rain', 'simulate', 'policy', or '|'.");
    }
  }
    
    consume(PIPE, "Expect '|' before inflow sources.");
    
    // Parse comma-separated inflow sources
    List<Expr> inflowExprs = new ArrayList<>();
    inflowExprs.add(dam_or_river());
    
    while (match(COMMA)) {
      inflowExprs.add(dam_or_river());
    }
    
    consume(SEMICOLON, "Expect ';' after dam declaration.");
    
    return new Expr.Dam(damDisplayName, capacity, maxFlow, startLevel, rainfall, 
                        rainfallArray, simulateDays, policyRules, inflowExprs);
  }

  private List<Expr.PolicyRule> parsePolicyRules() {
    List<Expr.PolicyRule> rules = new ArrayList<>();
    
    while (!check(RIGHT_BRACE)) {
      if (match(THRESHOLD)) {
        Token thresholdValue = consume(NUMBER, "Expect threshold value (0.0-1.0).");
        consume(RELEASE, "Expect 'release' keyword after threshold.");
        Token releaseValue = consume(NUMBER, "Expect release rate (0.0-1.0+).");
        consume(SEMICOLON, "Expect ';' after policy rule.");
        
        rules.add(new Expr.PolicyRule(
          Expr.PolicyRule.RuleType.THRESHOLD,
          (Double) thresholdValue.literal,
          (Double) releaseValue.literal
        ));
      } else if (match(DEFAULT)) {
        consume(RELEASE, "Expect 'release' keyword after 'default'.");
        Token releaseValue = consume(NUMBER, "Expect release rate (0.0-1.0+).");
        consume(SEMICOLON, "Expect ';' after policy rule.");
        
        rules.add(new Expr.PolicyRule(
          Expr.PolicyRule.RuleType.DEFAULT,
          null,
          (Double) releaseValue.literal
        ));
      } else {
        throw error(peek(), "Expected 'threshold' or 'default' in policy block.");
      }
    }
    
    return rules;
  }

  private Expr river() {
    // We already consumed the '@'
    Token riverId = consume(STRING, "Expect quoted river name after '@' (e.g., @'Zora River').");
    Token multiplier = consume(MULTIPLIER, "Expect multiplier (e.g., '5L') after river name.");
    
    consume(LEFT_BRACKET, "Expect '[' after multiplier.");
    List<Object> rainfallArray = number_list();
    consume(RIGHT_BRACKET, "Expect ']' after rainfall numbers.");

    return new Expr.River(riverId, multiplier, rainfallArray);
  }
  
  private List<Object> number_list() {
    List<Object> numbers = new ArrayList<>();
    
    numbers.add(consume(NUMBER, "Expect number in flow array.").literal);
    
    while (match(COMMA)) {
      numbers.add(consume(NUMBER, "Expect number after ','").literal);
    }
    
    return numbers;
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }
  
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

}
