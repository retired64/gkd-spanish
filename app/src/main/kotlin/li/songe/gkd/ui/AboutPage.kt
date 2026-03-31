package li.songe.gkd.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
import li.songe.gkd.META
import li.songe.gkd.MainActivity
import li.songe.gkd.R
import li.songe.gkd.store.storeFlow
import li.songe.gkd.ui.component.PerfIcon
import li.songe.gkd.ui.component.PerfIconButton
import li.songe.gkd.ui.component.PerfTopAppBar
import li.songe.gkd.ui.component.RotatingLoadingIcon
import li.songe.gkd.ui.component.SettingItem
import li.songe.gkd.ui.component.TextListDialog
import li.songe.gkd.ui.component.TextMenu
import li.songe.gkd.ui.component.waitResult
import li.songe.gkd.ui.share.LocalDarkTheme
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.share.asMutableState
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.itemPadding
import li.songe.gkd.ui.style.titleItemPadding
import li.songe.gkd.util.ISSUES_URL
import li.songe.gkd.util.PLAY_STORE_URL
import li.songe.gkd.util.REPOSITORY_URL
import li.songe.gkd.util.ShortUrlSet
import li.songe.gkd.util.UpdateChannelOption
import li.songe.gkd.util.findOption
import li.songe.gkd.util.format
import li.songe.gkd.util.getShareApkFile
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.openUri
import li.songe.gkd.util.saveFileToDownloads
import li.songe.gkd.util.shareFile
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast

@Serializable
data object AboutRoute : NavKey

@Composable
fun AboutPage() {
    val context = LocalActivity.current as MainActivity
    val mainVm = LocalMainViewModel.current
    val vm = viewModel<AboutVm>()
    val store by storeFlow.collectAsState()

    var showInfoDlg by vm.showInfoDlgFlow.asMutableState()
    if (showInfoDlg) {
        AlertDialog(
            onDismissRequest = { showInfoDlg = false },
            title = { Text(text = "Información de versión") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column {
                        Text(text = "Canal de compilación")
                        Text(text = META.channel)
                    }
                    Column {
                        Text(text = "Código de versión")
                        Text(text = META.versionCode.toString())
                    }
                    Column {
                        Text(text = "Nombre de versión")
                        Text(text = META.versionName)
                    }
                    Column {
                        Text(text = "Registro de código")
                        Text(
                            modifier = Modifier.clickable { openUri(META.commitUrl) },
                            text = META.tagName ?: META.commitId.substring(0, 16),
                            color = MaterialTheme.colorScheme.primary,
                            style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                        )
                    }
                    Column {
                        Text(text = "Fecha de commit")
                        Text(text = META.commitTime.format("yyyy-MM-dd HH:mm:ss ZZ"))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfoDlg = false
                }) {
                    Text(text = "Cerrar")
                }
            },
        )
    }
    var showShareAppDlg by vm.showShareAppDlgFlow.asMutableState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PerfTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    PerfIconButton(
                        imageVector = PerfIcon.ArrowBack,
                        onClick = {
                            mainVm.popPage()
                        },
                    )
                },
                title = { Text(text = "Acerca de") },
                actions = {
                    PerfIconButton(
                        imageVector = PerfIcon.Share,
                        onClick = {
                            showShareAppDlg = true
                        },
                    )
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedLogoIcon(
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = throttle { toast("¿Qué haces? ¡Ay!") }
                        )
                        .fillMaxWidth(0.33f)
                        .aspectRatio(1f)
                )
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable(onClick = { showInfoDlg = true })
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = META.appName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = META.versionName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            SettingItem(
                imageVector = null,
                title = "Código fuente",
                onClick = {
                    mainVm.openUrl(REPOSITORY_URL)
                },
            )
            if (META.isGkdChannel) {
                SettingItem(
                    imageVector = null,
                    title = "Donar",
                    onClick = {
                        mainVm.navigateWebPage(ShortUrlSet.URL10)
                    },
                )
            }
            SettingItem(
                imageVector = null,
                title = "Términos de uso",
                onClick = {
                    mainVm.navigateWebPage(ShortUrlSet.URL12)
                },
            )
            SettingItem(
                imageVector = null,
                title = "Política de privacidad",
                onClick = {
                    mainVm.navigateWebPage(ShortUrlSet.URL11)
                },
            )

            Text(
                text = "Comentarios",
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier
                    .clickable(onClick = throttle(mainVm.viewModelScope.launchAsFn {
                        mainVm.dialogFlow.waitResult(
                            title = "Aviso de comentarios",
                            textContent = {
                                Text(text = buildAnnotatedString {
                                    val highlightStyle = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    append("Gracias por tomarte el tiempo de enviarnos tus comentarios. ")
                                    withStyle(style = highlightStyle) {
                                        append("GKD no incluye ninguna regla por defecto, solo se aceptan comentarios relacionados con la funcionalidad de la propia app")
                                    }
                                    append("\n\n")
                                    append("Por favor, comprueba primero si el problema es de una suscripción de reglas de terceros. Si es así, debes reportarlo al proveedor de reglas, no aquí. ")
                                    withStyle(style = highlightStyle) {
                                        append("Si ya estás seguro de que el problema es de la propia app GKD")
                                    }
                                    append(", puedes continuar haciendo clic abajo")
                                })
                            },
                            confirmText = "Continuar",
                            dismissRequest = true,
                        )
                        mainVm.openUrl(ISSUES_URL)
                    }))
                    .fillMaxWidth()
                    .itemPadding()
            ) {
                Text(
                    text = "Reportar problema",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            SettingItem(
                title = "Exportar registro",
                imageVector = PerfIcon.Share,
                onClick = {
                    mainVm.showShareLogDlgFlow.value = true
                }
            )
            if (mainVm.updateStatus != null) {
                Text(
                    text = "Actualizar",
                    modifier = Modifier.titleItemPadding(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextMenu(
                    title = "Canal de actualización",
                    option = UpdateChannelOption.objects.findOption(store.updateChannel)
                ) {
                    if (mainVm.updateStatus.checkUpdatingFlow.value) return@TextMenu
                    if (it.value == UpdateChannelOption.Beta.value) {
                        mainVm.viewModelScope.launchTry {
                            mainVm.dialogFlow.waitResult(
                                title = "Canal de versión",
                                text = "El canal de versión beta se actualiza más rápido\npero es inestable y puede tener más errores\núsalo con precaución",
                            )
                            storeFlow.update { s -> s.copy(updateChannel = it.value) }
                        }
                    } else {
                        storeFlow.update { s -> s.copy(updateChannel = it.value) }
                    }
                }
                Row(
                    modifier = Modifier
                        .clickable(
                            onClick = throttle {
                                mainVm.updateStatus.checkUpdate(true)
                            }
                        )
                        .fillMaxWidth()
                        .itemPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Buscar actualizaciones",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    RotatingLoadingIcon(loading = mainVm.updateStatus.checkUpdatingFlow.collectAsState().value)
                }
            }
            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }

    if (showShareAppDlg) {
        TextListDialog(
            onDismiss = { showShareAppDlg = false },
            textList = listOf(
                "Compartir en otra app" to mainVm.viewModelScope.launchAsFn(Dispatchers.IO) {
                    if (!META.isGkdChannel) {
                        mainVm.dialogFlow.waitResult(
                            title = "Aviso de compartir",
                            textContent = { Text(text = exportPlayTipTemplate()) },
                            confirmText = "Continuar",
                        )
                    }
                    context.shareFile(getShareApkFile(), "Compartir archivo de instalación")
                },
                "Guardar en Descargas" to mainVm.viewModelScope.launchAsFn(Dispatchers.IO) {
                    if (!META.isGkdChannel) {
                        mainVm.dialogFlow.waitResult(
                            title = "Aviso de guardado",
                            textContent = { Text(text = exportPlayTipTemplate()) },
                            confirmText = "Continuar",
                        )
                    }
                    context.saveFileToDownloads(getShareApkFile())
                },
                "Google Play" to {
                    mainVm.openUrl(PLAY_STORE_URL)
                },
            )
        )
    }
}

@Composable
private fun exportPlayTipTemplate(): AnnotatedString {
    return buildAnnotatedString {
        append("El archivo APK exportado solo funciona en dispositivos con Google Play Services instalado, de lo contrario mostrará un error al abrirse. ")
        withLink(
            LinkAnnotation.Url(
                ShortUrlSet.URL13,
                TextLinkStyles(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                )
            )
        ) {
            append("Se recomienda descargarlo desde el sitio oficial")
        }
        append(", o haz clic abajo para continuar")
    }
}

@Composable
private fun AnimatedLogoIcon(
    modifier: Modifier = Modifier
) {
    val darkTheme = LocalDarkTheme.current
    val colorRid = if (darkTheme) R.color.better_white else R.color.better_black
    var atEnd by remember { mutableStateOf(false) }
    val animation = AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_anim_logo)
    val painter = rememberAnimatedVectorPainter(
        animation,
        atEnd
    )
    LaunchedEffect(Unit) {
        while (isActive) {
            atEnd = !atEnd
            delay(animation.totalDuration.toLong())
        }
    }
    Icon(
        modifier = modifier,
        painter = painter,
        contentDescription = null,
        tint = colorResource(colorRid),
    )
}
