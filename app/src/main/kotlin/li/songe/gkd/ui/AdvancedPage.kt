package li.songe.gkd.ui

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.dylanc.activityresult.launcher.launchForResult
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import li.songe.gkd.MainActivity
import li.songe.gkd.R
import li.songe.gkd.permission.canDrawOverlaysState
import li.songe.gkd.permission.foregroundServiceSpecialUseState
import li.songe.gkd.permission.notificationState
import li.songe.gkd.permission.requiredPermission
import li.songe.gkd.permission.shizukuGrantedState
import li.songe.gkd.service.ActivityService
import li.songe.gkd.service.ButtonService
import li.songe.gkd.service.EventService
import li.songe.gkd.service.HttpService
import li.songe.gkd.service.ScreenshotService
import li.songe.gkd.shizuku.shizukuContextFlow
import li.songe.gkd.shizuku.updateBinderMutex
import li.songe.gkd.store.storeFlow
import li.songe.gkd.ui.component.AuthCard
import li.songe.gkd.ui.component.CustomOutlinedTextField
import li.songe.gkd.ui.component.PerfCustomIconButton
import li.songe.gkd.ui.component.PerfIcon
import li.songe.gkd.ui.component.PerfIconButton
import li.songe.gkd.ui.component.PerfSwitch
import li.songe.gkd.ui.component.PerfTopAppBar
import li.songe.gkd.ui.component.SettingItem
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.component.autoFocus
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.share.asMutableState
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.iconTextSize
import li.songe.gkd.ui.style.itemPadding
import li.songe.gkd.ui.style.titleItemPadding
import li.songe.gkd.util.AndroidTarget
import li.songe.gkd.util.ShortUrlSet
import li.songe.gkd.util.appInfoMapFlow
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast
import li.songe.selector.Selector

@Serializable
data object AdvancedPageRoute : NavKey

@Composable
fun AdvancedPage() {
    val context = LocalActivity.current as MainActivity
    val mainVm = LocalMainViewModel.current
    val vm = viewModel<AdvancedVm>()
    val store by storeFlow.collectAsState()

    var showEditPortDlg by vm.showEditPortDlgFlow.asMutableState()
    if (showEditPortDlg) {
        val portRange = remember { 1000 to 65535 }
        val placeholderText = remember { "Introduce un número entre ${portRange.first} y ${portRange.second}" }
        var value by remember {
            mutableStateOf(store.httpServerPort.toString())
        }
        AlertDialog(
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text(text = "Puerto de servicio") },
            text = {
                OutlinedTextField(
                    value = value,
                    placeholder = {
                        Text(text = placeholderText)
                    },
                    onValueChange = {
                        value = it.filter { c -> c.isDigit() }.take(5)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .autoFocus(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text(
                            text = "${value.length} / 5",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    },
                )
            },
            onDismissRequest = {
                showEditPortDlg = false
            },
            confirmButton = {
                TextButton(
                    enabled = value.isNotEmpty(),
                    onClick = {
                        val newPort = value.toIntOrNull()
                        if (newPort == null || !(portRange.first <= newPort && newPort <= portRange.second)) {
                            toast(placeholderText)
                            return@TextButton
                        }
                        showEditPortDlg = false
                        if (newPort != store.httpServerPort) {
                            storeFlow.value = store.copy(
                                httpServerPort = newPort
                            )
                            toast("Actualizado correctamente")
                        }
                    }
                ) {
                    Text(
                        text = "Aceptar", modifier = Modifier
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPortDlg = false }) {
                    Text(
                        text = "Cancelar"
                    )
                }
            }
        )
    }

    var showShizukuState by vm.showShizukuStateFlow.asMutableState()
    if (showShizukuState) {
        val onDismissRequest = { showShizukuState = false }
        AlertDialog(
            title = { Text(text = "Estado de autorización") },
            text = {
                val states = shizukuContextFlow.collectAsState().value.states
                Column {
                    states.forEach { (name, value) ->
                        Text(
                            text = name,
                            textDecoration = if (value != null) null else TextDecoration.LineThrough,
                        )
                    }
                }
            },
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = "Entendido")
                }
            },
        )
    }

    var showCaptureScreenshotDlg by vm.showCaptureScreenshotDlgFlow.asMutableState()
    if (showCaptureScreenshotDlg) {
        var appIdValue by remember { mutableStateOf(store.screenshotTargetAppId) }
        var eventSelectorValue by remember { mutableStateOf(store.screenshotEventSelector) }
        AlertDialog(
            properties = DialogProperties(dismissOnClickOutside = false),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Captura de pantalla rápida")
                    PerfIconButton(
                        imageVector = PerfIcon.HelpOutline,
                        onClick = throttle {
                            showCaptureScreenshotDlg = false
                            mainVm.navigateWebPage(ShortUrlSet.URL15)
                        },
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CustomOutlinedTextField(
                        label = { Text("ID de aplicación") },
                        value = appIdValue,
                        placeholder = { Text(text = "Introduce el ID de la app objetivo") },
                        onValueChange = {
                            appIdValue = it
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomOutlinedTextField(
                        label = { Text("Selector de evento característico") },
                        value = eventSelectorValue,
                        placeholder = { Text(text = "Introduce el selector de evento característico") },
                        onValueChange = {
                            eventSelectorValue = it
                        },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .autoFocus(),
                    )
                }
            },
            onDismissRequest = {
                showCaptureScreenshotDlg = false
            },
            confirmButton = {
                TextButton(onClick = throttle {
                    if (appIdValue == store.screenshotTargetAppId && eventSelectorValue == store.screenshotEventSelector) {
                        showCaptureScreenshotDlg = false
                        return@throttle
                    }
                    if (appIdValue.isNotEmpty() && !appInfoMapFlow.value.contains(appIdValue)) {
                        toast("ID de aplicación inválido")
                        return@throttle
                    }
                    if (eventSelectorValue.isNotEmpty()) {
                        val s = Selector.parseOrNull(eventSelectorValue)
                        if (s == null) {
                            toast("Selector de evento inválido")
                            return@throttle
                        }
                    }
                    storeFlow.update {
                        it.copy(
                            screenshotTargetAppId = appIdValue,
                            screenshotEventSelector = eventSelectorValue,
                        )
                    }
                    toast("Actualizado correctamente")
                    showCaptureScreenshotDlg = false
                }) {
                    Text(
                        text = "Aceptar",
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCaptureScreenshotDlg = false }) {
                    Text(
                        text = "Cancelar",
                    )
                }
            })
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PerfTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    PerfIconButton(imageVector = PerfIcon.ArrowBack, onClick = {
                        mainVm.popPage()
                    })
                },
                title = { Text(text = "Ajustes avanzados") },
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .titleItemPadding(showTop = false),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier,
                    text = "Shizuku",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                PerfIcon(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable(onClickLabel = "Abrir estado de Shizuku", onClick = throttle {
                            showShizukuState = true
                        })
                        .iconTextSize(textStyle = MaterialTheme.typography.titleSmall),
                    imageVector = PerfIcon.Api,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Estado de Shizuku",
                )
            }
            val shizukuGranted by shizukuGrantedState.stateFlow.collectAsState()
            AnimatedVisibility(store.enableShizuku && !shizukuGranted) {
                AuthCard(
                    title = "Sin autorización",
                    subtitle = "Toca para autorizar y mejorar la experiencia",
                    onAuthClick = {
                        mainVm.requestShizuku()
                    }
                )
            }
            TextSwitch(
                title = "Activar optimización",
                subtitle = "Aumenta permisos para mejorar la experiencia",
                suffix = "Saber más",
                suffixUnderline = true,
                onSuffixClick = { mainVm.navigateWebPage(ShortUrlSet.URL14) },
                checked = store.enableShizuku,
                suffixIcon = {
                    if (updateBinderMutex.state.collectAsState().value) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp),
                        )
                    }
                },
                onCheckedChange = {
                    mainVm.switchEnableShizuku(it)
                },
                onClick = null,
            )

            val server by HttpService.httpServerFlow.collectAsState()
            val httpServerRunning = server != null
            val localNetworkIps by HttpService.localNetworkIpsFlow.collectAsState()

            Text(
                text = "HTTP",
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.itemPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Servicio HTTP",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium
                    ) {
                        Text(text = if (httpServerRunning) "Toca el enlace para conectar automáticamente" else "Conéctate con la herramienta de depuración desde el navegador")
                        AnimatedVisibility(httpServerRunning) {
                            Column {
                                Row {
                                    val localUrl = "http://127.0.0.1:${store.httpServerPort}"
                                    Text(
                                        text = localUrl,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                                        modifier = Modifier.clickable(onClick = throttle {
                                            mainVm.openUrl(localUrl)
                                        }),
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(text = "Solo accesible desde este dispositivo")
                                }
                                localNetworkIps.forEach { host ->
                                    val lanUrl = "http://${host}:${store.httpServerPort}"
                                    Text(
                                        text = lanUrl,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                                        modifier = Modifier.clickable(onClick = throttle {
                                            mainVm.openUrl(lanUrl)
                                        })
                                    )
                                }
                            }
                        }
                    }
                }
                PerfSwitch(
                    checked = httpServerRunning,
                    onCheckedChange = throttle(fn = vm.viewModelScope.launchAsFn<Boolean> {
                        if (it) {
                            requiredPermission(context, foregroundServiceSpecialUseState)
                            requiredPermission(context, notificationState)
                            HttpService.start()
                        } else {
                            HttpService.stop()
                        }
                    })
                )
            }

            SettingItem(
                title = "Puerto de servicio",
                subtitle = store.httpServerPort.toString(),
                imageVector = PerfIcon.Edit,
                onClickLabel = "Editar puerto de servicio",
                onClick = {
                    showEditPortDlg = true
                }
            )

            TextSwitch(
                title = "Limpiar suscripciones",
                subtitle = "Eliminar suscripciones en memoria al detener el servicio",
                checked = store.autoClearMemorySubs,
                onCheckedChange = {
                    storeFlow.update {
                        it.copy(autoClearMemorySubs = !it.autoClearMemorySubs)
                    }
                }
            )

            Text(
                text = "Instantánea",
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            SettingItem(
                title = "Registro de instantáneas",
                subtitle = "Información de nodos e imágenes de la interfaz",
                onClick = {
                    mainVm.navigatePage(SnapshotPageRoute)
                }
            )

            if (!AndroidTarget.R) {
                val screenshotRunning by ScreenshotService.isRunning.collectAsState()
                TextSwitch(
                    title = "Servicio de captura",
                    subtitle = "Requiere captura de pantalla para generar instantáneas",
                    checked = screenshotRunning,
                    onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                        if (it) {
                            requiredPermission(context, notificationState)
                            val mediaProjectionManager =
                                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            val activityResult =
                                context.launcher.launchForResult(mediaProjectionManager.createScreenCaptureIntent())
                            if (activityResult.resultCode == Activity.RESULT_OK && activityResult.data != null) {
                                ScreenshotService.start(intent = activityResult.data!!)
                            }
                        } else {
                            ScreenshotService.stop()
                        }
                    }
                )
            }

            TextSwitch(
                title = "Botón de instantánea",
                subtitle = "Muestra un botón para guardar instantáneas",
                checked = ButtonService.isRunning.collectAsState().value,
                onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        requiredPermission(context, foregroundServiceSpecialUseState)
                        requiredPermission(context, notificationState)
                        requiredPermission(context, canDrawOverlaysState)
                        ButtonService.start()
                    } else {
                        ButtonService.stop()
                    }
                },
            )

            TextSwitch(
                title = "Instantánea por volumen",
                subtitle = "Guardar instantánea al cambiar el volumen",
                checked = store.captureVolumeChange,
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        captureVolumeChange = it
                    )
                },
            )

            TextSwitch(
                title = "Instantánea por captura",
                subtitle = "Guardar instantánea al hacer captura de pantalla",
                checked = store.captureScreenshot,
                suffixIcon = {
                    PerfCustomIconButton(
                        size = 32.dp,
                        iconSize = 20.dp,
                        onClickLabel = "Abrir configuración de instantánea por captura",
                        onClick = throttle {
                            showCaptureScreenshotDlg = true
                        },
                        id = R.drawable.ic_page_info,
                        contentDescription = "Configuración de instantánea por captura",
                    )
                },
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        captureScreenshot = it
                    )
                    if (it && store.screenshotTargetAppId.isEmpty() || store.screenshotEventSelector.isEmpty()) {
                        toast("Configura la app objetivo y el selector de evento característico")
                    }
                }
            )

            TextSwitch(
                title = "Ocultar barra de estado",
                subtitle = "Ocultar la barra de estado en las capturas de instantánea",
                checked = store.hideSnapshotStatusBar,
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        hideSnapshotStatusBar = it
                    )
                }
            )

            TextSwitch(
                title = "Aviso de guardado",
                subtitle = "Mostrar «Guardando instantánea»",
                checked = store.showSaveSnapshotToast,
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        showSaveSnapshotToast = it
                    )
                }
            )

            SettingItem(
                title = "Github Cookie",
                subtitle = "Generar enlace de instantánea/registro",
                suffix = "Ver tutorial",
                suffixUnderline = true,
                onSuffixClick = {
                    mainVm.navigateWebPage(ShortUrlSet.URL1)
                },
                imageVector = PerfIcon.Edit,
                onClick = {
                    mainVm.showEditCookieDlgFlow.value = true
                }
            )

            Text(
                text = "Registro",
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            SettingItem(
                title = "Registro de pantallas",
                subtitle = "Registro de cambios de pantalla",
                onClick = {
                    mainVm.navigatePage(ActivityLogRoute)
                }
            )
            TextSwitch(
                title = "Servicio de pantalla",
                subtitle = "Muestra información de la pantalla actual",
                checked = ActivityService.isRunning.collectAsState().value,
                onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        requiredPermission(context, foregroundServiceSpecialUseState)
                        requiredPermission(context, notificationState)
                        requiredPermission(context, canDrawOverlaysState)
                        ActivityService.start()
                    } else {
                        ActivityService.stop()
                    }
                }
            )
            SettingItem(
                title = "Registro de eventos",
                subtitle = "Registro de eventos de accesibilidad",
                onClick = {
                    mainVm.navigatePage(A11yEventLogRoute)
                }
            )
            TextSwitch(
                title = "Servicio de eventos",
                subtitle = "Muestra eventos de accesibilidad",
                checked = EventService.isRunning.collectAsState().value,
                onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        requiredPermission(context, foregroundServiceSpecialUseState)
                        requiredPermission(context, notificationState)
                        requiredPermission(context, canDrawOverlaysState)
                        EventService.start()
                    } else {
                        EventService.stop()
                    }
                }
            )

            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }
}
