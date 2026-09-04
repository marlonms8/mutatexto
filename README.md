# MutaTexto

Aplicativo Android offline para **criptografar e descriptografar textos**,
com dois modos de operação:

- **Moderno**: AES-256-GCM com PBKDF2-HMAC-SHA256 (310.000 iterações), salt
  e IV aleatórios por mensagem, e autenticação integrada (GCM). Recomendado
  para todo uso novo.
- **Legado**: AES-128-CBC compatível byte a byte com o padrão **Collura
  Decrypter v1.0** 


## Recursos

- Seletor de modo (Moderno / Legado) direto na tela principal.
- **Editor em tela cheia** para o texto de entrada e para o resultado, ideal para mensagens longas.
- Botões de **Copiar** e **Limpar** (zera chave, texto de entrada e resultado).
- Kotlin + Android SDK nativo, sem dependências externas.
- Funciona 100% offline.
- Não solicita permissão de Internet.
- Nenhum texto ou chave é persistido pelo aplicativo.
- Tema escuro em toda a interface, combinando com o novo ícone.
- Mostrar/ocultar chave.
- Ícone próprio do MutaTexto.
- Min SDK 23 / Target SDK 35.

## Modo Moderno — AES-256-GCM

- Cifra: `AES/GCM/NoPadding` (autenticada — detecta chave errada ou texto adulterado)
- Chave: AES-256
- KDF: `PBKDF2WithHmacSHA256`, 310.000 iterações
- Salt: 16 bytes aleatórios, gerados a cada operação
- IV/nonce: 12 bytes aleatórios, gerados a cada operação
- Tag de autenticação: 128 bits
- Formato de saída: Base64( salt || IV || ciphertext+tag )

Duas criptografias do mesmo texto com a mesma chave **nunca** produzem o
mesmo resultado — isso é esperado e correto. Detalhes completos em
[docs/MODERN_CRYPTO.md](docs/MODERN_CRYPTO.md).

## Modo Legado — compatibilidade Collura v1.0

Reproduz o comportamento legado necessário para interoperabilidade com o
formato antigo do SINAPSE:

- Cifra: `AES/CBC/PKCS5Padding`
- Chave: AES-128
- IV: 16 bytes `0x00`
- Senha: UTF-8 → SHA-1
- Digest SHA-1: Base64 Android `NO_PADDING`, preservando o LF final usado pelo comportamento legado
- KDF: `PBKDF2WithHmacSHA1`, salt `"Salt"`, 1 iteração, chave de 128 bits
- Resultado: Base64

### Vetores de compatibilidade validados

| Texto | Chave | Resultado esperado |
|---|---|---|
| `teste` | `123456` | `aMS10Jp7EWXiOT+FcQxGRw==` |
| `teste1` | `123456` | `vgZ2fqx1dsnYp7/+rQdJ5w==` |
| `teste` | `654321` | `PHnyQlWpUWo0T4ktBAQqEg==` |

Esses vetores foram usados como teste de aceitação e continuam válidos para o Modo Legado. Detalhes completos em [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

⚠️ Mensagens dos dois modos **não são intercambiáveis**: o que foi cifrado no Modo Moderno só é descriptografado no Modo Moderno, e o mesmo vale para o Modo Legado.

## Estrutura

```text
MutaTexto/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mutatexto/crypto/
│       │   ├── ModernCrypto.kt
│       │   ├── ColluraCrypto.kt
│       │   └── MainActivity.kt
│       └── res/
├── docs/
│   ├── MODERN_CRYPTO.md
│   └── COMPATIBILITY.md
├── .github/
│   └── ISSUE_TEMPLATE/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── LICENSE
├── THIRD_PARTY_NOTICES.md
├── CHANGELOG.md
├── SECURITY.md
└── README.md
```

## Abrir no Android Studio

1. Clone ou baixe o repositório.
2. Abra a pasta raiz `MutaTexto` no Android Studio.
3. Selecione **JDK 17** para o Gradle.
4. Instale o **Android SDK 35**, se necessário.
5. Aguarde o Gradle Sync.
6. Execute em um aparelho/emulador ou use **Build → Build APK(s)**.

O APK de debug normalmente será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```
## Segurança e escopo

O Modo Legado preserva deliberadamente um esquema criptográfico antigo para
interoperabilidade. Seus parâmetros não representam uma recomendação de
desenho criptográfico moderno e não devem ser usados como referência para
proteção de material de alta sensibilidade — para isso, use o Modo Moderno.

A aplicação não possui permissão `INTERNET` no `AndroidManifest.xml` e
realiza as operações localmente no aparelho.

## Créditos e licença

O MutaTexto é distribuído sob a licença MIT. A compatibilidade do Modo
Legado foi baseada no comportamento do projeto open source
**android-hidden-aes**, de Roberto Xavier Collura, também licenciado sob
MIT. Consulte [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).