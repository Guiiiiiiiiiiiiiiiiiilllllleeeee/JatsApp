# JATSAPP - DOCUMENTACIÓN DEL PROYECTO

## Aplicación de Chat Multigrupal con Persistencia

**Autores:** [Tu nombre]  
**Fecha:** Febrero 2026  
**Versión:** 1.0

---

# ÍNDICE

1. Introducción
2. Arquitectura del Sistema
3. Modelo de Datos
4. Diagrama de Clases
5. Manual de Usuario
6. Instrucciones de Ejecución

---

# 1. INTRODUCCIÓN

JatsApp es una aplicación de mensajería instantánea desarrollada en Java que permite la comunicación en tiempo real entre usuarios, tanto de forma individual como en grupo. El sistema implementa una arquitectura cliente-servidor utilizando sockets TCP para la comunicación en red.

## Características Principales

- **Registro y autenticación** con verificación 2FA por email
- **Chat individual** en tiempo real con historial persistente
- **Chat grupal** con soporte para hasta 10 participantes
- **Gestión de contactos** con estados de conexión (conectado/desconectado)
- **Envío de archivos** (hasta 10MB)
- **Cifrado de mensajes** con AES
- **Confirmaciones de lectura** (entregado/leído)
- **Búsqueda de mensajes** en todos los chats
- **Interfaz gráfica moderna** con tema oscuro

## Tecnologías Utilizadas

| Componente | Tecnología |
|------------|------------|
| Lenguaje | Java 17 |
| Comunicación | Sockets TCP |
| Base de Datos | MySQL 8.0 |
| Interfaz Gráfica | Swing + FlatLaf |
| Gestión de Proyecto | Maven |
| Logging | SLF4J + Logback |
| Email | JavaMail API |

---

# 2. ARQUITECTURA DEL SISTEMA

## 2.1 Visión General

El sistema sigue una **arquitectura Cliente-Servidor** con los siguientes componentes:

```
┌─────────────────────────────────────────────────────────────────┐
│                        ARQUITECTURA JATSAPP                      │
├──────��──────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐   │
│   │   Cliente    │     │   Cliente    │     │   Cliente    │   │
│   │   Windows    │     │   Ubuntu     │     │   Windows    │   │
│   └──────┬───────┘     └──────┬───────┘     └──────┬───────┘   │
│          │                    │                    │            │
│          │         TCP/IP Sockets (Puerto 5555)    │            │
│          │                    │                    │            │
│          └────────────────────┼────────────────────┘            │
│                               │                                  │
│                    ┌──────────▼──────────┐                      │
│                    │      SERVIDOR       │                      │
│                    │    (ServerCore)     │                      │
│                    │                     │                      │
│                    │  ┌───────────────┐  │                      │
│                    │  │ClientHandler 1│  │                      │
│                    │  │ClientHandler 2│  │  ← Un hilo por       │
│                    │  │ClientHandler N│  │    cliente           │
│                    │  └───────────────┘  │                      │
│                    └──────────┬──────────┘                      │
│                               │                                  │
│              ┌────────────────┼────────────────┐                │
│              │                │                │                │
│     ┌────────▼────┐  ┌────────▼────┐  ┌───────▼─────┐          │
│     │   UserDAO   │  │ MessageDAO  │  │  GroupDAO   │          │
│     └──────┬──────┘  └──────┬──────┘  └──────┬──────┘          │
│            │                │                │                  │
│            └────────────────┼────────────────┘                  │
│                             │                                    │
│                    ┌────────▼────────┐                          │
│                    │     MySQL       │                          │
│                    │   jatsapp_db    │                          │
│                    └─────────────────┘                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## 2.2 Componentes del Servidor

### ServerCore
- Clase principal que gestiona el servidor
- Escucha conexiones entrantes en el puerto 5555
- Mantiene un mapa concurrente de clientes conectados (`ConcurrentHashMap`)
- Implementa sistema de heartbeat para detectar desconexiones
- Gestiona el envío de mensajes privados y grupales

### ClientHandler
- Un hilo independiente por cada cliente conectado
- Procesa todos los tipos de mensajes (login, chat, grupos, etc.)
- Implementa el protocolo de comunicación mediante `ObjectInputStream/ObjectOutputStream`

### Capa DAO (Data Access Object)
- **UserDAO**: Gestión de usuarios, contactos, autenticación
- **MessageDAO**: Persistencia y recuperación de mensajes
- **GroupDAO**: Gestión de grupos y miembros

### Servicios
- **SecurityService**: Hash de contraseñas (SHA-256), generación de códigos 2FA
- **EmailService**: Envío de correos para verificación
- **FileService**: Almacenamiento y recuperación de archivos

## 2.3 Componentes del Cliente

### ClientSocket (Singleton)
- Gestiona la conexión con el servidor
- Envía y recibe mensajes de forma asíncrona
- Distribuye los mensajes recibidos a los frames correspondientes

### Vistas (Swing)
- **LoginFrame**: Pantalla de inicio de sesión
- **RegisterFrame**: Registro de nuevos usuarios
- **ChatFrame**: Ventana principal de chat
- **ContactsFrame**: Gestión de contactos
- **GroupsFrame**: Gestión de grupos

## 2.4 Flujo de Comunicación

```
CLIENTE                          SERVIDOR                         BASE DE DATOS
   │                                │                                   │
   │─── LOGIN (user, pass) ────────>│                                   │
   │                                │──── Verificar credenciales ──────>│
   │                                │<─── Usuario válido ───────────────│
   │<── LOGIN_OK ───────────────────│                                   │
   │                                │                                   │
   │─── TEXT_MESSAGE ──────────────>│                                   │
   │                                │──── Guardar mensaje ─────────────>│
   │                                │──── Enviar a destinatario         │
   │<── MESSAGE_DELIVERED ──────────│                                   │
   │                                │                                   │
   │─── GET_HISTORY ───────────────>│                                   │
   │                                │──── Consultar historial ─────────>│
   │<── HISTORY_RESPONSE ───────────│<─── Mensajes ─────────────────────│
   │                                │                                   │
```

---

# 3. MODELO DE DATOS

## 3.1 Diagrama Entidad-Relación

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      MODELO ENTIDAD-RELACIÓN                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────┐         ┌─────────────┐         ┌─────────────┐        │
│  │  USUARIOS   │         │  CONTACTOS  │         │   GRUPOS    │        │
│  ├─────────────┤         ├─────────────┤         ├─────────────┤        │
│  │ PK id_usuario│◄───────┤FK id_propiet│         │ PK id_grupo │        │
│  │ nombre_usuario│        │FK id_contact│────────►│ nombre_grupo│        │
│  │ email        │◄───────┤ fecha_agregado│       │FK id_admin  │────┐   │
│  │ password_hash│         └─────────────┘         │ fecha_creac │    │   │
│  │ codigo_2fa   │                                 └──────┬──────┘    │   │
│  │ actividad    │                                        │           │   │
│  │ fecha_registro│                                       │           │   │
│  │ email_verif  │◄───────────────────────────────────────┘           │   │
│  └──────┬───────┘                                                    │   │
│         │                                                            │   │
│         │         ┌──────────────────┐                               │   │
│         │         │ MIEMBROS_GRUPO   │                               │   │
│         │         ├──────────────────┤                               │   │
│         └────────►│FK id_usuario     │◄──────────────────────────────┘   │
│                   │FK id_grupo       │                                   │
│                   │ es_admin         │                                   │
│                   │ fecha_union      │                                   │
│                   └──────────────────┘                                   │
│                                                                          │
│         ┌─────────────────────────────┐                                  │
│         │         MENSAJES            │                                  │
│         ├─────────────────────────────┤                                  │
│         │ PK id_mensaje               │                                  │
│         │ FK id_emisor ───────────────┼──► USUARIOS                      │
│         │ id_destinatario             │──► USUARIOS o GRUPOS             │
│         │ tipo_destinatario (USUARIO/GRUPO)│                             │
│         │ tipo_contenido (TEXTO/ARCHIVO)│                                │
│         │ contenido                   │                                  │
│         │ nombre_fichero              │                                  │
│         │ datos_fichero (BLOB)        │                                  │
│         │ fecha_envio                 │                                  │
│         │ entregado                   │                                  │
│         │ leido                       │                                  │
│         └─────────────────────────────┘                                  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 3.2 Script de Creación de Base de Datos

```sql
CREATE DATABASE IF NOT EXISTS jatsapp_db;
USE jatsapp_db;

-- TABLA DE USUARIOS
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario INTEGER PRIMARY KEY AUTO_INCREMENT,
    nombre_usuario VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(64) NOT NULL,
    codigo_2fa VARCHAR(6) DEFAULT NULL,
    fecha_expiracion_codigo BIGINT DEFAULT NULL,
    email_verificado BOOLEAN DEFAULT FALSE,
    actividad ENUM('activo', 'desconectado') DEFAULT 'desconectado',
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso DATETIME
);

-- TABLA DE GRUPOS
CREATE TABLE IF NOT EXISTS grupos (
    id_grupo INTEGER PRIMARY KEY AUTO_INCREMENT,
    nombre_grupo VARCHAR(100) NOT NULL,
    id_admin INTEGER NOT NULL,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_admin) REFERENCES usuarios(id_usuario)
);

-- TABLA DE MIEMBROS DE GRUPO
CREATE TABLE IF NOT EXISTS miembros_grupo (
    id_grupo INTEGER NOT NULL,
    id_usuario INTEGER NOT NULL,
    es_admin BOOLEAN DEFAULT FALSE,
    fecha_union DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_grupo, id_usuario),
    FOREIGN KEY (id_grupo) REFERENCES grupos(id_grupo),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- TABLA DE CONTACTOS
CREATE TABLE IF NOT EXISTS contactos (
    id_propietario INTEGER NOT NULL,
    id_contacto INTEGER NOT NULL,
    fecha_agregado DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_propietario, id_contacto),
    FOREIGN KEY (id_propietario) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_contacto) REFERENCES usuarios(id_usuario)
);

-- TABLA DE MENSAJES
CREATE TABLE IF NOT EXISTS mensajes (
    id_mensaje INTEGER PRIMARY KEY AUTO_INCREMENT,
    id_emisor INTEGER NOT NULL,
    id_destinatario INTEGER NOT NULL,
    tipo_destinatario ENUM('USUARIO', 'GRUPO') NOT NULL,
    tipo_contenido ENUM('TEXTO', 'IMAGEN', 'ARCHIVO') NOT NULL,
    contenido TEXT,
    ruta_fichero VARCHAR(255),
    nombre_fichero VARCHAR(255),
    datos_fichero LONGBLOB,
    fecha_envio DATETIME DEFAULT CURRENT_TIMESTAMP,
    entregado BOOLEAN DEFAULT FALSE,
    fecha_entrega DATETIME DEFAULT NULL,
    leido BOOLEAN DEFAULT FALSE,
    fecha_lectura DATETIME DEFAULT NULL,
    FOREIGN KEY (id_emisor) REFERENCES usuarios(id_usuario)
);
```

## 3.3 Descripción de Tablas

### usuarios
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_usuario | INT (PK) | Identificador único |
| nombre_usuario | VARCHAR(50) | Nombre de usuario único |
| email | VARCHAR(100) | Email único para verificación |
| password_hash | VARCHAR(64) | Contraseña cifrada con SHA-256 |
| codigo_2fa | VARCHAR(6) | Código temporal de verificación |
| email_verificado | BOOLEAN | Estado de verificación del email |
| actividad | ENUM | Estado: 'activo' o 'desconectado' |

### grupos
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_grupo | INT (PK) | Identificador único del grupo |
| nombre_grupo | VARCHAR(100) | Nombre del grupo |
| id_admin | INT (FK) | ID del administrador/creador |
| fecha_creacion | DATETIME | Fecha de creación |

### miembros_grupo
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_grupo | INT (PK, FK) | ID del grupo |
| id_usuario | INT (PK, FK) | ID del usuario miembro |
| es_admin | BOOLEAN | Si es administrador del grupo |
| fecha_union | DATETIME | Fecha en que se unió |

### contactos
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_propietario | INT (PK, FK) | Usuario que tiene el contacto |
| id_contacto | INT (PK, FK) | Usuario añadido como contacto |
| fecha_agregado | DATETIME | Fecha de agregación |

### mensajes
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_mensaje | INT (PK) | Identificador único |
| id_emisor | INT (FK) | Usuario que envía |
| id_destinatario | INT | Usuario o grupo destinatario |
| tipo_destinatario | ENUM | 'USUARIO' o 'GRUPO' |
| tipo_contenido | ENUM | 'TEXTO', 'IMAGEN', 'ARCHIVO' |
| contenido | TEXT | Texto del mensaje |
| datos_fichero | LONGBLOB | Bytes del archivo adjunto |
| entregado | BOOLEAN | Confirmación de entrega |
| leido | BOOLEAN | Confirmación de lectura |

---

# 4. DIAGRAMA DE CLASES

## 4.1 Paquete Common (Compartido Cliente-Servidor)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PAQUETE: com.jatsapp.common                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                        <<Serializable>>                          │    │
│  │                            Message                               │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ - type: MessageType                                              │    │
│  │ - senderId: int                                                  │    │
│  │ - senderName: String                                             │    │
│  │ - receiverId: int                                                │    │
│  │ - isGroupChat: boolean                                           │    │
│  │ - content: String                                                │    │
│  │ - timestamp: LocalDateTime                                       │    │
│  │ - fileName: String                                               │    │
│  │ - fileData: byte[]                                               │    │
│  │ - contactList: List<User>                                        │    │
│  │ - historyList: List<Message>                                     │    │
│  │ - groupList: List<Group>                                         │    │
│  │ - messageId: int                                                 │    │
│  │ - delivered: boolean                                             │    │
│  │ - read: boolean                                                  │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ + Message()                                                      │    │
│  │ + Message(type: MessageType, content: String)                    │    │
│  │ + getters/setters...                                             │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                        <<Serializable>>                          │    │
│  │                             User                                 │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ - id: int                                                        │    │
│  │ - username: String                                               │    │
│  │ - email: String                                                  │    │
│  │ - password: String                                               │    │
│  │ - activityStatus: String                                         │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ + User()                                                         │    │
│  │ + User(id, username, activityStatus)                             │    │
│  │ + User(username, email, password)                                │    │
│  │ + getters/setters...                                             │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                        <<Serializable>>                          │    │
│  │                            Group                                 │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ + MAX_MEMBERS: int = 10                                          │    │
│  │ - id: int                                                        │    │
│  │ - nombre: String                                                 │    │
│  │ - idAdmin: int                                                   │    │
│  │ - miembros: List<User>                                           │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ + addMiembro(user: User): boolean                                │    │
│  │ + removeMiembro(userId: int): boolean                            │    │
│  │ + isAdmin(userId: int): boolean                                  │    │
│  │ + isFull(): boolean                                              │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                      <<Enumeration>>                             │    │
│  │                       MessageType                                │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ LOGIN, LOGIN_OK, LOGIN_FAIL                                      │    │
│  │ REGISTER, REGISTER_OK, REGISTER_FAIL                             │    │
│  │ require_2FA, VERIFY_2FA                                          │    │
│  │ TEXT_MESSAGE, FILE_MESSAGE                                       │    │
│  │ GET_CONTACTS, LIST_CONTACTS                                      │    │
│  │ GET_HISTORY, HISTORY_RESPONSE                                    │    │
│  │ CREATE_GROUP, ADD_GROUP_MEMBER, REMOVE_GROUP_MEMBER              │    │
│  │ MESSAGE_DELIVERED, MESSAGE_READ, STATUS_UPDATE                   │    │
│  │ DISCONNECT, ERROR ...                                            │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 4.2 Paquete Servidor

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PAQUETE: com.jatsapp.server                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                         ServerCore                               │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ - PORT: int = 5555                                               │    │
│  │ - connectedClients: ConcurrentHashMap<Integer, ClientHandler>    │    │
│  │ - serverSocket: ServerSocket                                     │    │
│  │ - heartbeatScheduler: ScheduledExecutorService                   │    │
│  │ - userDAO: UserDAO                                               │    │
│  │ - messageDAO: MessageDAO                                         │    │
│  │ - groupDAO: GroupDAO                                             │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ + startServer(): void                                            │    │
│  │ + stopServer(): void                                             │    │
│  │ + addClient(userId, handler): void                               │    │
│  │ + removeClient(userId): void                                     │    │
│  │ + sendPrivateMessage(msg): void                                  │    │
│  │ + sendGroupMessage(msg): void                                    │    │
│  │ + getClientHandler(userId): ClientHandler                        │    │
│  │ - broadcastStatusUpdate(userId, status): void                    │    │
│  │ - startHeartbeat(): void                                         │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                              │                                           │
│                              │ crea                                      │
│                              ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    <<Runnable>>                                  │    │
│  │                    ClientHandler                                 │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ - socket: Socket                                                 │    │
│  │ - serverCore: ServerCore                                         │    │
│  │ - in: ObjectInputStream                                          │    │
│  │ - out: ObjectOutputStream                                        │    │
│  │ - currentUser: User                                              │    │
│  │ - running: boolean                                               │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ + run(): void                                                    │    │
│  │ + sendMessage(msg): void                                         │    │
│  │ + isAlive(): boolean                                             │    │
│  │ - handleMessage(msg): void                                       │    │
│  │ - processChatMessage(msg, tipo): void                            │    │
│  │ - closeConnection(): void                                        │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────┐  │
│  │      UserDAO        │  │    MessageDAO       │  │    GroupDAO     │  │
│  ├─────────────────────┤  ├─────────────────────┤  ├─────────────────┤  │
│  │+ registerUser()     │  │+ saveMessage()      │  │+ createGroup()  │  │
│  │+ login()            │  │+ getPrivateHistory()│  │+ addMember()    │  │
│  │+ getContacts()      │  │+ getGroupHistory()  │  │+ removeMember() │  │
│  │+ addContact()       │  │+ markAsRead()       │  │+ getGroupsByUser│  │
│  │+ updateStatus()     │  │+ markAsDelivered()  │  │+ isGroupAdmin() │  │
│  │+ set2FACode()       │  │+ searchMessages()   │  │+ promoteToAdmin│  │
│  │+ check2FA()         │  │+ getMessageById()   │  │+ leaveGroup()   │  │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────┘  │
│            │                       │                       │             │
│            └───────────────────────┼───────────────────────┘             │
│                                    │                                     │
│                                    ▼                                     │
│                    ┌───────────────────────────────┐                     │
│                    │    <<Singleton>>              │                     │
│                    │    DatabaseManager            │                     │
│                    ├───────────────────────────────┤                     │
│                    │ - instance: DatabaseManager   │                     │
│                    │ - connection: Connection      │                     │
│                    │ - properties: Properties      │                     │
│                    ├───────────────────────────────┤                     │
│                    │ + getInstance(): DatabaseManager│                   │
│                    │ + getConnection(): Connection │                     │
│                    │ - loadProperties(): void      │                     │
│                    └───────────────────────────────┘                     │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 4.3 Paquete Cliente

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PAQUETE: com.jatsapp.client                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌───────────────────��─────────────────────────────────────────────┐    │
│  │                       <<Singleton>>                              │    │
│  │                       ClientSocket                               │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ - instance: ClientSocket                                         │    │
│  │ - socket: Socket                                                 │    │
│  │ - out: ObjectOutputStream                                        │    │
│  │ - in: ObjectInputStream                                          │    │
│  │ - myUserId: int                                                  │    │
│  │ - myUsername: String                                             │    │
│  │ - loginFrame: LoginFrame                                         │    │
│  │ - chatFrame: ChatFrame                                           │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ + getInstance(): ClientSocket                                    │    │
│  │ + connect(host, port): void                                      │    │
│  │ + disconnect(): void                                             │    │
│  │ + send(msg): void                                                │    │
│  │ - listen(): void                                                 │    │
│  │ - handleMessage(msg): void                                       │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                              │                                           │
│         ┌────────────────────┼────────────────────┐                     │
│         │                    │                    │                     │
│         ▼                    ▼                    ▼                     │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐               │
│  │ LoginFrame  │     │ ChatFrame   │     │GroupsFrame  │               │
│  │ (JFrame)    │     │ (JFrame)    │     │ (JFrame)    │               │
│  ├─────────────┤     ├─────────────┤     ├─────────────┤               │
│  │- txtUser    │     │- listaContactos│  │- groupList  │               │
│  │- txtPass    │     │- areaChat   │     │- memberList │               │
│  │- btnLogin   │     │- txtMensaje │     │- btnCrear   │               │
│  ├─────────────┤     ├─────────────┤     ├─────────────┤               │
│  │+ doLogin()  │     │+ recibirMensaje()│ │+ crearGrupo()│              │
│  │+ onLoginOK()│     │+ enviarMensaje() │ │+ añadirMiembro│             │
│  │+ onLoginFail│     │+ cargarHistorial()││+ eliminarMiembro│           │
│  └─────────────┘     └─────────────┘     └─────────────┘               │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                        StyleUtil                                 │    │
│  ├────��────────────────────────────────────────────────────────────┤    │
│  │ + PRIMARY: Color                                                 │    │
│  │ + BG_DARK: Color                                                 │    │
│  │ + TEXT_PRIMARY: Color                                            │    │
│  │ + applyDarkTheme(): void                                         │    │
│  │ + createStyledButton(text, color): JButton                       │    │
│  │ + createStyledTextField(placeholder): JTextField                 │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                      EncryptionUtil                              │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ - ALGORITHM: String = "AES"                                      │    │
│  │ - SECRET_KEY: String                                             │    │
│  ├─────────────────────────────────────────────────────────────────┤    │
│  │ + encrypt(plainText): String                                     │    │
│  │ + decrypt(encryptedText): String                                 │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

# 5. MANUAL DE USUARIO

## 5.1 Registro de Usuario

1. Ejecutar el cliente JatsApp
2. En la pantalla de inicio, hacer clic en **"¿No tienes cuenta? Crear una"**
3. Completar el formulario:
   - **Nombre de usuario**: Identificador único (sin espacios)
   - **Correo electrónico**: Email válido para verificación
   - **Contraseña**: Mínimo 6 caracteres
   - **Confirmar contraseña**: Repetir la contraseña
4. Hacer clic en **"Crear Cuenta"**
5. Se enviará un código de verificación al email proporcionado
6. Introducir el código de 6 dígitos en el diálogo de verificación
7. Una vez verificado, volver a la pantalla de login

## 5.2 Inicio de Sesión

1. Introducir nombre de usuario
2. Introducir contraseña
3. Hacer clic en **"Iniciar Sesión"**
4. Si las credenciales son correctas, se abrirá la ventana principal

## 5.3 Gestión de Contactos

### Añadir Contacto
1. En la ventana principal, hacer clic en **"Contactos"**
2. Hacer clic en **"+ Añadir"**
3. Introducir el nombre de usuario del contacto
4. Confirmar

### Eliminar Contacto
1. En la ventana de Contactos, seleccionar un contacto
2. Hacer clic en **"Eliminar"**
3. Confirmar la eliminación

## 5.4 Chat Individual

1. En la lista de contactos (panel izquierdo), hacer clic en un contacto
2. El área de chat mostrará el historial de conversación
3. Escribir el mensaje en el campo inferior
4. Presionar **Enter** o hacer clic en el botón de enviar
5. Los indicadores muestran:
   - ✓ Mensaje enviado
   - ✓✓ Mensaje entregado
   - ✓✓ (azul) Mensaje leído

## 5.5 Envío de Archivos

1. Durante un chat, hacer clic en el botón de adjuntar (📎)
2. Seleccionar el archivo (máximo 10MB)
3. El archivo se enviará automáticamente
4. El receptor podrá descargarlo haciendo clic en el enlace

## 5.6 Gestión de Grupos

### Crear Grupo
1. Hacer clic en **"Grupos"**
2. Hacer clic en **"+ Crear Grupo"**
3. Introducir el nombre del grupo
4. Confirmar

### Añadir Miembros
1. Seleccionar el grupo en la lista
2. Hacer clic en **"Añadir"**
3. Introducir el nombre de usuario del nuevo miembro
4. Confirmar (máximo 10 miembros)

### Enviar Mensaje de Grupo
1. Hacer clic en el grupo en la lista de chats
2. Escribir el mensaje
3. Enviar - todos los miembros lo recibirán

### Administración del Grupo
- **Promover a Admin**: Convertir miembro en administrador
- **Degradar de Admin**: Quitar rol de administrador
- **Eliminar Miembro**: Solo administradores pueden hacerlo
- **Abandonar Grupo**: Salir voluntariamente del grupo

## 5.7 Búsqueda

### Buscar Chats
1. Hacer clic en el icono de búsqueda (🔍) en el panel izquierdo
2. Escribir el nombre del contacto o grupo
3. La lista se filtrará en tiempo real

### Buscar Mensajes
1. Dentro de un chat, hacer clic en el icono de búsqueda
2. Escribir el texto a buscar
3. Navegar entre resultados con las flechas

## 5.8 Configuración

- **Cerrar Sesión**: Menú de configuración → Cerrar sesión
- El archivo `config.properties` permite cambiar la IP del servidor

---

# 6. INSTRUCCIONES DE EJECUCIÓN

## 6.1 Requisitos Previos

- **Java JDK 17** o superior instalado
- **MySQL 8.0** o superior instalado y ejecutándose
- **Maven 3.6+** (solo para compilar desde código fuente)

## 6.2 Configuración de la Base de Datos

1. Abrir MySQL Workbench o consola de MySQL
2. Crear la base de datos ejecutando:
```sql
CREATE DATABASE jatsapp_db;
```
3. Ejecutar el script SQL proporcionado en la sección 3.2

## 6.3 Estructura de Archivos Entregados

```
dist/
├── servidor/
│   ├── JatsApp-Server-Console.jar    (Servidor sin interfaz)
│   ├── JatsApp-Server-GUI.jar        (Servidor con interfaz gráfica)
│   └── config.properties             (Configuración del servidor)
├── cliente-windows/
│   ├── JatsApp-Client-Windows.jar    (Cliente para Windows)
│   └── config.properties             (Configuración del cliente)
└── cliente-ubuntu/
    ├── JatsApp-Client-Ubuntu.jar     (Cliente para Linux)
    └── config.properties             (Configuración del cliente)
```

## 6.4 Configuración del Servidor

Editar `servidor/config.properties`:

```properties
# Configuración de Base de Datos
db.url=jdbc:mysql://127.0.0.1:3306/jatsapp_db?useUnicode=true&characterEncoding=UTF-8
db.user=root
db.password=TU_CONTRASEÑA_MYSQL
db.driver=com.mysql.cj.jdbc.Driver

# Configuración de Email (opcional, para 2FA)
mail.host=smtp.gmail.com
mail.port=587
mail.user=tu_email@gmail.com
mail.password=tu_contraseña_app
```

**IMPORTANTE**: Cambiar `db.user` y `db.password` según tu configuración de MySQL.

## 6.5 Configuración del Cliente

Editar `cliente-windows/config.properties` o `cliente-ubuntu/config.properties`:

```properties
# Configuración de conexión al servidor
server.ip=127.0.0.1
server.port=5555
```

**IMPORTANTE**: Cambiar `server.ip` a la IP del ordenador donde se ejecuta el servidor si no es local.

## 6.6 Ejecución del Servidor

### Opción A: Servidor con Interfaz Gráfica (Recomendado)
```bash
cd servidor
java -jar JatsApp-Server-GUI.jar
```

### Opción B: Servidor en Consola
```bash
cd servidor
java -jar JatsApp-Server-Console.jar
```

Comandos disponibles en consola:
- `status` - Ver clientes conectados
- `exit` - Detener servidor
- `help` - Ver ayuda

## 6.7 Ejecución del Cliente

### En Windows:
```bash
cd cliente-windows
java -jar JatsApp-Client-Windows.jar
```

### En Linux/Ubuntu:
```bash
cd cliente-ubuntu
java -jar JatsApp-Client-Ubuntu.jar
```

## 6.8 Orden de Ejecución

1. **Primero**: Iniciar MySQL Server
2. **Segundo**: Iniciar el servidor JatsApp
3. **Tercero**: Iniciar uno o más clientes

## 6.9 Solución de Problemas

### Error: "No se pudo conectar al servidor"
- Verificar que el servidor está ejecutándose
- Verificar que la IP en `config.properties` del cliente es correcta
- Verificar que el puerto 5555 no está bloqueado por el firewall

### Error: "Error conectando a BD"
- Verificar que MySQL está ejecutándose
- Verificar credenciales en `config.properties` del servidor
- Verificar que la base de datos `jatsapp_db` existe

### Error: "Usuario o contraseña incorrectos"
- Verificar que el usuario existe en la base de datos
- Verificar que el email ha sido verificado

### Los mensajes no llegan
- Verificar que ambos usuarios están conectados
- Revisar los logs del servidor en la carpeta `logs/`

---

# ANEXO: Logs del Sistema

El servidor genera archivos de log en la carpeta `logs/`:

- `jatsapp-server.log` - Log general del servidor
- `jatsapp-server-errors.log` - Solo errores
- `jatsapp-activity.log` - Actividad de usuarios (conexiones, mensajes)

Los logs rotan automáticamente cada día y se mantienen 30 días.

---

**FIN DEL DOCUMENTO**
