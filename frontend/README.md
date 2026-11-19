# Frontend — aplikacja trenerska

React + TypeScript, Vite, Tailwind i komponenty Radix. Interfejs jest po polsku i dostosowany do telefonów.

## Uruchomienie

Wymagany Node.js 22.14+ oraz backend na `localhost:8080`:

```bash
npm ci
npm run dev
```

Frontend: http://localhost:5173. Vite przekazuje żądania `/api` do backendu.

```bash
npm run build
npm run lint
npm test
```

Testy używają wbudowanego runnera Node i obsługi TypeScript (`--experimental-strip-types`). Sprawdzają ważność tokenów i reguły walidacji hasła, w tym limit bajtów BCrypt dla znaków Unicode.

## Strony i sesja

| Trasa | Dostęp i działanie |
| --- | --- |
| `/` | Publiczna strona wejściowa |
| `/logowanie`, `/rejestracja` | Dla niezalogowanych; rejestracja wymaga aktywacji e-mailem |
| `/profil` | Po zalogowaniu i potwierdzeniu sesji przez `GET /api/users/me` |
| `/trening?planId=…` | Wybór dnia planu i zapis rzeczywiście wykonanych serii |
| `/progres` | Wyniki, pomiary ciała, filtrowana historia i wykresy |
| `/wspolpraca` | Zaproszenia od trenerów, zgoda na dostęp do progresu i przypisane plany |
| `/trener?grupa=…` | Tylko TRAINER: grupy, zaproszenia, przypisywanie planów i podgląd progresu |
| `/forgot-password` | Wysłanie prośby o reset hasła |
| `/reset-password?token=…` | Ustawienie i powtórzenie nowego hasła |
| `/confirm-email?token=…` | Aktywacja konta przyciskiem; ponowna wysyłka linku |
| `/confirm-deletion?token=…` | Jawne, ostateczne potwierdzenie usunięcia konta |

Prośba o usunięcie konta wymaga aktualnego hasła i potwierdzenia zamiaru w profilu. Samo otwarcie linku z e-maila nie wykonuje żadnej operacji. Po udanym resecie hasła lub usunięciu konta lokalna sesja jest czyszczona, a token potwierdzenia usuwany z adresu.

JWT i dane sesji są przechowywane w `localStorage`. Guard sprawdza lokalny termin ważności i weryfikuje sesję na serwerze przed pokazaniem profilu. Odpowiedź 401 z chronionego API usuwa sesję. Wylogowanie i zmiana sesji synchronizują się między kartami; utrata połączenia z API pozwala ponowić sprawdzenie bez wylogowania.

Backend weryfikuje podpis JWT i uprawnienia; guard jest wyłącznie warstwą interfejsu. Publiczne operacje uwierzytelniania nie wysyłają starego JWT. Polityka `no-referrer` chroni linki z tokenami przed przekazaniem w nagłówku Referer.

W Dockerze Nginx przekazuje `/api` do kontenera backendu i zapewnia obsługę bezpośredniego wejścia na trasy SPA.

## Wdrożenie frontendu na Vercel

Frontend i backend są osobnymi projektami Vercel. W projekcie frontendu ustaw Root Directory na `frontend`, preset `Vite`, Node.js 22.x, instalację `npm ci`, build `npm run build` i Output Directory `dist`. Plik `vercel.json` zawiera fallback SPA i nagłówek `Referrer-Policy: no-referrer`.

Przed zbudowaniem dodaj w panelu frontendu publiczną zmienną:

```dotenv
VITE_API_URL=https://twoj-backend.vercel.app
```

Wpisz sam origin backendu, bez `/api`, ścieżki, parametrów i ukośnika na końcu. Frontend wywołuje API bezpośrednio pod tym adresem i zachowuje nagłówek JWT `Authorization`. Backend musi dopuścić dokładny origin frontendu w CORS. Ta konfiguracja nie używa zewnętrznego rewrite `/api` ani proxy Vite/Nginx. Nie wymaga ręcznego wpisywania domeny backendu do `vercel.json`.

`VITE_API_URL` jest wbudowywany w publiczny JavaScript. Nie umieszczaj w nim hasła bazy, tokenów ani kluczy Supabase. Zmiana tej zmiennej wymaga ponownego deploymentu frontendu. Na Vercel brak adresu lub adres inny niż HTTPS przerywa build z komunikatem konfiguracyjnym; chroni to przed przypadkowym kierowaniem API do statycznego frontendu. Poza Vercel pusta wartość zachowuje dotychczasowe lokalne `/api`.

Przykład lokalnego sprawdzenia konfiguracji produkcyjnej w PowerShell:

```powershell
$env:VERCEL = '1'
$env:VITE_API_URL = 'https://twoj-backend.vercel.app'
npm run build
Remove-Item Env:VERCEL, Env:VITE_API_URL
```

Pełna kolejność wdrożenia i zmienne backendu/Supabase: [instrukcja wdrożenia](../docs/DEPLOYMENT.md).

Frontend pokazuje standardowe komunikaty dotyczące poczty. Lokalny Mailpit przechwytuje wiadomości z linkami aktywacji, resetu hasła i usunięcia konta; sposób dostarczenia wiadomości konfiguruje backend.

Wykres ciężaru pokazuje największy ciężar serii danego ćwiczenia w każdym lokalnym dniu. Wykresy wagi i obwodów pokazują poszczególne pomiary. Każdy wykres ma rozwijaną tabelę dokładnych wartości. Historia wyników obejmuje maksymalnie 2000 ostatnich serii, a pomiary — 500 ostatnich wpisów dla wybranych filtrów; zakres dat pozwala przejrzeć starsze dane. Podopieczny akceptuje zaproszenie w aplikacji i może zakończyć współpracę. Trener widzi progres tylko przy aktywnej współpracy. Przypisanie planu tworzy niezależną kopię na koncie podopiecznego.
