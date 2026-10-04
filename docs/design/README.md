# Riorganizzazione del pannello

Proposta generata con lo strumento integrato imagegen in modalità `ui-mockup`.
Il concept è in `layout-concept.png`; il pannello reale usa i componenti Swing di UGS.

## Prompt usato

Desktop Java Swing CNC plugin in Italian, landscape, native light gray Linux
styling. Compact top cards: Area di lavoro (X/Y min/max, Dal G-code), Griglia
(righe/colonne, Genera griglia), Quote (avvicinamento, trasferimento, discesa).
Large 3×3 point grid on the left, Regolazione Z on the right with current work Z,
three up and three down buttons labelled with distances, editable base step and
multipliers. Start, stop and record controls near the jog buttons. Session and
export actions at the bottom. Align cards to the left and use the full width.

## Realizzazione

- Tre riquadri in alto sulle finestre ampie, due o uno sulle finestre più strette.
- Etichette e campi allineati con GridBagLayout.
- Griglia estesa nello spazio centrale e controlli Z raccolti in un unico riquadro.
- Sotto 760 px i controlli Z passano sotto la griglia.
- Salvataggi e stato in fondo; scorrimento per pannelli piccoli.
- Nessuna modifica alla sequenza di movimento.

## Verifica

Compilazione NBM riuscita. Rendering del componente Swing reale a 1200×760 e
520×1400, senza controller connesso: `layout-implemented.png` e `layout-narrow.png`.
Verificate etichette, disposizione su una/tre colonne e leggibilità dei sei pulsanti.
Il prompt completo dello strumento integrato è salvato in `prompt.txt`.
