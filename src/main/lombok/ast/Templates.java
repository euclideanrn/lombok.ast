/*
 * Copyright © 2010 Reinier Zwitserloot and Roel Spilker.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.ast;

import java.util.List;

import lombok.NonNull;
import lombok.ast.template.AdditionalCheck;
import lombok.ast.template.CopyMethod;
import lombok.ast.template.GenerateAstNode;
import lombok.ast.template.InitialValue;
import lombok.ast.template.NotChildOfNode;

@GenerateAstNode(extending=Statement.class)
class AssertTemplate {
	@NonNull Expression assertion;
	Expression message;
}

@GenerateAstNode(extending=Statement.class)
class CatchTemplate {
	@NonNull VariableDeclaration exceptionDeclaration;
	@NonNull Block body;
	
	/* check: exDecl must have exactly 1 VDEntry */
}

@GenerateAstNode(extending=Statement.class)
class BlockTemplate {
	List<Statement> contents;
}

@GenerateAstNode(extending=Statement.class)
class DoWhileTemplate {
	@NonNull Expression condition;
	@NonNull Statement statement;
}

@GenerateAstNode(extending=Statement.class)
class WhileTemplate {
	@NonNull Expression condition;
	@NonNull Statement statement;
}

@GenerateAstNode(extending=Statement.class)
class ForTemplate {
	Statement initialization;
	Expression condition;
	List<Statement> increments;
	@NonNull Statement statement;
}

@GenerateAstNode(extending=Statement.class)
class ForEachTemplate {
	@NonNull VariableDeclaration element;
	@NonNull Expression iterable;
	@NonNull Statement statement;
}

@GenerateAstNode(extending=Statement.class)
class IfTemplate {
	@NonNull Expression condition;
	@NonNull Statement statement;
	Statement elseStatement;
}

@GenerateAstNode(extending=Statement.class)
class SynchronizedTemplate {
	@NonNull Expression lock;
	@NonNull Block body;
}

@GenerateAstNode(extending=Statement.class)
class TryTemplate {
	@NonNull Block body;
	List<Catch> catches;
	Block finally_;
	
	@AdditionalCheck
	static void checkNotLoneTry(List<SyntaxProblem> problems, Try node) {
		if (node.catches().size() == 0 && node.getRawFinally() == null) {
			problems.add(new SyntaxProblem(node, "try statement with no catches and no finally"));
		}
	}
}

@GenerateAstNode(extending=Statement.class)
class VariableDeclarationTemplate {
	@NonNull TypeReference typeReference;
	List<VariableDeclarationEntry> variables;
}

@GenerateAstNode
class VariableDeclarationEntryTemplate {
	@NonNull Identifier name;
	Expression initializer;
}

@GenerateAstNode(extending=Expression.class)
class InlineIfExpressionTemplate {
	@NonNull Expression condition;
	@NonNull Expression ifTrue;
	@NonNull Expression ifFalse;
}

@GenerateAstNode(extending=Expression.class)
class IncrementExpressionTemplate {
	@NonNull Expression operand;
	@NotChildOfNode boolean decrement = false;
	@NotChildOfNode boolean prefix = false;
}

@GenerateAstNode
class IdentifierTemplate {
	@NotChildOfNode
	@NonNull String name;
}

@GenerateAstNode(extending=Expression.class)
class BinaryExpressionTemplate {
	@NonNull Expression left;
	@NonNull Expression right;
	@NotChildOfNode(rawFormParser="parseOperator", rawFormGenerator="generateOperator")
	@NonNull BinaryOperator operator;
	
	static String generateOperator(BinaryOperator op) {
		return op.getSymbol();
	}
	
	static BinaryOperator parseOperator(String op) {
		if (op == null) throw new IllegalArgumentException("missing operator");
		BinaryOperator result = BinaryOperator.fromSymbol(op.trim());
		if (result == null) throw new IllegalArgumentException("unknown binary operator: " + op.trim());
		return result;
	}
}

@GenerateAstNode(extending=Expression.class)
class UnaryExpressionTemplate {
	@NonNull Expression operand;
	@NotChildOfNode(rawFormParser="parseOperator", rawFormGenerator="generateOperator")
	@NonNull UnaryOperator operator;
	
	static String generateOperator(UnaryOperator op) {
		return op.getSymbol();
	}
	
	static UnaryOperator parseOperator(String op) {
		if (op == null) throw new IllegalArgumentException("missing operator");
		UnaryOperator result = UnaryOperator.fromSymbol(op.trim());
		if (result == null) throw new IllegalArgumentException("unknown unary operator: " + op.trim());
		return result;
	}
}

@GenerateAstNode
class TypeVariableTemplate {
	@NonNull Identifier name;
	List<TypeReference> extending;
}

@GenerateAstNode
class TypeReferenceTemplate {
	@NotChildOfNode
	@InitialValue("lombok.ast.WildcardKind.NONE")
	@NonNull WildcardKind wildcard;
	
	@NotChildOfNode
	int arrayDimensions;
	
	List<TypeReferencePart> parts;
	
	@CopyMethod
	static String getTypeName(TypeReference t) {
		StringBuilder out = new StringBuilder();
		for (TypeReferencePart p : t.parts().getContents()) {
			if (out.length() > 0) out.append(".");
			out.append(p.getTypeName());
		}
		
		for (int i = 0; i < t.getArrayDimensions(); i++) out.append("[]");
		
		return out.toString();
	}
	
	@CopyMethod
	static boolean hasGenerics(TypeReference t) {
		return getGenerics(t).isEmpty();
	}
	
	@CopyMethod
	static ListAccessor<TypeReference, TypeReference> getGenerics(TypeReference t) {
		return t.parts().last().generics().wrap(t);
	}
}

@GenerateAstNode
class TypeReferencePartTemplate {
	@NonNull Identifier identifier;
	@InitialValue("new TypeArguments()")
	@NonNull TypeArguments typeArguments;
	
	@CopyMethod
	static ListAccessor<TypeReference, TypeReferencePart> generics(TypeReferencePart self) {
		return self.getTypeArguments().generics().wrap(self);
	}
	
	@CopyMethod
	static String getTypeName(TypeReferencePart p) {
		if (p.generics().isEmpty()) return p.getIdentifier().getName();
		
		StringBuilder out = new StringBuilder();
		out.append(p.getIdentifier().getName()).append("<");
		boolean first = true;
		for (TypeReference t : p.generics().getContents()) {
			if (!first) out.append(", ");
			first = false;
			switch (t.getWildcard()) {
			case EXTENDS:
				out.append("? extends ");
				out.append(t.getTypeName());
				break;
			case SUPER:
				out.append("? super ");
				out.append(t.getTypeName());
				break;
			default:
			case NONE:
				out.append(t.getTypeName());
				break;
			case UNBOUND:
				out.append("?");
				break;
			}
		}
		return out.append(">").toString();
	}
}

@GenerateAstNode
class TypeArgumentsTemplate {
	List<TypeReference> generics;
}

@GenerateAstNode(extending=Expression.class)
class CastTemplate {
	@NonNull TypeReference typeReference;
	@NonNull Expression operand;
}

@GenerateAstNode(extending=Expression.class)
class IdentifierExpressionTemplate {
	@NonNull Identifier identifier;
}

@GenerateAstNode(extending=Expression.class)
class InstanceOfTemplate {
	@NonNull Expression objectReference;
	@NonNull TypeReference typeReference;
}

@GenerateAstNode(extending=Expression.class)
class ConstructorInvocationTemplate {
	Expression qualifier;
	TypeArguments constructorTypeArguments;
	@NonNull TypeReference typeReference;
	List<Expression> arguments;
	ClassBody anonymousClassBody;
}

@GenerateAstNode(extending=Expression.class)
class MethodInvocationTemplate {
	Expression operand;
	TypeArguments methodTypeArguments;
	@NonNull Identifier name;
	List<Expression> arguments;
}

@GenerateAstNode
class ClassBodyTemplate {
	
}

@GenerateAstNode(extending=Expression.class)
class SelectTemplate {
	@NonNull Expression operand;
	@NonNull Identifier identifier;
}

@GenerateAstNode(extending=Expression.class)
class ArrayAccessTemplate {
	@NonNull Expression operand;
	@NonNull Expression indexExpression;
}

@GenerateAstNode(extending=Expression.class)
class ArrayCreationTemplate {
	@NonNull TypeReference componentTypeReference;
	List<ArrayDimension> dimensions;
	ArrayInitializer initializer;
}

@GenerateAstNode
class ArrayDimensionTemplate {
	Expression dimension;
}

@GenerateAstNode(extending=Expression.class)
class ArrayInitializerTemplate {
	List<Expression> expressions;
}

@GenerateAstNode(extending=Expression.class)
class ThisTemplate {
	TypeReference qualifier;
}

@GenerateAstNode(extending=Expression.class)
class SuperTemplate {
	TypeReference qualifier;
}

@GenerateAstNode(extending=Expression.class)
class ClassLiteralTemplate {
	@NonNull TypeReference typeReference;
}

@GenerateAstNode
class KeywordModifierTemplate {
	@NotChildOfNode
	@NonNull String name;
}

@GenerateAstNode(extending=Statement.class)
class EmptyStatementTemplate {}

@GenerateAstNode(extending=Statement.class)
class LabelledStatementTemplate {
	@NonNull Identifier label;
	@NonNull Statement statement;
}

@GenerateAstNode(extending=Statement.class)
class SwitchTemplate {
	@NonNull Expression condition;
	@NonNull Block body;
}

@GenerateAstNode(extending=Statement.class)
class CaseTemplate {
	@NonNull Expression condition;
}

@GenerateAstNode(extending=Statement.class)
class DefaultTemplate {
}
