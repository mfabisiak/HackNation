const express = require('express');
const cors = require('cors');
const { generateAuthenticationOptions } = require('@simplewebauthn/server');

const app = express();
app.use(cors()); // Ważne, żeby frontend mógł gadać z backendem

const PORT = 3000;
// WAŻNE: To musi być domena, na której stoi Wasza "Prawdziwa" strona.
// Na localhost to 'localhost'. Na produkcji to np. 'moj-urzad.pl'.
const RP_ID = 'localhost';

app.get('/verify-request', async (req, res) => {
    // Generujemy opcje dla Passkeys
    const options = await generateAuthenticationOptions({
        rpID: RP_ID, // To jest kluczowe zabezpieczenie!
        userVerification: 'preferred',
        // AllowCredentials można pominąć w trybie "discoverable credential" (Passkeys)
        // lub wpisać tu ID jeśli chcecie wymusić konkretny klucz.
    });

    // Wysyłamy do frontu
    res.json(options);
});

app.listen(PORT, () => console.log(Backend działa na porcie ${PORT}));