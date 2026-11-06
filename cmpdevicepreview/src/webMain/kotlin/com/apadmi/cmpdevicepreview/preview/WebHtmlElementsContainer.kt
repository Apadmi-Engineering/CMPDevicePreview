@file:OptIn(ExperimentalWasmJsInterop::class)

package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.viewinterop.HtmlElementView

import org.w3c.dom.HTMLElement

import kotlinx.browser.document

// Every device whose marker has found its container, so that a container two share is left alone.
private val attached = mutableListOf<WebHtmlElementsContainer>()

// Relies on Compose 1.12's web interop: a viewport's HtmlElementViews are each in a wrapper in one
// container at the canvas's origin, placed at window pixels over the density they're composed at.
internal class WebHtmlElementsContainer : HtmlElementsContainer {
    private var container: JsAny? = null
    private var style: HtmlElementsStyle? = null

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Marker() {
        HtmlElementView(
            factory = { document.createElement("div") as HTMLElement },
            update = ::attach,
            onRelease = { detach() },
        )
    }

    // Compose first calls it once the marker is in its wrapper, before that frame's positions are dispatched.
    fun attach(marker: JsAny) {
        if (container != null) {
            return
        }
        val found = takeContainer(marker) ?: return
        container = found
        attached += this
        restyleShared(found)
    }

    fun detach() {
        val detached = container ?: return
        container = null
        attached -= this
        restyleShared(detached)
    }

    override fun restyle(style: HtmlElementsStyle?) {
        this.style = style
        container?.let(::restyleShared)
    }

    private fun restyleShared(shared: JsAny) {
        val sharing = attached.filter { device -> device.container?.let { isSame(it, shared) } == true }
        val style = sharing.singleOrNull()?.style
        if (style == null) {
            unstyle(shared)
        } else {
            applyStyle(shared, style.transform, style.clipPath)
        }
    }
}

@Composable
internal actual fun rememberHtmlElementsContainer(): HtmlElementsContainer? = remember { WebHtmlElementsContainer() }

// Compose rounds the marker's zero size up to a pixel, so its wrapper is kept off the pointer.
private fun takeContainer(marker: JsAny): JsAny? = js(
    """(() => {
        const wrapper = marker.parentElement;
        if (!wrapper || !wrapper.parentElement) return null;
        wrapper.style.setProperty('pointer-events', 'none');
        return wrapper.parentElement;
    })()"""
)

private fun applyStyle(
    container: JsAny,
    transform: String,
    clipPath: String
): Unit = js(
    """(() => {
        container.style.setProperty('transform-origin', '0 0');
        container.style.setProperty('transform', transform);
        container.style.setProperty('clip-path', clipPath);
    })()"""
)

private fun unstyle(container: JsAny): Unit = js(
    """(() => {
        container.style.removeProperty('transform-origin');
        container.style.removeProperty('transform');
        container.style.removeProperty('clip-path');
    })()"""
)

private fun isSame(first: JsAny, second: JsAny): Boolean = js("first === second")
