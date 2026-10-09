package com.sensibotpro.ui.screens.support

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sensibotpro.ui.theme.*

/**
 * Open Source Licenses dialog displaying legal notices for Qwen3-0.6B,
 * Google LiteRT-LM, MediaPipe GenAI, and Android Jetpack libraries.
 */
@Composable
fun OpenSourceLicensesDialog(
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CharcoalSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OPEN SOURCE LICENSES",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "Third-party software & AI model credits",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable License Cards
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Qwen Language Model
                    LicenseCard(
                        title = "Qwen3-0.6B-Instruct / Qwen Series",
                        author = "Alibaba Cloud / Qwen Team",
                        licenseType = "Apache License 2.0 / Qwen Research License",
                        summary = "Permitted for commercial and research use. Developed by Alibaba Cloud. Deployed locally on-device without cloud transmission or per-message subscription.",
                        licenseSnippet = """Copyright (c) Alibaba Cloud. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
You may obtain a copy of the License at:
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied."""
                    )

                    // 2. Google LiteRT-LM / MediaPipe
                    LicenseCard(
                        title = "Google LiteRT-LM & MediaPipe Tasks GenAI",
                        author = "Google LLC",
                        licenseType = "Apache License 2.0",
                        summary = "High-performance on-device machine learning inference engine for Android.",
                        licenseSnippet = """Copyright 2024 The TensorFlow & MediaPipe Authors. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License."""
                    )

                    // 3. Android Jetpack & Kotlin
                    LicenseCard(
                        title = "Android Jetpack & Kotlin Coroutines",
                        author = "The Android Open Source Project & JetBrains s.r.o.",
                        licenseType = "Apache License 2.0",
                        summary = "UI, architecture components, state management, and coroutine primitives for Android.",
                        licenseSnippet = """Copyright 2024 The Android Open Source Project / JetBrains s.r.o.
Licensed under the Apache License, Version 2.0."""
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = RubyRedPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LicenseCard(
    title: String,
    author: String,
    licenseType: String,
    summary: String,
    licenseSnippet: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ElevatedSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = RubyRedPrimary
            )
            Text(
                text = "$author • $licenseType",
                style = MaterialTheme.typography.labelSmall,
                color = AccentAmber
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = VoidBlack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = licenseSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
