package flowInterpreter;

import java.util.ArrayList;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {

  private Environment environment = new Environment();

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        return -(double)right;
    }
    // Unreachable.
    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return environment.get(expr.name);
  }

  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }

  private String stringify(Object object) {
    if (object == null) return "nil";

    if (object instanceof RiverData) {
      return object.toString();
    }

    if (object instanceof DamData) {
      return object.toString();
    }

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void executeBlock(List<Stmt> statements,
                    Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    environment.assign(expr.name, value);
    return value;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right); 

    switch (expr.operator.type) {
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double)left > (double)right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left >= (double)right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left < (double)right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left <= (double)right;
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return (double)left - (double)right;
      case BANG_EQUAL: return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        } 

        if (left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }

        throw new RuntimeError(expr.operator,
            "Operands must be two numbers or two strings.");
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double)left / (double)right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double)left * (double)right;
    }

    // Unreachable.
    return null;
  }

  @Override
  public Object visitRiverExpr(Expr.River expr) {
    String riverName = (String) expr.riverId.literal;
    double multiplier = (double) expr.multiplier.literal;
    
    // Convert rainfall array from Object to Double
    List<Double> rainfall = new ArrayList<>();
    for (Object rain : expr.rainfallArray) {
      rainfall.add((Double) rain);
    }
    
    return new RiverData(riverName, multiplier, rainfall);
  }

  @Override
  public Object visitDamExpr(Expr.Dam expr) {
    // Extract parameters with defaults
    String damName = (String) expr.damName.literal;
    double capacity = expr.capacity != null ? (double) expr.capacity.literal : 10000.0;
    double maxFlowRate = expr.maxFlow != null ? (double) expr.maxFlow.literal : 50.0; 
    double startLevel = expr.startLevel != null ? (double) expr.startLevel.literal : 0.5;
    int simulateDays = expr.simulateDays != null ? ((Double) expr.simulateDays.literal).intValue() : 10;
    
    // Extract rainfall
    List<Double> ownRainfall = new ArrayList<>();
    if (expr.rainfall != null && !expr.rainfallArray.isEmpty()) {
      double rainfallMultiplier = (double) expr.rainfall.literal;
      for (Object rain : expr.rainfallArray) {
        ownRainfall.add(rainfallMultiplier * (Double) rain);
      }
    }
    
    // Convert policy rules
    List<DamData.PolicyRule> policyRules = new ArrayList<>();
    for (Expr.PolicyRule rule : expr.policyRules) {
      DamData.PolicyRule.RuleType type = rule.type == Expr.PolicyRule.RuleType.THRESHOLD 
          ? DamData.PolicyRule.RuleType.THRESHOLD 
          : DamData.PolicyRule.RuleType.DEFAULT;
      policyRules.add(new DamData.PolicyRule(type, rule.threshold, rule.releaseRate));
    }

    // ADD DEFAULT POLICY if none specified:
    if (policyRules.isEmpty()) {
      policyRules.add(new DamData.PolicyRule(DamData.PolicyRule.RuleType.THRESHOLD, 0.8, 1.0));
      policyRules.add(new DamData.PolicyRule(DamData.PolicyRule.RuleType.THRESHOLD, 0.5, 0.8));
      policyRules.add(new DamData.PolicyRule(DamData.PolicyRule.RuleType.THRESHOLD, 0.1, 0.4));
    }
    
    // Evaluate all inflow expressions and apply decay to each
    List<List<Double>> inflowSources = new ArrayList<>();
    for (Expr inflowExpr : expr.inflowExprs) {
      Object inflowObj = evaluate(inflowExpr);
      
      List<Double> inflowFlow;
      if (inflowObj instanceof RiverData) {
        inflowFlow = ((RiverData) inflowObj).flowArray;
      } else if (inflowObj instanceof DamData) {
        inflowFlow = ((DamData) inflowObj).outflowPerDay;
      } else {
        throw new RuntimeError(expr.damName, 
            "Dam inflow must be a river or another dam.");
      }
      
      // Apply decay to this inflow source
      List<Double> decayedInflow = applyDecayToDam(inflowFlow, simulateDays);
      inflowSources.add(decayedInflow);
    }
    
    // Create dam with all computed inflows (ADD maxFlowRate parameter)
    return new DamData(damName, capacity, maxFlowRate, startLevel, ownRainfall, 
                      simulateDays, policyRules, inflowSources);
  }

  private List<Double> applyDecayToDam(List<Double> upstreamFlow, int simulateDays) {
    // Calculate maximum days needed (upstream days + 4 decay days)
    int maxDays = Math.max(upstreamFlow.size() + 4, simulateDays);
    
    List<Double> result = new ArrayList<>();
    
    // Initialize with zeros
    for (int i = 0; i < maxDays; i++) {
      result.add(0.0);
    }
    
    // Apply decay pattern: 50%, 25%, 12.5%, 6.25%, 3.125%
    double[] decayPattern = {0.5, 0.25, 0.125, 0.0625, 0.03125};
    
    // For each day of upstream flow
    for (int upstreamDay = 0; upstreamDay < upstreamFlow.size(); upstreamDay++) {
      double flowAmount = upstreamFlow.get(upstreamDay);
      
      // Apply decay over 5 days
      for (int decayDay = 0; decayDay < decayPattern.length; decayDay++) {
        int targetDay = upstreamDay + decayDay;
        if (targetDay < result.size()) {
          result.set(targetDay, result.get(targetDay) + flowAmount * decayPattern[decayDay]);
        }
      }
    }
    
    return result;
  }

  @Override
  public Object visitFlowChainExpr(Expr.FlowChain expr) {
    Object leftObj = evaluate(expr.left);
    Object rightObj = evaluate(expr.right);
    
    // Extract flow arrays
    List<Double> upstreamFlow;
    if (leftObj instanceof RiverData) {
      upstreamFlow = ((RiverData) leftObj).flowArray;
    } else if (leftObj instanceof DamData) {
      upstreamFlow = ((DamData) leftObj).outflowPerDay;  // CHANGED from .outflow
    } else {
      throw new RuntimeError(expr.operator, 
          "Flow operator '~' requires rivers or dams.");
    }
    
    // Get downstream river info
    RiverData downstream;
    if (rightObj instanceof RiverData) {
      downstream = (RiverData) rightObj;
    } else if (rightObj instanceof DamData) {
      // Can't flow into a dam this way - dams declare their inflows
      throw new RuntimeError(expr.operator, 
          "Cannot use '~' to flow into a dam. Dams must declare their inflows in the dam declaration.");
    } else {
      throw new RuntimeError(expr.operator, 
          "Flow operator '~' requires rivers or dams.");
    }
    
    // Apply decay to upstream flow and combine with downstream's own flow
    List<Double> combinedFlow = applyDecay(upstreamFlow, downstream.flowArray);
    
    // Create new river with combined flows
    RiverData result = new RiverData(downstream.name, downstream.multiplier, 
                                      downstream.rainfall, combinedFlow);
    
    // If right side is a variable, reassign it
    if (expr.right instanceof Expr.Variable) {
      Token varName = ((Expr.Variable)expr.right).name;
      environment.assign(varName, result);
    }
    
    return result;
  }

  private List<Double> applyDecay(List<Double> upstreamFlow, List<Double> downstreamFlow) {
    // Calculate maximum days needed (upstream days + 4 decay days)
    int maxDays = Math.max(
        upstreamFlow.size() + 4,
        downstreamFlow.size()
    );
    
    List<Double> result = new ArrayList<>();
    
    // Initialize with downstream's own flow
    for (int i = 0; i < maxDays; i++) {
      double flow = i < downstreamFlow.size() ? downstreamFlow.get(i) : 0.0;
      result.add(flow);
    }
    
    // Apply decay pattern: 50%, 25%, 12.5%, 6.25%, 3.125%
    double[] decayPattern = {0.5, 0.25, 0.125, 0.0625, 0.03125};
    
    // For each day of upstream flow
    for (int upstreamDay = 0; upstreamDay < upstreamFlow.size(); upstreamDay++) {
      double flowAmount = upstreamFlow.get(upstreamDay);
      
      // Apply decay over 5 days
      for (int decayDay = 0; decayDay < decayPattern.length; decayDay++) {
        int targetDay = upstreamDay + decayDay;
        if (targetDay < result.size()) {
          result.set(targetDay, result.get(targetDay) + flowAmount * decayPattern[decayDay]);
        }
      }
    }
    
    return result;
  }
  
  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator,
                                   Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

}
