# App Empresarial ERP / CRM (Android + Kotlin + Jetpack Compose)

Integración de una app móvil con un web service REST/JSON sobre HTTPS:
login con JWT, consulta de clientes (rol CRM), catálogo de productos y registro
de pedidos (rol ERP), con historial local en Room.

**Web service:** DummyJSON (https://dummyjson.com), API pública de pruebas sin datos reales.
Nota: DummyJSON simula las escrituras (`POST /carts/add` responde 201 pero no persiste),
por eso el historial se guarda localmente con Room.

## Flujo
1. `POST /auth/login` → JWT
2. `GET /auth/me` → exige `Authorization: Bearer <token>` (lo agrega un interceptor de OkHttp)
3. `GET /users` → clientes (CRM) · `GET /products` → catálogo (ERP)
4. `POST /carts/add` → registro del pedido (simulado)
5. El pedido confirmado se guarda en Room y se muestra en la pestaña Historial

## Arquitectura
`MainActivity (Compose)` → `VentasViewModel (StateFlow)` → `ErpRepository (Resultado)` → `ErpService (Retrofit)` → `ApiClient (OkHttp + Bearer)`
y `AppDatabase / PedidoDao (Room)` para el historial.

## Ejecutar
Requiere Internet. Usuario: `emilys` · Contraseña: `emilyspass` (credenciales de prueba públicas de DummyJSON).

## Seguridad y ética
- Solo HTTPS; token en memoria, sin hardcodear; encabezado `Authorization` censurado en logs.
- Sin datos personales reales; `allowBackup=false`.
- Mejoras futuras: OAuth 2.0 completo, EncryptedSharedPreferences, WorkManager para sincronización offline.