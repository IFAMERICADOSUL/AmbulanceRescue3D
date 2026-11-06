# Frontiera Selvaggia

Progetto Godot 4 per un survival 3D top-down in italiano, pensato per Android.

## Controlli
- PC: WASD per muoversi, E per raccogliere.
- Android: joystick tattile sul lato sinistro; pulsante AZIONE per raccogliere.
- Pannello a destra: selezione costruzione, creazione ascia, inventario e salvataggio.

## Funzioni presenti nella build 0.2
- Mondo 3D esplorabile con vegetazione, rocce e rottami.\n- Telecamera orbitale: trascina sul lato destro del touch screen per ruotare; rotella mouse per zoom su PC.\n- Nuovi elementi costruibili: pavimento, letto, fornace e rastrelliera.\n- Raccolta di erbe e ricette rapide per strumenti/difesa.
- Raccolta di legno, pietra, fibra, bacche e rottami.
- Indicatori di vita, acqua, cibo, energia e bisogno fisiologico.
- Nemici che inseguono il giocatore e infliggono danni.
- Costruzione di muri, porte, finestre, tetto, fuoco, armadio e banco da lavoro.
- Scelta personaggio, nuova partita e caricamento.
- Salvataggio automatico ogni 120 secondi, senza mettere in pausa il gioco, con pallino in basso a destra.

## Aprire il progetto
1. Installa Godot 4.3 o una versione 4.x compatibile.
2. Importa questa cartella in Godot tramite `project.godot`.
3. Avvia la scena principale.

## Creare l'APK con GitHub Actions
1. Crea un repository GitHub e carica tutti i file di questa cartella.
2. Apri **Actions** e abilita i workflow se richiesto.
3. Esegui **Build APK Android** con **Run workflow**, oppure fai push su `main`.
4. Al termine, apri l'esecuzione completata e scarica l'artifact `Frontiera-Selvaggia-Android`.

Il workflow produce un APK di debug per test. Per distribuire su Google Play occorrono firma release, keystore privato e configurazione di pubblicazione.

## Nota sullo stato
Questa è una base giocabile/prototipo tecnico, non un survival commerciale completo. Grafica 3D realizzata con primitive e materiali Godot: non è pixel art, ma per un aspetto fotorealistico servono modelli 3D, animazioni, texture e audio originali aggiuntivi. Le meccaniche di costruzione, combattimento, bisogni e inventario sono una prima implementazione da espandere.
