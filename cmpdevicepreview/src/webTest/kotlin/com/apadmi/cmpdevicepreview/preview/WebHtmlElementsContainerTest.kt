@file:OptIn(ExperimentalWasmJsInterop::class)

package com.apadmi.cmpdevicepreview.preview

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// The device's content is at a density of 1 on a host of 2, so Compose places elements at twice their size.
private const val Scale = "scale(0.5)"

// The page from 60 CSS pixels down, in the container's units, which are twice the page's.
private const val BelowTheBar = "path(nonzero, \"M0 120L1600 120L1600 1200L0 1200Z\")"

/**
 * Lays out Compose's DOM for a viewport by hand: a container at the page's origin holding the
 * marker's wrapper and an element's, which Compose placed at window pixels over the content's
 * density, so at (200, 100) and 120 × 80 for one at (100, 50) and 60 × 40 on the page.
 */
class WebHtmlElementsContainerTest {
    private lateinit var root: JsAny
    private lateinit var container: JsAny
    private lateinit var element: JsAny

    @BeforeTest
    fun addViewport() {
        root = addRoot()
        container = addChild(root, "position: absolute; top: 0px; left: 0px;")
        element = addWrapped(container, left = 200, top = 100, width = 120, height = 80)
    }

    @AfterTest
    fun removeViewport() = removeElement(root)

    private fun attachedDevice(): WebHtmlElementsContainer =
        WebHtmlElementsContainer().apply { attach(addWrapped(container, left = 0, top = 0, width = 1, height = 1)) }

    @Test
    fun anElementLandsWhereTheDeviceLaidItOut() {
        attachedDevice().restyle(HtmlElementsStyle(Scale, BelowTheBar))
        assertEquals("100 50 60 40", boundsOf(element))
    }

    @Test
    fun anElementOutsideTheClipDoesNotTakeThePointer() {
        attachedDevice().restyle(HtmlElementsStyle(Scale, BelowTheBar))
        assertFalse(isAt(element, x = 130, y = 55))
        assertTrue(isAt(element, x = 130, y = 75))
    }

    @Test
    fun elementsAreHiddenWhileTheDeviceTurns() {
        attachedDevice().restyle(HtmlElementsStyle(Scale, HiddenClipPath))
        assertFalse(isAt(element, x = 130, y = 75))
    }

    @Test
    fun theMarkerNeverTakesThePointer() {
        attachedDevice()
        assertTrue(isAt(root, x = 0, y = 0))
    }

    @Test
    fun theContainerIsRestoredWhenTheDeviceLeaves() {
        val device = attachedDevice()
        device.restyle(HtmlElementsStyle(Scale, BelowTheBar))
        device.detach()
        assertEquals("position: absolute; top: 0px; left: 0px;", styleOf(container))
    }

    @Test
    fun aContainerTwoDevicesShareIsLeftAlone() {
        val first = attachedDevice()
        first.restyle(HtmlElementsStyle(Scale, BelowTheBar))
        val second = attachedDevice()
        second.restyle(HtmlElementsStyle(Scale, BelowTheBar))
        assertEquals("position: absolute; top: 0px; left: 0px;", styleOf(container))
        second.detach()
        assertEquals("100 50 60 40", boundsOf(element))
        first.detach()
    }
}

private fun addRoot(): JsAny = js(
    """(() => {
        const root = document.createElement('div');
        root.style.cssText = 'position: fixed; left: 0px; top: 0px; width: 800px; height: 600px;';
        document.body.appendChild(root);
        return root;
    })()"""
)

private fun addChild(parent: JsAny, cssText: String): JsAny = js(
    """(() => {
        const child = document.createElement('div');
        child.setAttribute('style', cssText);
        parent.appendChild(child);
        return child;
    })()"""
)

// An element in a wrapper styled as Compose styles them, returning the element.
private fun addWrapped(
    container: JsAny,
    left: Int,
    top: Int,
    width: Int,
    height: Int
): JsAny = js(
    """(() => {
        const wrapper = document.createElement('div');
        wrapper.style.cssText = 'position: absolute; visibility: visible; clip-path: inset(0px); left: ' + left +
            'px; top: ' + top + 'px; width: ' + width + 'px; height: ' + height + 'px;';
        const element = document.createElement('div');
        element.style.cssText = 'width: 100%; height: 100%;';
        wrapper.appendChild(element);
        container.appendChild(wrapper);
        return element;
    })()"""
)

private fun removeElement(element: JsAny): Unit = js("element.remove()")

private fun boundsOf(element: JsAny): String = js(
    """(() => {
        const rect = element.getBoundingClientRect();
        return [rect.left, rect.top, rect.width, rect.height].map(Math.round).join(' ');
    })()"""
)

private fun isAt(
    element: JsAny,
    x: Int,
    y: Int
): Boolean = js("document.elementFromPoint(x, y) === element")

private fun styleOf(element: JsAny): String = js("element.getAttribute('style')")
