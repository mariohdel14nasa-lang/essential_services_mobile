package com.essential.services.ui.components.dialogs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.essential.services.R
import com.essential.services.utils.AboutLinkData
import com.essential.services.utils.SettingsMenuManager
import com.essential.services.utils.TranslationManager

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AboutSection(
    modifier: Modifier = Modifier,
    appName: String = stringResource(R.string.app_name),
    developerName: String = stringResource(R.string.app_developer_name),
    description: String = stringResource(R.string.app_description),
    links: List<AboutLinkData>? = null,
    onAvatarLongClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val versionName =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            "1.0"
        }

    val translatedDesc = TranslationManager.getString(context, description, description)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "$appName v$versionName",
                style = MaterialTheme.typography.headlineLarge,
                modifier =
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            onAvatarLongClick()
                        },
                    ),
            )
            Text(
                text = translatedDesc,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            val defaultDevBy = stringResource(R.string.developed_by_format, developerName)
            val translatedDevBy = TranslationManager.getString(context, "37", TranslationManager.getString(context, "developed_by_format", defaultDevBy))

            Text(
                text = translatedDevBy,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 3,
            ) {
                if (!links.isNullOrEmpty()) {
                    links.forEach { link ->
                        val iconRes = SettingsMenuManager.getDrawableResId(context, link.icon ?: "")
                        Button(
                            onClick = {
                                if (link.url.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, link.url.toUri())
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(TranslationManager.getString(context, link.title, link.title))
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            val websiteUrl = "https://sameerasw.com"
                            val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_globe_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(TranslationManager.getString(context, "action_website", stringResource(R.string.action_website)))
                    }

                    Button(
                        onClick = {
                            val websiteUrl = "https://github.com/sameerasw/MeDrop"
                            val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.brand_github),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(TranslationManager.getString(context, "action_view_on_github", stringResource(R.string.action_view_on_github)))
                    }

                    OutlinedButton(
                        onClick = {
                            val mailUri = "mailto:mail@sameerasw.com".toUri()
                            val emailIntent =
                                Intent(Intent.ACTION_SENDTO, mailUri).apply {
                                    putExtra(Intent.EXTRA_SUBJECT, "Hello from Essential Services")
                                }
                            try {
                                context.startActivity(
                                    Intent.createChooser(
                                        emailIntent,
                                        context.getString(R.string.send_email_chooser_title),
                                    ),
                                )
                            } catch (e: ActivityNotFoundException) {
                                Log.w("AboutSection", "No email app available", e)
                                Toast
                                    .makeText(context, R.string.error_no_email_app, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_mail_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(TranslationManager.getString(context, "action_contact", stringResource(R.string.action_contact)))
                    }
                }
            }
        }
    }
}
