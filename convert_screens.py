#!/usr/bin/env python3
import os
import re

SCREENS_DIR = "app/src/main/java/com/toxicplants/database/ui/screens"

REPLACEMENTS = [
    # Reemplazo de containerColor con colores semánticos de MaterialTheme
    (r"containerColor\s*=\s*Color\.White", "containerColor = MaterialTheme.colorScheme.surface"),
    (r"containerColor\s*=\s*Color\(0xFFF5F5F5\)", "containerColor = MaterialTheme.colorScheme.surfaceVariant"),
    (r"containerColor\s*=\s*Color\(0xFFFFF3E0\)", "containerColor = MaterialTheme.colorScheme.tertiaryContainer"),
    (r"containerColor\s*=\s*Color\(0xFFFFF8E1\)", "containerColor = MaterialTheme.colorScheme.tertiaryContainer"),
    (r"containerColor\s*=\s*Color\(0xFFFFEBEE\)", "containerColor = MaterialTheme.colorScheme.errorContainer"),
    (r"containerColor\s*=\s*Color\(0xFFFBE9E7\)", "containerColor = MaterialTheme.colorScheme.errorContainer"),
    (r"containerColor\s*=\s*Color\(0xFFE8F5E9\)", "containerColor = MaterialTheme.colorScheme.secondaryContainer"),
    
    # Reemplazo de texto y tintes a variables semánticas
    (r"color\s*=\s*Color\.Black", "color = MaterialTheme.colorScheme.onSurface"),
    (r"color\s*=\s*Color\.DarkGray", "color = MaterialTheme.colorScheme.onSurfaceVariant"),
    (r"color\s*=\s*Color\(0xFF1B5E20\)", "color = MaterialTheme.colorScheme.onSecondaryContainer"),
    (r"tint\s*=\s*Color\.Black", "tint = MaterialTheme.colorScheme.onSurface"),
    (r"tint\s*=\s*Color\.DarkGray", "tint = MaterialTheme.colorScheme.onSurfaceVariant"),
]

def process_file(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    original = content
    
    # Aplicar todos los reemplazos
    for pattern, replacement in REPLACEMENTS:
        content = re.sub(pattern, replacement, content)
    
    # Si hubo cambios, asegurarnos de que MaterialTheme esté importado
    if content != original:
        if "MaterialTheme" in content and "import androidx.compose.material3.MaterialTheme" not in content:
            content = content.replace(
                "import androidx.compose.material3.*",
                "import androidx.compose.material3.*\nimport androidx.compose.material3.MaterialTheme"
            )
            if "import androidx.compose.material3.MaterialTheme" not in content:
                content = re.sub(
                    r"(import androidx\.compose\.material3\.[a-zA-Z0-9_]+)",
                    r"import androidx.compose.material3.MaterialTheme\n\1",
                    content,
                    count=1
                )
        
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"✅ Modificado: {os.path.basename(filepath)}")

def main():
    if not os.path.exists(SCREENS_DIR):
        print(f"No existe el directorio: {SCREENS_DIR}")
        return
    
    for filename in os.listdir(SCREENS_DIR):
        if filename.endswith(".kt"):
            process_file(os.path.join(SCREENS_DIR, filename))

if __name__ == "__main__":
    main()
