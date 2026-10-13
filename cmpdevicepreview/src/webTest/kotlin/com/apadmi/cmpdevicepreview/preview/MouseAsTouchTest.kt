@file:OptIn(ExperimentalWasmJsInterop::class)

package com.apadmi.cmpdevicepreview.preview

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Dispatches mouse events at a 200 × 100 canvas whose left half is the node. */
class MouseAsTouchTest {

    private lateinit var canvas: JsAny
    private lateinit var received: JsAny

    @BeforeTest
    fun addCanvas() {
        canvas = addCanvas(width = 200, height = 100)
        received = recordPointerTypes(canvas)
    }

    @AfterTest
    fun removeCanvas() = removeElement(canvas)

    private fun pressAndRelease(x: Int) {
        dispatchMouse(canvas, "pointerdown", x, buttons = 1)
        dispatchMouse(canvas, "pointermove", x + 10, buttons = 1)
        dispatchMouse(canvas, "pointerup", x + 10, buttons = 0)
    }

    @Test
    fun aPressThatStartsOnTheNodeArrivesAsATouch() {
        val stop = assertNotNull(relabelMouseAsTouch { x, _ -> x < 100f })
        try {
            pressAndRelease(x = 50)
        } finally {
            stop()
        }
        assertEquals("pointerdown:touch pointermove:touch pointerup:touch", joined(received))
    }

    @Test
    fun aPressThatStartsOffTheNodeStaysAMouse() {
        val stop = assertNotNull(relabelMouseAsTouch { x, _ -> x < 100f })
        try {
            pressAndRelease(x = 150)
        } finally {
            stop()
        }
        assertEquals("pointerdown:mouse pointermove:mouse pointerup:mouse", joined(received))
    }

    @Test
    fun onceStoppedPressesStayMice() {
        assertNotNull(relabelMouseAsTouch { _, _ -> true })()
        pressAndRelease(x = 50)
        assertEquals("pointerdown:mouse pointermove:mouse pointerup:mouse", joined(received))
    }
}

/** A canvas at the top-left of the page whose pixels are CSS pixels. */
private fun addCanvas(width: Int, height: Int): JsAny = js(
    """(() => {
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        canvas.style.cssText = 'position: fixed; left: 0; top: 0; width: ' + width + 'px; height: ' + height + 'px';
        document.body.appendChild(canvas);
        return canvas;
    })()"""
)

private fun removeElement(element: JsAny): Unit = js("element.remove()")

/** Records "type:pointerType" for each pointer event that reaches [canvas]. */
private fun recordPointerTypes(canvas: JsAny): JsAny = js(
    """(() => {
        const received = [];
        for (const type of ['pointerdown', 'pointermove', 'pointerup']) {
            canvas.addEventListener(type, (event) => received.push(event.type + ':' + event.pointerType));
        }
        return received;
    })()"""
)

private fun dispatchMouse(canvas: JsAny, type: String, x: Int, buttons: Int): Unit = js(
    """canvas.dispatchEvent(new PointerEvent(type, {
        bubbles: true, cancelable: true, composed: true, pointerId: 1, pointerType: 'mouse', isPrimary: true,
        clientX: x, clientY: 50, button: 0, buttons: buttons,
    }))"""
)

private fun joined(list: JsAny): String = js("list.join(' ')")
