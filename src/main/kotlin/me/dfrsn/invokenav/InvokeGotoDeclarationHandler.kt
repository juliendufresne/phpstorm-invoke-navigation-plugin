package me.dfrsn.invokenav

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParenthesizedExpression
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.PhpTypedElement

/**
 * Navigates from the parentheses of an "invokable object" call to the `__invoke()` method of the callee's class.
 *
 * Handles calls whose callee is an expression rather than a function name, for example:
 * - `($this->handler)($args)`
 * - `$callback($args)`
 * - `$this->getHandler()($args)`
 *
 * The handler only reacts to the parentheses (those wrapping the callee and those of the argument list), so
 * Ctrl+Click on `$this`, `handler` or `$callback` keeps its usual behavior.
 */
class InvokeGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        val call = findInvokableCall(sourceElement ?: return null) ?: return null
        val callee = call.firstChild as? PhpTypedElement ?: return null

        val targets = findInvokeMethods(callee)
        return if (targets.isEmpty()) null else targets.toTypedArray()
    }

    /**
     * Returns the call whose parentheses [leaf] is, if that call invokes an expression rather than a named function.
     */
    private fun findInvokableCall(leaf: PsiElement): FunctionReference? {
        val type = leaf.node?.elementType
        if (type != PhpTokenTypes.chLPAREN && type != PhpTokenTypes.chRPAREN) return null

        val parent = leaf.parent
        val call = when {
            // `(` or `)` of the argument list: `($this->handler)(...)`
            parent is FunctionReference -> parent
            // `(` or `)` wrapping the callee: `(...)($args)`
            parent is ParenthesizedExpression && parent.parent is FunctionReference && parent.parent.firstChild == parent ->
                parent.parent as FunctionReference
            else -> return null
        }

        // Method calls are regular named calls; a plain function call starts with an identifier or namespace.
        if (call is MethodReference) return null
        if (call.firstChild !is PhpExpression) return null

        return call
    }

    private fun findInvokeMethods(callee: PhpTypedElement): List<Method> {
        val phpIndex = PhpIndex.getInstance(callee.project)

        return callee.globalType.types
            .asSequence()
            .flatMap { phpIndex.getAnyByFQN(it) }
            .mapNotNull { it.findMethodByName(INVOKE) }
            .distinct()
            .toList()
    }

    private companion object {
        const val INVOKE = "__invoke"
    }
}
