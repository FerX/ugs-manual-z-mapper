# Stato dello sviluppo

Aggiornamento: 22 settembre 2026.

## Completato e verificato

- Repository autonomo con `pom.xml`, licenza GPL-3.0 e script di build che scarica il tag UGS `v2.1.26` senza includerne i sorgenti nel repository.
- Modulo `.nbm` compilato con Java 17, riconosciuto dal runtime UGS Platform 2.1.26 in un'installazione Linux isolata.
- Verifica grafica in UGS isolato: la voce **Finestra → Plugin → Mappa Z manuale** apre il pannello; la griglia 3 × 3 si genera anche a macchina scollegata. I controlli scorrono verticalmente nella colonna stretta predefinita di UGS.
- Pannello **Mappa Z manuale** con area da G-code o manuale, griglia, selezione dei punti, stato, Z corrente, parametri di movimento, tasti di jog, arresto, sessione ed esportazione.
- Griglia con passo comune X/Y, massimo iniziale 3 × 3, bordi inclusi, zero iniziale distinto dallo stato misurato, ordine a serpentina e controllo di punti troppo vicini per AutoLeveler.
- Sessioni parziali persistenti e copia di recupero automatica dopo ogni quota confermata; esportazione `.xyz` solo a griglia completa.
- Sequenza di movimento controllata per fasi, con attesa di stato aggiornato e posizione raggiunta prima della fase successiva; nessun comando automatico prima di **Inizia misura**.
- Test automatici di griglia, file, area e sequenza di movimento. Test di integrazione con backend UGS reale e grblHAL Simulator su seriale virtuale: sollevamento, trasferimento XY, discesa, jog e quota confermata.
- Test delle classi reali `SurfaceScanner` di AutoLeveler 2.1.26: il file XYZ esportato ricostruisce la griglia attesa e riempie tutti i punti, anche con un ultimo intervallo più corto.

## Limiti e verifiche successive

- La versione precisa installata sul computer della CNC e il suo firmware non sono ancora noti. Il modulo è progettato e verificato contro UGS Platform 2.1.26.
- Il pannello e la generazione della griglia sono stati provati tramite click nell'interfaccia grafica. La sequenza completa tramite click con macchina simulata e il comando grafico **Apri scansione** restano da collaudare. Gli automatismi software sono stati testati tramite il backend, il simulatore e le classi di AutoLeveler.
- AutoLeveler 2.1.26 ha un controllo che può impedire l'importazione di una mappa con Z tutte identiche. Vedere [compatibilita.md](compatibilita.md).
- Il collaudo sulla CNC fisica richiede la macchina disponibile e una verifica iniziale a mandrino fermo e con corse libere.
- La voce **Dal G-code** riconosce linee G1 di lavorazione a Z ≤ 0. Gli archi richiedono limiti X/Y manuali. Percorsi con quota di lavorazione positiva o strategie speciali devono essere controllati e possono richiedere limiti manuali.
- Il plugin non modifica automaticamente le impostazioni di compensazione di AutoLeveler; il file `.xyz` va aperto manualmente.
