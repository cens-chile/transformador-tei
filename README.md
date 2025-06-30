# Transformador-tei (TEI FHIR Server)

## Prerequisitos

- Oracle Java (JDK) instalado: Mínimo JDK17 or newer.
- Apache Maven build tool (newest version)

## Configuración

- Para configuración de otras opciones editar application.yaml

## DEV

### Puesta en Marcha Local

```bash
mvn spring-boot:run
```

* La url base del servidor FHIR es http://localhost:8080/fhir

## Docker Compose


### Crear .ENV

```
TEI_FHIR_PACKAGE_TEI=package_tei.tgz
TEI_FHIR_PACKAGE_CORE=package_core.tgz
SERVER_PORT=8080
```

### Puesta en Marcha

```bash
docker compose up -d
```