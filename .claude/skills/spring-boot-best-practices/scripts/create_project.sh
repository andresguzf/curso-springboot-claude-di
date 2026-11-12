#!/usr/bin/env bash
# Genera un proyecto Spring Boot con Spring Initializr (https://start.spring.io)
# directamente en el directorio del workspace, sin sobrescribir archivos existentes.
#
# Uso:
#   bash create_project.sh [directorio-workspace] [--with-jpa] [--with-thymeleaf]
#
# Por defecto NO incluye persistencia: --with-jpa agrega data-jpa + postgresql
# solo cuando el usuario pide JPA/Hibernate/base de datos PostgreSQL.
#
# - artifactId = nombre del directorio del workspace
# - groupId    = com.andres.course.claude.springboot
# - package    = com.andres.course.claude.springboot.{artifactId-saneado}.app
# - Spring Boot: última versión GA (sin SNAPSHOT/RC/M) >= 4.1.2
set -euo pipefail

MIN_BOOT="4.1.2"
JAVA_VERSION="25"
GROUP_ID="com.andres.course.claude.springboot"
INITIALIZR="https://start.spring.io"
DEPENDENCIES="web,validation,springdoc-openapi,devtools,actuator"

WORKSPACE="."
WITH_THYMELEAF="false"
WITH_JPA="false"
for arg in "$@"; do
  case "$arg" in
    --with-thymeleaf) WITH_THYMELEAF="true" ;;
    --with-jpa) WITH_JPA="true" ;;
    *) WORKSPACE="$arg" ;;
  esac
done

WORKSPACE="$(cd "$WORKSPACE" && pwd)"
ARTIFACT_ID="$(basename "$WORKSPACE")"

if [[ "$WITH_JPA" == "true" ]]; then
  DEPENDENCIES="$DEPENDENCIES,data-jpa,postgresql"
fi
if [[ "$WITH_THYMELEAF" == "true" ]]; then
  DEPENDENCIES="$DEPENDENCIES,thymeleaf"
fi

if [[ -f "$WORKSPACE/pom.xml" ]]; then
  echo "ERROR: ya existe $WORKSPACE/pom.xml; el proyecto ya fue generado." >&2
  exit 1
fi

# Segmento de package válido en Java: minúsculas, sin dígitos ni separadores al
# inicio y solo caracteres alfanuméricos. Ej: "2-spring-di" -> "springdi".
PKG_SEGMENT="$(echo "$ARTIFACT_ID" | tr '[:upper:]' '[:lower:]' | sed -E 's/^[^a-z]+//; s/[^a-z0-9]//g')"
if [[ -z "$PKG_SEGMENT" ]]; then
  echo "ERROR: no se pudo derivar un package válido desde '$ARTIFACT_ID'." >&2
  exit 1
fi
PACKAGE_NAME="$GROUP_ID.$PKG_SEGMENT.app"

# Resolver la última versión GA de Spring Boot publicada en Initializr.
METADATA="$(curl -sf -H 'Accept: application/vnd.initializr.v2.2+json' "$INITIALIZR")"
BOOT_VERSION="$(python3 -I -c '
import json, sys, re
min_v = tuple(int(x) for x in sys.argv[1].split("."))
data = json.loads(sys.stdin.read())
ga = []
for v in data["bootVersion"]["values"]:
    vid = v["id"]
    if re.fullmatch(r"\d+\.\d+\.\d+", vid):
        ga.append(tuple(int(x) for x in vid.split(".")))
ga = [v for v in ga if v >= min_v]
if not ga:
    sys.exit("No hay version GA >= " + sys.argv[1])
print(".".join(str(x) for x in max(ga)))
' "$MIN_BOOT" <<< "$METADATA")"

echo "Spring Boot : $BOOT_VERSION"
echo "Java        : $JAVA_VERSION"
echo "groupId     : $GROUP_ID"
echo "artifactId  : $ARTIFACT_ID"
echo "package     : $PACKAGE_NAME"
echo "dependencias: $DEPENDENCIES"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

curl -sf "$INITIALIZR/starter.zip" \
  -d type=maven-project \
  -d language=java \
  -d bootVersion="$BOOT_VERSION" \
  -d groupId="$GROUP_ID" \
  -d artifactId="$ARTIFACT_ID" \
  -d name="$ARTIFACT_ID" \
  -d packageName="$PACKAGE_NAME" \
  -d packaging=jar \
  -d javaVersion="$JAVA_VERSION" \
  -d configurationFileFormat=properties \
  -d dependencies="$DEPENDENCIES" \
  -o "$TMP_DIR/project.zip"

# -n: nunca sobrescribe archivos existentes (CLAUDE.md, .claude/, etc.)
unzip -qn "$TMP_DIR/project.zip" -d "$WORKSPACE"
chmod +x "$WORKSPACE/mvnw"

for f in mvnw mvnw.cmd .mvn/wrapper/maven-wrapper.properties pom.xml src/main/resources/application.properties; do
  [[ -e "$WORKSPACE/$f" ]] || { echo "ERROR: falta $f en el proyecto generado." >&2; exit 1; }
done

echo "Proyecto generado en $WORKSPACE"
echo "PACKAGE_DIR=src/main/java/$(echo "$PACKAGE_NAME" | tr '.' '/')"
