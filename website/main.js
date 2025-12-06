const verifyBtn = document.getElementById("verify-btn");

async function verify(){
    console.log("Verify sent");
    navigator.credentials.get({
        publicKey: {
            challenge: new Uint8Array([1, 2, 3, 4, 5, 6, 7, 8, 9, 0]),

            timeout: 60000,

            userVerification: "preferred"
        }
    })
}

verifyBtn.addEventListener("click", verify);