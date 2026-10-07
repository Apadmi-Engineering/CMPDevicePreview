package com.kappmaker.devicepreview.preview

internal actual fun relabelMouseAsTouch(startsOnNode: (x: Float, y: Float) -> Boolean): (() -> Unit)? {
    val stop = installMouseAsTouch { x, y -> startsOnNode(x.toFloat(), y.toFloat()) }
    return { callStop(stop) }
}

/**
 * Listens on the window, ahead of Compose's own listeners on its canvas, and replaces a mouse press
 * that starts on the node with the same events labelled as a touch. Compose then reads it as a
 * finger. Returns the function that removes the listeners.
 */
private fun installMouseAsTouch(startsOnNode: (Double, Double) -> Boolean): JsAny = js(
    """(() => {
        let pressed = null; // The pointer being relabelled, and the canvas its press started on.
        const relabel = (event, canvas) => {
            event.stopImmediatePropagation();
            const touch = new PointerEvent(event.type, {
                bubbles: event.bubbles, cancelable: event.cancelable, composed: event.composed,
                clientX: event.clientX, clientY: event.clientY,
                screenX: event.screenX, screenY: event.screenY,
                button: event.button, buttons: event.buttons, pressure: event.pressure,
                pointerId: event.pointerId, pointerType: 'touch', isPrimary: true, width: 1, height: 1,
                ctrlKey: event.ctrlKey, shiftKey: event.shiftKey,
                altKey: event.altKey, metaKey: event.metaKey,
            });
            if (!canvas.dispatchEvent(touch)) event.preventDefault();
        };
        const listener = (event) => {
            if (event.pointerType !== 'mouse') return;
            if (pressed) {
                if (event.pointerId !== pressed.id) return;
                const canvas = pressed.canvas;
                if (event.type === 'pointerup' || event.type === 'pointercancel') pressed = null;
                relabel(event, canvas);
                return;
            }
            if (event.type !== 'pointerdown' || event.button !== 0) return;
            const canvas = event.composedPath()[0];
            if (!(canvas instanceof HTMLCanvasElement)) return;
            // From CSS pixels to the canvas's own, which are Compose's window pixels.
            const rect = canvas.getBoundingClientRect();
            if (rect.width === 0 || rect.height === 0) return;
            const x = (event.clientX - rect.left) * canvas.width / rect.width;
            const y = (event.clientY - rect.top) * canvas.height / rect.height;
            if (!startsOnNode(x, y)) return;
            pressed = { id: event.pointerId, canvas: canvas };
            relabel(event, canvas);
        };
        const types = ['pointerdown', 'pointermove', 'pointerup', 'pointercancel'];
        for (const type of types) window.addEventListener(type, listener, true);
        return () => {
            for (const type of types) window.removeEventListener(type, listener, true);
        };
    })()"""
)

private fun callStop(stop: JsAny): Unit = js("stop()")
