# Specifica funzionale — Mappa Z manuale

## Obiettivo e architettura

Realizzare un plugin autonomo dentro UGS Platform che accompagni l'operatore nella misura manuale delle altezze di una superficie. Il plugin utilizza la connessione, le coordinate e i comandi di movimento di UGS e salva una mappa `.xyz` che AutoLeveler può importare.

AutoLeveler resta responsabile della compensazione del G-code. Decisione concordata per la prima versione: il plugin esporta il file `.xyz`; l'operatore lo apre manualmente con **Apri scansione** in AutoLeveler. Dopo il collaudo completo si potrà valutare l'apertura automatica del file, se le API UGS lo consentono.

La specifica riguarda UGS Platform e va verificata sulla versione effettivamente utilizzata dall'utente.

## Voce di menu

Nome concordato, in sostituzione del segnaposto `XXXX`:

**Finestra → Plugin → Mappa Z manuale**

La collocazione esatta nel menu va verificata sulla versione UGS scelta; il nome **Mappa Z manuale** è confermato.

La voce apre un pannello di UGS con configurazione della griglia, rappresentazione dei punti, quota corrente, avanzamento e comandi di acquisizione e salvataggio. Aprire il pannello non muove la macchina. Il pannello si apre in modalità **Configurazione**: i clic sui punti non inviano movimenti. Solo dopo la validazione dei parametri e la pressione esplicita di **Inizia misura** il clic su un punto può avviare la sequenza di movimento.

## Area da misurare dal lavoro corrente

Decisione concordata: usare automaticamente la **lavorazione G-code attuale**. La selezione di forme nel Designer non è necessaria: una forma nascosta o esclusa dall'esportazione conta solo se compare effettivamente nel percorso che verrà eseguito.

1. Ricavare dal percorso attuale l'estensione X/Y dei **movimenti di lavorazione**, escludendo gli spostamenti a vuoto quando identificabili con certezza.
2. Proporre una griglia rettangolare che copra tale estensione; non serve misurare l'intera tavola di legno.
3. Mostrare origine dell'area e limiti X/Y prima della conferma; consentire la modifica dei limiti.
4. Verificare che l'area finale contenga tutti i tratti da compensare; segnalare chiaramente qualsiasi tratto escluso.

La distinzione fra movimenti di lavorazione e spostamenti a vuoto va verificata sul G-code reale. Un comando `G1`, da solo, non prova il contatto con il materiale: la quota Z, il contesto e il modo in cui UGS rappresenta il percorso vanno esaminati sulla versione scelta. Decisione concordata per il caso incerto: richiedere all'operatore di **impostare manualmente i limiti X/Y**, mostrare l'area e la griglia risultante e attendere la sua conferma prima di **Inizia misura**. Non includere automaticamente tutti i movimenti a vuoto.

Senza un lavoro utilizzabile, consentire la definizione manuale dei limiti oppure invitare a caricare un file. Un'area con estensione nulla su un asse deve essere ampliata dall'operatore prima di generare una griglia bidimensionale. Le coordinate dell'area devono corrispondere alle coordinate di lavoro usate dalla CNC.

## Generazione della griglia

Decisione concordata: il plugin propone inizialmente **3 righe massimo × 3 colonne massimo**, cioè **fino a 9 misure manuali**. Mostra due campi modificabili, **N righe massimo** e **N colonne massimo**, così l'operatore può aumentare la densità se serve. Per un'area allungata il limite 3 × 3 può essere incompatibile con il passo comune di AutoLeveler: in quel caso spiegare il problema e non aumentare automaticamente il numero di misure. La proposta 3 × 3 è adatta a piccole variazioni diffuse del compensato, ma potrebbe non rilevare una deformazione localizzata fra i punti.

Compatibilità da rispettare: l'importatore `.xyz` di AutoLeveler esaminato ricava un **solo passo** dalla differenza Y delle prime due righe e ricostruisce con quel passo entrambi gli assi. Il compensatore interpola i punti della griglia, ma questo non rende validi due passi indipendenti per X e Y. I numeri inseriti sono quindi **limiti massimi**, non quantità esatte: mantenere l'area della lavorazione, calcolare un passo comune e mostrare righe, colonne e punti **effettivi** prima della generazione.

Per un'area di larghezza `W` e altezza `H`, con massimi `C` colonne e `R` righe, un passo candidato è `max(W/(C-1), H/(R-1))`. Deve anche essere non maggiore di `min(W,H)`, altrimenti le prime due righe del file indurrebbero AutoLeveler a ricostruire una griglia diversa. Se non esiste un passo valido che rispetti entrambi i massimi, non generare una griglia incompatibile: mostrare quali limiti aumentare o consentire la modifica dell'area. Per esempio, su 100 × 60 mm con massimi 5 × 5 si ottengono 5 colonne e 4 righe; su 100 × 10 mm gli stessi massimi sono incompatibili senza aumentare il massimo di colonne.

- Impostare un passo comune per X e Y, coerente con l'importazione attuale di AutoLeveler.
- Mostrare il numero complessivo di punti prima della generazione.
- Coprire anche i bordi dell'area; l'ultimo intervallo può essere più corto del passo.
- Generare almeno due coordinate distinte per ciascun asse.
- Inizializzare **tutte le quote Z a 0**, come richiesto.
- Mantenere separato lo stato di acquisizione: inizialmente ogni punto è **da misurare**.

Uno zero iniziale non è una misura. Dopo una conferma, anche una quota esattamente uguale a zero diventa una misura valida. Il pannello distingue visivamente i punti da misurare, quello attivo e quelli confermati, usando anche simboli o etichette oltre al colore.

La griglia contiene punti anche negli spazi fra lavorazioni separate. Prima di misurare va controllato che tali punti ricadano sul materiale e siano raggiungibili. L'esclusione arbitraria di celle non fa parte della prima versione: AutoLeveler richiede una griglia completa.

Rigenerare la griglia dopo aver registrato delle quote richiede una conferma e non deve cancellare accidentalmente le misure.

## Selezione e raggiungimento di un punto

Durante la sessione attivata con **Inizia misura**, il clic su un punto richiede lo spostamento verso quel punto. Il pannello deve rendere evidente che la griglia è in modalità di movimento.

Sequenza:

1. Verificare che la macchina sia collegata, ferma e disponibile per i movimenti manuali.
2. Portare Z alla quota di trasferimento, se si trova più in basso.
3. Attendere il completamento del movimento Z prima di iniziare lo spostamento XY.
4. Spostarsi alle coordinate X/Y del punto e attendere l'arresto.
5. Scendere alla quota di avvicinamento con velocità controllata.
6. Attendere l'arresto e abilitare la regolazione manuale e la conferma.

Non basta che il controller abbia accettato un comando: bisogna attendere uno stato aggiornato che confermi il completamento. Il pannello mostra sempre la fase corrente. Durante questa sequenza, ulteriori clic non devono accodare spostamenti verso altri punti.

### Quota di avvicinamento: Z=+1 mm

Aggiornamento concordato: dopo il trasferimento XY, scendere alla quota **Z=+1 mm rispetto allo zero di lavoro**, poi attendere che l'operatore abbassi manualmente l'utensile fino alla superficie.

Questo valore è un riferimento di coordinate: senza una misura precedente non è possibile garantire che l'utensile sia esattamente 1 mm sopra il legno in ogni punto. Se una zona del pezzo supera Z=+1 mm, la discesa può incontrare il materiale. Per usare questo valore, lo zero iniziale e l'altezza massima del pezzo devono lasciare libera la quota di avvicinamento.

Decisione concordata: proporre **+1 mm** come quota iniziale di avvicinamento, modificabile nel pannello prima di **Inizia misura** per pezzi con dislivelli maggiori. Mostrare sempre la quota di lavoro risultante; se viene cambiata dopo che alcune misure sono state confermate, richiedere una sospensione e una nuova verifica prima di altri movimenti.

### Quota di trasferimento dai parametri UGS

Decisione concordata: proporre la quota di trasferimento partendo dall'**altezza di sicurezza configurata in UGS**. Questa impostazione va interpretata secondo la versione UGS scelta: nel codice esaminato è un'altezza aggiunta a una quota di riferimento, non direttamente una coordinata Z di lavoro. Il plugin deve calcolare e mostrare la **Z di lavoro finale** che userà per lo spostamento XY, lasciandola modificare prima della sessione.

La quota finale deve essere almeno pari alla Z di avvicinamento e superare pezzo e ostacoli lungo il trasferimento. Se l'impostazione UGS non è disponibile o il riferimento necessario al calcolo è ambiguo, richiedere l'inserimento esplicito della quota di trasferimento; non avviare automaticamente il movimento con una quota presunta.

La sequenza vale anche per i punti già misurati: non scendere automaticamente alla quota di contatto registrata.

### Velocità dei movimenti automatici

Decisione concordata: il plugin offre un campo modificabile per la **velocità della sola discesa Z** verso la quota di avvicinamento. Alla prima sessione l'operatore imposta esplicitamente il valore; il plugin lo ricorda e lo ripropone nelle sessioni successive, lasciandolo modificare. Sollevamento Z e trasferimento XY usano le impostazioni di movimento di UGS, senza campi di velocità aggiuntivi nel plugin. Mostrare le unità della velocità e verificare sulla versione UGS scelta quali impostazioni vengono realmente applicate a ogni fase. Non avviare la discesa se il valore non è disponibile o non è valido; non presumere una velocità universale per tutte le CNC.

## Regolazione manuale della Z

Una volta raggiunto il punto, l'operatore usa i normali comandi di jog di UGS per avvicinare l'utensile alla superficie, a mandrino fermo.

Il pannello mostra:

- identificativo del punto e coordinate X/Y previste;
- Z corrente nelle coordinate di lavoro;
- quota precedentemente registrata, se presente;
- passo di regolazione e stato della macchina.

Comandi concordati: **freccia su = Z positiva**, **freccia giù = Z negativa**. Ogni pressione genera un micro movimento del passo base, inizialmente **0,01 mm**; **Maiusc + freccia = 5 volte** il passo base (inizialmente 0,05 mm); **Ctrl + freccia = 10 volte** il passo base (inizialmente 0,1 mm). Passo base e moltiplicatori sono personalizzabili nel pannello e vengono conservati nelle impostazioni del plugin. Devono essere positivi e produrre movimenti compatibili con la risoluzione e la corsa della CNC. La combinazione simultanea Maiusc+Ctrl non deve produrre un movimento inatteso: per la prima versione, non eseguire il jog quando entrambi sono premuti.

Accanto alla griglia sono presenti sei pulsanti, tre **▲** e tre **▼**, con le distanze effettive scritte sull'etichetta. I valori iniziali sono ±0,01, ±0,05 e ±0,10 mm e si aggiornano quando cambiano passo base o moltiplicatori. I pulsanti rimangono disabilitati fino allo stato di regolazione manuale. La tastiera è una scorciatoia facoltativa.

Le frecce agiscono soltanto quando l'area dedicata alla regolazione ha il focus e il punto è pronto per la misura. Non devono muovere la macchina mentre si scrive in una casella numerica o si usa un altro pannello. Va verificata la convivenza con le scorciatoie esistenti di UGS, evitando doppi comandi e accumulo di movimenti da ripetizione automatica dei tasti. Il pannello mostra il passo effettivo, inclusi i moltiplicatori attivi.

## Conferma della quota

L'operatore conferma con **Invio** nell'area di acquisizione oppure con il pulsante **Registra quota e avanti**.

La conferma:

1. È disponibile soltanto a macchina ferma, con posizione aggiornata e X/Y corrispondenti al punto attivo entro una tolleranza definita.
2. Legge la Z di lavoro effettivamente riportata da UGS.
3. Memorizza il valore e marca il punto come misurato.
4. Salva automaticamente l'avanzamento della sessione.
5. Avvia la sequenza di raggiungimento del prossimo punto da misurare.

Premere Invio ripetutamente non deve registrare più punti né saltarli. Nei campi di configurazione, Invio conferma il campo e non acquisisce una misura.

**Registrare una quota non azzera l'asse Z.** Tutti i punti condividono lo stesso sistema di coordinate e lo stesso zero di lavoro.

Metodo concordato: l'operatore stabilisce lo **zero Z principale con un foglio di carta** nel punto di riferimento, azzerando UGS quando il foglio inizia a fare attrito. Usa lo stesso foglio e lo stesso criterio di attrito in ogni punto della griglia. Il plugin registra la **Z di lavoro letta da UGS senza sottrarre lo spessore del foglio**: essendo il foglio presente sia allo zero iniziale sia nelle misure, il suo spessore si annulla nelle differenze di quota. Non azzerare nuovamente Z nei punti successivi.

Questa convenzione colloca lo zero di lavoro alla posizione dell'utensile con il foglio interposto, non esattamente sulla superficie fisica del legno. Va mantenuta coerente anche con il riferimento Z del G-code e con l'impostazione della superficie nominale in AutoLeveler. Se si cambia foglio, utensile o metodo di contatto durante la sessione, sospendere e verificare il riferimento prima di proseguire.

## Ordine dei punti e correzioni

Ordine concordato: percorso a serpentina fra punti adiacenti, saltando quelli già confermati. È possibile scegliere manualmente un altro punto o rimisurarne uno completato.

Se si parte da un punto intermedio, al termine del percorso si prosegue sui punti ancora mancanti: la sessione è completa solo quando tutti sono confermati.

Se si seleziona un punto già misurato, la vecchia quota rimane memorizzata fino alla nuova conferma. Non si scende automaticamente alla vecchia quota di contatto: si segue la stessa procedura di avvicinamento degli altri punti.

## Fine, interruzioni e ripresa

Dopo l'ultima conferma, sollevare Z alla quota di trasferimento, attendere l'arresto e mostrare **Mappa completata**. Non effettuare altri spostamenti XY automatici.

Un comando **Interrompi movimento** deve essere disponibile durante la procedura e usare i meccanismi di arresto appropriati di UGS. Dopo un'interruzione, conservare le misure già confermate e richiedere una nuova azione esplicita prima di riprendere i movimenti.

In caso di disconnessione, allarme o posizione non aggiornata, sospendere l'acquisizione. Dopo una riconnessione, verificare il riferimento delle coordinate prima di riprendere.

Il cambio di zero, sistema di coordinate di lavoro, utensile, unità o file durante la sessione può rendere incoerenti le misure: rilevare le variazioni disponibili tramite UGS e sospendere il flusso per verificarle. Una sessione riaperta non autorizza automaticamente i movimenti.

### Unità della prima versione

Decisione concordata: interfaccia, griglia, quote, passi di jog, velocità ed esportazione `.xyz` in **millimetri**. Prima di **Inizia misura** verificare che le coordinate usate da UGS e dal percorso attuale siano interpretate correttamente in millimetri; se questo non è verificabile, non avviare movimenti automatici. Un cambio di unità durante la sessione sospende la misura. All'importazione della mappa, anche AutoLeveler deve essere configurato per leggere i valori in millimetri.

## Salvataggio

Decisione concordata: usare **due pulsanti distinti**, con scopi espliciti:

- **Salva sessione:** conserva griglia, quote, punti ancora da misurare, unità, riferimenti e impostazioni. È disponibile anche a lavoro incompleto e consente di riprendere o correggere la mappa.
- **Esporta mappa per AutoLeveler:** disponibile quando tutti i punti sono confermati, produce il file `.xyz` che l'operatore aprirà con **Apri scansione** in AutoLeveler.

Decisione concordata: oltre al pulsante **Salva sessione**, il plugin salva automaticamente la sessione **dopo ogni punto confermato**. La copia di recupero è distinta dal file `.xyz` e deve essere recuperabile dopo un'interruzione senza sovrascrivere silenziosamente un altro lavoro. Se il salvataggio automatico fallisce, mostrare l'errore e non passare automaticamente al punto successivo.

L'esportazione non deve sostituire con zero i punti mancanti. I dati della sessione restano disponibili anche dopo l'esportazione.

Il formato `.xyz` contiene una riga per punto, con tre numeri `X Y Z`, separati da spazi e con punto decimale, senza intestazioni. L'ordine del file è quello richiesto dall'importazione, indipendentemente dall'ordine usato per misurare.

Il file non contiene metadati sulle unità: indicare chiaramente **mm** nel pannello e nel nome proposto del file. Prima dell'importazione UGS deve usare millimetri. Gli offset sonda di AutoLeveler devono essere coerenti con quote già riferite all'utensile, senza aggiungere correzioni indesiderate.

## Verifiche di compatibilità già individuate

Dall'analisi dei sorgenti UGS:

- `BackendAPIReadOnly.getWorkPosition()` fornisce la posizione di lavoro.
- `JogService` offre i comandi di movimento manuale.
- `UpdateMinMaxFromGcode` ricava i limiti dalle statistiche del file caricato.
- `OpenScannedSurfaceAction` ricostruisce la griglia e cerca tutti i punti attesi nel file.
- Il passo è ricavato dalla differenza Y tra i primi due punti: l'esportazione deve rispettare questa convenzione e produrre un passo positivo.
- Il passo ricavato deve ricostruire correttamente entrambi gli assi, anche per aree strette o con dimensioni non multiple del passo richiesto.
- `XyzSurfaceReader` interpreta le coordinate nelle unità preferite di UGS.
- `SurfaceScanner.probeEvent()` applica gli offset configurati anche ai punti importati.
- Nel codice esaminato, `SurfaceScanner.update()` aggiorna i limiti solo se anche Z minima e massima sono diverse. Il caso di mappa con tutte le quote identiche richiede una prova specifica. Non alterare artificialmente le misure per aggirare il problema.

Queste sono osservazioni sul codice, non una certificazione di compatibilità con l'installazione dell'utente. La scelta della versione di UGS precede lo sviluppo.

## Prove senza CNC

Per la verifica senza macchina fisica si propone UGS collegato a grblHAL Simulator su Linux. Il collegamento e le funzioni effettivamente disponibili devono essere verificati nella configurazione scelta.

Casi di accettazione previsti:

1. Griglia 3 × 3: nove quote iniziali a zero, nessuna confermata.
2. Selezione di un punto: completamento del sollevamento prima dello spostamento XY e successivo avvicinamento.
3. Modifica manuale di Z: registrazione della coordinata riportata dal controller e avanzamento una sola volta.
4. Quota misurata uguale a zero: viene considerata valida.
5. Rimisura di un punto, scelta di un punto intermedio e completamento di tutti i punti mancanti.
6. Tastiera: assenza di movimenti da campi di testo e assenza di doppie acquisizioni con Invio.
7. Salvataggio e riapertura di una sessione incompleta; esportazione impedita fino al completamento.
8. Arresto, disconnessione e cambio del riferimento Z: nessuna prosecuzione automatica con dati incoerenti.
9. Importazione in AutoLeveler e confronto dei punti con quelli salvati, inclusi unità, offset, bordi e mappa costante.
10. Applicazione della mappa a un percorso noto e controllo delle quote compensate.

Le prove simulate verificano il comportamento software. Il contatto con il materiale, la precisione e le quote operative richiedono una successiva verifica sulla CNC.

## Verifiche tecniche ancora aperte

- Identificare la versione di UGS e verificare come distinguere i movimenti di lavorazione dagli spostamenti a vuoto nel percorso attuale.
- Verificare sulla versione scelta l'altezza di sicurezza di UGS e il riferimento da cui ricavare la quota di trasferimento; avvicinamento richiesto a Z=+1 mm, con valore modificabile proposto.
- Verificare che il G-code e la superficie nominale di AutoLeveler usino lo stesso riferimento Z stabilito con il foglio.

## Riferimenti

- [API di lettura UGS](https://github.com/winder/Universal-G-Code-Sender/blob/master/ugs-core/src/com/willwinder/universalgcodesender/model/BackendAPIReadOnly.java)
- [Servizio di jog](https://github.com/winder/Universal-G-Code-Sender/blob/master/ugs-core/src/com/willwinder/universalgcodesender/services/JogService.java)
- [Modulo AutoLeveler, importazione e gestione della superficie](https://github.com/winder/Universal-G-Code-Sender/tree/master/ugs-platform/ugs-platform-surfacescanner/src/main/java/com/willwinder/ugs/platform/surfacescanner)
- [Discussione UGS sulla modifica manuale delle mappe](https://github.com/winder/Universal-G-Code-Sender/discussions/3051)
- [grblHAL Simulator](https://github.com/grblHAL/Simulator)

Analisi di riferimento: sorgenti UGS al commit `7e13ab7e2d85ca2f98cbfdb9c49925b73410008b`; controlli mirati sull'importazione anche sul tag `v2.1.26`.
