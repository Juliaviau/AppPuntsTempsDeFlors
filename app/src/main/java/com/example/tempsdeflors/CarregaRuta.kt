import android.content.Context
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color

object CarregaRuta {
    fun carregarRuta(context: Context, nombreRuta: String, colorHex: String): Polyline {
        val jsonString = context.assets.open("rutes.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        val puntosArray = jsonObject.getJSONArray(nombreRuta)

        val puntss = mutableListOf<GeoPoint>()
        for (i in 0 until puntosArray.length()) {
            val obj = puntosArray.getJSONObject(i)
            puntss.add(GeoPoint(obj.getDouble("lat"), obj.getDouble("lng")))
        }

        return Polyline().apply {
            setPoints(puntss)
            outlinePaint.color = Color.parseColor(colorHex)
            outlinePaint.strokeWidth = 10f
        }
    }
}