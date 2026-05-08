package com.example.tempsdeflors

import CarregaRuta
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.benchmark.perfetto.ExperimentalPerfettoTraceProcessorApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.TypedArrayUtils.getString
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tempsdeflors.ui.theme.TempsDeFlorsTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
val llistaDeMarkers = mutableListOf<Marker>()
var mapa = mutableListOf<MapView>()
private var dadesCarregades by mutableStateOf(false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            !dadesCarregades
        }

        // carrega en segon pla
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                PuntRepository.init(this@MainActivity)

                carregarPuntsDesDeJSON(this@MainActivity)

                Configuration.getInstance().load(
                    applicationContext,
                    PreferenceManager.getDefaultSharedPreferences(applicationContext)
                )
            }
            //delay(5000)
            dadesCarregades = true
        }

        setContent {
            TempsDeFlorsTheme {
                if (dadesCarregades) {
                    PantallaMapa()
                }
            }
        }
    }
}

@Composable
fun lletraModeFosc() : androidx.compose.ui.graphics.Color{
    return if (isSystemInDarkTheme()) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black
}

@Composable
fun backgroundModeFosc(): androidx.compose.ui.graphics.Color {
    return if (isSystemInDarkTheme()) { Color(0xFF1E1E1E) } else {Color(0xFFFCFAED) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,ExperimentalPerfettoTraceProcessorApi::class)
@Composable
fun PantallaMapa() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val context = LocalContext.current
    val punts = remember { carregarPuntsDesDeJSON(context)}
    val grouped = punts.groupBy { it.ruta }

    //Menu de l'esquerra
    ModalNavigationDrawer (
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            if (drawerState.isOpen) {
                ModalDrawerSheet (modifier = Modifier
                    .width(if (drawerState.isOpen) 350.dp else 0.dp)
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Transparent)
                ){
                    Text("Espais",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = lletraModeFosc(),
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Button(
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("visitats")
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.colorpred))
                    ) {
                        Icon(Icons.Default.List, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Veure llocs visitats")
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        grouped.forEach { (ruta, punts) ->
                            stickyHeader {
                                Text(
                                    text = "Ruta $ruta",
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier
                                        .background(
                                            when (ruta) {
                                                "1" -> {androidx.compose.ui.graphics.Color(0xFF00a80d)}
                                                "2" -> {androidx.compose.ui.graphics.Color(0xFF7d007d)}
                                                "3" -> {androidx.compose.ui.graphics.Color(0xFF004988)}
                                                "ACCESSIBLE" -> {androidx.compose.ui.graphics.Color.Gray}
                                                else -> {androidx.compose.ui.graphics.Color.Gray}
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .padding(horizontal = 6.dp)
                                        .wrapContentSize(Alignment.Center)
                                )
                            }
                            itemsIndexed(punts) { index, punt ->
                                val nextPunt = punts.getOrNull(index + 1)
                                val puntv = PuntRepository.existeixPuntByNumero(punt.numero) ?: false
                                val nextpuntv = nextPunt?.numero?.let { PuntRepository.existeixPuntByNumero(it) } ?: false
                                TimelineItem(
                                    punt = punt,
                                    isFirst = index == 0,
                                    isLast = index == punts.lastIndex,
                                    scope = scope,
                                    drawerState = drawerState,
                                    puntv = puntv,
                                    nextpuntv = nextpuntv
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            //barra lila de sobre del mapa, que diu temps de flors i l'icona del menu
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name), fontSize = 24.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, color = colorResource(R.color.colorpred), fontWeight = FontWeight.Bold)},
                    //icona del menu
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Obrir el menú")
                        }
                    },
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "map",
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                composable("map") { OsmMapView() }
                composable("visitats") {LlistaVisitatsView(navController, punts)}
            }
        }
    }
}

fun onPuntClick(punt: Punts,mapView: MapView,drawerState: DrawerState,markers: List<Marker>,scope: CoroutineScope) {
    // Tanca el Drawer
    scope.launch {
        drawerState.close()
    }

    // Cerca el Marker associat
    val marker = markers.find { it.relatedObject == punt } ?: return

    val desplaçament = 0.014  // Ajusta segons el teu zoom
    val posicioDesplaçada = GeoPoint(marker.position.latitude + desplaçament, marker.position.longitude)

    // Anima la càmera
    mapView.controller.animateTo(posicioDesplaçada)

    // Mostra l'InfoWindow després d’un petit delay per assegurar el moviment
    Handler(Looper.getMainLooper()).postDelayed({
        marker.showInfoWindow()
    }, 30)
}

@Composable
fun TimelineItem( punt: Punts, isFirst: Boolean, isLast: Boolean,
    scope: CoroutineScope, drawerState: DrawerState, puntv: Boolean, nextpuntv: Boolean) {
    val circleColor = if (/*punt.visitat == "si"*/puntv) Color(0xFF4CAF50) else Color(0xFFF44336) // Verd o vermell

    // Degradat entre el color del punt actual i el següent
    val lineGradient = Brush.verticalGradient(
        colors = listOf(
            circleColor,
            if (nextpuntv) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    )

    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(top = if (isFirst) 16.dp else 0.dp)) {
        // Timeline
        Column(
            modifier = Modifier
                .width(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(16.dp)
                        .background(circleColor)
                )
            }

            // Punt indicador (cercle)
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(circleColor)
            )

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(64.dp)
                        .background(brush = lineGradient)
                )
            }
        }

        // Targeta de contingut
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable {
                    onPuntClick(
                        punt = punt,
                        mapView = mapa.get(0),
                        drawerState = drawerState,
                        markers = llistaDeMarkers,
                        scope = scope
                    )
                },
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = punt.titol,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = lletraModeFosc()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (puntv) "Visitat" else "No visitat",
                    fontSize = 16.sp,
                    color = circleColor
                )
            }
        }
    }
}

fun carregarPuntsDesDeJSON(context: Context): MutableList<Punts> {
    val json = context.assets.open("punts.json").bufferedReader().use { it.readText() }
    val gson = Gson()
    val tipus = object : TypeToken<List<Punts>>() {}.type
    return gson.fromJson(json, tipus)
}

fun crearUriDeFoto(context: Context): Uri {
    val fotoFile = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "foto_${System.currentTimeMillis()}.jpg"
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        fotoFile
    )
}


@Composable
fun OsmMapView() {
    val context = LocalContext.current

    val uriFoto = remember { mutableStateOf<Uri?>(null) }
    val fotoCallbackRef = remember { mutableStateOf<FotoCallback?>(null) }
    val idpunturi = remember { mutableStateOf<String?>(null) }
    var pendingFotoCallback by remember { mutableStateOf<((Uri) -> Unit)?>(null) }
    var pendingPuntId by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && uriFoto.value != null && pendingPuntId != null) {
            pendingFotoCallback?.invoke(uriFoto.value!!)
            pendingFotoCallback = null
            pendingPuntId = null
            Log.i("Foto", "Foto feta correctament")
        }
    }

    val fotoCallback = object : FotoCallback {
        override fun ferFoto(puntId: String, onFotoFeta: (Uri) -> Unit) {
            val uri = crearUriDeFoto(context) // Crea la URI
            idpunturi.value = puntId // Guarda l'ID del punt
            uriFoto.value = uri // Guarda la URI
            //fotoUriPerPuntId[puntId] = uri  // Guarda-la
            cameraLauncher.launch(uri)
            // Guardem el callback temporalment
            pendingFotoCallback = onFotoFeta
            pendingPuntId = puntId
        }

        override fun onFotoFeta(puntId: String, uri: Uri) {
            // 1. Comprimir la imatge
            val compressedUri = comprimirImatge(uri, context)

            // 2. Desa la URI comprimida a la base de dades:
            if (compressedUri != null) {
                //PuntRepository.updateFotoUri(puntId, compressedUri.toString())
                Log.i("Foto", "Foto guardada a la base de dades")
                // 3. Guarda la imatge a la galeria
                guardarImatgeAlSistema(context, compressedUri)

                // 4. Notifica la vista (opcional si la vols refrescar en temps real)
                Log.i("Foto", "Imatge guardada correctament a la galeria")
            }
        }
    }

    LaunchedEffect(Unit) {
        fotoCallbackRef.value = fotoCallback
    }

    val punts = remember { carregarPuntsDesDeJSON(context) }

    val mapView = MapView(context)
    val mapController = mapView.controller

    mapa = mutableListOf(mapView)

    val activity = context as? Activity
    val locationPermissionState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationProvider = GpsMyLocationProvider(context)
    val locationOverlay = MyLocationNewOverlay(locationProvider, mapView)

    LaunchedEffect(Unit) {
        if (!locationPermissionState.value && activity != null && !cameraPermissionState.value) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.CAMERA),
                1001
            )
        } else {
            startLocationOverlay(locationOverlay, mapView)
        }
    }

    if (!locationPermissionState.value) {Text("Cal activar el permís de localització.")}

    if (!cameraPermissionState.value) {Text("Cal activar el permís de camera.")}

    //Una vista per a veure el mapa
    AndroidView(
        factory = { context ->
            val mapView = MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                val limits = BoundingBox(42.048, 2.895, 41.940, 2.740)
                setScrollableAreaLimitDouble(limits)
                setHorizontalMapRepetitionEnabled(false)
                setVerticalMapRepetitionEnabled(false)
                minZoomLevel = 14.0
                maxZoomLevel = 19.5
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(41.983, 2.824))
            }

            var color = ContextCompat.getColor(context, R.color.ruta1)

            //rutes
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val ruta1 = CarregaRuta.carregarRuta(context, "ruta1", "#00a80d")
                    val ruta2 = CarregaRuta.carregarRuta(context, "ruta2", "#7d007d")
                    val ruta3 = CarregaRuta.carregarRuta(context, "ruta3", "#004988")

                    withContext(Dispatchers.Main) {
                        configurarEstilRuta(ruta1, Color.rgb(0, 168, 132))
                        configurarEstilRuta(ruta2, Color.rgb(125, 0, 125))
                        configurarEstilRuta(ruta3, Color.rgb(0, 73, 136))

                        mapView.overlays.add(ruta1)
                        mapView.overlays.add(ruta2)
                        mapView.overlays.add(ruta3)

                        mapView.invalidate() // Refresca el mapa
                    }
                } catch (e: Exception) {
                    Log.e("Rutes", "Error: ${e.message}")
                }
            }

            punts.forEach { punt ->
                val visitat = PuntRepository.existeixPuntByNumero(punt.numero)
                val marker = Marker(mapView)
                marker.position = GeoPoint(punt.lat, punt.lon)
                marker.title = punt.titol
                marker.subDescription = punt.descripcio
                marker.snippet = punt.snippet
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.relatedObject = punt

                val infoWindow = InfoPuntMarker(mapView, fotoCallback)
                marker.infoWindow = infoWindow

                when (punt.ruta) {
                    "1" -> {
                        color = if (!visitat)
                        ContextCompat.getColor(mapView.context, R.color.ruta1) else ContextCompat.getColor(mapView.context, R.color.ruta1clar)
                    }
                    "2" -> {
                        color = if (!visitat)
                            ContextCompat.getColor(mapView.context, R.color.ruta2) else ContextCompat.getColor(mapView.context, R.color.ruta2clar)
                    }
                    "3" -> {
                        color = if (!visitat)
                            ContextCompat.getColor(mapView.context, R.color.ruta3) else ContextCompat.getColor(mapView.context, R.color.ruta3clar)
                    }
                    "ACCESSIBLE" -> {
                        color = if (punt.visitat.equals("no"))
                            ContextCompat.getColor(mapView.context, R.color.accessible) else ContextCompat.getColor(mapView.context, R.color.accessibleclar)
                    }
                }
                marker.icon = createNumberedMarkerDrawable(context, punt.numero.toInt(), color)
                mapView.overlays.add(marker)
                llistaDeMarkers.add(marker)
            }

            //Rotar mapa
            val rotationGestureOverlay = RotationGestureOverlay(mapView)
            rotationGestureOverlay.isEnabled
            mapView.setMultiTouchControls(true)
            mapView.overlays.add(rotationGestureOverlay)

            //Bruixola
            val compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), mapView).apply {
                enableCompass()
            }
            mapView.overlays.add(compassOverlay)

            val isDarkTheme = (context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

            if(isDarkTheme) {
                mapView.overlayManager.tilesOverlay.apply {
                    setColorFilter(TilesOverlay.INVERT_COLORS)
                    loadingBackgroundColor = android.R.color.black
                    loadingLineColor = Color.argb(255, 0, 255, 0)
                }
            } else {
                mapView.getOverlayManager().getTilesOverlay().setColorFilter(null);
            }

            mapView
        },
        modifier = Modifier.fillMaxSize()
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(onClick = {
            val loc = locationOverlay.myLocation
            if (loc != null) {
                Handler(Looper.getMainLooper()).post {
                    mapController.animateTo(loc)
                }
            }
        },
            shape = CircleShape,
            containerColor = colorResource(R.color.colorpred),
            contentColor = androidx.compose.ui.graphics.Color.White,
            ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Centrar ubicació"
            )
        }
    }

    val puntsVisitats by PuntRepository.quantitatDePunts

    Box(
        modifier = Modifier
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = puntsVisitats.toString()+"/113",
            color = lletraModeFosc(),
            fontSize = 18.sp,
            modifier = Modifier.background(backgroundModeFosc(), shape = CircleShape).padding(10.dp)
        )
    }
}

// Funció auxiliar per no repetir codi
fun configurarEstilRuta(polyline: Polyline, colorRgb: Int) {
    polyline.outlinePaint.color = colorRgb
    polyline.outlinePaint.strokeCap = Paint.Cap.ROUND
    polyline.outlinePaint.strokeWidth = 8.0f
}

fun startLocationOverlay(locationOverlay: MyLocationNewOverlay,mapView: MapView) {

    locationOverlay.enableMyLocation()
    locationOverlay.enableFollowLocation()

    locationOverlay.runOnFirstFix {
        val location = locationOverlay.myLocation
        if (location != null) {
            Handler(Looper.getMainLooper()).post {
                mapView.controller.animateTo(location)
            }
        }
    }

    mapView.overlays.add(locationOverlay)
}

fun comprimirImatge(uri: Uri, context: Context): Uri? {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream)

    // Redimensionar la imatge a un tamany més petit (exemple: 800x600 píxels)
    val width = 800
    val height = (bitmap.height * (width.toFloat() / bitmap.width.toFloat())).toInt()

    val compressedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)

    // Crear un fitxer temporal per guardar la imatge comprimida
    val file = File(context.cacheDir, "compressed_image.jpg")
    val outputStream = FileOutputStream(file)

    // Comprimir la imatge (el format pot ser JPEG, PNG, etc. i pots especificar la qualitat)
    compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream) // 80 és la qualitat (1-100)

    outputStream.flush()
    outputStream.close()

    // Retornar el URI del fitxer comprimit
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

fun guardarImatgeAlSistema(context: Context, compressedUri: Uri) {
    // Creem una carpeta personalitzada dins de la carpeta "Pictures"
    val directoriPersonalitzat = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "FotosTempsDeFlors"  // El nom de la teva carpeta personalitzada
    )

    Log.i("PuntRepository", "Directori personalitzat: ${directoriPersonalitzat.absolutePath}")

    if (!directoriPersonalitzat.exists()) {
        directoriPersonalitzat.mkdirs()  // Crea la carpeta si no existeix
        Log.i("PuntRepository", "Carpeta creada")
    }

    // Creem un fitxer a la carpeta personalitzada
    val fotoFile = File(directoriPersonalitzat, "foto_${System.currentTimeMillis()}.jpg")

    // Afegim els valors necessaris a ContentValues
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fotoFile.name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FotosTempsDeFlors")  // La ruta serà dins "Pictures/FotosTempsDeFlors"
    }

    // Obtenim la URI on desarem la imatge
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    if (uri != null) {
        // Obrim un flux per copiar la imatge des de la URI comprimida a la nova URI
        val inputStream = context.contentResolver.openInputStream(compressedUri)
        val outputStream = context.contentResolver.openOutputStream(uri)

        inputStream?.copyTo(outputStream!!)
        inputStream?.close()
        if (outputStream != null) {
            outputStream.close()
        }

        // Notifiquem a la galeria que s'ha afegit una nova imatge
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        Log.i("PuntRepository", "foto guardada al sistema")
    }
}

fun createNumberedMarkerDrawable(context: Context, number: Int, colorp: Int): Drawable {
    val width = 100
    val height = 120
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val markerPaint = Paint().apply {
        color = colorp
        isAntiAlias = true
    }

    val markerPath = Path().apply {
        moveTo(width / 2f, height.toFloat()) // Punta inferior
        cubicTo(width / 2f, height * 0.75f, width * 0.8f, height * 0.6f, width * 0.8f, height * 0.4f)
        arcTo(width * 0.2f, 0f, width * 0.8f, height * 0.8f, 0f, 180f, false)
        cubicTo(width * 0.2f, height * 0.6f, width / 2f, height * 0.75f, width / 2f, height.toFloat())
        close()
    }
    canvas.drawPath(markerPath, markerPaint)

    val numberBackgroundPaint = Paint().apply {
        color = colorp
        isAntiAlias = true
    }

    val numberBackgroundRadius = 30f
    val numberBackgroundX = width / 2f

    val numberBackgroundY = height * 0.4f

    canvas.drawCircle(numberBackgroundX, numberBackgroundY, numberBackgroundRadius, numberBackgroundPaint)

    val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 35f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    val textX = width / 2f
    val textY = numberBackgroundY - ((textPaint.descent() + textPaint.ascent()) / 2)

    canvas.drawText(number.toString(), textX, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}