package com.apadmi.devicepreview.preview

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
        let passedThrough = null; // A mouse press left as a mouse, such as one off the node.
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
        // The canvas under the event when it's on the node, or null.
        const canvasOnNode = (event) => {
            const canvas = event.composedPath()[0];
            if (!(canvas instanceof HTMLCanvasElement)) return null;
            // From CSS pixels to the canvas's own, which are Compose's window pixels.
            const rect = canvas.getBoundingClientRect();
            if (rect.width === 0 || rect.height === 0) return null;
            const x = (event.clientX - rect.left) * canvas.width / rect.width;
            const y = (event.clientY - rect.top) * canvas.height / rect.height;
            return startsOnNode(x, y) ? canvas : null;
        };
        const listener = (event) => {
            if (event.pointerType !== 'mouse') return;
            if (pressed) {
                if (event.pointerId !== pressed.id) return;
                // Compose doesn't read raw updates; a mouse one mid-touch would only confuse it.
                if (event.type === 'pointerrawupdate') {
                    event.stopImmediatePropagation();
                    return;
                }
                const canvas = pressed.canvas;
                if (event.type === 'pointerup' || event.type === 'pointercancel') pressed = null;
                relabel(event, canvas);
                return;
            }
            if (event.type === 'pointermove' || event.type === 'pointerrawupdate') {
                // Some browsers report a pressed button in a move just before its pointerdown.
                // Compose would take that move as a mouse press, and the touch that follows would
                // carry the pressed item along under the pointer until the release clicks it.
                if ((event.buttons & 1) !== 0 && event.pointerId !== passedThrough && canvasOnNode(event)) {
                    event.stopImmediatePropagation();
                }
                return;
            }
            if (event.type === 'pointerup' || event.type === 'pointercancel') {
                if (event.pointerId === passedThrough) passedThrough = null;
                return;
            }
            const canvas = event.button === 0 ? canvasOnNode(event) : null;
            if (!canvas) {
                passedThrough = event.pointerId;
                return;
            }
            pressed = { id: event.pointerId, canvas: canvas };
            relabel(event, canvas);
        };
        const types = ['pointerdown', 'pointermove', 'pointerrawupdate', 'pointerup', 'pointercancel'];
        for (const type of types) window.addEventListener(type, listener, true);
        return () => {
            for (const type of types) window.removeEventListener(type, listener, true);
        };
    })()"""
)

private fun callStop(stop: JsAny): Unit = js("stop()")
