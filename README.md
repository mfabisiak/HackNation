# Prawda w sieci

## 1. Cel projektu

Celem projektu było stworzenie narzędzia umożliwiającego użytkownikom natychmiastowe i pewne potwierdzenie tożsamości strony internetowej. System ma za zadanie chronić przed phishingiem wizualnym (podrobione strony), dostarczając mechanizm weryfikacji oparty na kryptografii, a nie na subiektywnej ocenie użytkownika. Kluczowym założeniem było wykorzystanie nowoczesnych standardów bezpieczeństwa, w tym technologii **Passkey**, do automatyzacji procesu sprawdzania wiarygodności domeny.

## 2. Opis rozwiązania

System działa w modelu **Cross-Device Assurance** (weryfikacja międzyurządzeniowa). Proces przebiega następująco:
1.  Użytkownik na stronie internetowej inicjuje proces weryfikacji.
2.  Witryna generuje unikalne wyzwanie kryptograficzne prezentowane w formie kodu QR.
3.  Użytkownik skanuje kod za pomocą zaufanej aplikacji mobilnej (np. mObywatel).
4.  Aplikacja mobilna komunikuje się z witryną, wykorzystując mechanizmy standardu FIDO2.
5.  Po pomyślnej walidacji podpisu cyfrowego oraz sprawdzeniu domeny w rejestrze zaufanych podmiotów, aplikacja wyświetla status bezpieczeństwa witryny (pozytywny lub ostrzegawczy).

## 3. Kluczowa technologia Passkey

Decyzja o oparciu systemu na technologii **Passkey (FIDO2/WebAuthn)** podyktowana była konkretnymi, technicznymi przewagami tego standardu nad innymi popularnymi metodami autentykacji.

Oto nasze kluczowe argumenty za tym wyborem:

### A. Natywna integracja z systemami operacyjnymi

To największy atut. Passkey nie jest niszową biblioteką, którą trzeba "doklejać" do systemu. Jest to standard wspierany natywnie przez Apple (iOS/macOS), Google (Android/ChromeOS) i Microsoft (Windows).
*   **Argument:** Dzięki temu nasze rozwiązanie jest **interoperacyjne**. Nie musimy tworzyć skomplikowanego kodu do obsługi kryptografii na każdym modelu telefonu – korzystamy z gotowego API systemowego, które jest utrzymywane i aktualizowane przez gigantów technologicznych. Gwarantuje to stabilność i działanie na szerokiej gamie urządzeń "z pudełka".

### B. Automatyczna walidacja domeny

Passkey rozwiązuje problem weryfikacji "u źródła". W specyfikacji protokołu WebAuthn zaszyty jest mechanizm **Origin Binding**.
*   **Argument:** Przeglądarka internetowa oraz system operacyjny telefonu automatycznie sprawdzają, czy domena żądająca weryfikacji zgadza się z domeną zapisaną w kluczu. Jest to proces wymuszony na poziomie protokołu sieciowego. Jeśli haker podstawi fałszywą stronę (nawet z kłódką SSL), Passkey technicznie **nie pozwoli** na wygenerowanie poprawnego podpisu. Eliminujemy czynnik błędu ludzkiego – to matematyka decyduje o autentyczności, a nie użytkownik.

### C. Weryfikacja fizycznej obecności

Wykorzystanie Passkeys w modelu hybrydowym (skanowanie QR) aktywuje weryfikację bliskości poprzez Bluetooth (BLE).
*   **Argument:** System wymaga, aby telefon i weryfikowana strona znajdowały się blisko siebie. Uniemożliwia to ataki zdalne, w których atakujący próbuje wymusić weryfikację na użytkowniku znajdującym się w innej lokalizacji. Jest to innowacyjne zabezpieczenie wbudowane w standard, którego nie oferują klasyczne metody weryfikacji oparte tylko na sieci internetowej.

### D. Wygoda i szybkość (User Experience)

*   **Argument:** Mimo zaawansowanej kryptografii "pod maską", dla użytkownika proces jest banalny: **Skanuj -> Potwierdź -> Gotowe**. Wykorzystujemy nawyki użytkowników, co drastycznie obniża próg wejścia i zachęca do korzystania z zabezpieczeń.

## 4. Innowacyjność podejścia

Nasze rozwiązanie jest nowatorskie, ponieważ adaptuje technologię Passkey – kojarzoną głównie z logowaniem – do **roli cyfrowego notariusza**. Wykorzystujemy ten sam bezpieczny standard do potwierdzania tożsamości infrastruktury (strony www), a nie użytkownika.
Dzięki temu tworzymy skalowalny ekosystem: w przyszłości każda instytucja zaufania publicznego może wdrożyć ten standard, ponieważ obsługa Passkeys jest już obecna w niemal każdym nowoczesnym smartfonie.

## 5. Potencjalne usprawnienia (Model Decentralizowany)

Obecnie rozwiązanie opiera się na serwerze pośredniczącym (backend). Dzięki właściwościom kryptografii klucza publicznego (na której bazuje Passkey), w przyszłości możliwa jest eliminacja centralnego serwera w procesie weryfikacji. Aplikacja mobilna mogłaby posiadać lokalną bazę kluczy publicznych zaufanych instytucji, co pozwoliłoby na weryfikację stron w trybie offline, wyłącznie na linii Telefon <-> Przeglądarka, co jeszcze bardziej zwiększyłoby prywatność i niezawodność systemu.

## 6. Technologie i implementacja

*   **Backend:** Node.js z frameworkiem Express.js – obsługa żądań WebAuthn i zarządzanie listą zaufanych domen.
*   **Frontend:** HTML/CSS/JS – integracja z API przeglądarki do generowania kodów QR zgodnie ze specyfikacją FIDO.
*   **Aplikacja mobilna:** Natywna aplikacja Android (Kotlin) – pełni funkcję autoryzatora (FIDO Authenticator), wykorzystując bezpieczne moduły sprzętowe telefonu do przechowywania kluczy kryptograficznych.

## 7. Podsumowanie

Zrealizowany projekt demonstruje, jak nowoczesne standardy takie jak Passkey mogą zostać wykorzystane w nietypowy, innowacyjny sposób do podniesienia poziomu bezpieczeństwa w sieci. Zamiast tworzyć własne, podatne na błędy protokoły, oparliśmy się na globalnym standardzie FIDO2. Dzięki temu dostarczamy rozwiązanie, które jest nie tylko kryptograficznie bezpieczne i odporne na phishing, ale również niezwykle proste w obsłudze dla końcowego użytkownika i gotowe do szerokiej implementacji rynkowej.

## 8. Uwaga końcowa – status wdrożenia i wymagania środowiskowe

Projekt jest **w pełni kompletny programistycznie i funkcjonalny**. Należy jednak podkreślić, że specyfikacja standardu WebAuthn/Passkey narzuca bardzo rygorystyczne wymogi bezpieczeństwa, w szczególności konieczność działania w tzw. **Secure Context** (wymuszony protokół HTTPS oraz poprawna konfiguracja domeny).

Ze względu na mechanizm **Origin Binding**, który ściśle wiąże klucze kryptograficzne z konkretną domeną internetową, uruchomienie systemu w środowisku lokalnym (localhost) nie pozwala na poprawne przeprowadzenie pełnego procesu weryfikacji międzyurządzeniowej (cross-device). Do przeprowadzenia demonstracji "na żywo" system wymagałby wdrożenia na zewnętrznym serwerze pod publiczną domeną z ważnym certyfikatem SSL. Jest to celowe ograniczenie standardu FIDO2, mające na celu zapobieganie manipulacjom, co uniemożliwia łatwe zaprezentowanie rozwiązania w warunkach laboratoryjnych bez dedykowanej infrastruktury domenowej.