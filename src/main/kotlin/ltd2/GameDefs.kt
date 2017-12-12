package ltd2

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.stanfy.gsonxml.GsonXmlBuilder
import com.stanfy.gsonxml.XmlParserCreator
import javafx.scene.image.Image
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.nio.file.Paths
import java.util.zip.ZipFile
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

enum class ArmorType(val internalName:String) {
    Immaterial("arm_unarmored"),
    Swift("arm_light"),
    Natural("arm_medium"),
    Arcane("arm_heavy"),
    Fortified("arm_fortified"),
    Illegal("")
}
enum class AttackType(val internalName:String) {
    Pierce("atk_pierce"),
    Impact("atk_normal"),
    Magic("atk_magic"),
    Siege("atk_siege"),
    Pure("atk_chaos"),
    Illegal("")
}
enum class AttackMode(val internalName: String) {
    None("atkmode_none"),
    Melee("atkmode_melee"),
    Ranged("atkmode_ranged"),
    Illegal("")
}

enum class UnitClass(val internalName: String) {
    Fighter("ai_figher"),
    Creature("ai_creature"),
    Mercenary("ai_attacker"),
    None("ai_none"),
    King("ai_king"),
    Worker("ai_worker"),
    Illegal("")
}
enum class Legion(val internalName: String) {
    Element("element_legion_id"),
    Mech("mech_legion_id"),
    Grove("grove_legion_id"),
    Forsaken("forsaken_legion_id"),
    Creature("creature_legion_id"),
    Nether("nether_legion_id"),
    Aspect("aspect_legion_id"),
    Illegal("")
}

data class UnitDef(
        @SerializedName("@id") val id:String,
        @SerializedName("legion") val legion:Legion,
        @SerializedName("unitclass") val unitClass:UnitClass,
        @SerializedName("aspd") val attackSpeed:Double,
        @SerializedName("armortype") val armorType: ArmorType,
        @SerializedName("attackmode") val attackMode:AttackMode,
        @SerializedName("attacktype") val attackType: AttackType,
        @SerializedName("dmgbase") val dmgBase:Int,
        @SerializedName("dmgspread") val dmgSpread:Int,
        @SerializedName("defensebase") val defenseBase:Int,
        @SerializedName("goldbounty") val goldBounty:Int,
        @SerializedName("goldcost") val goldCost:Int,
        @SerializedName("hp") val hitpoints:Int,
        @SerializedName("hpregen") val hitpointsRegen:Double,
        @SerializedName("mp") val mana:Int,
        @SerializedName("mpregen") val manaRegen:Double,
        @SerializedName("mythiumcost") val mythiumCost:Int,
        @SerializedName("splashpath") val splashPath:String,
        @SerializedName("upgradesfrom") val upgradesFrom:String?,
        @SerializedName("totalvalue") val totalValue:Int,
        @SerializedName("totalfood") val totalFood:Int,
        @SerializedName("incomebonus") val incomeBonus:Int
) {
    fun loadImage() : Image? {
        val name = splashPath.replace("Splashes/", "icons/")
        val file = Paths.get(name).toFile()
        if( file.exists() ) {
            return Image(file.toURI().toURL().toString())
        }
        return null
    }
}

data class UnitDefs(@SerializedName("unit") val unitDefs: List<UnitDef>) {
    fun find(unitId:String) : UnitDef? {
        return unitDefs.find { it.id==unitId }
    }
}

data class UnitId(val id:String, var unitDef: UnitDef?) {

    fun resolve(unitDefs: UnitDefs) : UnitDef {
        unitDef = unitDefs.find(id)
        return unitDef!!
    }
}
data class Global(
        @SerializedName("@id") val id:String,
        @SerializedName("attackchartchaos") val attackChaos : DecimalArray,
        @SerializedName("attackchartmagic") val attackMagic : DecimalArray,
        @SerializedName("attackchartnormal") val attackNormal : DecimalArray,
        @SerializedName("attackchartpierce") val attackPierce : DecimalArray,
        @SerializedName("attackchartsiege") val attackSiege : DecimalArray,
        @SerializedName("startinggold") val startingGold: Int,
        @SerializedName("startingmythium") val startingMythium: Int
) {
    fun getModifier(attackType: AttackType, armorType: ArmorType) : Double {
        when(attackType) {
            AttackType.Pierce -> return attackPierce.list[armorType.ordinal]
            AttackType.Impact -> return attackNormal.list[armorType.ordinal]
            AttackType.Magic -> return attackMagic.list[armorType.ordinal]
            AttackType.Siege -> return attackSiege.list[armorType.ordinal]
            AttackType.Pure -> return attackChaos.list[armorType.ordinal]
        }
        throw IllegalStateException()
    }
}
data class Globals(@SerializedName("global") val globals:List<Global>)

data class WaveDef(
        @SerializedName("@id") val id:String,
        @SerializedName("amount") val amount:Int,
        @SerializedName("amount2") val amount2:Int,
        @SerializedName("preparetime") val prepareTime:Int,
        @SerializedName("recommendedvalue") val recommendedValue:Int,
        @SerializedName("unit") val unit: String,
        @SerializedName("spellunit2") val unit2: String?,
        @SerializedName("levelnum") val levelNum:Int,
        @SerializedName("totalreward") val totalReward:Int
)
data class WaveDefs(@SerializedName("wave") val waveDefs:List<WaveDef>) {
    fun find(id:Int) : WaveDef? {
        return waveDefs.find { it.levelNum==id }
    }

}

abstract class PrimitivTypeAdapter<T>(val type:String) : TypeAdapter<T?>() {
    abstract fun convert(value:String) : T?
    override fun read(input: JsonReader): T? {
        val text = input.nextString()
        val split = text.split(":::")
        if( split.size!=2) {
            return convert(text)
        }
        val type = split[0].trim()
        val value = split[1].trim()
        if( (this.type=="*" || type==this.type) && value!="" ) {
            return convert(value)
        } else {
            return null
        }
    }

    override fun write(out: JsonWriter?, value: T?) {
    }
}
class ArmorTypeAdapter : PrimitivTypeAdapter<ArmorType>("preset") {
    override fun convert(value: String): ArmorType? {
        return ArmorType.values().find { it.internalName==value }
    }
}
class AttackTypeAdapter : PrimitivTypeAdapter<AttackType>("preset") {
    override fun convert(value: String): AttackType? {
        return AttackType.values().find { it.internalName==value }
    }
}
class AttackModeAdapter : PrimitivTypeAdapter<AttackMode>("preset") {
    override fun convert(value: String): AttackMode? {
        return AttackMode.values().find { it.internalName==value }
    }
}
class LegionAdapter : PrimitivTypeAdapter<Legion>("legion_id") {
    override fun convert(value: String): Legion? {
        return Legion.values().find { it.internalName==value }
    }
}
class UnitClassAdapter : PrimitivTypeAdapter<UnitClass>("preset") {
    override fun convert(value: String): UnitClass? {
        return UnitClass.values().find { it.internalName==value }
    }
}

class DoubleTypeAdapter : PrimitivTypeAdapter<Double>("double") {
    override fun convert(value: String): Double? {
        return value.toDouble()
    }
}
class IntTypeAdapter : PrimitivTypeAdapter<Int>("int") {
    override fun convert(value: String): Int? {
        return value.toInt()
    }
}
data class DecimalArray(val list:List<Double>) {

}
class DoubleListTypeAdapter : PrimitivTypeAdapter<DecimalArray>("decimalarray") {
    override fun convert(value: String): DecimalArray? {
        return DecimalArray(value.split(",").map { it.toDouble() })
    }
}
class UnitIdTypeAdapter : PrimitivTypeAdapter<UnitId>("unit_id") {
    override fun convert(value: String): UnitId? {
        return UnitId(value, null)
    }
}
class StringTypeAdapter : PrimitivTypeAdapter<String>("*") {
    override fun convert(value: String): String? {
        return value
    }
}

data class GameData(val unitDefs: UnitDefs, val global: Global, val waveDefs: WaveDefs)
fun loadData(ltd2Folder:String) : GameData {
    val p = object : XmlParserCreator{
        override fun createParser(): XmlPullParser {
            return XmlPullParserFactory.newInstance().newPullParser()
        }
    }
    val builder = GsonBuilder()
            .registerTypeAdapter(Double::class.java, DoubleTypeAdapter())
            .registerTypeAdapter(String::class.java, StringTypeAdapter())
            .registerTypeAdapter(Int::class.java, IntTypeAdapter())
            .registerTypeAdapter(DecimalArray::class.java, DoubleListTypeAdapter())
            .registerTypeAdapter(UnitId::class.java, UnitIdTypeAdapter())
            .registerTypeAdapter(AttackType::class.java, AttackTypeAdapter())
            .registerTypeAdapter(ArmorType::class.java, ArmorTypeAdapter())
            .registerTypeAdapter(AttackMode::class.java, AttackModeAdapter())
            .registerTypeAdapter(UnitClass::class.java, UnitClassAdapter())
            .registerTypeAdapter(Legion::class.java, LegionAdapter())
    val gsonXml = GsonXmlBuilder().wrap(builder).setXmlParserCreator(p).setSameNameLists(true).create()
    val zipFile = "$ltd2Folder\\Legion TD 2_Data\\StreamingAssets\\Maps\\legiontd2.zip"
    val zip = ZipFile(zipFile)
    val units = gsonXml.fromXml(InputStreamReader(zip.getInputStream(zip.getEntry("units.xml"))), UnitDefs::class.java)
    val globals = gsonXml.fromXml(InputStreamReader(zip.getInputStream(zip.getEntry("globals.xml"))), Globals::class.java).globals[0]
    val waves = gsonXml.fromXml(InputStreamReader(zip.getInputStream(zip.getEntry("waves.xml"))), WaveDefs::class.java)
    return GameData(units, globals, waves)
}
