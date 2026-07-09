# Seguridad

## Claves API

La app permite configurar claves para Pl@ntNet, Groq y Gemini mediante `local.properties`
o variables de entorno. Durante la compilación se inyectan como campos de `BuildConfig`.

Esto es práctico para desarrollo y uso personal, pero **no es seguro para una distribución
pública**: cualquier clave incluida en un APK puede extraerse mediante decompilación o
inspección del binario.

Recomendaciones:

1. **No publiques builds con claves privadas de Groq/Gemini embebidas.**
2. Usa un **backend/proxy propio** para llamadas a LLMs y guarda allí las claves sensibles.
3. Limita las claves externas por dominio, cuota, proyecto, IP o uso cuando el proveedor lo permita.
4. Rota inmediatamente cualquier clave que haya sido expuesta en un repositorio o APK público.
5. Mantén `local.properties` fuera de Git. Este repo ya lo ignora en `.gitignore`.

## Tráfico HTTP

El `AndroidManifest.xml` tiene `android:usesCleartextTraffic="true"` para permitir recursos
HTTP. Si la app se distribuye públicamente, considera sustituirlo por una configuración de
seguridad de red (`networkSecurityConfig`) limitada solo a dominios concretos que realmente
necesiten HTTP.

## Reporte de problemas

Si detectas una clave expuesta, fuga de datos o problema de seguridad, evita publicarlo con
credenciales reales. Revoca primero la clave afectada y abre un issue con la información
mínima necesaria para reproducir el problema.
