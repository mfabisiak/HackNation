package com.example.myapplication // <--- UPEWNIJ SIĘ, ŻE PAKIET ZGADZA SIĘ Z TWOIM PROJEKTEM (linia 1 w Twoim pliku)

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.net.URL
import java.util.concurrent.Executors

// Kolory Rządowe
val ColorGovBlue = Color(0xFF003764)
val ColorGovBg = Color(0xFFF5F7FA)
val ColorSuccess = Color(0xFF008A4B)
val ColorError = Color(0xFFD32F2F)

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

enum class Screen { HOME, SCANNER, RESULT_SUCCESS, RESULT_ERROR }

@Composable
fun HackNationApp() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var scannedDomain by remember { mutableStateOf("") }

    // Funkcja weryfikująca czy to strona rządowa
    fun handleScan(url: String) {
        try {
            val domain = try {
                URL(url).host.lowercase()
            } catch (e: Exception) {
                // Jeśli to nie URL, tylko tekst, traktuj jako błąd/zagrożenie
                url.lowercase()
            }

            scannedDomain = domain
            // Logika weryfikacji gov.pl
            val isGov = domain.endsWith(".gov.pl") || domain == "gov.pl"

            currentScreen = if (isGov) Screen.RESULT_SUCCESS else Screen.RESULT_ERROR
        } catch (e: Exception) {
            scannedDomain = "Błąd odczytu"
            currentScreen = Screen.RESULT_ERROR
        }
    }

    // Router ekranów (zamiast React Navigation)
    when (currentScreen) {
        Screen.HOME -> HomeScreen(onScanClick = { currentScreen = Screen.SCANNER })
        Screen.SCANNER -> CameraScreen(
            onCodeScanned = { code -> handleScan(code) },
            onClose = { currentScreen = Screen.HOME }
        )
        Screen.RESULT_SUCCESS -> ResultScreen(
            isSuccess = true,
            domain = scannedDomain,
            onBack = { currentScreen = Screen.HOME }
        )
        Screen.RESULT_ERROR -> ResultScreen(
            isSuccess = false,
            domain = scannedDomain,
            onBack = { currentScreen = Screen.HOME }
        )
    }
}

// --- EKRAN 1: STARTOWY (HOME) ---
@Composable
fun HomeScreen(onScanClick: () -> Unit) {
    val context = LocalContext.current

    // Obsługa uprawnień kamery (standardowy Androidowy launcher)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) onScanClick()
            else Toast.makeText(context, "Wymagany dostęp do kamery", Toast.LENGTH_SHORT).show()
        }
    )

    Column(
        modifier = Modifier.fillMaxSize().background(ColorGovBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Wielki Przycisk "Skanuj"
        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    onScanClick()
                } else {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier.size(280.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Prosta wizualizacja ikony QR kropkami
                Row(modifier = Modifier.padding(bottom = 16.dp)) {
                    Box(Modifier.size(20.dp).background(ColorGovBlue, RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(20.dp).background(ColorGovBlue, RoundedCornerShape(4.dp)))
                }
                Text("Skanuj kod QR", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorGovBlue)
                Text("Weryfikuj autentyczność", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

// --- EKRAN 2: KAMERA I SKANER (CameraX + ML Kit) ---
@Composable
fun CameraScreen(onCodeScanned: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isScanning by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Widok Kamery (AndroidView pozwala używać starych widoków w Compose)
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    // Analiza obrazu dla ML Kit
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        processImageProxy(imageProxy, onCodeScanned = { code ->
                            if (isScanning) {
                                isScanning = false
                                // Wracamy na wątek UI żeby zmienić ekran
                                previewView.post { onCodeScanned(code) }
                            }
                        })
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("Camera", "Błąd uruchomienia kamery", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Przycisk Zamknij "X"
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(30.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        // Ramka skanowania
        Box(
            modifier = Modifier.align(Alignment.Center)
                .size(250.dp)
                .border(2.dp, Color.White, RoundedCornerShape(16.dp))
        )

        Text(
            "Nakieruj na kod",
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(16.dp)
        )
    }
}

// --- EKRAN 3: WYNIK ---
@Composable
fun ResultScreen(isSuccess: Boolean, domain: String, onBack: () -> Unit) {
    val bgColor = if (isSuccess) ColorSuccess else ColorError
    val icon = if (isSuccess) "🛡️" else "⚠️"
    val title = if (isSuccess) "Bezpiecznie" else "Zagrożenie!"

    Column(
        modifier = Modifier.fillMaxSize().background(bgColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
        ) {
            Text(icon, fontSize = 50.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            domain,
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(60.dp))

        OutlinedButton(
            onClick = onBack,
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text("WRÓĆ", fontWeight = FontWeight.Bold)
        }
    }
}

// --- Logika ML Kit (Analiza obrazu klatka po klatce) ---
@OptIn(ExperimentalGetImage::class)
fun processImageProxy(imageProxy: ImageProxy, onCodeScanned: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { code ->
                        onCodeScanned(code)
                    }
                }
            }
            .addOnCompleteListener {
                // BARDZO WAŻNE: Musimy zamknąć klatkę, inaczej kamera się zawiesi
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}