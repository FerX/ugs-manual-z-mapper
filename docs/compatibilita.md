# Compatibilità con UGS e AutoLeveler

## Versione verificata

- Sorgenti UGS al tag `v2.1.26`.
- Modulo compilato come NetBeans Module (`.nbm`) con Java 17.
- Archivio Linux x64 ufficiale UGS Platform 2.1.26, SHA-256 `05cfdcc85d57046f5dc4c16e880530ac9d3bdcd992c03f654d80e243a3a066f0`.
- Avvio in ambiente Xvfb con userdir isolata: il log di UGS elenca `it.serex.ugs.ugs.manual.heightmap` tra i moduli caricati e registra `ManualHeightmapTopComponent` nella modalità della finestra.
- Integrazione backend UGS reale + grblHAL Simulator su seriale virtuale: sollevamento Z, trasferimento XY, discesa, jog Z e acquisizione della posizione verificati dal test `SimulatorIntegrationTest`. Non è ancora stata eseguita una prova tramite click nell'interfaccia grafica completa.
- Il test `AutoLevelerCompatibilityTest` usa `SurfaceScanner` della versione 2.1.26 per ricostruire la griglia da un `.xyz` esportato e associare tutti i punti, compreso un bordo con intervallo più corto. Il comando grafico **Apri scansione** non è stato ancora azionato nel test.

## Convenzione del file XYZ

AutoLeveler prende il passo della griglia dalla differenza Y fra le prime due righe del file. Il plugin scrive quindi i punti per colonna, con Y crescente, e usa lo stesso passo nominale per entrambi gli assi. L'ultimo intervallo può essere più corto: `SurfaceScanner.reset()` limita esplicitamente l'ultima coordinata al bordo massimo. `OpenScannedSurfaceAction.updatePoints()` richiede una quota per ogni punto della griglia, confrontando X/Y con tolleranza 0,01. Per questo l'esportazione è bloccata finché manca anche un solo punto.

Le coordinate sono scritte in millimetri con punto decimale e sei cifre dopo la virgola. Il formato XYZ non contiene un'indicazione delle unità: impostare UGS in mm prima dell'importazione.

## Mappa con tutte le Z uguali

Nel sorgente UGS 2.1.26, `SurfaceScanner.update()` aggiorna gli estremi solo se **X, Y e Z** hanno ciascuno un intervallo diverso da zero. Una mappa perfettamente costante, per esempio nove quote `0`, non soddisfa il controllo su Z; AutoLeveler mantiene gli estremi precedenti e l'importazione può fallire. La quota non viene alterata dal plugin per aggirare il controllo. La correzione appropriata è una modifica upstream di AutoLeveler che richieda un intervallo positivo soltanto su X e Y. Fino a quella modifica, l'importazione della mappa piatta non è garantita.

## Riferimento Z

Il plugin registra la coordinata Z di lavoro che UGS riporta quando il foglio produce lo stesso attrito usato per stabilire lo zero principale. Non sottrae spessori. Non usare correzioni sonda di AutoLeveler non pertinenti a quote già riferite all'utensile.

## Arresto e sicurezza

Il pulsante di interruzione chiama `IController.cancelJog()` e sospende il flusso del plugin. Nel controller GRBL UGS usa il cancel jog hardware quando disponibile e `cancelSend()` altrimenti. Il comportamento reale dell'arresto va verificato sulla propria macchina; i comandi di emergenza della macchina rimangono il riferimento per fermare immediatamente un movimento pericoloso.
