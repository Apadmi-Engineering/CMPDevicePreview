package com.apadmi.cmpdevicepreview.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A realistic Google Chrome web browser top bar with macOS traffic lights, active tab,
 * Omnibox navigation bar, and action icons.
 */
@Composable
public fun WebBrowserTopBar(
    title: String = "Web App",
    url: String = "https://localhost:8080/",
    isDark: Boolean = true,
    favicon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
): Unit {
    val tabStripBg = if (isDark) Color(0xFF1E1F22) else Color(0xFFDEE1E6)
    val tabBg = if (isDark) Color(0xFF2B2D30) else Color(0xFFFFFFFF)
    val tabTextColor = if (isDark) Color(0xFFE8EAED) else Color(0xFF3C4043)
    val tabCloseColor = if (isDark) Color(0xFF9AA0A6) else Color(0xFF5F6368)
    val addressStripBg = if (isDark) Color(0xFF2B2D30) else Color(0xFFFFFFFF)
    val navIconColor = if (isDark) Color(0xFFC4C7C5) else Color(0xFF444746)
    val navIconDisabledColor = if (isDark) Color(0xFF5E6062) else Color(0xFFB0B4BA)
    val urlBoxBg = if (isDark) Color(0xFF1E1F22) else Color(0xFFF1F3F4)
    val urlBoxBorder = if (isDark) Color(0xFF3C4043) else Color(0xFFE3E3E3)
    val urlPrimaryTextColor = if (isDark) Color(0xFFE8EAED) else Color(0xFF1F1F1F)
    val urlMutedTextColor = if (isDark) Color(0xFF8E918F) else Color(0xFF747775)
    val lockColor = if (isDark) Color(0xFF8AB4F8) else Color(0xFF1A73E8)
    val bottomDividerColor = if (isDark) Color(0xFF383A3E) else Color(0xFFE0E2E5)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tabStripBg)
    ) {
        // --- 1. Tab Strip (Top section with macOS traffic lights & Chrome tabs) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 8.dp, end = 14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // macOS Window Controls (Traffic Lights)
            Row(
                modifier = Modifier.padding(bottom = 8.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFFFF5F56), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFFFFBD2E), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF27C93F), CircleShape)
                )
            }

            // Active Tab (Chrome Tab shape)
            Surface(
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                color = tabBg,
                modifier = Modifier.width(260.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (favicon != null) {
                        favicon()
                    } else {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF4285F4), Color(0xFF9B51E0))
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = title,
                        color = tabTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tab",
                        tint = tabCloseColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // New Tab "+" Button
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .size(24.dp)
                    .background(Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab",
                    tint = navIconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // --- 2. Address / Navigation Toolbar (Omnibox Row) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(addressStripBg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = navIconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Forward",
                tint = navIconDisabledColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reload",
                tint = navIconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))

            // Omnibox (Address Bar)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = urlBoxBg,
                border = BorderStroke(1.dp, urlBoxBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Connection",
                        tint = lockColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val scheme = if (url.startsWith("https://")) "https://" else if (url.startsWith("http://")) "http://" else ""
                    val withoutScheme = url.removePrefix(scheme)
                    val slashIdx = withoutScheme.indexOf('/')
                    val host = if (slashIdx != -1) withoutScheme.substring(0, slashIdx) else withoutScheme
                    val path = if (slashIdx != -1) withoutScheme.substring(slashIdx) else ""

                    Text(
                        text = buildAnnotatedString {
                            if (scheme.isNotEmpty()) {
                                withStyle(SpanStyle(color = urlMutedTextColor, fontWeight = FontWeight.Normal)) {
                                    append(scheme)
                                }
                            }
                            withStyle(SpanStyle(color = urlPrimaryTextColor, fontWeight = FontWeight.Medium)) {
                                append(host)
                            }
                            if (path.isNotEmpty()) {
                                withStyle(SpanStyle(color = urlMutedTextColor, fontWeight = FontWeight.Normal)) {
                                    append(path)
                                }
                            }
                        },
                        fontSize = 12.5.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Default.StarBorder,
                        contentDescription = "Bookmark",
                        tint = navIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = "Extensions",
                tint = navIconColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF1A73E8), Color(0xFF4285F4))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Customize and control Google Chrome",
                tint = navIconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // --- 3. Bottom Divider Line ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(bottomDividerColor)
        )
    }
}

/**
 * Renders a full web browser frame window containing [WebBrowserTopBar] and the target [content].
 */
@Composable
public fun WebBrowserFrame(
    title: String = "Web App",
    url: String = "https://localhost:8080/",
    isDark: Boolean = true,
    modifier: Modifier = Modifier,
    favicon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
): Unit {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF202124) else Color(0xFFF1F3F4))
    ) {
        WebBrowserTopBar(
            title = title,
            url = url,
            isDark = isDark,
            favicon = favicon,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

