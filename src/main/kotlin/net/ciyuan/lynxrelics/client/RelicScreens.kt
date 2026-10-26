// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.client

import com.google.gson.JsonParser
import net.ciyuan.lynxrelics.core.relics
import net.ciyuan.lynxrelics.craft.CraftRecipe
import net.ciyuan.lynxrelics.craft.CraftRecipes
import net.ciyuan.lynxrelics.menu.AccessoryMenu
import net.ciyuan.lynxrelics.menu.WorkstationMenu
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

private const val PANEL=0xff21192e.toInt()
private const val BORDER=0xff9b7cbd.toInt()
private const val INK=0xffe8d5ff.toInt()
private fun tr(key:String,vararg args:Any)=Component.translatable("ui.lynxrelics.$key",*args)

class AccessoryScreen(menu:AccessoryMenu,inv:Inventory,title:Component):AbstractContainerScreen<AccessoryMenu>(menu,inv,title,176,184){
    init{inventoryLabelY=76}
    override fun extractLabels(g:GuiGraphicsExtractor,mouseX:Int,mouseY:Int){
        g.text(font,title,titleLabelX,titleLabelY,INK)
        g.text(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,INK)
    }
    override fun extractBackground(g:GuiGraphicsExtractor,mouseX:Int,mouseY:Int,delta:Float){
        super.extractBackground(g,mouseX,mouseY,delta)
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,PANEL);g.outline(leftPos,topPos,imageWidth,imageHeight,BORDER)
        for(slot in menu.slots){g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff090811.toInt());g.outline(leftPos+slot.x-1,topPos+slot.y-1,18,18,0xff70557f.toInt())}
        listOf("necklace","belt","gauntlet","cloak").forEachIndexed{i,label->g.centeredText(font,tr("slot.$label"),leftPos+42+i*36,topPos+30,INK)}
        minecraft.player?.relics()?.let{s->
            g.text(font,tr("resource.breath",s.breath),leftPos+8,topPos+48,0xffcf96ef.toInt())
            g.text(font,tr("resource.tide",s.tide),leftPos+93,topPos+48,0xff70d9d8.toInt())
            g.text(font,tr("resource.pressure",s.pressure.toInt()),leftPos+8,topPos+64,0xffedbf79.toInt())
        }
    }
}

/** Internal variant IDs are network identifiers; the visible list uses a short code and a localized title. */
class WorkshopScreen(menu:WorkstationMenu,inv:Inventory,title:Component):AbstractContainerScreen<WorkstationMenu>(menu,inv,title,330,222){
    private lateinit var search:EditBox
    private val rows=mutableListOf<Button>()
    private var page=0
    private var route=""
    private var version=-1
    private var selected=-1
    private var choices=listOf<Pair<Int,CraftRecipe>>()
    private var routeRecipes=listOf<CraftRecipe>()
    init{inventoryLabelY=114}
    override fun extractLabels(g:GuiGraphicsExtractor,mouseX:Int,mouseY:Int){
        g.text(font,title,titleLabelX,titleLabelY,INK);g.text(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,INK)
    }
    override fun init(){
        super.init()
        addRenderableWidget(Button.builder(Component.literal("◀")){minecraft.gameMode?.handleInventoryButtonClick(menu.containerId,0)}.bounds(leftPos+7,topPos+17,20,16).build())
        addRenderableWidget(Button.builder(Component.literal("▶")){minecraft.gameMode?.handleInventoryButtonClick(menu.containerId,1)}.bounds(leftPos+149,topPos+17,20,16).build())
        search=addRenderableWidget(EditBox(font,leftPos+184,topPos+18,136,18,tr("craft.search")))
        search.setHint(tr("craft.search_hint"));search.setResponder{page=0;refreshChoices()}
        rows.clear()
        repeat(7){row->rows.add(addRenderableWidget(Button.builder(Component.empty()){
            choices.getOrNull(page*7+row)?.let{(index,_)->minecraft.gameMode?.handleInventoryButtonClick(menu.containerId,100+index)}
        }.bounds(leftPos+184,topPos+42+row*21,136,20).build()))}
        addRenderableWidget(Button.builder(Component.literal("◀")){if(page>0){page--;refreshRows()}}.bounds(leftPos+184,topPos+194,24,18).build())
        addRenderableWidget(Button.builder(Component.literal("▶")){if((page+1)*7<choices.size){page++;refreshRows()}}.bounds(leftPos+296,topPos+194,24,18).build())
        refreshChoices()
    }
    private fun titleKey(recipe:CraftRecipe):String{
        if(recipe.mode=="ritual")return "ui.lynxrelics.ritual.${recipe.encounter}"
        if(recipe.enchantment.isNotEmpty())return "enchantment.lynxrelics.${recipe.enchantment}"
        val id=recipe.gear.ifEmpty{recipe.outputs.keys.firstOrNull()?:return "ui.lynxrelics.craft.unknown"}
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id)).descriptionId
    }
    private fun recipeName(recipe:CraftRecipe):Component{
        val name=Component.translatable(titleKey(recipe))
        return when{
            recipe.enchantment.isNotEmpty()->tr("craft.book_name",name,Component.translatable("enchantment.level.${recipe.bookLevel}"))
            recipe.mode in setOf("specialize","respecialize")->tr("craft.specialization_name",name,recipe.specialization.uppercase())
            else->name
        }
    }
    private fun code(recipe:CraftRecipe)=recipe.id.substringBefore('.')
    private fun refreshChoices(){
        route=listOf("dragon","echo","tide").getOrElse(menu.data.get(3)){"dragon"};version=CraftRecipes.catalogVersion
        routeRecipes=CraftRecipes.clientRecipes.filter{it.route==route||it.route=="any"}
        val query=if(::search.isInitialized)search.value.trim()else""
        choices=routeRecipes.mapIndexed{i,r->i to r}.filter{(_,r)->query.isEmpty()||r.id.contains(query,true)||recipeName(r).string.contains(query,true)||SearchLanguages.matches(titleKey(r),query)}
        page=page.coerceIn(0,((choices.size-1)/7).coerceAtLeast(0));refreshRows()
    }
    private fun fit(text:String,width:Int)=if(font.width(text)<=width)text else font.plainSubstrByWidth(text,width-font.width("…"))+"…"
    private fun refreshRows(){
        selected=menu.data.get(0)
        for((row,button)in rows.withIndex()){
            val entry=choices.getOrNull(page*7+row);button.visible=entry!=null;button.active=entry!=null
            if(entry!=null)button.message=Component.literal(fit("${code(entry.second)}  ${recipeName(entry.second).string}",126)).withStyle(if(entry.first==selected)ChatFormatting.GOLD else ChatFormatting.WHITE)
        }
    }
    private fun recipeDetails(recipe:CraftRecipe):List<Component>{
        fun name(id:String):Component=if(id.startsWith("#"))tr("ingredient.${id.substringAfter(':')}")else ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))).hoverName
        val lines=mutableListOf<Component>(tr("craft.tooltip_title",code(recipe),recipeName(recipe)).withStyle(ChatFormatting.LIGHT_PURPLE),tr("craft.duration",recipe.ticks/20.0).withStyle(ChatFormatting.GOLD))
        lines.add(tr("craft.ingredients").withStyle(ChatFormatting.AQUA))
        for((id,count)in recipe.inputs)lines.add(tr("craft.ingredient",name(id),count).withStyle(ChatFormatting.GRAY))
        if(recipe.catalyst.isNotEmpty())lines.add(tr("craft.catalyst_retained",name(recipe.catalyst)).withStyle(ChatFormatting.GREEN))
        if(recipe.mode in setOf("specialize","respecialize"))lines.add(tr("craft.preserve_gear",recipe.specialization.uppercase()).withStyle(ChatFormatting.LIGHT_PURPLE))
        if(recipe.mode=="ritual")lines.add(tr("craft.ritual_commit").withStyle(ChatFormatting.YELLOW))
        for((id,count)in recipe.outputs)lines.add(tr("craft.output",name(id),count).withStyle(ChatFormatting.GREEN))
        lines.add(tr("craft.full_output").withStyle(ChatFormatting.DARK_GRAY));return lines
    }
    override fun extractBackground(g:GuiGraphicsExtractor,mouseX:Int,mouseY:Int,delta:Float){
        super.extractBackground(g,mouseX,mouseY,delta)
        if(route!=listOf("dragon","echo","tide").getOrElse(menu.data.get(3)){"dragon"}||version!=CraftRecipes.catalogVersion)refreshChoices()
        else if(selected!=menu.data.get(0))refreshRows()
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,PANEL);g.outline(leftPos,topPos,imageWidth,imageHeight,BORDER)
        for(slot in menu.slots){g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff090811.toInt());g.outline(leftPos+slot.x-1,topPos+slot.y-1,18,18,0xff70557f.toInt())}
        g.centeredText(font,tr("craft.catalyst"),leftPos+89,topPos+42,INK)
        g.centeredText(font,tr("craft.outputs"),leftPos+143,topPos+32,INK)
        g.centeredText(font,routeRecipes.getOrNull(selected)?.let{code(it)}?:"—",leftPos+88,topPos+21,INK)
        val max=menu.data.get(2).coerceAtLeast(1)
        g.fill(leftPos+8,topPos+99,leftPos+168,topPos+106,0xff090811.toInt());g.fill(leftPos+8,topPos+99,leftPos+8+(160*menu.data.get(1)/max).coerceIn(0,160),topPos+106,0xff9a63d4.toInt())
        g.fill(leftPos+176,topPos+8,leftPos+177,topPos+214,0xff70557f.toInt());g.centeredText(font,tr("craft.page",page+1,maxOf(1,(choices.size+6)/7)),leftPos+252,topPos+199,INK)
        for((row,button)in rows.withIndex())if(button.visible&&button.isMouseOver(mouseX.toDouble(),mouseY.toDouble()))choices.getOrNull(page*7+row)?.let{g.setComponentTooltipForNextFrame(font,recipeDetails(it.second),mouseX,mouseY)}
    }
}

private object SearchLanguages{
    private val tables by lazy{listOf("zh_cn","en_us").map{locale->
        WorkshopScreen::class.java.getResourceAsStream("/assets/lynxrelics/lang/$locale.json")!!.bufferedReader().use{JsonParser.parseReader(it).asJsonObject}
    }}
    fun matches(key:String,query:String)=tables.any{it.get(key)?.asString?.contains(query,true)==true}
}
