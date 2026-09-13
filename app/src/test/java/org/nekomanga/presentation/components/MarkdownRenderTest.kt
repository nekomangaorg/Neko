package org.nekomanga.presentation.components

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import com.mikepenz.markdown.annotator.AnnotatorSettings
import com.mikepenz.markdown.annotator.DefaultAnnotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.model.ReferenceLinkHandlerImpl
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Test

class MarkdownRenderTest {

    // Description from github issue #1277: bracketed text with no reference definition,
    // followed by one real inline link.
    private val issue1277Description =
        "[Would you like to reverse time?] [All stats will be reset] [Please pick a date.] " +
            "\"February 28th, 1985. The day I was born.\" Wealth, I will seize all the money in the " +
            "world. Monopoly, I will seize all the dungeons in the world. At this time, while the " +
            "world is still peaceful.  \n\n---\n- [Original Webtoon <Webtoon kakao>]" +
            "(https://webtoon.kakao.com/content/%EC%A0%84%EC%83%9D%EC%9E%90/2353)"

    private val kakaoUrl = "https://webtoon.kakao.com/content/%EC%A0%84%EC%83%9D%EC%9E%90/2353"

    @Test
    fun bracketedTextWithoutDefinitionIsNotALink() {
        val links = renderLinks(issue1277Description)

        links.shouldHaveSize(1)
        links.single().first.url shouldBe kakaoUrl
    }

    // MangaDex shows inline html as literal text, so the link text keeps its angle brackets.
    @Test
    fun inlineHtmlInsideLinkTextIsKeptAsText() {
        val links = renderLinks(issue1277Description)

        links.single().second shouldBe "Original Webtoon <Webtoon kakao>"
    }

    @Test
    fun inlineHtmlOutsideLinkIsKeptAsText() {
        renderText("see <b>bold</b> and <Webtoon kakao> here") shouldBe
            "see <b>bold</b> and <Webtoon kakao> here"
    }

    @Test
    fun htmlLineBreakTagsRenderAsNewlines() {
        renderText(
            "First line<br>Second line<br/>Third line<br />Fourth line<BR>Fifth line"
        ) shouldBe "First line\nSecond line\nThird line\nFourth line\nFifth line"
    }

    @Test
    fun htmlTagsThatAreNotLineBreaksAreKeptAsText() {
        renderText("<span>text</span> and <branch>not a break</branch>") shouldBe
            "<span>text</span> and <branch>not a break</branch>"
    }

    @Test
    fun bracketedTextIsRenderedVerbatim() {
        val text = renderText(issue1277Description)

        text shouldContain "[Would you like to reverse time?]"
        text shouldContain "[All stats will be reset]"
        text shouldContain "[Please pick a date.]"
    }

    private fun parse(content: CharSequence): ASTNode =
        MarkdownParser(
                SimpleMarkdownFlavourDescriptor,
                cancellationToken = CancellationToken.NonCancellable,
            )
            .buildMarkdownTreeFromString(content)

    private fun renderText(content: String): String =
        collectParagraphs(parse(content)).joinToString("\n") { paragraph ->
            content.buildMarkdownAnnotatedString(paragraph, TextStyle.Default, settings()).text
        }

    // Returns every URL annotation with the text it spans, across all paragraphs in the document.
    private fun renderLinks(content: String): List<Pair<LinkAnnotation.Url, String>> {
        val settings = settings()
        return collectParagraphs(parse(content)).flatMap { paragraph ->
            val annotated =
                content.buildMarkdownAnnotatedString(paragraph, TextStyle.Default, settings)
            annotated.getLinkAnnotations(0, annotated.length).mapNotNull { range ->
                (range.item as? LinkAnnotation.Url)?.let { url ->
                    url to annotated.text.substring(range.start, range.end)
                }
            }
        }
    }

    private fun settings(): AnnotatorSettings =
        DefaultAnnotatorSettings(
            linkTextSpanStyle = TextLinkStyles(),
            codeSpanStyle = SpanStyle(),
            annotator = NekoMarkdownAnnotator,
            referenceLinkHandler = ReferenceLinkHandlerImpl(),
        )

    private fun collectParagraphs(node: ASTNode): List<ASTNode> =
        if (node.type == MarkdownElementTypes.PARAGRAPH) listOf(node)
        else node.children.flatMap { collectParagraphs(it) }
}
