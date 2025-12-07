const express = require('express');
const session = require('express-session');
const crypto = require('crypto');
const cors = require('cors');
const {
    generateRegistrationOptions,
    verifyRegistrationResponse,
    generateAuthenticationOptions,
    verifyAuthenticationResponse,
} = require('@simplewebauthn/server');
const config = require('./config'); // Import konfiguracji

const app = express();

// --- MIDDLEWARE ---

// 1. Naprawa bĹ‚Ä™du CORS
app.use(cors({
    origin: config.frontendUrl, // Pozwalamy temu adresowi gadaÄ‡ z serwerem
    credentials: true    // Pozwalamy przesyĹ‚aÄ‡ ciasteczka (sesjÄ™)
}));

app.use(express.json());

// 2. Naprawa sesji na HTTP
app.use(session({
    secret: config.sessionSecret,
    resave: false,
    saveUninitialized: false,
    cookie: {
        secure: false, // NA LOCALHOST MUSI BYÄ† FALSE (bo nie masz https)
        httpOnly: true,
        maxAge: config.sessionMaxAge
    }
}));

// --- BAZA DANYCH (MOCK) ---
const userStore = {};
const credentialStore = {};



function createAnonymousUser() {
    const userID = crypto.randomBytes(32); // Buffer zamiast hex string
    const userIDHex = userID.toString('hex'); // Hex string dla klucza w userStore
    userStore[userIDHex] = {
        id: userID, // Przechowujemy Buffer
        idHex: userIDHex, // Przechowujemy teĹĽ hex dla wygody
        displayName: `User ${userIDHex.substring(0, 6)}`
    };
    return userStore[userIDHex];
}



// ==========================================
// 1. REJESTRACJA
// ==========================================

app.get('/register/options', async (req, res) => {
    const newUser = createAnonymousUser();
    req.session.registeringUserId = newUser.idHex; // Zapisujemy hex string w sesji

    const options = await generateRegistrationOptions({
        rpName: config.rpName,
        rpID: config.rpID,
        userID: newUser.id,
        userName: newUser.displayName,
        authenticatorSelection: {
            authenticatorAttachment: 'platform',
            userVerification: 'required',
            residentKey: 'required',
            requireResidentKey: true
        },
    });

    // Zapisujemy challenge w sesji
    req.session.currentChallenge = options.challenge;

    // Musimy rÄ™cznie zapisaÄ‡ sesjÄ™ w niektĂłrych konfiguracjach store'a
    req.session.save();

    res.json(options);
});

app.post('/register/verify', async (req, res) => {
    const expectedChallenge = req.session.currentChallenge;
    const registeringUserId = req.session.registeringUserId;

    if (!expectedChallenge || !registeringUserId) {
        return res.status(400).json({ error: 'Brak sesji rejestracji (odĹ›wieĹĽ stronÄ™)' });
    }

    try {
        // ZMIANA: Pozwalamy na rejestracjÄ™ z WWW i Androida dla testĂłw na localhost.
        // Lista dozwolonych originĂłw jest konfigurowana w config.js

        let verificationResponse = await verifyRegistrationResponse({
            response: req.body,
            expectedChallenge,
            expectedOrigin: config.getAllowedOrigins(), // Akceptujemy wszystkie dozwolone originy
            expectedRPID: config.rpID,
        });

        const { verified, registrationInfo } = verificationResponse;

        if (verified && registrationInfo) {
            const { credentialID, credentialPublicKey, counter } = registrationInfo;
            const credIdString = Buffer.from(credentialID).toString('base64url');

            credentialStore[credIdString] = {
                userId: registeringUserId, // To jest hex string (klucz w userStore)
                credentialID,
                credentialPublicKey,
                counter,
                transports: req.body.response.transports
            };

            delete req.session.currentChallenge;
            delete req.session.registeringUserId;
            req.session.save();

            res.json({ verified: true, userId: registeringUserId });
        } else {
            res.status(400).json({ verified: false, error: 'Weryfikacja nieudana' });
        }
    } catch (error) {
        console.error('BĹ‚Ä…d rejestracji:', error);
        res.status(400).json({ error: error.message });
    }
});

// ==========================================
// 2. LOGOWANIE
// ==========================================

app.get('/login/options', async (req, res) => {
    const options = await generateAuthenticationOptions({
        rpID: config.rpID,
        userVerification: 'preferred',
    });

    req.session.currentChallenge = options.challenge;
    req.session.save();

    res.json(options);
});

app.post('/login/verify', async (req, res) => {
    const expectedChallenge = req.session.currentChallenge;

    if (!expectedChallenge) {
        return res.status(400).json({ error: 'Brak challenge w sesji.' });
    }

    const bodyCredID = req.body.id;
    const storedCredential = credentialStore[bodyCredID];

    if (!storedCredential) {
        return res.status(400).json({ error: 'Klucz nieznany (czy na pewno siÄ™ zarejestrowaĹ‚eĹ› na localhost?)' });
    }

    const user = userStore[storedCredential.userId];

    try {
        const verification = await verifyAuthenticationResponse({
            response: req.body,
            expectedChallenge,
            expectedOrigin: config.getAllowedOrigins(), // Akceptujemy obie formy originów
            expectedRPID: config.rpID,
            authenticator: {
                credentialID: storedCredential.credentialID,
                credentialPublicKey: storedCredential.credentialPublicKey,
                counter: storedCredential.counter,
            },
        });

        const { verified, authenticationInfo } = verification;

        if (verified) {
            storedCredential.counter = authenticationInfo.newCounter;
            delete req.session.currentChallenge;
            req.session.userId = user.id;
            req.session.save();

            res.json({ verified: true, user: user });
        } else {
            res.status(400).json({ verified: false });
        }
    } catch (error) {
        console.error('BĹ‚Ä…d logowania:', error);
        res.status(400).json({ error: error.message });
    }
});

// Endpoint dla Cross-Origin Passkeys (Related Origins)
app.get('/.well-known/webauthn', (req, res) => {
    const allowedDomains = require('./allowed-domains.json');
    res.json(allowedDomains);
});

app.get('/.well-known/assetlinks.json', (req, res) => {
    res.json(config.getAndroidAssetLinks());
});



module.exports = app;
