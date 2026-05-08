package com.example.tempsdeflors

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InfoPuntMarker(private val mapView: MapView,  private val fotoCallback: FotoCallback) :
    InfoWindow(R.layout.info_punt_marker, mapView) {
    val context = mapView.context

    override fun onClose() {
        // Do something
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOpen(item: Any?) {

        val marker = item as Marker
        val punt = marker.relatedObject as? Punts ?: return

        val visitat = PuntRepository.existeixPuntByNumero(punt.numero) //true es que existeix

        val titleView = mView.findViewById<TextView>(R.id.title)
        val descView = mView.findViewById<TextView>(R.id.description)
        val subDescView = mView.findViewById<TextView>(R.id.snippet)
        val visitatButton = mView.findViewById<Button>(R.id.visitat_button)
        val ruta = mView.findViewById<TextView>(R.id.ruta)
        val visita = mView.findViewById<TextView>(R.id.visitatono)

        val afegirfoto = mView.findViewById<ImageButton>(R.id.afegirFoto)
        val foto = mView.findViewById<ImageView>(R.id.imatgepunt)

        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")

        // Suponiendo que en PuntRepository has creado una función para obtener la nota actual
        val notaGuardada = PuntRepository.getEstrellesByNumero(punt.numero) ?: 0
        Log.i("InfoPuntMarker", "nota guardada: $notaGuardada")
        gestionarEstrelles(punt.numero, notaGuardada)

        // Si el punto no está visitado, quizás quieras esconder las estrellas:
        val layoutEstrelles = mView.findViewById<View>(R.id.layout_estrelles)
        layoutEstrelles.visibility = if (visitat) View.VISIBLE else View.GONE


        titleView.text = marker.title
        descView.text = marker.subDescription
        subDescView.text = marker.snippet
        visita.text = if (!visitat) "No visitat" else "Visitat a " + simpleDateFormat.format(
            PuntRepository.getDataByNumero(punt.numero)?.toLong() ?:
            System.currentTimeMillis()
        )
        visitatButton.text = if (!visitat) "Marcar com a visitat" else "Eliminar de visitats"
        ruta.text = "RUTA " + punt.ruta

        when (punt.ruta) {
            "1" -> {
                    ruta.setTextColor(ContextCompat.getColor(mapView.context, R.color.ruta1))
                    visitatButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mapView.context, R.color.ruta1))
                    afegirfoto.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mapView.context, R.color.ruta1))                }
            "2" -> {
                    ruta.setTextColor(ContextCompat.getColor(mapView.context, R.color.ruta2))
                    visitatButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mapView.context, R.color.ruta2))
                    afegirfoto.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mapView.context, R.color.ruta2))
            }
            "3" -> {
                    ruta.setTextColor(ContextCompat.getColor(mapView.context, R.color.ruta3))
                    visitatButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mapView.context, R.color.ruta3))
                    afegirfoto.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mapView.context, R.color.ruta3))
            }
            "ACCESSIBLE" -> {
                    ruta.setTextColor(ContextCompat.getColor(mapView.context, R.color.accessible))
                    visitatButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(mapView.context, R.color.accessible))
                }
        }

        val uriFoto = PuntRepository.getFotoUriByNumero(punt.numero)
        if (!uriFoto.isNullOrEmpty()) {
            foto.visibility = View.VISIBLE
            afegirfoto.visibility = View.VISIBLE
            Glide.with(context).load(PuntRepository.getFotoUriByNumero(punt.numero)).into(foto).clearOnDetach()
            Log.i("InfoPuntMarker", " num: ${punt.numero}")
        } else {
            foto.visibility = View.GONE
        }


        if (!visitat) {
            afegirfoto.visibility = View.GONE
        } else {
            Glide.with(context)
                .load(PuntRepository.getFotoUriByNumero(punt.numero))
                .into(foto)
            afegirfoto.visibility = View.VISIBLE
            afegirfoto.setOnClickListener {
                punt?.let {
                    fotoCallback.ferFoto(it.numero) {uri ->
                        foto.setImageURI(uri)
                        foto.visibility = View.VISIBLE
                        fotoCallback.onFotoFeta(it.numero, uri)
                        PuntRepository.updateFotoUri(punt.numero, uri.toString())
                    }
                }
            }
        }

        mapView.setOnClickListener {
            close()
        }

        //nomes guardo els que estan visitats
        visitatButton.setOnClickListener {
            if (!visitat) {
                CoroutineScope(Dispatchers.IO).launch {
                    PuntRepository.marcaPuntComVisitat(ruta.text.toString(), punt.numero, "si")
                    withContext(Dispatchers.Main) {
                        punt.visitat = "si"
                        val color = getColorPerRuta(punt.ruta, visitat = true)
                        marker.icon = createNumberedMarkerDrawable(mapView.context, punt.numero.toInt(), color)
                        mapView.invalidate() // refresca el mapa
                        //close() // tanca la finestra cal?
                    }
                }

            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    PuntRepository.eliminarPunt(punt.numero)
                    withContext(Dispatchers.Main) {
                        punt.visitat = "no"
                        val color = getColorPerRuta(punt.ruta, visitat = false)
                        marker.icon = createNumberedMarkerDrawable(mapView.context, punt.numero.toInt(), color)
                        mapView.invalidate() // refresca el mapa
                        //close() cal?
                    }
                }
            }
            mapView.invalidate()
            close()
        }

        mView.setOnClickListener{close()}
        mapView.setOnClickListener {close()}

        closeAllInfoWindowsOn(mapView)
    }

    private fun gestionarEstrelles(numeroPunt: String, notaActual: Int) {
        val estrelles = listOf<ImageButton>(
            mView.findViewById(R.id.star1),
            mView.findViewById(R.id.star2),
            mView.findViewById(R.id.star3),
            mView.findViewById(R.id.star4),
            mView.findViewById(R.id.star5)
        )


        // Función para pintar las estrellas según la nota
        fun pintarEstrelles(nota: Int) {
            estrelles.forEachIndexed { index, button ->
                val icon = if (index < nota) R.drawable.baseline_star_24 else R.drawable.baseline_star_border_24
                button.setImageResource(icon)
                // Opcional: poner el color de la ruta
            }
        }

        // Inicializar con la nota que ya tenga guardada
        pintarEstrelles(notaActual)
        var notaviva = notaActual
        // Configurar el click para cada estrella
        estrelles.forEachIndexed { index, button ->
            button.setOnClickListener {
                val novaNota = index + 1
                pintarEstrelles(novaNota)

                // Guardar en la base de datos
                CoroutineScope(Dispatchers.IO).launch {
                    PuntRepository.updateEstrelles(numeroPunt, novaNota)
                }
            }
        }
    }

    private fun getColorPerRuta(ruta: String, visitat: Boolean): Int {
        val context = mapView.context
        return when (ruta) {
            "1" -> if (visitat) ContextCompat.getColor(context, R.color.ruta1clar) else ContextCompat.getColor(context, R.color.ruta1)
            "2" -> if (visitat) ContextCompat.getColor(context, R.color.ruta2clar) else ContextCompat.getColor(context, R.color.ruta2)
            "3" -> if (visitat) ContextCompat.getColor(context, R.color.ruta3clar) else ContextCompat.getColor(context, R.color.ruta3)
            "ACCESSIBLE" -> if (visitat) ContextCompat.getColor(context, R.color.accessibleclar) else ContextCompat.getColor(context, R.color.accessible)
            else -> ContextCompat.getColor(context, R.color.white)
        }
    }
}