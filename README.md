# Minha Rotina

App Android de checklist diário. Tarefas por dia da semana, lembrete por tarefa, nota do dia, histórico e frases de incentivo. Tudo fica salvo no celular, sem internet e sem conta.

## Como gerar o APK

**Opção A: GitHub (sem instalar nada no PC)**
1. Crie um repositório privado no GitHub e envie esta pasta inteira.
2. A aba **Actions** roda o workflow "Gerar APK" sozinha a cada envio.
3. Ao terminar, abra a execução e baixe o artefato **MinhaRotina-apk** (é um .zip com o `app-debug.apk` dentro).

**Opção B: Android Studio**
1. File › Open › selecione esta pasta e espere o Gradle sincronizar.
2. Build › Build App Bundle(s) / APK(s) › Build APK(s).
3. O arquivo sai em `app/build/outputs/apk/debug/app-debug.apk`.

## Como instalar no celular

Pelas regras de verificação de desenvolvedor do Android, em vigor no Brasil, um app fora da loja precisa ser instalado por ADB (ou pelo fluxo avançado de instalação do próprio Android).

Via ADB:
1. No celular: Configurações › Sobre o telefone › toque 7x em "Número da versão". Depois, em Opções do desenvolvedor, ative a **Depuração USB**.
2. Ligue no PC por USB e aceite a autorização no celular.
3. `adb install app-debug.apk`

Para atualizar depois, use `adb install -r app-debug.apk`. Os dados dela são mantidos.

## Primeira abertura

O app pede permissão de notificação. Na tela **Hoje** aparece um cartão "Configure os lembretes" enquanto faltar alguma liberação:
- **Alarmes no horário exato** (Android 12)
- **Economia de bateria**: importante em Samsung e Xiaomi, senão o sistema segura os alarmes.

Em Xiaomi/Redmi/POCO, vale também ativar o **Início automático** do app nas configurações de segurança.

## Como funciona

- **Rotina**: cada tarefa tem os dias em que aparece (qualquer combinação) e um horário de lembrete opcional.
- **Só hoje**: tarefa avulsa que existe apenas no dia em que foi criada.
- À meia-noite o dia vira. A lista nova começa toda desmarcada, e o dia anterior vai para o histórico. Dias em que o app não foi aberto entram como não feitos.
- **Nota**: toque numa tarefa para escrever o que aconteceu naquele dia. Ela aparece no histórico.
- O lembrete toca uma vez no horário e só se a tarefa ainda não foi ticada. A notificação tem o botão "Marcar como feita".
- Editar ou excluir uma rotina vale a partir de hoje. O histórico não muda.

## Estrutura

- `data/`: modelos, repositório e armazenamento em JSON (`rotina.json` nos arquivos internos do app)
- `alarm/`: agendamento dos alarmes, notificações e reagendamento após reiniciar o celular
- `quotes/`: banco de frases e lógica da frase do dia
- `ui/`: telas em Jetpack Compose
