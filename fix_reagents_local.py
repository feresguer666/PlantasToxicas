#!/usr/bin/env python3
import os

path = "app/src/main/java/com/toxicplants/database/ui/screens/ChemicalReagentsScreen.kt"

if os.path.exists(path):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Restauramos el color estático del reactivo Keller-Kiliani (rojo)
    # pero mantenemos el onErrorContainer en la tarjeta de seguridad
    content = content.replace(
        'color = MaterialTheme.colorScheme.onErrorContainer\n    )',
        'color = Color(0xFFB71C1C)\n    )'
    )
    content = content.replace(
        'color = MaterialTheme.colorScheme.onErrorContainer\n    ),',
        'color = Color(0xFFB71C1C)\n    ),'
    )
    
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("¡Contraste de reactivos reparado con éxito!")
else:
    print("No se encontró ChemicalReagentsScreen.kt")
