# Piccola Fattoria

Piccola simulazione agricola Android nativa, realizzata in Java con Android SDK e Gradle.

## Funzioni
- Monete, semi, acqua e raccolto
- Semina, irrigazione, crescita e raccolta
- Acquisto di semi e acqua
- Espansione della fattoria fino al livello 5
- Salvataggio automatico locale

## Generare l'APK su GitHub
1. Crea un repository GitHub.
2. Carica tutto il contenuto di questa cartella nella radice del repository.
3. Apri la scheda **Actions**.
4. Se richiesto, abilita i workflow.
5. Avvia **Build APK Android** con **Run workflow**.
6. Al termine, apri l'esecuzione e scarica l'artifact **Piccola-Fattoria-APK**.

Il file APK è `app-debug.apk`.

## Compilazione locale
Serve JDK 17 e Gradle 8.9:
`gradle assembleDebug`

L'APK viene creato in `app/build/outputs/apk/debug/`.
