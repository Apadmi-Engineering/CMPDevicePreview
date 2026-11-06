package com.apadmi.cmpdevicepreview.sample

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView

import org.w3c.dom.HTMLElement

import kotlinx.browser.document

@Suppress("NULLABLE_PROPERTY_TYPE")  // The expect's type, which is null where there's no browser.
actual val embeddedHtmlCard: (@Composable (Modifier) -> Unit)? = { EmbeddedHtmlCard(it) }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EmbeddedHtmlCard(modifier: Modifier = Modifier) {
    HtmlElementView(factory = ::createCard, modifier = modifier.height(120.dp))
}

private fun createCard(): HTMLElement {
    val card = createElement("div")
    card.style.cssText = "box-sizing: border-box; display: flex; flex-direction: column; justify-content: center; gap: 8px; " +
            "padding: 16px; border-radius: 20px; background: linear-gradient(135deg, #f2994a, #b4234b); " +
            "color: white; font-family: sans-serif;"
    val title = createElement("strong").apply {
        textContent = "An HTML element"
    }
    val button = createElement("button")
    button.style.cssText = "align-self: flex-start; padding: 6px 14px; border: 0; border-radius: 16px; font-size: 14px;"
    button.textContent = "Click me"
    var clicks = 0
    button.addEventListener("click", { button.textContent = "Clicked ${++clicks}" })
    card.append(title, button)
    return card
}

private fun createElement(tagName: String) = document.createElement(tagName) as HTMLElement
