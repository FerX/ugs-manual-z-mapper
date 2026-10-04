# Roadmap e checklist di sviluppo

Data: 22 settembre 2026. Stato: **piano originario; per l'avanzamento verificato vedere [stato-sviluppo.md](stato-sviluppo.md)**. Le caselle sottostanti sono mantenute come checklist tecnica originale e non rappresentano da sole lo stato corrente.

Leggere prima la [specifica funzionale](specifica-funzionale.md). Questa roadmap la traduce in attività tecniche. Una casella si spunta solo dopo aver prodotto e verificato il risultato indicato; la presenza di codice da sola non basta.

## 1. Risultato da consegnare

Un modulo installabile in UGS Platform con la voce **Mappa Z manuale**. Il modulo deve ricavare l'area dai movimenti di lavorazione del G-code attuale; generare la griglia; accompagnare l'operatore sui punti; registrare la Z di lavoro; salvare una mappa importabile in AutoLeveler.

Sequenza operativa concordata: **sollevamento → spostamento XY → avvicinamento a Z=+1 mm → regolazione manuale → Invio o pulsante → punto successivo**. Il +1 mm è rispetto allo zero di lavoro, non rispetto a una superficie ancora sconosciuta. La quota di trasferimento è separata.

Si sviluppa prima con un controller simulato. La verifica fisica sulla CNC costituisce un passaggio successivo e distinto.

### Come usare la checklist

- Procedere in ordine. Le fasi indicate come prerequisito devono essere concluse prima di quelle dipendenti.
- Per ogni fase registrare versione dei sorgenti, risultato della verifica, eventuali limiti e percorso degli artefatti prodotti.
- Se una funzione non è esposta dalle API pubbliche, documentare il limite e decidere come risolverlo prima di costruirvi sopra altre funzioni.
- Non sostituire una funzione richiesta con un comportamento diverso senza aggiornare la specifica e concordare il cambiamento.
- In particolare, distinguere i movimenti di lavorazione dagli spostamenti a vuoto, oppure mostrare esplicitamente quando questa distinzione non è affidabile.

## 2. Fase 0 — Chiudere le decisioni iniziali

**Obiettivo:** evitare che lo sviluppatore debba indovinare il comportamento desiderato.

- [ ] Rilevare versione esatta di UGS, edizione Platform, versione Java inclusa e architettura Linux.
- [ ] Identificare firmware e versione della CNC reale, anche se al momento non disponibile.
- [x] Definire la fonte dell'area: movimenti di lavorazione del G-code attuale, senza una seconda selezione di forme.
- [x] Se i tratti di lavorazione non sono distinguibili con affidabilità, richiedere limiti X/Y manuali e conferma della griglia; non usare automaticamente tutti i movimenti del file.
- [x] Scegliere la proposta iniziale della griglia: densità automatica basata sull'area, con campi modificabili N righe e N colonne.
- [x] Trattare N righe e N colonne come limiti massimi, non quantità esatte; mantenere l'area e mostrare i conteggi effettivi compatibili con il passo comune di AutoLeveler.
- [x] Proporre inizialmente massimi 3 righe × 3 colonne, fino a 9 misure manuali; non aumentarli automaticamente se l'area è troppo allungata.
- [x] Confermare il nome della voce di menu: **Mappa Z manuale**; verificarne la collocazione esatta sulla versione UGS scelta.
- [x] Confermare il percorso a serpentina e il ritorno ai punti mancanti quando si parte dal centro.
- [x] Confermare Z di avvicinamento iniziale +1 mm modificabile nel pannello prima della misura.
- [x] Scegliere la fonte iniziale della quota di trasferimento: altezza di sicurezza configurata in UGS, con Z di lavoro risultante visibile e modificabile.
- [x] Definire quali velocità personalizzare: solo la discesa Z verso la quota di avvicinamento; sollevamento e XY seguono le impostazioni UGS.
- [x] Definire il comportamento del campo velocità Z: inserimento esplicito alla prima sessione, poi ultimo valore ricordato e modificabile; nessun valore universale predefinito.
- [x] Definire le unità della prima versione: solo millimetri, con verifica prima dei movimenti e sospensione se le unità cambiano durante la sessione.
- [x] Definire i tasti di jog Z: frecce su/giù, Maiusc ×5 e Ctrl ×10 rispetto al passo base, attivi solo nel pannello di misura.
- [x] Scegliere il passo base iniziale per il jog Z: 0,01 mm, modificabile insieme ai moltiplicatori Maiusc ×5 e Ctrl ×10.
- [x] Definire il metodo di contatto: stesso foglio e stesso criterio di attrito per lo zero Z principale e per ogni punto; registrare direttamente la Z di lavoro, senza una seconda correzione dello spessore.
- [x] Abilitare i movimenti da clic soltanto dopo la pressione esplicita di **Inizia misura**; pannello inizialmente in sola configurazione.
- [x] Prevedere due pulsanti distinti: **Salva sessione** anche se incompleta ed **Esporta mappa** solo a misure complete.
- [x] Salvare automaticamente la sessione dopo ogni punto confermato; un errore di scrittura impedisce l'avanzamento automatico.
- [x] Per la prima versione, aprire il `.xyz` manualmente con **Apri scansione** in AutoLeveler; valutare l'apertura automatica solo dopo il collaudo.

**Uscita:** aggiornare la specifica con le decisioni. La verifica tecnica dei movimenti di lavorazione non impedisce di sviluppare il modello della griglia e i test offline.

## 3. Fase 1 — Preparare l'ambiente riproducibile

**Prerequisito:** versione di UGS scelta.

- [ ] Inizializzare un repository Git per il progetto, se assente, e includere la documentazione esistente.
- [ ] Scaricare i sorgenti ufficiali UGS in una cartella dedicata, senza sovrascrivere `docs`.
- [ ] Fare checkout del tag corrispondente alla versione scelta e annotare anche il commit risolto.
- [ ] Leggere README, eventuali AGENTS.md e POM di quella versione.
- [ ] Installare il JDK richiesto da quella versione, non quello suggerito da una guida relativa a un'altra release.
- [ ] Verificare `java -version` e `./mvnw -version` dalla radice dei sorgenti UGS.
- [ ] Compilare e avviare UGS originale prima di aggiungere il plugin, usando le istruzioni del tag selezionato.
- [ ] Preparare un'installazione e un profilo utente di prova separati dall'UGS usato sulla CNC; verificare le opzioni di avvio effettive con l'help del launcher.
- [ ] Identificare dove vengono salvate le impostazioni del profilo di prova e verificare che non coincidano con quelle operative.
- [ ] Annotare i comandi effettivamente riusciti in `docs/ambiente-sviluppo.md`.
- [ ] Escludere da Git output di compilazione, log, profili utente e dipendenze scaricate.

Nel README UGS esaminato il primo build usa `./mvnw install` e l'avvio usa `./mvnw nbm:run-platform -pl ugs-platform/application`. Verificarli sul tag scelto. Il primo build può compilare molte dipendenze; questo non implica che ogni modifica successiva richieda tutto il build.

**Uscita:** UGS originale parte nel profilo di prova; lo stesso procedimento è ripetibile seguendo il documento.

## 4. Fase 2 — Verificare le integrazioni prima di costruire l'interfaccia

### 4.1 API pubbliche

- [ ] Leggere `BackendAPIReadOnly`, `BackendAPI`, `JogService` e il servizio di lookup della versione scelta.
- [ ] Controllare nei POM quali package sono pubblici per altri moduli NetBeans.
- [ ] Verificare come ottenere l'istanza del backend esistente. Non creare una seconda connessione seriale.
- [ ] Verificare quali eventi riportano stato macchina, posizione, modifica delle impostazioni e cambio del file.
- [ ] Verificare unità e sistema di riferimento dei metodi di lettura e movimento. Il nome del metodo da solo non garantisce che usi coordinate macchina o di lavoro.
- [ ] Verificare che coordinate del percorso, stato UGS, jog, velocità e file `.xyz` siano interpretati in millimetri; bloccare **Inizia misura** se non è possibile garantirlo.
- [ ] Verificare se i metodi di movimento propagano gli errori o li registrano soltanto nei log. Il flusso deve rilevare anche un comando non eseguito.
- [ ] Leggere l'altezza di sicurezza UGS, verificarne unità e significato nella versione scelta e definire la quota di riferimento da cui calcolare la Z di trasferimento assoluta in coordinate di lavoro.
- [ ] Se l'impostazione o il riferimento non sono disponibili, richiedere una Z di trasferimento esplicita prima dei movimenti automatici.

### 4.2 Area della lavorazione attuale

- [ ] Individuare il modello del G-code attualmente generato o caricato in UGS e le sue API pubbliche.
- [ ] Verificare che il percorso usato per calcolare l'area coincida con quello che AutoLeveler compenserà, anche dopo esclusioni nel Designer o modifiche del file.
- [ ] Individuare coordinate X/Y di tutti i tratti di lavorazione, considerando modalità assoluta/relativa, unità, origine e trasformazioni applicate.
- [ ] Definire e verificare il criterio per distinguere lavorazione da trasferimento: `G0`/`G1` da soli non bastano sempre; controllare Z e contesto del percorso.
- [ ] Verificare con esempi i tratti di ingresso/uscita, gli archi, più profondità di passata, coordinate negative e lavorazioni disgiunte.
- [ ] Se la distinzione non è affidabile, richiedere l'inserimento manuale dei limiti X/Y e mostrare l'area risultante prima di **Inizia misura**.
- [ ] Mostrare i limiti proposti, permettere la modifica e segnalare se i limiti finali lasciano fuori tratti da compensare.
- [ ] Rilevare l'assenza di un lavoro e i cambi del percorso durante una sessione; impedire l'uso silenzioso di un'area obsoleta.

### 4.3 Contratto con AutoLeveler

- [ ] Leggere `OpenScannedSurfaceAction`, `XyzSurfaceReader`, `SaveScannedSurfaceAction` e `SurfaceScanner` della versione scelta.
- [ ] Creare manualmente una piccola mappa non costante e verificarne l'importazione in UGS scollegato.
- [ ] Verificare ordine delle righe, passo comune, copertura dei bordi, unità e offset sonda.
- [ ] Verificare una mappa a Z costante e una a Z=0. Il codice precedentemente esaminato contiene una condizione sospetta quando Z minima e massima coincidono.
- [ ] Se il caso costante fallisce, registrare una riproduzione minima e scegliere una versione corretta, una correzione separata di UGS o un limite esplicito. Non introdurre piccole altezze false nel file.
- [ ] Salvare i file di prova in `tests/fixtures/` e i risultati in `docs/compatibilita.md`.

**Uscita:** prova concreta dell'importazione e tabella delle API utilizzabili. Nessun automatismo di movimento è ancora necessario.

## 5. Fase 3 — Collegare un controller simulato

- [ ] Preparare grblHAL Simulator per Linux seguendo il README ufficiale e fissarne il commit o la versione.
- [ ] Utilizzare TCP locale o una porta seriale virtuale; registrare configurazione e comandi in `docs/simulatore.md`.
- [ ] Se si usa TCP, scegliere una porta non privilegiata e verificare l'indirizzo di ascolto; usare il simulatore solo nell'ambiente di prova.
- [ ] Collegare UGS con il driver e il firmware appropriati e attendere una connessione realmente operativa.
- [ ] Verificare coordinate aggiornate, stato Idle, jog X/Y/Z, azzeramento di lavoro e movimento assoluto.
- [ ] Verificare arresto del jog, allarme, disconnessione e riconnessione; annotare eventuali limiti del simulatore.
- [ ] Verificare che il controller simulato non venga scambiato nel documento per il firmware effettivo della CNC reale.
- [ ] Conservare una configurazione iniziale ripristinabile.

**Uscita:** dai pulsanti standard di UGS si muovono gli assi virtuali e si osservano le coordinate cambiare. Se questo non funziona, risolvere il collegamento prima di aggiungere i movimenti del plugin.

## 6. Fase 4 — Creare il modulo minimo

- [ ] Creare un modulo Maven NetBeans con packaging `nbm`, seguendo un piccolo plugin presente nella release scelta.
- [ ] Proposta di nome: `ugs-platform-manual-heightmap`; proposta di package: `com.willwinder.ugs.nbp.manualheightmap`.
- [ ] Dichiarare soltanto le dipendenze necessarie e allinearle alla versione UGS selezionata.
- [ ] Registrare un `TopComponent` e la relativa azione nel menu dei plugin, seguendo le convenzioni della release.
- [ ] Aggiungere nome italiano, descrizione e testi dell'interfaccia ai bundle di localizzazione.
- [ ] Aprire un pannello vuoto con titolo corretto, senza inviare comandi alla macchina.
- [ ] Collegare e rimuovere correttamente i listener all'apertura e chiusura.
- [ ] Verificare che aprire più volte il pannello non crei connessioni o listener duplicati.
- [ ] Generare il pacchetto `.nbm` e verificare l'installazione nel profilo di prova. Non assumere che basti copiare un JAR.
- [ ] Documentare il comando per ricompilare il solo modulo quando le dipendenze sono già disponibili.

**Uscita:** modulo installabile, apribile, chiudibile e rimovibile. Nessun movimento automatico.

## 7. Struttura del codice proposta

I seguenti nomi sono proposte per classi nuove, non API già presenti in UGS.

| Parte | Responsabilità |
|---|---|
| `ManualHeightmapTopComponent` | Finestra UGS e ciclo di vita dei listener |
| `ManualHeightmapPanel` | Campi, griglia, pulsanti e messaggi |
| `HeightmapSession` | Dati e avanzamento della misura |
| `GridPoint` | Indici, X/Y previsti, Z e stato misurato |
| `GridGenerator` | Costruzione deterministica della griglia |
| `AreaProvider` | Calcolo dell'area XY dai tratti di lavorazione attuali |
| `UgsMachineAdapter` | Accesso a coordinate, eventi e movimenti di UGS |
| `MeasurementController` | Stati e transizioni della procedura |
| `SessionRepository` | Salvataggio e recupero della sessione |
| `XyzExporter` | Validazione ed esportazione per AutoLeveler |

Il modello e l'esportatore non devono dipendere dai widget Swing. Il controller deve poter usare un adattatore finto nei test. Tutti i comandi alla macchina passano da un unico punto del codice.

## 8. Fase 5 — Modello e generazione della griglia

- [ ] Memorizzare per la sessione: versione del formato, unità, area, passo richiesto ed effettivo, quote operative, velocità, identificativo del lavoro e riferimento delle coordinate.
- [ ] Memorizzare massimi di righe/colonne e conteggi effettivi, mostrando quando differiscono.
- [ ] Memorizzare per ogni punto: indici X/Y, coordinate previste, Z iniziale 0, flag `measured=false` e data dell'ultima acquisizione.
- [ ] Mantenere separati punto selezionato, punto in movimento e punto pronto per la misura.
- [ ] Validare numeri finiti, passo positivo e limiti ordinati con estensione positiva su entrambi gli assi.
- [ ] Impostare un limite documentato al numero di punti per evitare blocchi dell'interfaccia e allocazioni eccessive.
- [ ] Calcolare i bordi senza accumulare errori aggiungendo ripetutamente il passo: usare indice e coordinata iniziale.
- [ ] Calcolare il passo candidato come `max(W/(C-1), H/(R-1))` per estensioni `W,H` e massimi `C,R`; verificare che non superi `min(W,H)` e che i conteggi effettivi siano entro entrambi i massimi.
- [ ] Se i massimi sono impossibili da rispettare senza cambiare l'area, bloccare la generazione con un messaggio che indichi quale massimo aumentare; non esportare una griglia che AutoLeveler ricostruirebbe diversamente.
- [ ] Evitare punti quasi duplicati e precisioni superiori alla tolleranza di importazione; definire una risoluzione minima supportata.
- [ ] Generare un ordine di visita a serpentina e un ordine di esportazione distinto.
- [ ] Implementare la ricerca del prossimo punto non misurato con ritorno all'inizio, senza saltare i punti precedenti a una selezione intermedia.
- [ ] Testare griglia 3 × 3, coordinate negative, area rettangolare, bordi non multipli del passo, area stretta, passo invalido e dimensione eccessiva.
- [ ] Verificare che i valori iniziali siano massimi 3 × 3 e che, per un'area troppo allungata, il plugin mostri l'incompatibilità senza aumentare da solo il numero dei punti.
- [ ] Testare un'area 100 × 60 mm con richiesta 5 righe × 5 colonne: verificare che il risultato e il file esportato rispettino il singolo passo ricostruito da AutoLeveler.
- [ ] Testare un'area 100 × 10 mm con massimi 5 × 5: deve comparire l'incompatibilità, senza generare punti fuori dall'area.

**Uscita:** griglie deterministiche e complete; lo zero misurato è distinto dallo zero iniziale.

## 9. Fase 6 — Interfaccia utilizzabile senza macchina

- [ ] Mostrare origine dell'area: tratti di lavorazione oppure limiti inseriti manualmente; non passare silenziosamente ai limiti di tutti i movimenti.
- [ ] Mostrare limiti X/Y, passo e numero di punti; offrire **Genera griglia**.
- [ ] Proporre 3 × 3 come limiti iniziali; mostrare campi **N righe massimo** e **N colonne massimo**, valori effettivi e numero totale di punti prima della generazione.
- [ ] Mostrare la quota di avvicinamento proposta a Z=+1 mm e consentirne la modifica prima di **Inizia misura**; una modifica durante la sessione sospende i movimenti fino a nuova verifica.
- [ ] Mostrare e validare un campo per la velocità della discesa Z, con unità chiare; sollevamento Z e trasferimento XY non hanno campi di velocità nel plugin.
- [ ] Se manca una preferenza precedente, richiedere l'inserimento della velocità Z prima di **Inizia misura**; ricordare il valore valido per le sessioni successive e consentirne la modifica.
- [ ] Rappresentare gli assi con direzione e unità chiare. Indicare l'identificativo di ciascun punto.
- [ ] Distinguere punti da misurare, attivo e completati con testo o simboli oltre al colore.
- [ ] Mostrare conteggio delle misure, Z corrente, Z registrata e fase operativa.
- [ ] Predisporre **Inizia misura**, **Registra quota e avanti**, **Interrompi movimento**, **Salva sessione**, **Apri sessione**, **Esporta mappa per AutoLeveler**.
- [ ] Verificare che aprire il pannello, generare la griglia e cliccare punti in modalità Configurazione non inviino movimenti; **Inizia misura** abilita i clic solo dopo la validazione.
- [ ] In assenza di connessione consentire configurazione e gestione dei file, ma disabilitare acquisizione reale e movimenti.
- [ ] Richiedere conferma prima di rigenerare una griglia con misure o sostituire una sessione non salvata.
- [ ] Gestire ridimensionamento e griglie grandi con scorrimento o zoom, senza rendere obbligatorio uno schermo molto grande.
- [ ] Aggiornare Swing sul suo thread grafico (EDT); eseguire I/O e attese fuori dall'EDT.

**Uscita:** l'utente capisce quali punti mancano e cosa farà il prossimo comando, anche senza controller.

## 10. Fase 7 — Implementare gli stati dei movimenti

**Prerequisiti:** simulatore operativo, unità e coordinate verificate, modello testato.

Proposta di stati:

| Stato | Azioni permesse e transizione |
|---|---|
| Configurazione | Modifica area; inizio sessione dopo validazione |
| Pronto | Selezione di un punto → Sollevamento |
| Sollevamento | Attesa Z sicura → Trasferimento XY |
| Trasferimento XY | Attesa XY target → Avvicinamento |
| Avvicinamento | Attesa Z di avvicinamento → Regolazione manuale |
| Regolazione manuale | Jog Z; conferma → Salvataggio misura |
| Salvataggio misura | Persistenza riuscita → prossimo punto o Sollevamento finale |
| Sollevamento finale | Attesa Z sicura → Completato |
| Completato | Esportazione o rimisura di un punto |
| Sospeso/Errore | Nessuna sequenza automatica; recupero esplicito dopo verifica |

- [ ] Definire ogni transizione in `MeasurementController`, evitando logica sparsa nei pulsanti.
- [ ] Usare un identificativo di operazione per ignorare eventi o callback appartenenti a un movimento annullato.
- [ ] Prima di ogni fase controllare connessione, stato, riferimento delle coordinate e mandrino fermo per quanto osservabile.
- [ ] Verificare che la discesa Z usi la velocità scelta nel plugin, mentre sollevamento Z e XY seguono le impostazioni UGS effettive; documentare il metodo di movimento impiegato.
- [ ] Non avviare una misura mentre UGS sta eseguendo un file.
- [ ] Se Z è già più alta della quota di trasferimento, mantenerla fino alla fine dello spostamento XY.
- [ ] Mostrare la Z di trasferimento risultante prima di iniziare; controllare che sia almeno pari alla quota di avvicinamento e lasciare che l'operatore la modifichi.
- [ ] Inviare una sola fase alla volta. Non mettere sollevamento, XY e discesa in coda tutti insieme.
- [ ] Riconoscere il completamento con posizione target e stato aggiornato successivo all'invio; non utilizzare un vecchio Idle né il solo `ok` di accettazione.
- [ ] Gestire anche movimenti molto brevi per i quali non arriva uno stato intermedio Run/Jog.
- [ ] Definire tolleranza di posizione, massima età del dato e timeout coerenti con precisione del controller e distanza/velocità richieste; registrarli nella documentazione tecnica.
- [ ] In caso di timeout, comando rifiutato o errore, sospendere senza avviare la fase successiva e senza ritentare il movimento automaticamente.
- [ ] Rifiutare ulteriori clic di movimento durante una sequenza attiva.
- [ ] Gestire movimenti esterni dai pannelli UGS: se alterano la destinazione o il riferimento previsto, sospendere; non presumere il controllo esclusivo della macchina.
- [ ] In caso di arresto, invalidare tutte le azioni successive. Non eseguire automaticamente un rientro o una risalita dopo un allarme.
- [ ] Verificare che chiusura del pannello o rimozione del modulo non lasci una sequenza pendente; definire e collaudare la procedura di interruzione.
- [ ] Testare con un adattatore finto: ritardo, Idle vecchio, errore comando, target non raggiunto, disconnessione e doppio clic.
- [ ] Ripetere le sequenze fondamentali con grblHAL Simulator e conservare il log degli eventi.

**Uscita:** nessun movimento XY inizia prima della risalita verificata, nessuna discesa inizia prima dell'arrivo XY e un'interruzione impedisce ogni prosecuzione automatica.

## 11. Fase 8 — Jog manuale, tastiera e acquisizione

- [ ] Consentire il jog standard di UGS durante lo stato Regolazione manuale.
- [ ] Mostrare il passo attivo; se il plugin lo modifica globalmente, rendere visibile questa scelta e verificare l'effetto sugli altri pannelli.
- [ ] Offrire campi per passo base, moltiplicatore Maiusc e moltiplicatore Ctrl; validare valori positivi, mostrare i tre passi risultanti e conservare le preferenze del plugin.
- [ ] Collegare freccia su/giù soltanto all'area di acquisizione con focus, usando i meccanismi Swing appropriati.
- [ ] Applicare i moltiplicatori personalizzati (inizialmente Maiusc ×5 e Ctrl ×10) al passo base; mostrare il passo effettivo e ignorare la combinazione simultanea Maiusc+Ctrl.
- [ ] Impedire conflitti con scorciatoie globali e invio doppio del medesimo comando.
- [ ] Usare un singolo micro movimento per pressione e una politica esplicita per la ripetizione; la prima versione può richiedere il rilascio del tasto prima del movimento successivo.
- [ ] Perdere il focus deve cancellare i flag di tasto premuto e impedire ulteriori movimenti.
- [ ] Abilitare Invio e pulsante soltanto se posizione fresca, macchina ferma e X/Y entro tolleranza.
- [ ] Copiare uno snapshot coerente della posizione, senza leggere X, Y e Z da aggiornamenti diversi.
- [ ] Registrare la Z di lavoro senza inviare azzeramenti, modifiche di offset o comandi di probing.
- [ ] Verificare che le quote acquisite con il foglio siano registrate senza sottrarne lo spessore; lo zero principale deve essere stato stabilito con lo stesso foglio.
- [ ] Conservare le X/Y nominali della griglia; registrare eventualmente X/Y effettive a fini diagnostici dopo averne verificato la tolleranza.
- [ ] Rendere atomica la conferma: una pressione acquisisce una sola misura e disabilita subito ulteriori conferme.
- [ ] Attendere il salvataggio riuscito prima dell'avanzamento automatico. Se il disco non è scrivibile, conservare la misura in memoria e fermare l'avanzamento.
- [ ] Per la rimisura mantenere il vecchio valore fino alla nuova conferma.
- [ ] All'ultimo punto effettuare solo il sollevamento finale, senza tornare automaticamente allo zero XY.
- [ ] Testare zero valido, quota negativa, Maiusc ×5, Ctrl ×10, entrambi i modificatori, doppio Invio, tasto tenuto premuto, focus in un campo di testo, jog XY accidentale e rimisura.

**Uscita:** una griglia completa può essere acquisita nel simulatore usando pulsanti UGS, frecce locali e Invio.

## 12. Fase 9 — Persistenza e ripresa

- [ ] Definire un formato di sessione versionato, ad esempio JSON, distinto dal file `.xyz`.
- [ ] Creare una copia di recupero automatica dopo ogni conferma, separata dall'esportazione `.xyz` e dal salvataggio esplicito scelto dall'operatore.
- [ ] Identificare le copie di recupero per lavoro/sessione, evitando che un nuovo lavoro sovrascriva silenziosamente le misure di uno precedente.
- [ ] Salvare tutti i dati necessari a ricostruire la griglia, inclusi flag dei punti misurati e riferimento del lavoro.
- [ ] Scrivere prima su un file temporaneo nella stessa cartella e sostituire il file definitivo solo dopo la scrittura riuscita; gestire il caso in cui la sostituzione atomica non sia disponibile.
- [ ] Predisporre un recupero dell'ultima sessione senza sovrascrivere altri lavori.
- [ ] Validare in apertura dimensioni, valori finiti, indici duplicati, punti mancanti e versione del formato.
- [ ] Un file non valido deve produrre un messaggio comprensibile e lasciare intatta la sessione aperta.
- [ ] Dopo riapertura partire in stato sospeso/configurazione, mai con movimento automatico.
- [ ] Verificare nuovamente unità, origine, utensile e posizione del pezzo prima di permettere la continuazione.
- [ ] Specificare quali cambiamenti sono rilevabili da UGS e quali, come spostamento fisico del pezzo o cambio utensile non dichiarato, richiedono verifica dell'operatore.
- [ ] Testare salvataggio/riapertura parziale, file corrotto, formato sconosciuto e errore di scrittura.

**Uscita:** la sessione sopravvive a riavvio o interruzione mantenendo esattamente misure e punti mancanti.

## 13. Fase 10 — Esportazione XYZ e compensazione

- [ ] Bloccare l'esportazione operativa se anche un solo punto non è stato misurato.
- [ ] Validare che la griglia ricostruita dal criterio dell'importatore coincida con quella esportata.
- [ ] Ordinare per X crescente e, per ogni X, Y crescente: le prime due righe devono avere la stessa X e una differenza Y positiva pari al passo effettivo.
- [ ] Scrivere tre numeri finiti per riga, separati da spazi, con punto decimale indipendente dalla lingua del sistema e senza intestazioni.
- [ ] Usare precisione sufficiente a non spostare i nodi oltre la tolleranza di importazione.
- [ ] Esplicitare le unità nel pannello e nel nome proposto del file, senza aggiungere metadati incompatibili dentro `.xyz`.
- [ ] Gestire file già esistente, annullamento del dialogo e fallimento della scrittura.
- [ ] Preparare istruzioni per aprire AutoLeveler, controllare unità/offset, usare **Apri scansione** e applicare la mappa una sola volta.
- [ ] Verificare che la superficie nominale impostata in AutoLeveler e il riferimento Z del G-code siano coerenti con lo zero principale definito tramite il foglio.
- [ ] Provare l'importazione effettiva in UGS, non soltanto rileggere il file con il nostro codice.
- [ ] Confrontare punti, Z e limiti importati con i valori originali.
- [ ] Provare un piano noto: per X/Y da 0 a 20 mm, passo 10 mm e superficie `Z = 0,01·X + 0,02·Y`, il punto centrale ha Z=0,3 mm. Con superficie nominale 0 e percorso a Z=-0,2 mm, verificare al centro una quota compensata pari a +0,1 mm, usando impostazioni senza offset aggiuntivi.
- [ ] Provare anche un percorso che attraversa più celle per verificare l'interpolazione e la suddivisione dei segmenti.
- [ ] Ripetere i casi con area stretta, bordi non multipli, coordinate negative e tutte le Z uguali; registrare gli eventuali limiti della release.

**Uscita:** AutoLeveler legge correttamente le mappe e il G-code compensato soddisfa esempi calcolati indipendentemente.

## 14. Fase 11 — Collaudo completo a casa

Eseguire nel profilo di prova con il simulatore; nessuna connessione a una macchina reale.

- [ ] Creare un lavoro con due forme distinte: escluderne una dal G-code e verificare che l'area segua soltanto la lavorazione attuale; reincluderla e verificare l'area complessiva.
- [ ] Verificare rapidi fuori dal pezzo, tratti di taglio, archi, coordinate negative e origine diversa da zero.
- [ ] Generare nove punti e verificare nove zeri iniziali ma zero punti misurati.
- [ ] Simulare uno zero iniziale posto al contatto con il foglio e una superficie inclinata: il punto di riferimento deve registrare Z=0 e le altre quote le differenze rispetto a quel punto, senza una sottrazione dello spessore.
- [ ] Misurare una sequenza con quote negative, zero e positive inferiori alla quota di avvicinamento.
- [ ] Selezionare un punto intermedio e verificare che alla fine nessun punto venga dimenticato.
- [ ] Rimisurare un punto e controllare che cambi soltanto la misura confermata.
- [ ] Interrompere durante ciascuna delle tre fasi automatiche; verificare che non parta la successiva.
- [ ] Chiudere e riaprire il pannello e riavviare UGS recuperando la sessione.
- [ ] Simulare cambi di unità, zero e file durante la misura e verificare la sospensione prevista.
- [ ] Completare, esportare, importare in AutoLeveler e controllare il risultato numerico e visivo.
- [ ] Eseguire i test automatici del modulo e i controlli richiesti dal progetto UGS scelto.
- [ ] Scrivere `docs/collaudo.md` con versione UGS/simulatore, passi eseguiti, risultati, file di prova e problemi rimasti.

**Uscita:** dimostrazione ripetibile del flusso completo. Lo stato da dichiarare è **collaudato in simulazione**, non collaudato sulla CNC.

## 15. Fase 12 — Pacchetto installabile e istruzioni

- [ ] Assegnare una versione al plugin e indicare le versioni UGS verificate.
- [ ] Compilare il `.nbm` usando il procedimento documentato.
- [ ] Installarlo in un secondo profilo pulito della stessa versione UGS, verificando che non dipenda da file dell'ambiente di sviluppo.
- [ ] Documentare installazione, aggiornamento, disinstallazione e ritorno alla configurazione precedente.
- [ ] Scrivere `docs/guida-utente.md` con schermate e un esempio di misura simulata.
- [ ] Includere istruzioni per area della lavorazione attuale, zero comune con lo stesso foglio, +1 mm, quota di trasferimento, focus della tastiera, rimisura e salvataggi.
- [ ] Documentare i limiti accertati, in particolare eventuali problemi delle mappe costanti o versioni non supportate.
- [ ] Includere un esempio di sessione e una mappa etichettati chiaramente come dati dimostrativi.
- [ ] Controllare licenze e avvisi per eventuale codice riutilizzato da UGS; mantenere le attribuzioni richieste.
- [ ] Verificare checksum e contenuto dell'archivio finale.

**Uscita:** pacchetto installabile da chi non possiede JDK o sorgenti, con guida e risultati dei test.

## 16. Fase 13 — Validazione sulla CNC, quando disponibile

Questa fase richiede la macchina reale e l'operatore presente. Non va simulata nei risultati.

- [ ] Confermare firmware, unità, corsa, origine, utensile e impostazioni usati nelle prove.
- [ ] Verificare prima i movimenti sopra il pezzo, con mandrino fermo e quote scelte dall'operatore.
- [ ] Controllare segno di Z, passo reale dei micro movimenti e coerenza delle coordinate mostrate.
- [ ] Misurare una piccola griglia su un pezzo di prova, controllando margine di avvicinamento e staffe.
- [ ] Verificare che arresto e ripresa si comportino come previsto sul firmware reale.
- [ ] Confrontare alcune misure ripetute per valutare la ripetibilità del metodo di contatto.
- [ ] Importare la mappa e verificare il percorso compensato prima della lavorazione.
- [ ] Eseguire una lavorazione di prova soltanto su decisione dell'operatore e registrare il risultato.
- [ ] Aggiornare `docs/collaudo.md` distinguendo chiaramente verifica software e verifica fisica.

**Uscita:** compatibilità verificata con la CNC dell'utente e limiti operativi documentati.

## 17. Traguardi e definizione di completamento

| Traguardo | Fasi | Dimostrazione richiesta |
|---|---|---|
| A — Fattibilità verificata | 0–3 | Area della lavorazione verificata, mappa importata e simulatore collegato |
| B — Plugin visibile | 4–6 | Modulo installabile, area e griglia configurabili |
| C — Acquisizione manuale | 7–9 | Movimenti sequenziati, quote confermate e sessione recuperabile |
| D — Compensazione verificata | 10–11 | Esportazione importata e correzione numerica controllata |
| E — Consegna per prove | 12 | Pacchetto pulito, guida e rapporto di simulazione |
| F — Uso sulla macchina | 13 | Prova fisica documentata |

Non dichiarare completato il prodotto se l'area finale non copre i tratti da compensare, se i punti iniziali a zero vengono esportati come misure, se un comando può avanzare su uno stato macchina vecchio o se la mappa non è stata aperta realmente in AutoLeveler.

Funzioni da valutare dopo questi traguardi: apertura automatica del `.xyz` in AutoLeveler, trasferimento diretto senza file, griglie non rettangolari, acquisizione con sonda e supporto a più versioni UGS. Non sono prerequisiti per la prima consegna.

## 18. Riferimenti tecnici

- [Specifica funzionale locale](specifica-funzionale.md)
- [Sorgenti e istruzioni di build UGS](https://github.com/winder/Universal-G-Code-Sender)
- [Guida ai plugin UGS](https://winder.github.io/ugs_website/dev/plugin/) — contiene parti storiche: confrontare sempre con i POM della versione scelta.
- [Importazione delle mappe](https://github.com/winder/Universal-G-Code-Sender/blob/master/ugs-platform/ugs-platform-surfacescanner/src/main/java/com/willwinder/ugs/platform/surfacescanner/actions/OpenScannedSurfaceAction.java)
- [Generazione e gestione della griglia](https://github.com/winder/Universal-G-Code-Sender/blob/master/ugs-platform/ugs-platform-surfacescanner/src/main/java/com/willwinder/ugs/platform/surfacescanner/SurfaceScanner.java)
- [grblHAL Simulator](https://github.com/grblHAL/Simulator)

I link a `master` possono cambiare. In fase 1 e 2 sostituire nei documenti di compatibilità i riferimenti generici con link al commit realmente usato per lo sviluppo.
