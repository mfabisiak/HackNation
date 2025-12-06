const verifyBtn = document.getElementById("verify-btn");
const registerBtn = document.getElementById("register-btn")

async function verify(){
    console.log("Verifying...");

    const _options = await fetch("http://localhost:3000/signinRequest");

    const decoded_options = await _options.json();

    const options = PublicKeyCredential.parseRequestOptionsFromJSON(decoded_options);
    console.log(options)

    const credential = await navigator.credentials.get({
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

registerBtn.addEventListener("click", registerKey);