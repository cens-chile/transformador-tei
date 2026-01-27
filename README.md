<!-- Improved compatibility of back to top link: See: https://github.com/othneildrew/Best-README-Template/pull/73 -->
<a id="readme-top"></a>
<!--
*** Thanks for checking out the Best-README-Template. If you have a suggestion
*** that would make this better, please fork the repo and create a pull request
*** or simply open an issue with the tag "enhancement".
*** Don't forget to give the project a star!
*** Thanks again! Now go create something AMAZING! :D
-->



<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![Unlicense License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]



<!-- PROJECT LOGO -->
<br />
<div align="center">

  <h3 align="center">Componente de Transformación de Mensajes</h3>

  <p align="center">
    Componente que simplifica la construcción de mensajes estandarizados FHIR recibiendo JSON simples no estandarizados para transformarlos en mensajes estandarizados FHIR.
    <br />
    <a href="https://interoperabilidad.minsal.cl/fhir/ig/tei/0.2.1/index.html"><strong>Guía TEI »</strong></a>
    <br />
    <br />
    <a href="https://github.com/cens-chile/transformador-tei">Repositorio</a>
    &middot;
    <a href="https://github.com/cens-chile/transformador-tei/issues/new?labels=bug&template=bug-report---.md">Reportar Bug</a>
    &middot;
    <a href="https://github.com/cens-chile/transformador-tei/issues/new?labels=enhancement&template=feature-request---.md">Solicitar Funcionalidades</a>
  </p>
</div>



<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#acerca-del-proyecto">Acerca del Proyecto</a>
      <ul>
        <li><a href="#construido-con">Desarrollado con</a></li>
      </ul>
    </li>
    <li>
      <a href="#como-empezar">Como Empezar</a>
      <ul>
        <li><a href="#requisitos-del-sistema-operativo">Requisitos del sistema operativo</a></li>
        <li><a href="#hardware-recomendado">Hardware recomendado</a></li>
        <li><a href="#prerrequisitos">Prerequisitos</a></li>
        <li><a href="#instalación">Instalación</a></li>
        <li><a href="#desarrollo">Desarrollo</a></li>
      </ul>
    </li>
    <li>
      <a href="#uso">Uso</a>
      <ul>
        <li><a href="#funcionalidades">Funcionalidades</a></li>
      </ul>
    </li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contribuir">Contribuir</a></li>
    <li><a href="#licencia">Licencia</a></li>
    <li><a href="#contacto">Contacto</a></li>
    <li><a href="#agradecimientos">Agradecimientos</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## Acerca del Proyecto

El sistema de salud en Chile se estructura en niveles (primario, secundario y terciario), 
siendo el nivel primario el con mayor despliegue en el territorio, con atenciones de menor complejidad
y la puerta de entrada a todas las atenciones de salud en la red pública de establecimientos. 
Para optar a una atención de especialidad, las personas deben ser derivadas desde la atención primaria 
a un centro de mayor complejidad, teniendo que esperar para recibir esta atención en el nivel secundario o terciario.

Las personas y tiempos que deben esperar para una atención de salud han sido y son una preocupación para todo el 
sistema sanitario.

Los sistemas que soportan actualmente la información de las personas y tiempos de espera por su estructura y forma
de operar, no permiten conocer la realidad de la situación, trazar al paciente y tampoco permite mantener informado
al paciente. Para mejorar la gestión de la red asistencial y la coordinación entre sus niveles, se requiere implementar
un proceso interoperable de solicitud de nueva consulta de especialidad desde APS a nivel secundario, para patologías
no adscritas a las garantías explícitas de salud (GES).

El componente de Transformación de Mensajes en particular permite ejecutar acelerar la implementación de transacciones 
para el envío de los eventos, por medio de una API que recibe mensajes
en formato de JSON no estandarizado, pero siguiendo una estructura muy simple, los cuales los transforma en formato 
FHIR que cumpla con las indicaciones de la Guia de Implementación para Tiempos de Espera Interoperable, asistiendo 
además con mensajes que ayudan a construir el JSON de entrada permitiendo que los usuarios (desarrolladores 
de software de sistemas de salud) puedan abordar la interoperabilidad de tiempos de espera sin la necesidad de aprender
FHIR ni leer Guías de Implementación.

<p align="right">(<a href="#readme-top">volver al inicio</a>)</p>



### Construido con

* [![Java][Java-logo]][Java-url]
* [![Spring Boot][SpringBoot-logo]][SpringBoot-url]
* [![Maven][Maven-logo]][Maven-url]
* [![HAPI FHIR][HAPI-FHIR-logo]][HAPI-FHIR-url]
* [![Git][Git-scm.com]][Git-url]
* [![Docker][Docker.com]][Docker-url]


<p align="right">(<a href="#readme-top">volver al inicio</a>)</p>



<!-- GETTING STARTED -->
## Como Empezar

Inicialmente se necesita un servidor donde desplegar el componente de transformación con acceso a internet

### Requisitos del sistema operativo

* GNU/Linux 3.10 o superior.

### Hardware recomendado

* 1 GB de RAM
* 1 GB o más de espacio de disco duro.

### Prerrequisitos

* [Instalación de Docker](https://docs.docker.com/desktop/setup/install/linux/)
* [Instalación de GIT](https://git-scm.com/downloads/linux)

Si se quiere editar el proyecto:

* Oracle Java (JDK) instalado: Mínimo JDK17 or newer.
* Apache Maven build tool (newest version)
* IDE a tu elección. 



### Instalación

1. Clonar el repo
    ```sh
    cd $HOME/
    git https://github.com/cens-chile/transformador-tei
   
2. Ingresamos al directorio en donde se descargo el código
    ```sh
    cd $HOME/transformador-tei
    ```  
3. Crear .ENV

```
TEI_FHIR_PACKAGE_TEI=package_tei.tgz
TEI_FHIR_PACKAGE_CORE=package_core.tgz
SERVER_PORT=8080
```

4. Puesta en Marcha

```bash
docker compose up -d
```


<p align="right">(<a href="#readme-top">volver al inicio</a>)</p>

<!-- USAGE EXAMPLES -->
## Uso

==Para el caso de los DateTime, la zona horaria se determina de acuerdo a la zona del servidor que corre este transformador==

Docker compose disponibiliza una API en el servidor.
La API soporta transformaciones para cada evento de tiempos de espera:

- Iniciar
- Referenciar
- Revisar
- Priorizar
- Agendar
- Atender
- Terminar

Se ha diseñado una transformación para cada evento.
Se debe confeccionar un JSON de entrada con los datos requeridos para cada evento.
Puedes encontrar un ejemplo de cada JSON de entrada en

- [JSON de entrada para evento Iniciar](ejemplos/CoreDataSetIniciarToBundle.json)
- [JSON de entrada para evento Referenciar](ejemplos/CoreDataSetReferenciarToBundle.json)
- [JSON de entrada para evento Revisar](ejemplos/CoreDataSetRevisarToBundle.json)
- [JSON de entrada para evento Priorizar](ejemplos/CoreDataSetPriorizarToBundle.json)
- [JSON de entrada para evento Agendar](ejemplos/CoreDataSetAgendarToBundle.json)
- [JSON de entrada para evento Atender](ejemplos/CoreDataSetAtenderToBundle.json)
- [JSON de entrada para evento Terminar](ejemplos/CoreDataSetTerminarToBundle.json)

El json de entrada que confecciones en base a los ejemplos anteriores se entrega por POST 
a la API del servicio de Transformacion de Mensajes, usando las siguientes URL:

- ``` <nombreDelServidor>:8080/fhir/StructureMap/$transform?source=http://interoperabilidad.minsal.gob.cl/tei/IniciarToBundle ```


- ``` <nombreDelServidor>:8080/fhir/StructureMap/$transform?source=http://interoperabilidad.minsal.gob.cl/tei/ReferenciarToBundle```


- ``` <nombreDelServidor>:8080/fhir/StructureMap/$transform?source=http://interoperabilidad.minsal.gob.cl/tei/RevisarToBundle ```


- ``` <nombreDelServidor>:8080/fhir/StructureMap/$transform?source=http://interoperabilidad.minsal.gob.cl/tei/PriorizarToBundle ```


- ``` <nombreDelServidor>:8080/fhir/StructureMap/$transform?source=http://interoperabilidad.minsal.gob.cl/tei/AgendarToBundle ```


- ``` <nombreDelServidor>:8080/fhir/StructureMap/$transform?source=http://interoperabilidad.minsal.gob.cl/tei/AtenderToBundle ```


- ``` <nombreDelServidor>:8080/fhir/StructureMap/$transform?source=http://interoperabilidad.minsal.gob.cl/tei/TerminarToBundle ```

<p align="right">(<a href="#readme-top">volver al inicio</a>)</p>


Ver la sección de [open issues](https://github.com/cens-chile/transformador-tei/issues) para una lista complete de las nuevas funcionalidades (y errores conocidos).

<p align="right">(<a href="#readme-top">volver al inicio</a>)</p>



<!-- Contribuir -->
## Contribuir

Toda contribución que hagas será agradecida

Si tienes alguna sugenrencia para hacer mejor este proyecto, por favor crea tu fork y crea un pull request. También puedes abrir un issue con el tag "mejora"
No olvides dar una estrella al proyecto! Gracias!

1. Crea un fork de este proyecto
2. Crea un branch para tu funcionalidad (`git checkout -b feature/AmazingFeature`)
3. Haz el Commit con tus cambios(`git commit -m 'Add: mi funcionalidad'`)
4. Sube tus cambios al repositorio (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

### Top contributors:

<a href="https://github.com/cens-chile/transformador-tei/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=cens-chile/transformador-tei" />
</a>

<p align="right">(<a href="#readme-top">volver al inicio</a>)</p>



<!-- LICENSE -->
## Licencia

Apache 2.0

Ver el archivo incluido `LICENSE` para detalles.

<p align="right">(<a href="#readme-top">volver al inicio</a>)</p>



<!-- CONTACT -->
## Contacto

Interoperabilidad - [@CENSChile](https://x.com/CENSChile) - interoperabilidad@cens.cl

Link al Proyecto: [https://github.com/cens-chile/transformador-tei](https://github.com/cens-chile/transformador-tei)

<p align="right">(<a href="#readme-top">volver al inicio</a>)</p>



<!-- ACKNOWLEDGMENTS -->
## Agradecimientos

* Equipo CENS

<p align="right">(<a href="#readme-top">volver al inicio</a>)</p>



<!-- MARKDOWN LINKS & IMAGES -->https://github.com/cens-chile/transformador-tei
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/cens-chile/transformador-tei.svg?style=for-the-badge
[contributors-url]: https://github.com/cens-chile/transformador-tei/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/cens-chile/transformador-tei.svg?style=for-the-badge
[forks-url]: https://github.com/cens-chile/transformador-tei/network/members
[stars-shield]: https://img.shields.io/github/stars/cens-chile/transformador-tei.svg?style=for-the-badge
[stars-url]: https://github.com/cens-chile/transformador-tei/stargazers
[issues-shield]: https://img.shields.io/github/issues/cens-chile/transformador-tei.svg?style=for-the-badge
[issues-url]: https://github.com/cens-chile/transformador-tei/issues
[license-shield]: https://img.shields.io/badge/Apache-LICENSE-as?style=for-the-badge&logo=apache
[license-url]: https://github.com/cens-chile/cens-chile/transformador-tei/blob/master/LICENSE.txt
[linkedin-shield]: https://img.shields.io/badge/cens-chile-red?style=for-the-badge&labelColor=blue
[linkedin-url]: https://linkedin.com/in/othneildrew
[Python-url]: https://www.python.org/
[Python.org]: https://img.shields.io/badge/python-3670A0?style=for-the-badge&logo=python&logoColor=ffdd54
[Postgres.org]: https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white
[Postgres-url]: https://www.postgresql.org/
[RabbitMQ.com]: https://img.shields.io/badge/Rabbitmq-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white
[RabbitMQ-url]: https://www.rabbitmq.com/
[DjangoREST-url]: https://www.django-rest-framework.org/
[DjangoREST.org]: https://img.shields.io/badge/DJANGO-REST-ff1709?style=for-the-badge&logo=django&logoColor=white&color=ff1709&labelColor=gray
[Celery.org]: https://img.shields.io/badge/celery-%23a9cc54.svg?style=for-the-badge&logo=celery&logoColor=ddf4a4
[Celery-url]: https://docs.celeryq.dev/en/stable/getting-started/introduction.html
[Git-scm.com]: https://img.shields.io/badge/git-%23F05033.svg?style=for-the-badge&logo=git&logoColor=white
[Git-url]: https://git-scm.com/
[Docker.com]: https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white
[Docker-url]: https://www.docker.com/
[Java-logo]: https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://www.java.com/
[Springboot-logo]: https://img.shields.io/badge/SpringBoot-6DB33F?style=flat-square&logo=Spring&logoColor=white
[Springboot-url]: https://spring.io/projects/spring-boot
[Maven-logo]: https://img.shields.io/badge/-maven-blue
[Maven-url]: https://maven.apache.org/
[HAPI-FHIR-logo]: https://img.shields.io/badge/HAPI-FHIR-orange
[HAPI-FHIR-url]: https://hapifhir.io/
