const verifyBtn = document.getElementById("verify-btn");
// const registerBtn = document.getElementById("register-btn")

function arrayBufferToBase64Url(buffer) {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    const base64 = btoa(binary);
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

async function verify(){
    console.log("Verifying...");

    const challenge = crypto.getRandomValues(new Uint8Array(32));

    const decoded_options = {
        allowCredentials: [],
        challenge: arrayBufferToBase64Url(challenge.buffer),
        rpId: "localhost",
        timeout: 60000
    }

    const options = PublicKeyCredential.parseRequestOptionsFromJSON(decoded_options);
    console.log(options)

    await navigator.credentials.get({
        publicKey: options

    })
}

async function registerKey() {
    const _options = await fetch("http://localhost:3000/registerRequest");

    const decoded_options = await _options.json();

    const options = PublicKeyCredential.parseCreationOptionsFromJSON(decoded_options);

    const credential = await navigator.credentials.create({
        publicKey: options
    })

}

verifyBtn.addEventListener("click", verify);

// registerBtn.addEventListener("click", registerKey);