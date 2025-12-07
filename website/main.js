const verifyBtn = document.getElementById("verify-btn");
const registerBtn = document.getElementById("register-btn");

const { startAuthentication, startRegistration } = SimpleWebAuthnBrowser;


verifyBtn.addEventListener('click', async () => {
    // Resetujemy UI i blokujemy przycisk
    verifyBtn.disabled = true;

    try {
        // 1. Pobierz opcje generowania (challenge) z serwera
        // Nie wysyłamy username, bo to logowanie "Resident Key"
        const optionsResp = await fetch('http://localhost:3005/login/options');
        const options = await optionsResp.json();

        // 2. Przekaż opcje do przeglądarki (to wywoła okno systemowe/biometrię)
        // Przeglądarka sprawdzi, czy ma klucze pasujące do domeny (rpID)
        const authResponse = await startAuthentication(options);

        // 3. Wyślij podpisany rezultat z powrotem do serwera w celu weryfikacji
        const verifyResp = await fetch('http://localhost:3005/login/verify', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(authResponse),
        });

        await verifyResp.json();


    } catch (error) {
        console.error('Błąd logowania:', error);
    } finally {
        // Odblokuj przycisk niezależnie od wyniku
        verifyBtn.disabled = false;
    }
});


registerBtn?.addEventListener('click', async () => {
    registerBtn.disabled = true;

    try {
        const optionsResp = await fetch('http://localhost:3005/register/options');
        const options = await optionsResp.json();
        const attResponse = await startRegistration(options);
        const verifyResp = await fetch('http://localhost:3005/register/verify', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(attResponse),
        });

        await verifyResp.json();
    } catch (error) {
        console.error('Błąd rejestracji:', error);
    } finally {
        registerBtn.disabled = false;
    }
});


// registerBtn.addEventListener("click", registerKey);