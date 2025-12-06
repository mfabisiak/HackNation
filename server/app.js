var express = require('express');
var crypto = require('crypto');
var cors = require('cors');

var app = express();

// --- KONFIGURACJA ---
// WAŻNE: Jeśli używasz ngrok, wpisz tu domenę z ngrok (bez https://)
// Jeśli testujesz lokalnie, zostaw 'localhost'
const RP_ID = 'localhost';

// --- MIDDLEWARE ---
app.use(cors()); // Pozwala na zapytania z innej domeny/portu
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

// --- FUNKCJA POMOCNICZA ---
// WebAuthn wymaga Base64URL (zamiana znaków + i / na - i _)
function bufferToBase64Url(buffer) {
    return buffer.toString('base64')
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');
}

// --- TWOJE ZADANIE (ENDPOINT) ---
// To jest ten request, który generuje challenge
app.get('/signinRequest', function(req, res) {

    // 1. Generujemy losowe bajty (Challenge)
    const challengeBuffer = crypto.randomBytes(32);
    const challengeString = bufferToBase64Url(challengeBuffer);

    console.log('--- NOWE ŻĄDANIE ---');
    console.log('Generuję challenge:', challengeString);

    // 2. Tworzymy obiekt opcji
    const options = {
        challenge: challengeString,
        rpId: RP_ID,         // To zabezpiecza przed phishingiem
        allowCredentials: [], // Puste = Passkey (discoverable)
        userVerification: 'preferred',
        timeout: 60000,
    };

    // 3. Wysyłamy do frontendu
    res.json(options);
});

// --- WAŻNE DLA BIN/WWW ---
// Eksportujemy aplikację, żeby bin/www mógł ją uruchomić
module.exports = app;
