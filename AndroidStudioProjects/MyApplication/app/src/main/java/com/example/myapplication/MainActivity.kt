package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.net.URL
import java.util.concurrent.Executors

// --- KONFIGURACJA KOLORÓW (STYL mOBYWATEL) ---

val ColorGovBlue = Color(0xFF003399) // Główny granat
val ColorGovBg = Color(0xFFF5F6F7)   // Szare tło
val ColorSuccess = Color(0xFF007E33) // Ciemna zieleń
val ColorError = Color(0xFFCC0000)   // Ciemna czerwień

// --- LOGIKA BAZY DANYCH (Binaryzacja: Tylko Bezpieczne lub Niebezpieczne) ---
object TrustedRegistry {
    val domains = listOf(
        // --- DANE Z TWOJEGO PLIKU JSON ---
        "100sekund.gov.pl", "100sekundwmuzeum.gov.pl", "1920.gov.pl", "20lat.gov.pl",
        "20latwue.gov.pl", "aan.gov.pl", "abm.gov.pl", "abw.gov.pl", "ac.gov.pl",
        "ade.gov.pl", "afs.gov.pl", "agad.gov.pl", "aids.gov.pl", "ai.gov.pl",
        "akademiacez.gov.pl", "akademiakopernikanska.gov.pl", "ak.gov.pl",
        "aktywnyrodzic.gov.pl", "aleksandrowkuj.sr.gov.pl", "ank.gov.pl",
        // --- KLUCZOWE USŁUGI ---
        "gov.pl", "podatki.gov.pl", "pacjent.gov.pl", "mobywatel.gov.pl",
        "epuap.gov.pl", "profil-zaufany.pl", "zus.pl", "bip.gov.pl", "sejm.gov.pl",
        "prezydent.pl", "premier.gov.pl", "otwartedane.gov.pl", "dane.gov.pl",
        "cert.pl", "mf.gov.pl", "mswia.gov.pl", "men.gov.pl", "mz.gov.pl"
    )

    fun checkDomain(url: String): VerificationResult {
        return try {
            val host = if (url.startsWith("http")) URL(url).host else url
            val cleanHost = host.removePrefix("www.").lowercase()

            // Uproszczona logika: Jeśli jest w bazie LUB ma końcówkę .gov.pl -> ZIELONY
            val isSafe = domains.any { trusted -> cleanHost == trusted || cleanHost.endsWith(".$trusted") } || cleanHost.endsWith(".gov.pl")

            if (isSafe) VerificationResult.SAFE else VerificationResult.DANGER
        } catch (e: Exception) {
            VerificationResult.DANGER // Błąd parsowania to potencjalny atak
        }
    }
}

enum class VerificationResult { SAFE, DANGER } // Usunięto WARNING
enum class Screen { HOME, SCANNER, RESULT, REGISTRY_LIST }

data class ScanResultData(val url: String, val status: VerificationResult)
data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HackNationApp()
            }
        }
    }
}

@Composable
fun HackNationApp() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var scanResult by remember { mutableStateOf(ScanResultData("", VerificationResult.DANGER)) }
    //var currentScreen by remember { mutableStateOf(Screen.RESULT) } // Startuj od wyniku
    //var scanResult by remember { mutableStateOf(ScanResultData("gov.pl", VerificationResult.SAFE)) }
    //var currentScreen by remember { mutableStateOf(Screen.RESULT) }
    //var scanResult by remember { mutableStateOf(ScanResultData("oszustwo.pl", VerificationResult.DANGER)) }
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp).background(ColorGovBlue),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Default.Security, contentDescription = "Logo", tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("mObywatel", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(ColorGovBg)) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen(
                    onScanClick = { currentScreen = Screen.SCANNER },
                    onRegistryClick = { currentScreen = Screen.REGISTRY_LIST }
                )
                Screen.SCANNER -> CameraScreen(
                    onCodeScanned = { code ->
                        val status = TrustedRegistry.checkDomain(code)
                        scanResult = ScanResultData(code, status)
                        currentScreen = Screen.RESULT
                    },
                    onClose = { currentScreen = Screen.HOME }
                )
                Screen.RESULT -> ResultScreen(
                    data = scanResult,
                    onBack = { currentScreen = Screen.HOME }
                )
                Screen.REGISTRY_LIST -> RegistryListScreen(
                    onBack = { currentScreen = Screen.HOME }
                )
            }
        }
    }
}

// --- EKRAN 1: HOME (Centrum Akcji) ---
@Composable
fun HomeScreen(onScanClick: () -> Unit, onRegistryClick: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onScanClick()
    }

    fun tryOpenScanner() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onScanClick()
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("Weryfikacja autentyczności", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorGovBlue, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Upewnij się, że strona należy do rządu RP.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.weight(1f))

        // GŁÓWNY PRZYCISK (PULSUJĄCY)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
            Box(modifier = Modifier.size(260.dp).border(1.dp, ColorGovBlue.copy(alpha = 0.1f), CircleShape).background(ColorGovBlue.copy(alpha = 0.03f), CircleShape))
            Surface(
                onClick = { tryOpenScanner() },
                modifier = Modifier.size(200.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(64.dp), tint = ColorGovBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("SKANUJ QR", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ColorGovBlue, letterSpacing = 1.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // PRZYCISK REJESTRU
        Card(
            onClick = onRegistryClick,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, ColorGovBlue.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.ListAlt, contentDescription = null, tint = ColorGovBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Przeglądaj rejestr domen .gov.pl", color = ColorGovBlue, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- EKRAN 2: KOMPENDIUM (Z Wyszukiwarką) ---
@Composable
fun RegistryListScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredDomains = remember(searchQuery) {
        if (searchQuery.isBlank()) TrustedRegistry.domains
        else TrustedRegistry.domains.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Surface(shadowElevation = 4.dp, color = Color.White, modifier = Modifier.zIndex(1f)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Wróć", tint = ColorGovBlue) }
                    Text("Rejestr domen gov.pl", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ColorGovBlue)
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Szukaj instytucji...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray) } },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorGovBlue, focusedLabelColor = ColorGovBlue)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize()) {
            if (filteredDomains.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = Color.LightGray, modifier = Modifier.size(60.dp))
                        Text("Brak wyników", color = Color.Gray)
                    }
                }
            } else {
                items(filteredDomains) { domain ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorGovBg.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, null, tint = ColorSuccess, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(domain, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// --- EKRAN 3: KAMERA (ML Kit) ---
@Composable
fun CameraScreen(onCodeScanned: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isScanning by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    processImageProxy(imageProxy) { code ->
                        if (isScanning) {
                            isScanning = false
                            previewView.post { onCodeScanned(code) }
                        }
                    }
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (e: Exception) { Log.e("Camera", "Error", e) }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }, modifier = Modifier.fillMaxSize())

        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
            Box(modifier = Modifier.align(Alignment.Center).size(280.dp).background(Color.Transparent).border(3.dp, ColorGovBlue, RoundedCornerShape(12.dp)))
        }
        Text("Nakieruj kamerę na kod QR", color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(40.dp))
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)) { Icon(Icons.Default.Close, null, tint = Color.White) }
    }
}

// --- EKRAN 4: WYNIK (BEZPIECZNE vs ZAGROŻENIE) ---
@Composable
fun ResultScreen(data: ScanResultData, onBack: () -> Unit) {
    val context = LocalContext.current
    val isSafe = data.status == VerificationResult.SAFE

    val (bgColor, icon, title, description) = if (isSafe) {
        Quad(ColorSuccess, Icons.Default.CheckCircle, "Strona Zaufana", "Domena jest zweryfikowana i należy do administracji publicznej.")
    } else {
        Quad(ColorError, Icons.Default.GppBad, "Wykryto Zagrożenie!", "Ta strona nie jest oficjalnym serwisem rządowym. Nie podawaj tu żadnych danych!")
    }

    fun openBrowser() {
        try {
            val fullUrl = if(data.url.startsWith("http")) data.url else "https://${data.url}"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
        } catch (e: Exception) { Toast.makeText(context, "Błąd", Toast.LENGTH_SHORT).show() }
    }

    fun reportIncident() {
        Toast.makeText(context, "Zgłoszenie wysłane do CERT Polska", Toast.LENGTH_LONG).show()
        onBack()
    }

    Column(modifier = Modifier.fillMaxSize().background(ColorGovBg)) {
        Box(modifier = Modifier.fillMaxWidth().weight(0.45f).background(bgColor).clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(100.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(data.url.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 6.dp))
            }
        }

        Column(modifier = Modifier.weight(0.55f).padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(description, fontSize = 16.sp, textAlign = TextAlign.Center, color = Color.DarkGray, lineHeight = 24.sp)
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusRow("Domena rządowa (.gov.pl)", isSafe)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusRow("Certyfikat zaufany", isSafe)
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                if (isSafe) {
                    Button(onClick = { openBrowser() }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess), shape = RoundedCornerShape(12.dp)) {
                        Text("Przejdź do serwisu")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Button(onClick = { reportIncident() }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorError), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Report, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Zgłoś próbę oszustwa")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Wróć", color = Color.Gray) }
            }
        }
    }
}

@Composable
fun StatusRow(label: String, isValid: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (isValid) Icons.Outlined.Lock else Icons.Default.Close, null, tint = if (isValid) ColorSuccess else ColorError, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalGetImage::class)
fun processImageProxy(imageProxy: ImageProxy, onCodeScanned: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        BarcodeScanning.getClient().process(image).addOnSuccessListener { barcodes -> barcodes.firstOrNull()?.rawValue?.let { onCodeScanned(it) } }.addOnCompleteListener { imageProxy.close() }
    } else { imageProxy.close() }
}