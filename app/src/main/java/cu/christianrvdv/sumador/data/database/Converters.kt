// data/database/Converters.kt
package cu.christianrvdv.sumador.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromMapToString(map: Map<Int, Int>): String {
        val type = object : TypeToken<Map<Int, Int>>() {}.type
        return Gson().toJson(map, type)
    }

    @TypeConverter
    fun fromStringToMap(string: String): Map<Int, Int> {
        val type = object : TypeToken<Map<Int, Int>>() {}.type
        return Gson().fromJson(string, type) ?: emptyMap()
    }
}