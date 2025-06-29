package com.damn.anotherglass.ui.notifications.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.damn.anotherglass.R
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.anotherglass.utility.AppDetails
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun NotificationHistoryItemView(
    notification: NotificationData,
    appDetails: AppDetails?,
    formattedTime: String,
    onCreateFilter: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    appDetails?.icon?.let {
                        Image(
                            painter = rememberDrawablePainter(drawable = it),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = appDetails?.appName ?: notification.packageName ?: "Unknown App",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (!notification.title.isNullOrBlank()) {
                Text(
                    text = notification.title!!,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            if (!notification.text.isNullOrBlank()) {
                Text(
                    text = notification.text!!,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            if (!notification.tickerText.isNullOrBlank()
                && notification.tickerText != notification.title
                && notification.tickerText != notification.text
            ) {
                Text(
                    text = "Ticker: ${notification.tickerText}",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (notification.isOngoing) {
                Text(
                    text = "Ongoing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onCreateFilter,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    Icons.Filled.FilterAlt,
                    contentDescription = "Filter Icon",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_create_filter))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationHistoryItemPreview() {
    val sampleNotification = createNotificationData(
        NotificationData.Action.Posted,
        123,
        "com.sample.app",
        System.currentTimeMillis(),
        true,
        "Sample Notification Title - Very Long Title That Will Likely Overflow",
        "This is the main text of the notification, it can also be quite long and might need to be truncated after a few lines to keep the UI clean and readable.",
        "Ticker: Short summary"
    )
    MaterialTheme {
        NotificationHistoryItemView(
            notification = sampleNotification,
            appDetails = AppDetails("Sample App", null),
            formattedTime = "Jan 01, 12:00:00",
            onCreateFilter = {}
        )
    }
}