/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.engine.internal.hint;

import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.InterfaceType;

/**
 * Instances of the class {@code BestPracticesVerifier} traverse an AST structure looking for
 * violations of Dart best practices.
 * 
 * @coverage dart.engine.resolver
 */
public class BestPracticesVerifier extends RecursiveASTVisitor<Void> {

  private static final String GETTER = "getter";

  private static final String HASHCODE_GETTER_NAME = "hashCode";

  private static final String METHOD = "method";

  private static final String SETTER = "setter";

  private static final String TO_INT_METHOD_NAME = "toInt";

  /**
   * Given a parenthesized expression, this returns the parent (or recursively grand-parent) of the
   * expression that is a parenthesized expression, but whose parent is not a parenthesized
   * expression.
   * <p>
   * For example given the code {@code (((e)))}: {@code (e) -> (((e)))}.
   * 
   * @param parenthesizedExpression some expression whose parent is a parenthesized expression
   * @return the first parent or grand-parent that is a parenthesized expression, that does not have
   *         a parenthesized expression parent
   */
  private static ParenthesizedExpression wrapParenthesizedExpression(
      ParenthesizedExpression parenthesizedExpression) {
    if (parenthesizedExpression.getParent() instanceof ParenthesizedExpression) {
      return wrapParenthesizedExpression((ParenthesizedExpression) parenthesizedExpression.getParent());
    }
    return parenthesizedExpression;
  }

  /**
   * The class containing the AST nodes being visited, or {@code null} if we are not in the scope of
   * a class.
   */
  private ClassElement enclosingClass;

  /**
   * The error reporter by which errors will be reported.
   */
  private ErrorReporter errorReporter;

  /**
   * Create a new instance of the {@link BestPracticesVerifier}.
   * 
   * @param errorReporter the error reporter
   */
  public BestPracticesVerifier(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    checkForDivisionOptimizationHint(node);
    return super.visitBinaryExpression(node);
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ClassElement outerClass = enclosingClass;
    try {
      enclosingClass = node.getElement();
      // Commented out until we decide that we want this hint in the analyzer
//    checkForOverrideEqualsButNotHashCode(node);
      return super.visitClassDeclaration(node);
    } finally {
      enclosingClass = outerClass;
    }
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    // Commented out until we decide that we want this hint in the analyzer
    checkForOverridingPrivateMember(node);
    return super.visitMethodDeclaration(node);
  }

  /**
   * Check for the passed binary expression for the {@link HintCode#DIVISION_OPTIMIZATION}.
   * 
   * @param node the binary expression to check
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#DIVISION_OPTIMIZATION
   */
  private boolean checkForDivisionOptimizationHint(BinaryExpression node) {
    if (!node.getOperator().getType().equals(TokenType.SLASH)) {
      return false;
    }
    if (node.getParent() instanceof ParenthesizedExpression) {
      ParenthesizedExpression parenthesizedExpression = wrapParenthesizedExpression((ParenthesizedExpression) node.getParent());
      if (parenthesizedExpression.getParent() instanceof MethodInvocation) {
        MethodInvocation methodInvocation = (MethodInvocation) parenthesizedExpression.getParent();
        if (TO_INT_METHOD_NAME.equals(methodInvocation.getMethodName().getName())
            && methodInvocation.getArgumentList().getArguments().isEmpty()) {
          errorReporter.reportError(HintCode.DIVISION_OPTIMIZATION, methodInvocation);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check for the passed class declaration for the
   * {@link HintCode#OVERRIDE_EQUALS_BUT_NOT_HASH_CODE} hint code.
   * 
   * @param node the class declaration to check
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#OVERRIDE_EQUALS_BUT_NOT_HASH_CODE
   */
  @SuppressWarnings("unused")
  private boolean checkForOverrideEqualsButNotHashCode(ClassDeclaration node) {
    ClassElement classElement = node.getElement();
    if (classElement == null) {
      return false;
    }
    MethodElement equalsOperatorMethodElement = classElement.getMethod(TokenType.EQ_EQ.getLexeme());
    if (equalsOperatorMethodElement != null) {
      PropertyAccessorElement hashCodeElement = classElement.getGetter(HASHCODE_GETTER_NAME);
      if (hashCodeElement == null) {
        errorReporter.reportError(
            HintCode.OVERRIDE_EQUALS_BUT_NOT_HASH_CODE,
            node.getName(),
            classElement.getDisplayName());
        return true;
      }
    }
    return false;
  }

  /**
   * Check for the passed class declaration for the
   * {@link HintCode#OVERRIDE_EQUALS_BUT_NOT_HASH_CODE} hint code.
   * 
   * @param node the class declaration to check
   * @return {@code true} if and only if a hint code is generated on the passed node
   * @see HintCode#OVERRIDDING_PRIVATE_MEMBER
   */
  private boolean checkForOverridingPrivateMember(MethodDeclaration node) {
    // If not in an enclosing class, return false
    if (enclosingClass == null) {
      return false;
    }
    // If the member is not private, return false
    if (!Identifier.isPrivateName(node.getName().getName())) {
      return false;
    }
    // Get the element of the member, if null, return false
    ExecutableElement executableElement = node.getElement();
    if (executableElement == null) {
      return false;
    }
    // Loop through all of the superclasses looking for a matching method or accessor
    // TODO(jwren) If the HintGenerator needs or has easy access to the InheritanceManager in the
    // future then this could be refactored down to be more readable, however, since we are only
    // looking through super classes (and not the entire interface graph) there is no pressing need
    String elementName = executableElement.getName();
    boolean isGetterOrSetter = executableElement instanceof PropertyAccessorElement;
    InterfaceType superType = enclosingClass.getSupertype();
    if (superType == null) {
      return false;
    }
    ClassElement classElement = superType.getElement();
    while (classElement != null) {
      if (!enclosingClass.getLibrary().equals(classElement.getLibrary())) {
        if (isGetterOrSetter) {
          PropertyAccessorElement overriddenAccessor = null;
          PropertyAccessorElement[] accessors = classElement.getAccessors();
          for (PropertyAccessorElement propertyAccessorElement : accessors) {
            if (elementName.equals(propertyAccessorElement.getName())) {
              overriddenAccessor = propertyAccessorElement;
              break;
            }
          }
          if (overriddenAccessor != null) {
            String memberType = ((PropertyAccessorElement) executableElement).isGetter() ? GETTER
                : SETTER;
            errorReporter.reportError(
                HintCode.OVERRIDDING_PRIVATE_MEMBER,
                node.getName(),
                memberType,
                executableElement.getDisplayName(),
                classElement.getDisplayName());
            return true;
          }
        } else {
          MethodElement overriddenMethod = classElement.getMethod(elementName);
          if (overriddenMethod != null) {
            errorReporter.reportError(
                HintCode.OVERRIDDING_PRIVATE_MEMBER,
                node.getName(),
                METHOD,
                executableElement.getDisplayName(),
                classElement.getDisplayName());
            return true;
          }
        }
      }
      superType = classElement.getSupertype();
      classElement = superType != null ? superType.getElement() : null;
    }
    return false;
  }

}