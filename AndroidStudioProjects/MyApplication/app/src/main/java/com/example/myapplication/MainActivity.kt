package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
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
import com.example.myapplication.passkey.PasskeyRepository
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

// --- KONFIGURACJA KOLORÓW (STYL mOBYWATEL) ---

val ColorGovBlue = Color(0xFF003399) // Główny granat
val ColorGovBg = Color(0xFFF5F6F7)   // Szare tło
val ColorSuccess = Color(0xFF007E33) // Ciemna zieleń
val ColorError = Color(0xFFCC0000)   // Ciemna czerwień

// --- REJESTR DOMEN (dla ekranu listy) ---
object TrustedRegistry {
    val domains = listOf(
        "100sekund.gov.pl", "100sekundwmuzeum.gov.pl", "1920.gov.pl", "20lat.gov.pl",
        "20latwue.gov.pl", "aan.gov.pl", "abm.gov.pl", "abw.gov.pl", "ac.gov.pl",
        "ade.gov.pl", "afs.gov.pl", "agad.gov.pl", "aids.gov.pl", "ai.gov.pl",
        "akademiacez.gov.pl", "akademiakopernikanska.gov.pl", "ak.gov.pl",
        "aktywnyrodzic.gov.pl", "aleksandrowkuj.sr.gov.pl", "ank.gov.pl",
        "gov.pl", "podatki.gov.pl", "pacjent.gov.pl", "mobywatel.gov.pl",
        "epuap.gov.pl", "profil-zaufany.pl", "zus.pl", "bip.gov.pl", "sejm.gov.pl",
        "prezydent.pl", "premier.gov.pl", "otwartedane.gov.pl", "dane.gov.pl",
        "cert.pl", "mf.gov.pl", "mswia.gov.pl", "men.gov.pl", "mz.gov.pl"
    )
}

// --- WALIDACJA KODÓW FIDO ---
private const val FIDO_PREFIX = "FIDO:/"

enum class VerificationResult { PASSKEY, INVALID }
enum class PasskeyStatus { NOT_SENT, PENDING, SUCCESS, FAILURE }
enum class Screen { HOME, SCANNER, RESULT, REGISTRY_LIST }

sealed class PasskeyRegistrationState {
    object Loading : PasskeyRegistrationState()
    object Success : PasskeyRegistrationState()
    data class Error(val message: String) : PasskeyRegistrationState()
}

data class ScanResultData(val payload: String, val status: VerificationResult, val passkeyStatus: PasskeyStatus = PasskeyStatus.NOT_SENT)
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
    val context = LocalContext.current
    val passkeyRepository = remember(context.applicationContext) { PasskeyRepository(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    var passkeyRegistrationState by remember { mutableStateOf<PasskeyRegistrationState>(PasskeyRegistrationState.Loading) }
    val demoResult = remember { ScanResultData("FIDO:/demo-passkey", VerificationResult.PASSKEY, PasskeyStatus.SUCCESS) }
    val startOnSuccessPreview = remember { BuildConfig.DEBUG }
    var currentScreen by remember { mutableStateOf(if (startOnSuccessPreview) Screen.RESULT else Screen.HOME) }
    var scanResult by remember { mutableStateOf(if (startOnSuccessPreview) demoResult else ScanResultData("", VerificationResult.INVALID)) }

    val passkeyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val newStatus = when (result.resultCode) {
            Activity.RESULT_OK -> PasskeyStatus.SUCCESS
            Activity.RESULT_CANCELED -> PasskeyStatus.FAILURE
            else -> PasskeyStatus.FAILURE
        }
        scanResult = scanResult.copy(passkeyStatus = newStatus)
        val feedback = if (newStatus == PasskeyStatus.SUCCESS) "Android potwierdził przesłanie passkey" else "Android przerwał lub odrzucił żądanie passkey"
        Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
    }

    fun launchPasskeyRegistration() {
        coroutineScope.launch {
            passkeyRegistrationState = PasskeyRegistrationState.Loading
            val success = passkeyRepository.registerPasskey(context)
            passkeyRegistrationState = if (success) {
                PasskeyRegistrationState.Success
            } else {
                PasskeyRegistrationState.Error("Nie udało się zainstalować passkey. Spróbuj ponownie.")
            }
        }
    }

    LaunchedEffect(passkeyRepository) {
        launchPasskeyRegistration()
    }

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
                    onRegistryClick = { currentScreen = Screen.REGISTRY_LIST },
                    registrationState = passkeyRegistrationState,
                    onRetryRegistration = { launchPasskeyRegistration() }
                )
                Screen.SCANNER -> CameraScreen(
                    onCodeScanned = { code ->
                        val (status, passkeyStatus) = handleScannedPayload(context, code, passkeyLauncher)
                        scanResult = ScanResultData(code, status, passkeyStatus)
                        currentScreen = Screen.RESULT
                    },
                    onClose = { currentScreen = Screen.HOME }
                )
                Screen.RESULT -> ResultScreen(
                    data = scanResult,
                    onBack = { currentScreen = Screen.HOME },
                    onRetryPasskey = { payload ->
                        scanResult = scanResult.copy(passkeyStatus = PasskeyStatus.PENDING)
                        val launched = launchPasskeyIntent(context, payload, passkeyLauncher)
                        if (!launched) {
                            scanResult = scanResult.copy(passkeyStatus = PasskeyStatus.FAILURE)
                        }
                    }
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
fun HomeScreen(onScanClick: () -> Unit, onRegistryClick: () -> Unit, registrationState: PasskeyRegistrationState, onRetryRegistration: () -> Unit) {
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
        PasskeyRegistrationBanner(registrationState, onRetryRegistration)
        Spacer(modifier = Modifier.height(12.dp))
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
fun ResultScreen(data: ScanResultData, onBack: () -> Unit, onRetryPasskey: (String) -> Unit) {
    val context = LocalContext.current
    val isFidoRequest = data.status == VerificationResult.PASSKEY

    val hero = when {
        isFidoRequest && data.passkeyStatus == PasskeyStatus.SUCCESS -> Quad(
            ColorSuccess,
            Icons.Default.CheckCircle,
            "Strona zaufana",
            "Android potwierdził autoryzację passkey. Możesz kontynuować logowanie."
        )
        !isFidoRequest -> Quad(
            ColorError,
            Icons.Default.GppBad,
            "Wykryto zagrożenie!",
            "Ten kod QR nie spełnia wymagań FIDO:/ i nie może zostać użyty."
        )
        data.passkeyStatus == PasskeyStatus.FAILURE -> Quad(
            ColorError,
            Icons.Default.GppBad,
            "Wykryto zagrożenie!",
            "Android odrzucił żądanie passkey – potraktuj stronę jako podrobioną."
        )
        else -> Quad(
            ColorGovBlue,
            Icons.Default.HourglassTop,
            "Trwa weryfikacja",
            "Żądanie passkey zostało wysłane do Androida. Poczekaj na potwierdzenie."
        )
    }

    val (bgColor, icon, title, heroDescription) = hero
    val isSuccess = hero === hero // placeholder removed below
    val showSuccess = isFidoRequest && data.passkeyStatus == PasskeyStatus.SUCCESS

    fun retryPasskey() {
        onRetryPasskey(data.payload)
    }

    fun reportIncident() {
        Toast.makeText(context, "Zgłoszenie wysłane do CERT Polska", Toast.LENGTH_LONG).show()
        onBack()
    }

    Column(modifier = Modifier.fillMaxSize().background(ColorGovBg)) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(0.45f).background(bgColor).clip(
                RoundedCornerShape(
                    bottomStart = 30.dp,
                    bottomEnd = 30.dp
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(100.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    when {
                        showSuccess -> "Kod FIDO potwierdzony"
                        data.passkeyStatus == PasskeyStatus.FAILURE -> "Kod FIDO odrzucony"
                        !isFidoRequest -> "Kod bez prefiksu FIDO"
                        else -> "Oczekiwanie na odpowiedź Androida"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.background(
                        Color.Black.copy(alpha = 0.2f),
                        RoundedCornerShape(50)
                    ).padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(0.55f).padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(heroDescription, fontSize = 16.sp, textAlign = TextAlign.Center, color = Color.DarkGray, lineHeight = 24.sp)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusRow("Prefiks FIDO:/", isFidoRequest)
                    Spacer(modifier = Modifier.height(8.dp))
                    PasskeyStatusRow(data.passkeyStatus)
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                when {
                    showSuccess -> {
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Zakończ i wróć")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        }
                    }
                    data.passkeyStatus == PasskeyStatus.PENDING || data.passkeyStatus == PasskeyStatus.NOT_SENT -> {
                        Button(
                            onClick = { retryPasskey() },
                            enabled = data.passkeyStatus != PasskeyStatus.PENDING,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorGovBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Wyślij ponownie do Androida")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        }
                    }
                    else -> {
                        Button(
                            onClick = { reportIncident() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorError),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Report, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Zgłoś próbę oszustwa")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Wróć", color = Color.Gray)
                }
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

@Composable
fun PasskeyStatusRow(status: PasskeyStatus) {
    val (icon, tint, message) = when (status) {
        PasskeyStatus.SUCCESS -> Triple(Icons.Default.Verified, ColorSuccess, "Kod potwierdzony – Android autoryzował passkey")
        PasskeyStatus.FAILURE -> Triple(Icons.Default.Warning, ColorError, "Niepowodzenie – traktuj stronę jako podrobioną")
        PasskeyStatus.PENDING -> Triple(Icons.Default.History, ColorGovBlue, "Czekamy na autoryzację Androida")
        PasskeyStatus.NOT_SENT -> Triple(Icons.Default.HourglassEmpty, Color.Gray, "Żądanie jeszcze nie zostało wysłane")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, fontSize = 14.sp)
    }
}

fun handleScannedPayload(context: Context, rawValue: String, launcher: ActivityResultLauncher<Intent>?): Pair<VerificationResult, PasskeyStatus> {
    return if (rawValue.startsWith(FIDO_PREFIX, ignoreCase = true)) {
        val launched = launchPasskeyIntent(context, rawValue, launcher)
        val status = if (launched) PasskeyStatus.PENDING else PasskeyStatus.FAILURE
        VerificationResult.PASSKEY to status
    } else {
        VerificationResult.INVALID to PasskeyStatus.NOT_SENT
    }
}

fun launchPasskeyIntent(context: Context, payload: String, launcher: ActivityResultLauncher<Intent>? = null): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(payload)).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    return runCatching {
        launcher?.launch(intent) ?: context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Brak aplikacji obsługującej passkey", Toast.LENGTH_LONG).show()
    }.isSuccess
}

@OptIn(ExperimentalGetImage::class)
fun processImageProxy(imageProxy: ImageProxy, onCodeScanned: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        BarcodeScanning.getClient().process(image).addOnSuccessListener { barcodes -> barcodes.firstOrNull()?.rawValue?.let { onCodeScanned(it) } }.addOnCompleteListener { imageProxy.close() }
    } else { imageProxy.close() }
}

@Composable
fun PasskeyRegistrationBanner(state: PasskeyRegistrationState, onRetry: () -> Unit) {
    val (bgColor, icon, text, showRetry) = when (state) {
        PasskeyRegistrationState.Loading -> Quad(ColorGovBlue, Icons.Default.HourglassTop, "Trwa instalowanie passkey...", false)
        PasskeyRegistrationState.Success -> Quad(ColorSuccess, Icons.Default.Verified, "Passkey zainstalowany pomyślnie.", false)
        is PasskeyRegistrationState.Error -> Quad(ColorError, Icons.Default.Warning, state.message, true)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, bgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = bgColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = bgColor, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (showRetry) {
                TextButton(onClick = onRetry) { Text("Ponów", color = bgColor) }
            }
        }
    }
}
