package flowInterpreter;

class AstPrinter implements Expr.Visitor<String> {
  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme,
                        expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return ("LITERAL "+expr.value.toString());
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return parenthesize("=", new Expr.Variable(expr.name), expr.value);
  }
  
  @Override
  public String visitRiverExpr(Expr.River expr) {
    StringBuilder builder = new StringBuilder();
    builder.append("@'").append(expr.riverId.literal).append("'");
    builder.append(" ").append(expr.multiplier.literal).append("L");
    builder.append("[");
    
    for (int i = 0; i < expr.rainfallArray.size(); i++) {
      if (i > 0) builder.append(", ");
      builder.append(expr.rainfallArray.get(i));
    }
    
    builder.append("]");
    return builder.toString();
  }

  @Override
  public String visitDamExpr(Expr.Dam expr) {
    StringBuilder builder = new StringBuilder();
    builder.append("dam | '").append(expr.damName.literal).append("'");
    
    if (expr.capacity != null) {
      builder.append(" capacity ").append(expr.capacity.literal).append("L");
    }
    if (expr.maxFlow != null) {  // ADD THIS
      builder.append(" maxflow ").append(expr.maxFlow.literal).append("L");
    }
    if (expr.startLevel != null) {
      builder.append(" start ").append(expr.startLevel.literal);
    }
    if (expr.rainfall != null) {
      builder.append(" rainfall ").append(expr.rainfall.literal).append("L[");
      for (int i = 0; i < expr.rainfallArray.size(); i++) {
        if (i > 0) builder.append(", ");
        builder.append(expr.rainfallArray.get(i));
      }
      builder.append("]");
    }
    if (expr.simulateDays != null) {
      builder.append(" simulate ").append(expr.simulateDays.literal);
    }
    if (!expr.policyRules.isEmpty()) {
      builder.append(" policy { ");
      for (Expr.PolicyRule rule : expr.policyRules) {
        if (rule.type == Expr.PolicyRule.RuleType.THRESHOLD) {
          builder.append("threshold ").append(rule.threshold).append(" release ").append(rule.releaseRate);
        } else {
          builder.append("default release ").append(rule.releaseRate);
        }
        builder.append("; ");
      }
      builder.append("}");
    }
    
    builder.append(" | ");
    for (int i = 0; i < expr.inflowExprs.size(); i++) {
      if (i > 0) builder.append(", ");
      builder.append(expr.inflowExprs.get(i).accept(this));
    }
    
    return builder.toString();
  }

  @Override
  public String visitFlowChainExpr(Expr.FlowChain expr) {
    return parenthesize(expr.operator.lexeme,
                        expr.left, expr.right);
  }
    
  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");

    return builder.toString();
  }
}
