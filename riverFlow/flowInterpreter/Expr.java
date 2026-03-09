package flowInterpreter;

import java.util.List;

abstract class Expr {
  interface Visitor<R> {
    R visitAssignExpr(Assign expr);
    R visitBinaryExpr(Binary expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);
    R visitRiverExpr(River expr);
    R visitFlowChainExpr(FlowChain expr);
    // R visitCombinationExpr(Combination expr);
    R visitDamExpr(Dam expr);
  }
  static class Assign extends Expr {
    Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }

    final Token name;
    final Expr value;
  }
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }
  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;
  }
  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;
  }
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }
  static class Variable extends Expr {
    Variable(Token name) {
      this.name = name;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }

    final Token name;
  }
  static class River extends Expr {
    River(Token riverId, Token multiplier, List<Object> rainfallArray) {
      this.riverId = riverId;
      this.multiplier = multiplier;
      this.rainfallArray = rainfallArray;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitRiverExpr(this);
    }

    final Token riverId;        // Now a STRING token with river name
    final Token multiplier;     // Multiplier (e.g., 5L)
    final List<Object> rainfallArray;  // Array of rainfall values
  }
  static class FlowChain extends Expr {
    FlowChain(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFlowChainExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }
  static class PolicyRule {
    enum RuleType { THRESHOLD, DEFAULT }
    
    final RuleType type;
    final Double threshold;     // For THRESHOLD rules (0.0-1.0), null for DEFAULT
    final Double releaseRate;   // Release rate (0.0-1.0+)
    
    PolicyRule(RuleType type, Double threshold, Double releaseRate) {
      this.type = type;
      this.threshold = threshold;
      this.releaseRate = releaseRate;
    }
  }
  static class Dam extends Expr {
  Dam(Token damName, Token capacity, Token maxFlow, Token startLevel, Token rainfall, 
      List<Object> rainfallArray, Token simulateDays, List<PolicyRule> policyRules, 
      List<Expr> inflowExprs) {
    this.damName = damName;
    this.capacity = capacity;
    this.maxFlow = maxFlow;
    this.startLevel = startLevel;
    this.rainfall = rainfall;
    this.rainfallArray = rainfallArray;
    this.simulateDays = simulateDays;
    this.policyRules = policyRules;
    this.inflowExprs = inflowExprs;
  }

  @Override
  <R> R accept(Visitor<R> visitor) {
    return visitor.visitDamExpr(this);
  }

  final Token damName;                    // Display name (STRING)
  final Token capacity;                   // Optional capacity (MULTIPLIER or null)
  final Token maxFlow;                    // Optional max outflow rate (MULTIPLIER or null)
  final Token startLevel;                 // Optional starting level 0.0-1.0 (NUMBER or null)
  final Token rainfall;                   // Optional rainfall multiplier (MULTIPLIER or null)
  final List<Object> rainfallArray;       // Optional rainfall array (or empty list)
  final Token simulateDays;               // Optional simulation days (NUMBER or null)
  final List<PolicyRule> policyRules;     // Policy rules (or empty list)
  final List<Expr> inflowExprs;           // Multiple inflow sources
}

  abstract <R> R accept(Visitor<R> visitor);
}
