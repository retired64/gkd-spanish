package li.songe.gkd.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import li.songe.gkd.ui.component.EmptyText
import li.songe.gkd.ui.component.PerfIcon
import li.songe.gkd.ui.component.PerfIconButton
import li.songe.gkd.ui.component.PerfTopAppBar
import li.songe.gkd.ui.component.updateDialogOptions
import li.songe.gkd.ui.share.ListPlaceholder
import li.songe.gkd.ui.share.LocalMainViewModel
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.itemPadding
import li.songe.gkd.ui.style.scaffoldPadding
import li.songe.gkd.util.appInfoMapFlow
import li.songe.gkd.util.ruleSummaryFlow
import li.songe.gkd.util.throttle

@Serializable
data object SlowGroupRoute : NavKey

@Composable
fun SlowGroupPage() {
    val mainVm = LocalMainViewModel.current
    val ruleSummary by ruleSummaryFlow.collectAsState()
    val appInfoCache by appInfoMapFlow.collectAsState()

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
                title = { Text(text = "Consultas lentas") },
                actions = {
                    PerfIconButton(imageVector = PerfIcon.Info, onClick = throttle {
                        mainVm.dialogFlow.updateDialogOptions(
                            title = "Consultas lentas",
                            text = arrayOf(
                                "Una regla individual se considera consulta lenta si cumple simultáneamente las siguientes 3 condiciones",
                                "1. El lado derecho del selector no puede consultarse rápidamente y no es una consulta activa, o usa << internamente y no puede consultarse rápidamente\n2. preKeys está vacío\n3. matchTime está vacío o es mayor a 10s",
                                "Las consultas lentas pueden provocar activaciones lentas o mayor consumo de batería. Algunas sugerencias de optimización:\n1. Reducir el número de veces que el selector obtiene nuevos nodos\n2. Reducir o limitar el tiempo o número de consultas de la regla"
                            ).joinToString("\n\n"),
                        )
                    })
                }
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.scaffoldPadding(contentPadding)
        ) {
            items(
                ruleSummary.slowGlobalGroups,
                { (_, r) -> r.subsItem.id to r.group.key }
            ) { (group, rule) ->
                SlowGroupCard(
                    modifier = Modifier
                        .clickable(onClick = throttle {
                            mainVm.navigatePage(
                                SubsGlobalGroupListRoute(
                                    rule.subsItem.id,
                                    group.key
                                )
                            )
                        })
                        .itemPadding(),
                    title = group.name,
                    desc = "${rule.rawSubs.name}/Regla global"
                )
            }
            items(
                ruleSummary.slowAppGroups,
                { (_, r) -> Triple(r.subsItem.id, r.appId, r.group.key) }
            ) { (group, rule) ->
                SlowGroupCard(
                    modifier = Modifier
                        .clickable(onClick = throttle {
                            mainVm.navigatePage(
                                SubsAppGroupListRoute(
                                    rule.subsItem.id,
                                    rule.app.id,
                                    group.key
                                )
                            )
                        })
                        .itemPadding(),
                    title = group.name,
                    desc = "${rule.rawSubs.name}/Regla de aplicación/${appInfoCache[rule.app.id]?.name ?: rule.app.name ?: rule.app.id}"
                )
            }
            item(ListPlaceholder.KEY, ListPlaceholder.TYPE) {
                Spacer(modifier = Modifier.height(EmptyHeight))
                if (ruleSummary.slowGroupCount == 0) {
                    EmptyText(text = "Sin reglas")
                }
            }
        }
    }
}

@Composable
fun SlowGroupCard(title: String, desc: String, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PerfIcon(
            imageVector = PerfIcon.KeyboardArrowRight,
        )
    }
}
