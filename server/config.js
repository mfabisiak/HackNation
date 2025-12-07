// config.js - Konfiguracja środowiska i domen

module.exports = {
    // --- FRONTEND ---
    frontendUrl: 'http://localhost:63343',

    // --- WEBAUTHN / PASSKEYS ---
    rpID: 'localhost', // WAŻNE: Na localhoście MUSI być 'localhost'
    rpName: 'Twoja Appka Local',

    // --- ANDROID ---
    androidHash: '36:EC:BC:13:CF:06:1E:97:DB:29:4B:B0:D6:FF:CD:50:6E:40:E2:8C:1A:56:F7:79:8F:A5:24:E5:0F:F5:07:B8',
    androidPackageName: 'com.example.myapplication', // Zmień na swoją nazwę pakietu Android
    androidSha256Fingerprint: '36:EC:BC:13:CF:06:1E:97:DB:29:4B:B0:D6:FF:CD:50:6E:40:E2:8C:1A:56:F7:79:8F:A5:24:E5:0F:F5:07:B8', // Zmień na swój SHA256 fingerprint

    // --- SESJA ---
    sessionSecret: 'super-secret-key',
    sessionMaxAge: 1000 * 60 * 15, // 15 minut

    // --- DOZWOLONE ORIGINY ---
    // Lista origin'ów akceptowanych podczas weryfikacji WebAuthn
    getAllowedOrigins: function() {
        return [
            this.frontendUrl, // http://localhost:63343
            `android:apk-key-hash:${this.androidHash}` // Android origin
        ];
    },

    // Tylko web origin (dla ściślejszej weryfikacji)
    getWebOrigin: function() {
        return this.frontendUrl;
    },

    // Tylko Android origin
    getAndroidOrigin: function() {
        return `android:apk-key-hash:${this.androidHash}`;
    },

    // Konfiguracja assetlinks.json dla Androida
    getAndroidAssetLinks: function() {
        return [
            {
                "relation": [
                    "delegate_permission/common.handle_all_urls",
                    "delegate_permission/common.get_login_creds"
                ],
                "target": {
                    "namespace": "android_app",
                    "package_name": this.androidPackageName,
                    "sha256_cert_fingerprints": [this.androidSha256Fingerprint]
                }
            }
        ];
    }
};

