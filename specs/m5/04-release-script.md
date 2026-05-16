# M5.4 — Script de Release

**Marco:** M5 — GraalVM Native Image  
**Dependencias:** M5.3  
**Entrega:** `make release` gera .zip pronto pra distribuir

## Implementacao

### scripts/release.sh

```bash
#!/bin/bash
set -euo pipefail

VERSION=${1:-0.1.0}
ARCH="linux-x64"
RELEASE_DIR="release/jauri-${VERSION}-${ARCH}"

echo "[Jauri] Building release ${VERSION} for ${ARCH}..."

# 1. Build native image
bash scripts/build-native.sh

# 2. Prepare release directory
rm -rf "$RELEASE_DIR" "release/jauri-${VERSION}-${ARCH}.zip"
mkdir -p "$RELEASE_DIR"

# 3. Copy artifacts
cp jauri "$RELEASE_DIR/"
cp README.md "$RELEASE_DIR/"
cp INSTALL.md "$RELEASE_DIR/"

# 4. Create .zip
cd release
zip -r "jauri-${VERSION}-${ARCH}.zip" "jauri-${VERSION}-${ARCH}"
cd ..

# 5. Cleanup temp
rm -rf "$RELEASE_DIR"

echo "[Jauri] OK Release: release/jauri-${VERSION}-${ARCH}.zip"
ls -lh "release/jauri-${VERSION}-${ARCH}.zip"
```

### Makefile (atualizado)

```makefile
.PHONY: build run test clean release

build:
	mvn compile

run:
	mvn exec:java

test:
	mvn test

clean:
	mvn clean
	rm -rf target/ release/

release:
	bash scripts/release.sh

package:
	mvn package -DskipTests
```

## Criterio de Aceite

`make release` -- `release/jauri-0.1.0-linux-x64.zip` contem binario + README + INSTALL.md. Tamanho < 10MB comprimido.
