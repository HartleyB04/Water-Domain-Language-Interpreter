package flowInterpreter;

enum TokenType {
  // Single-character tokens.
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
  COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

  // Water flow tokens 
  AT,             // @
  TILDE,          // ~
  LEFT_BRACKET,   // [
  RIGHT_BRACKET,  // ]
  PIPE,           // | for dam syntax

  // One or two character tokens.
  BANG, BANG_EQUAL,
  EQUAL, EQUAL_EQUAL,
  GREATER, GREATER_EQUAL,
  LESS, LESS_EQUAL,

  // Water flow multi-character 
  // ARROW,          // ->
  
  // Literals.
  IDENTIFIER, STRING, NUMBER,

  // Water flow literals 
  RAINFALL,         // R followed by a number (R8, R5, etc)
  MAXFLOW,          // maxflow keyword for dam maximum outflow capacity 
  MULTIPLIER,       // L followed by flow rate (5L, 10L, etc)

  // Keywords.
  AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
  PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,
  
  // Dam keywords
  DAM,            // dam keyword
  CAPACITY,       // capacity keyword for dam storage
  RELEASE,        // release keyword for dam output policy
  START,          // start keyword for initial dam level
  SIMULATE,       // simulate keyword for simulation days
  POLICY,         // policy keyword for dam rules
  THRESHOLD,      // threshold keyword for level-based rules
  DEFAULT,        // default keyword for fallback rule
  LEFT_CURLY,     // { for policy blocks (already exists as LEFT_BRACE)
  RIGHT_CURLY,    // } for policy blocks (already exists as RIGHT_BRACE)

  EOF
}
