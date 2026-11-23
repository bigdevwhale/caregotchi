package app.caregotchi.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.caregotchi.container
import app.caregotchi.domain.Item
import app.caregotchi.domain.Shop
import app.caregotchi.domain.tr
import app.caregotchi.ui.Data
import app.caregotchi.ui.Nav
import app.caregotchi.ui.components.Art
import app.caregotchi.ui.components.Banner
import app.caregotchi.ui.components.Btn
import app.caregotchi.ui.components.Ghost
import app.caregotchi.ui.components.Leaves
import app.caregotchi.ui.components.Segments
import app.caregotchi.ui.components.Sheet
import app.caregotchi.ui.components.lip
import app.caregotchi.ui.components.lerp
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import kotlinx.coroutines.launch

@Composable
fun ShopScreen(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var pick by remember { mutableStateOf<Item?>(null) }
    val lvl = d.level.first
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Banner("banner_shop", tr("Лавка Барсука", "Badger's shop"), tr("Листики – за каждую заботу о себе", "Leaves come with every bit of self-care")) {
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(10.dp).shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(C.paper).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Leaves(d.s.leaves, color = C.inkPaper)
            }
        }
        Column(Modifier.padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(6.dp))
            Segments(listOf(tr("Уют", "Cozy"), tr("Особое", "Special"), tr("В дупле", "In the hollow")), tab) { tab = it }
            Spacer(Modifier.height(10.dp))
            val items = when (tab) { 0 -> Shop.cozy.filter { it.id !in d.s.items }; 1 -> Shop.special; else -> Shop.cozy.filter { it.id in d.s.items } }
            if (items.isEmpty()) Text(if (tab == 2) tr("Пока ничего не куплено.", "Nothing bought yet.") else tr("Всё уже куплено – дупло полное!", "All bought – the hollow is full!"), style = T.small, modifier = Modifier.padding(vertical = 18.dp))
            items.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { it -> Tile(it, d, lvl, Modifier.weight(1f)) { pick = it } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (tab == 1) Text(tr("Пледов в запасе: ${d.s.plaids}. Плед сам укроет день без отметок, чтобы серия не порвалась.", "Blankets in store: ${d.s.plaids}. A blanket covers a day without marks so the streak holds."), style = T.small)
            Spacer(Modifier.height(16.dp))
        }
    }
    pick?.let { it ->
        val owned = it.id in d.s.items && !it.special
        Sheet({ pick = null }) {
            Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp)).background(Brush.radialGradient(listOf(Color(0xFFFFF3D6), C.bg))), contentAlignment = Alignment.Center) {
                Art(it.picture, Modifier.size(120.dp))
            }
            Text(it.title, style = T.h2, modifier = Modifier.padding(top = 8.dp))
            Text(when { it.id == "plaid" -> tr("Укроет день, когда некогда было отмечаться. Серия не порвётся.", "Covers a day you had no time to mark. The streak won't break.")
                it.id == "egg" -> tr("Яйцо случайного жильца, которого у вас ещё нет.", "An egg of a random pet you don't have yet.")
                it.id == "chest" -> tr("Листики, а может, вещь для дупла или плед.", "Leaves, maybe a thing for the hollow or a blanket.")
                owned -> tr("Стоит в дупле.", "Stands in the hollow.")
                else -> tr("Встанет в дупле – питомец будет рад.", "It will stand in the hollow – the pet will be glad.") }, style = T.small)
            when {
                owned -> Ghost(if (it.id in d.s.hidden) tr("Поставить обратно", "Put it back") else tr("Убрать в чулан", "Put it away"), Modifier.fillMaxWidth().padding(top = 14.dp)) { scope.launch { c.toggleItem(it.id) }; pick = null }
                lvl < it.level -> Text(tr("Откроется на ${it.level} уровне.", "Opens at level ${it.level}."), style = T.bodyB, modifier = Modifier.padding(top = 14.dp))
                else -> Btn(tr("Купить за ${it.price}", "Buy for ${it.price}"), Modifier.fillMaxWidth().padding(top = 14.dp), enabled = d.s.leaves >= it.price, icon = "icon_leaf") {
                    scope.launch { if (c.buy(it.id) && !it.special) nav.tab = 2 }; pick = null
                }
            }
        }
    }
}

@Composable
private fun Tile(it: Item, d: Data, lvl: Int, modifier: Modifier, onClick: () -> Unit) {
    val locked = lvl < it.level
    val owned = it.id in d.s.items && !it.special
    Column(modifier.lip(if (owned) C.okSoft else C.card2, lerp(if (owned) C.okSoft else C.card2, Color.Black, .12f), 14.dp).clickable(onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Art(it.picture, Modifier.size(64.dp), alpha = if (locked) .45f else 1f, tint = if (locked) app.caregotchi.ui.components.grey else null)
        Text(it.title, style = T.tiny.copy(color = C.ink), textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.height(30.dp).padding(top = 2.dp))
        when {
            owned -> Text(if (it.id in d.s.hidden) tr("в чулане", "put away") else tr("в дупле", "in the hollow"), style = T.tiny.copy(color = C.sage))
            locked -> Row(verticalAlignment = Alignment.CenterVertically) { Art("icon_lock", Modifier.size(14.dp)); Text(tr(" ${it.level} ур.", " lvl ${it.level}"), style = T.tiny) }
            else -> Leaves(it.price, size = 18.dp)
        }
    }
}
