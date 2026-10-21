@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.apadmi.cmpdevicepreview.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.compose.resources.ComposeEnvironment
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.ScriptQualifier

// Compose Resources picks a `values-` folder from an environment it builds from the platform's
// locale, and the local that holds it is internal to it, so this file reaches past its visibility
// to give the content inside the device a language of its own. CONTRIBUTING.md lists this and the
// library's other uses of Compose internals.

// Null if the Compose Resources this runs with no longer has it.
private val LocalResourceEnvironment: ProvidableCompositionLocal<ComposeEnvironment>? = findLocalResourceEnvironment()

// The theme and density come from the outer environment, read where the content is: inside what
// simulates them for the device.
@OptIn(InternalResourceApi::class)
private class LanguageEnvironment(
    private val outer: ComposeEnvironment,
    private val locale: Locale,
) : ComposeEnvironment {
    @Composable
    override fun rememberEnvironment(): ResourceEnvironment {
        val base = outer.rememberEnvironment()
        return remember(base, locale) {
            ResourceEnvironment(
                LanguageQualifier(locale.language),
                ScriptQualifier(locale.script),
                RegionQualifier(locale.region),
                base.theme,
                base.density,
            )
        }
    }
}

@Composable
internal fun ProvideLanguage(language: String?, content: @Composable () -> Unit) {
    val local = LocalResourceEnvironment
    if (language == null || local == null) {
        content()
        return
    }
    val outer = local.current
    val environment = remember(outer, language) { LanguageEnvironment(outer, Locale(language)) }
    CompositionLocalProvider(local provides environment, content = content)
}

// An app can pair this library with a newer Compose Resources than it was built against, which may
// no longer have the local. The reference is then broken when the app runs: on the JVM it throws a
// LinkageError the first time it is read, and on iOS and the web Kotlin's partial linkage throws
// here instead. Catching it leaves the content in the host's language rather than crashing the
// preview. It catches Throwable because the error partial linkage throws is internal to the Kotlin
// runtime. The read happens once, here, because Compose doesn't allow a try/catch around composable
// calls.
private fun findLocalResourceEnvironment(): ProvidableCompositionLocal<ComposeEnvironment>? =
    try {
        org.jetbrains.compose.resources.LocalComposeEnvironment
    } catch (_: Throwable) {
        null
    }
