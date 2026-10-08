package me.dfrsn.invokenav

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.elements.Method

class InvokeGotoDeclarationHandlerTest : BasePlatformTestCase() {

    private val classes = """
        namespace App;

        class Handler { public function __invoke(int ${'$'}x): int { return ${'$'}x; } }
        class ChildHandler extends Handler {}
        class OtherHandler { public function __invoke() {} }
        class NotInvokable { public function run() {} }
    """.trimIndent()

    fun testArgumentListParenthesis() {
        assertTargets(
            """
            class Service {
                private Handler ${'$'}handler;
                public function run() { (${'$'}this->handler)<caret>(1); }
            }
            """,
            "App\\Handler::__invoke",
        )
    }

    fun testClosingArgumentListParenthesis() {
        assertTargets(
            """
            class Service {
                private Handler ${'$'}handler;
                public function run() { (${'$'}this->handler)(1<caret>); }
            }
            """,
            "App\\Handler::__invoke",
        )
    }

    fun testCalleeParenthesis() {
        assertTargets(
            """
            class Service {
                private Handler ${'$'}handler;
                public function run() { <caret>(${'$'}this->handler)(1); }
            }
            """,
            "App\\Handler::__invoke",
        )
    }

    fun testVariableCallee() {
        assertTargets(
            """
            function run(Handler ${'$'}cb) { ${'$'}cb<caret>(1); }
            """,
            "App\\Handler::__invoke",
        )
    }

    fun testInheritedInvoke() {
        assertTargets(
            """
            function run(ChildHandler ${'$'}cb) { ${'$'}cb<caret>(1); }
            """,
            "App\\Handler::__invoke",
        )
    }

    fun testMethodReturningInvokable() {
        assertTargets(
            """
            class Factory {
                public function make(): Handler { return new Handler(); }
                public function run() { ${'$'}this->make()<caret>(1); }
            }
            """,
            "App\\Handler::__invoke",
        )
    }

    fun testUnionType() {
        assertTargets(
            """
            function run(Handler|OtherHandler ${'$'}cb) { ${'$'}cb<caret>(1); }
            """,
            "App\\Handler::__invoke",
            "App\\OtherHandler::__invoke",
        )
    }

    fun testIgnoresPlainFunctionCall() {
        assertTargets(
            """
            function foo() {}
            foo<caret>();
            """,
        )
    }

    fun testIgnoresNamespacedFunctionCall() {
        assertTargets(
            """
            function foo() {}
            \App\foo<caret>();
            """,
        )
    }

    fun testIgnoresMethodCall() {
        assertTargets(
            """
            function run(Handler ${'$'}h) { ${'$'}h->__invoke<caret>(1); }
            """,
        )
    }

    fun testIgnoresClassWithoutInvoke() {
        assertTargets(
            """
            function run(NotInvokable ${'$'}cb) { ${'$'}cb<caret>(); }
            """,
        )
    }

    fun testIgnoresCalleeIdentifier() {
        assertTargets(
            """
            class Service {
                private Handler ${'$'}handler;
                public function run() { (${'$'}this->hand<caret>ler)(1); }
            }
            """,
        )
    }

    fun testIgnoresParenthesesInsideArguments() {
        assertTargets(
            """
            function run(Handler ${'$'}cb, Handler ${'$'}other) { ${'$'}cb(<caret>(${'$'}other)); }
            """,
        )
    }

    fun testHandlerIsRegistered() {
        assertTrue(GotoDeclarationHandler.EP_NAME.extensionList.any { it is InvokeGotoDeclarationHandler })
    }

    private fun assertTargets(code: String, vararg expected: String) {
        myFixture.configureByText("test.php", "<?php\n$classes\n\n${code.trimIndent()}\n")
        val offset = myFixture.caretOffset
        val element = myFixture.file.findElementAt(offset)

        val targets = InvokeGotoDeclarationHandler()
            .getGotoDeclarationTargets(element, offset, myFixture.editor)
            .orEmpty()
            .map { (it as Method).fqn.removePrefix("\\").replace(".", "::") }
            .sorted()

        assertEquals(expected.sorted(), targets)
    }
}
