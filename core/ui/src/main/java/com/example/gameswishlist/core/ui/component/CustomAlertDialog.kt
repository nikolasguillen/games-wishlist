package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

@Composable
fun CustomAlertDialog(
    title: String,
    confirmButtonText: String = "",
    onConfirm: () -> Unit = {},
    dismissButtonText: String = "",
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val dialogWidth = LocalWindowInfo.current.containerDpSize.width * 0.8f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = content,
        confirmButton = {
            if (confirmButtonText.isNotEmpty()) {
                TextButton(onClick = onConfirm) {
                    Text(text = confirmButtonText)
                }
            }
        },
        dismissButton = if (dismissButtonText.isNotEmpty()) {
            {
                TextButton(onClick = onDismiss) {
                    Text(text = dismissButtonText)
                }
            }
        } else null,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.width(dialogWidth)
    )
}

@Composable
fun CustomAlertDialog(
    title: String,
    message: AnnotatedString,
    confirmButtonText: String = "",
    onConfirm: () -> Unit = {},
    dismissButtonText: String = "",
    onDismiss: () -> Unit = {},
) {
    CustomAlertDialog(
        title = title,
        confirmButtonText = confirmButtonText,
        onConfirm = onConfirm,
        dismissButtonText = dismissButtonText,
        onDismiss = onDismiss,
        content = {
            Text(text = message)
        }
    )
}

@Composable
fun CustomAlertDialog(
    title: String,
    message: String,
    confirmButtonText: String = "",
    onConfirm: () -> Unit = {},
    dismissButtonText: String = "",
    onDismiss: () -> Unit = {}
) {
    CustomAlertDialog(
        title = title,
        message = buildAnnotatedString { append(message) },
        confirmButtonText = confirmButtonText,
        onConfirm = onConfirm,
        dismissButtonText = dismissButtonText,
        onDismiss = onDismiss
    )
}
