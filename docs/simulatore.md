# Prova con grblHAL Simulator su Linux

Questa prova usa il backend seriale reale di UGS, ma nessuna CNC fisica. È stata eseguita con UGS 2.1.26 e Java 17. I processi e i file rimangono in una directory temporanea separata.

## Preparazione

Installare `cmake`, un compilatore C, `socat` e Java 17. Da una directory temporanea:

```bash
git clone --depth 1 --recurse-submodules --shallow-submodules https://github.com/grblHAL/Simulator.git Simulator
cmake -S Simulator -B Simulator/build
cmake --build Simulator/build -j 4
```

Avviare in un terminale, sostituendo `/tmp/ugs-z-test` con una directory scrivibile:

```bash
mkdir -p /tmp/ugs-z-test
socat -d -d PTY,raw,echo=0,link=/tmp/ugs-z-test/ttyUGS,mode=666 \
  "EXEC:'./Simulator/build/grblHAL_sim -n -e /tmp/ugs-z-test/eeprom.dat -s /tmp/ugs-z-test/steps.out -b /tmp/ugs-z-test/blocks.out',pty,raw,echo=0"
```

Dal repository del plugin, in un secondo terminale:

```bash
./scripts/build.sh
UGS_SIM_PORT=/tmp/ugs-z-test/ttyUGS JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
  ./upstream/ugs/mvnw -Dtest=SimulatorIntegrationTest test -ntp
```

Il test verifica che UGS si colleghi alla porta, che il plugin completi le fasi Z alta → XY → Z di avvicinamento, che un micro movimento Z sia riportato dal controller e che la quota confermata sia quella letta da UGS. Senza `UGS_SIM_PORT`, il test viene saltato; gli altri test vengono comunque eseguiti.

Per una verifica manuale dell'interfaccia, avviare UGS Platform con una directory utente di prova, collegarsi alla stessa seriale come firmware **GRBL**, aprire **Finestra → Plugin → Mappa Z manuale** e usare un piccolo G-code di prova. Non collegare contemporaneamente il test automatico e l'applicazione UGS alla stessa porta seriale.
